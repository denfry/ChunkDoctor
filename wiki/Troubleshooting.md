# Troubleshooting

Start with `/cd status`, the Paper log, and current TPS/MSPT observations.

## ChunkDoctor does not enable

Check:

1. Java is version 21.
2. The server is Paper 1.21.8.
3. Only one ChunkDoctor JAR exists in `plugins/`.
4. The installed file is not the `-plain.jar`.
5. The first ChunkDoctor stack trace in the log, not later follow-on errors.

## No scan results

Check:

- passive monitoring is started;
- the current world passes the whitelist/blacklist;
- TPS is above the resume threshold;
- chunks around players are loaded;
- the analysis queue is not full;
- results have not expired from the in-memory cache.

Run:

```text
/cd status
/cd scan
/cd top
```

## Monitoring remains paused

Compare the server's current TPS with:

```yaml
pause-below-tps: 17.0
resume-above-tps: 18.5
```

The monitor does not resume until TPS exceeds the higher threshold. This is
intentional hysteresis.

## Deep scan times out

Possible causes:

- a tall world section;
- heavy concurrent server load;
- very conservative block/time budgets;
- a scan interrupted by world unload.

Prefer reducing concurrent server activity or scanning a smaller representative
area. Increase only one deep-scan budget at a time and monitor MSPT.

## Export is rejected

`export.directory` accepts a safe relative directory name. Values containing
`..`, `/`, `\`, `:`, or an absolute path are intentionally rejected.

Also verify write access to `plugins/ChunkDoctor/`.

## Score is high but TPS is normal

This is valid. Score estimates concentration of potentially expensive objects;
it does not measure exact tick time. Profile the area with spark during the
relevant workload.

## Score appears incorrect

1. Run a deep scan if safe.
2. Export the result.
3. Record the ChunkDoctor, Paper, and Java versions.
4. Describe what exists in the chunk and why the score seems wrong.
5. Open the
   [incorrect-score template](https://github.com/denfry/ChunkDoctor/issues/new?template=false_score.yml).

Review exports for sensitive coordinates before posting.

## Command denied

Verify the exact permission from [[Commands and Permissions]]. The base
`chunkdoctor.use` permission does not grant every subcommand.

## Getting support

Use the [issue tracker](https://github.com/denfry/ChunkDoctor/issues) for normal
bugs. Follow the
[security policy](https://github.com/denfry/ChunkDoctor/security/policy) for
path escapes, permission bypasses, denial-of-service issues, or other
vulnerabilities.
