# ChunkDoctor Wiki

ChunkDoctor is a performance-conscious Paper plugin that finds suspicious
loaded chunks, assigns an explainable risk score, and tells administrators what
to inspect.

> A ChunkDoctor score estimates potential load. It is not a measurement of the
> exact TPS cost of a chunk. Use ChunkDoctor to prioritize investigation and
> [spark](https://spark.lucko.me/) to profile execution time.

## Start here

- [[Installation]] — requirements, installation, upgrades, and first-run checks.
- [[Commands and Permissions]] — every command, alias, permission, and console
  restriction.
- [[Configuration]] — safe defaults and the complete configuration surface.
- [[Risk Scoring]] — how score, level, reasons, recommendations, and confidence
  are calculated.
- [[Performance and Safety]] — thread boundaries, budgets, TPS protection, and
  overload behavior.
- [[Reports and Exports]] — JSON export behavior and schema overview.
- [[Metrics and Privacy]] — exactly what bStats collects and how to opt out.
- [[Troubleshooting]] — symptom-first operational guidance.
- [[Building and Releasing]] — source builds and release automation.

## Supported environment

| Component | Requirement |
|---|---|
| Server | Paper 1.21.8 |
| Java | 21 |
| Client mod | Not required |
| Required plugins | None |
| Storage | In-memory results; optional JSON exports |

ChunkDoctor intentionally does not claim Spigot, Folia, Purpur, or cross-version
support until those combinations are tested and listed in a release.

## Core guarantees

- Only already-loaded chunks are analyzed.
- Passive monitoring never scans every block in a chunk.
- Deep scans are manual, tick-sliced, timed, and concurrency-limited.
- Bukkit objects remain on the server thread.
- Worker and export queues are bounded.
- Unsafe export paths and invalid security-sensitive settings fail closed.
- Every administrative capability has a dedicated permission.

## Project links

- [Source code](https://github.com/denfry/ChunkDoctor)
- [Releases](https://github.com/denfry/ChunkDoctor/releases)
- [Issue tracker](https://github.com/denfry/ChunkDoctor/issues)
- [bStats](https://bstats.org/plugin/bukkit/ChunkDoctor/32969)
- [Security policy](https://github.com/denfry/ChunkDoctor/security/policy)
- [MIT License](https://github.com/denfry/ChunkDoctor/blob/main/LICENSE)
