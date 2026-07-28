<p align="center">
  <img src="docs/assets/chunkdoctor-icon.png" alt="ChunkDoctor icon" width="180">
</p>

<h1 align="center">ChunkDoctor</h1>

<p align="center">
  A performance-conscious Paper plugin that finds suspicious loaded chunks,<br>
  assigns an explainable risk score, and tells administrators what to inspect.
</p>

<p align="center">
  <a href="https://github.com/denfry/ChunkDoctor/actions/workflows/build.yml"><img alt="Build" src="https://github.com/denfry/ChunkDoctor/actions/workflows/build.yml/badge.svg"></a>
  <a href="https://github.com/denfry/ChunkDoctor/releases"><img alt="Release" src="https://img.shields.io/github/v/release/denfry/ChunkDoctor?display_name=tag&sort=semver"></a>
  <img alt="Paper 1.21.8" src="https://img.shields.io/badge/Paper-1.21.8-2f3136?logo=paper">
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-e76f00?logo=openjdk&logoColor=white">
  <a href="LICENSE"><img alt="MIT License" src="https://img.shields.io/badge/License-MIT-22c55e.svg"></a>
</p>

<p align="center">
  <a href="#installation">Installation</a> ·
  <a href="#commands">Commands</a> ·
  <a href="#configuration">Configuration</a> ·
  <a href="docs/README_RU.md">Русская документация</a>
</p>

![ChunkDoctor scanning a risky chunk](docs/assets/chunkdoctor-hero.png)

## Why ChunkDoctor?

Minecraft servers rarely lag because of one obvious block. The expensive area is
usually a dense combination of entities, villagers, dropped items, minecarts,
hoppers, furnaces, redstone, spawners, and block entities.

ChunkDoctor turns those observations into an explainable **risk score from
0–100**:

```text
Risk score: 84/100
Level: CRITICAL
Confidence: HIGH

Top reasons:
- 218 hoppers
- 76 villagers
- 41 minecarts
- high block-entity density
```

