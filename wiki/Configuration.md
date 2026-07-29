# Configuration

ChunkDoctor generates `plugins/ChunkDoctor/config.yml` with comments describing
every setting.

## Recommended starting point

```yaml
monitoring:
  interval-ticks: 100
  chunks-per-cycle: 2
  max-milliseconds-per-tick: 2.0
  pause-below-tps: 17.0
  resume-above-tps: 18.5
  maximum-pending-analyses: 64

deep-scan:
  blocks-per-tick: 2048
  maximum-milliseconds-per-tick: 3.0
  maximum-duration-seconds: 30
  maximum-concurrent-scans: 1
```

Start conservatively and observe MSPT with spark before increasing budgets.

## Monitoring

| Setting | Purpose |
|---|---|
| `enabled` | Start passive monitoring when the plugin enables |
| `interval-ticks` | Delay between small monitoring cycles |
| `chunks-per-cycle` | Maximum snapshots accepted per cycle |
| `max-milliseconds-per-tick` | Main-thread collection deadline |
| `rescan-cooldown-seconds` | Minimum delay before revisiting a chunk |
| `result-expiration-minutes` | Lifetime of cached results |
| `pause-below-tps` | Pause threshold |
| `resume-above-tps` | Resume threshold; must exceed pause threshold |
| `worker-threads` | Size of the bounded analysis pool; restart required |
| `maximum-pending-analyses` | Hard upper bound for queued analysis |

The separate pause and resume thresholds provide hysteresis, preventing rapid
start/stop oscillation around one TPS value.

## World filters

`worlds.mode` accepts:

- `blacklist` — analyze every world except the listed names;
- `whitelist` — analyze only listed names.

World names are compared case-insensitively. Invalid modes fall back to a safe
validated value and produce a warning.

## Manual scans

`manual-scan.maximum-radius` and `manual-scan.maximum-chunks` are independent
limits. Raising one does not bypass the other.

Manual quick scans:

- inspect loaded chunks only;
- spread collection across ticks;
- use the same bounded analysis queue as passive monitoring.

## Deep scans

| Setting | Protection |
|---|---|
| `blocks-per-tick` | Maximum block operations attempted in one slice |
| `maximum-milliseconds-per-tick` | Time deadline for one slice |
| `maximum-duration-seconds` | Overall scan timeout |
| `maximum-concurrent-scans` | Global concurrency cap |

Deep scans are manual-only. Do not increase both block and time budgets at once;
change one value and measure the result.

## Risk settings

- `risk.weights.*` controls base contributions.
- `risk.excess-thresholds.*` controls where nonlinear penalties begin.
- level thresholds map the final 0–100 score to LOW, MEDIUM, HIGH, or CRITICAL.

See [[Risk Scoring]] before changing weights.

## Notifications

Notification settings control:

- minimum level;
- minimum score increase;
- per-chunk cooldown.

Players also need `chunkdoctor.notify`. `/cd notify` stores the player's opt-out
in persistent player data.

## Exports

`export.directory` must be one safe relative directory name inside the plugin
data folder. Paths containing separators, drive syntax, or traversal components
are rejected.

See [[Reports and Exports]].

## Applying changes

Run:

```text
/cd reload
```

Most settings apply immediately. Worker-pool sizing requires a restart.
Validation warnings identify values that were rejected or clamped.