The score estimates **potential load**. It never claims that a chunk consumes an
exact amount of TPS, because the public Paper API cannot measure that reliably.
Use ChunkDoctor to prioritize investigation and [spark](https://spark.lucko.me/)
to profile execution time.

## Highlights

- **Passive monitoring** that rotates fairly through loaded chunks.
- **Fast manual scans** for the current chunk or a loaded-chunk radius.
- **Tick-sliced deep scans** for redstone and block-level diagnostics.
- **Nonlinear scoring** with configurable weights, excess penalties, and density.
- **Human-readable reasons** and recommendations based on observed metrics.
- **LOW / MEDIUM / HIGH / CRITICAL** levels with confidence reporting.
- **TPS-aware pause/resume hysteresis** to avoid working during server distress.
- **Strict work budgets** for chunks, milliseconds, queues, and deep scans.
- **Interactive Adventure chat UI** with hover, info, teleport, and pagination.
- **Asynchronous atomic JSON exports** confined to the plugin data directory.
- **Permission-gated notifications** with cooldowns and persistent player opt-out.
- **No NMS, reflection, database, or mandatory plugin dependencies.**

## Performance model

ChunkDoctor follows one rule: a lag diagnostic must not become the source of lag.

```text
Paper main thread                 Bounded worker pool
─────────────────                 ───────────────────
Loaded chunk only
Collect counters ──immutable──▶   Calculate score
No disk I/O                       Sort explanations
No full block scan                Build recommendations
        ▲                         Serialize reports
        └──── result callback ────┘
```

- Unloaded chunks are never loaded for analysis.
- Background monitoring never iterates every block in a chunk.
- Manual quick scans are split across ticks.
- Deep scans are manual-only, incremental, timed, and concurrency-limited.
- Worker queues are bounded and reject excess work instead of growing forever.
- Bukkit objects never cross into asynchronous analysis tasks.

## Requirements

| Requirement | Version |
|---|---|
| Server | Paper 1.21.8 |
| Java | 21 |
| Client mod | Not required |
| Other plugins | Not required |

ChunkDoctor intentionally targets Paper and does not claim Spigot compatibility.

## Installation

1. Download `ChunkDoctor-1.0.0.jar` from
   [GitHub Releases](https://github.com/denfry/ChunkDoctor/releases).
2. Stop the server.
3. Copy the JAR into `plugins/`.
4. Start Paper 1.21.8 on Java 21.
5. Review `plugins/ChunkDoctor/config.yml`.
6. Run `/chunkdoctor status`.

Do not use the server-wide `/reload` command. ChunkDoctor provides `/cd reload`
for its own validated configuration.

## Commands

The primary command is `/chunkdoctor`; `/cd` is the short alias.

| Command | Description | Console |
|---|---|:---:|
| `/cd help` | Show help | ✓ |
| `/cd status` | Show monitor state, queues, and budgets | ✓ |
| `/cd scan [radius]` | Scan loaded chunks around the player |  |
| `/cd deep` | Deep-scan the current loaded chunk |  |
| `/cd top [page]` | Show the highest-risk chunks | ✓ |
| `/cd info <world> <x> <z>` | Show a detailed result | ✓ |
| `/cd teleport <world> <x> <z>` | Teleport to a validated safe location |  |
| `/cd export` | Export every cached result | ✓ |
| `/cd export <world> <x> <z>` | Export one result | ✓ |
| `/cd reload` | Validate and reload configuration | ✓ |
| `/cd start` / `/cd stop` | Control passive monitoring | ✓ |
| `/cd clear [confirm]` | Two-step cache clearing | ✓ |
| `/cd notify` | Toggle personal notifications |  |

## Permissions

All permissions default to server operators.

| Permission | Grants |
|---|---|
| `chunkdoctor.use` | Base command, help, and status |
| `chunkdoctor.scan` | Quick scans |
| `chunkdoctor.deep` | Deep scans |
| `chunkdoctor.top` | Risk ranking |
| `chunkdoctor.info` | Detailed reports |
| `chunkdoctor.teleport` | Safe teleport |
| `chunkdoctor.export` | JSON export |
| `chunkdoctor.reload` | Config reload |
| `chunkdoctor.control` | Start and stop |
| `chunkdoctor.clear` | Cache clearing |
| `chunkdoctor.notify` | Notifications |
| `chunkdoctor.admin` | Every permission above |

## Configuration

The generated [`config.yml`](src/main/resources/config.yml) documents every
setting. Important controls include:

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

Invalid numbers are clamped to safe ranges with clear warnings. Unsafe export
paths, invalid world modes, and broken TPS thresholds fail closed rather than
disabling the plugin.

The complete scoring weights, world filters, notification settings, message
templates, JSON schema, operational guidance, and troubleshooting are documented
in the [Russian reference](docs/README_RU.md). An English configuration reference
will expand as the public API stabilizes.

## Building from source

```bash
git clone https://github.com/denfry/ChunkDoctor.git
cd ChunkDoctor
./gradlew clean build
```

Windows:

```powershell
.\gradlew.bat clean build
```

The installable artifact is:

```text
build/libs/ChunkDoctor-1.0.0.jar
```

The Gradle distribution is checksum-pinned. Unit tests cover scoring, levels,
threshold penalties, recommendations, sorting, pagination, configuration
hardening, JSON serialization, and stable chunk identity.

## Project status

ChunkDoctor 1.0.0 is the first stable baseline. Please test it on a staging server
before production deployment and report false positives, false negatives, and
timing observations. Live server behavior varies with world height, entity mix,
view distance, hardware, and other plugins.

See the [changelog](CHANGELOG.md) for released changes and
[contribution guide](CONTRIBUTING.md) before opening a pull request.

## Security

Please do not publish exploitable issues, path escapes, permission bypasses, or
denial-of-service findings in a public issue. Follow [SECURITY.md](SECURITY.md)
for private reporting.

## License

ChunkDoctor is available under the [MIT License](LICENSE).

The repository artwork is original project branding generated for ChunkDoctor.
It is not affiliated with or endorsed by Mojang Studios or Microsoft.
