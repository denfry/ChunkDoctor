<div align="center">

<img src="https://raw.githubusercontent.com/denfry/ChunkDoctor/main/docs/assets/chunkdoctor-icon.png" alt="ChunkDoctor logo: a Minecraft chunk with a diagnostic pulse" width="160">

# ChunkDoctor

**Find suspicious loaded chunks before they become a server-wide problem.**

[![Build status](https://img.shields.io/github/actions/workflow/status/denfry/ChunkDoctor/build.yml?branch=main&style=for-the-badge&label=build)](https://github.com/denfry/ChunkDoctor/actions/workflows/build.yml)
[![Latest release](https://img.shields.io/github/v/release/denfry/ChunkDoctor?display_name=tag&sort=semver&style=for-the-badge)](https://github.com/denfry/ChunkDoctor/releases)
[![Paper 1.21.8](https://img.shields.io/badge/Paper-1.21.8-2f3136?style=for-the-badge)](https://papermc.io/)
[![Java 21](https://img.shields.io/badge/Java-21-e76f00?style=for-the-badge&logo=openjdk&logoColor=white)](https://adoptium.net/)
[![MIT license](https://img.shields.io/badge/License-MIT-22c55e?style=for-the-badge)](https://github.com/denfry/ChunkDoctor/blob/main/LICENSE)
[![Servers using ChunkDoctor](https://bstats.org/signatures/bukkit/32969.svg)](https://bstats.org/plugin/bukkit/ChunkDoctor/32969)

</div>

![ChunkDoctor showing an explainable risk score for a loaded Minecraft chunk](https://raw.githubusercontent.com/denfry/ChunkDoctor/main/docs/assets/chunkdoctor-hero.png)

ChunkDoctor is a performance-conscious Paper plugin that monitors already-loaded
chunks, assigns an explainable risk score, and tells administrators what is
worth inspecting.

Minecraft servers rarely struggle because of one obvious block. The expensive
area is often a dense combination of entities, villagers, dropped items,
minecarts, hoppers, furnaces, redstone, spawners, and block entities.
ChunkDoctor turns those observations into a clear score from 0 to 100.

> **ChunkDoctor estimates potential load; it does not pretend to measure the
> exact TPS cost of a chunk.** Use it to prioritize an investigation, then use
> [spark](https://spark.lucko.me/) when you need execution-time profiling.

## Why ChunkDoctor?

- **Find the chunks worth checking.** Passive monitoring rotates fairly through
  loaded chunks and keeps a ranked list of suspicious areas.
- **Understand every result.** Scores include confidence, top contributing
  factors, and recommendations based on the metrics actually observed.
- **Inspect deeper when needed.** Manual deep scans reveal redstone and
  block-level signals without forcing neighboring chunks to load.
- **Stay in control.** Interactive chat actions provide details, pagination,
  exports, and permission-gated safe teleportation.
- **Protect the server being diagnosed.** TPS hysteresis, bounded queues,
  tick slicing, deadlines, and concurrency limits keep analysis work contained.

## Performance-first by design

| Safety property | What ChunkDoctor does |
|---|---|
| Chunk loading | Inspects loaded chunks only; never loads a chunk for analysis |
| Passive scans | Collects counters without iterating every block |
| Deep scans | Manual-only, tick-sliced, timed, and concurrency-limited |
| Async work | Scores immutable snapshots in a bounded worker pool |
| Overload behavior | Rejects excess analysis instead of growing an unbounded queue |
| TPS protection | Pauses and resumes monitoring with configurable hysteresis |

Bukkit objects never cross into asynchronous analysis tasks. Disk exports run
away from the server thread and use atomic replacement inside the plugin data
directory.

## What ChunkDoctor can detect

- entities, villagers, dropped items, armor stands, and hostile mobs;
- minecarts, boats, and other vehicles;
- hoppers, furnaces, containers, spawners, and other block entities;
- redstone components, pistons, observers, and powered components during deep
  scans;
- unusually dense combinations that are more concerning together than alone.

Results use four configurable levels: **LOW**, **MEDIUM**, **HIGH**, and
**CRITICAL**. Confidence indicates how complete the observation was rather than
overstating what the Paper API can prove.

## Requirements

| Requirement | Supported value |
|---|---|
| Server software | **Paper** |
| Minecraft | **1.21.8** |
| Java | **21** |
| Client installation | **Not required** |
| Required plugins | **None** |

ChunkDoctor intentionally targets Paper. It does not claim compatibility with
Spigot, Folia, Purpur, or other implementations unless a future release
explicitly lists them.

## Installation

1. Stop the server.
2. Download the production `ChunkDoctor-<version>.jar`.
3. Place it in the server's `plugins/` directory.
4. Start Paper 1.21.8 using Java 21.
5. Review `plugins/ChunkDoctor/config.yml`.
6. Run `/chunkdoctor status`.

Do not use the server-wide `/reload` command. ChunkDoctor provides `/cd reload`
for its own validated configuration.

## Commands

The main command is `/chunkdoctor`; `/cd` is the short alias.

| Command | Description | Console |
|---|---|:---:|
| `/cd help` | Show command help | Yes |
| `/cd status` | Show monitor, queue, and budget status | Yes |
| `/cd scan [radius]` | Scan loaded chunks around the player | No |
| `/cd deep` | Deep-scan the current loaded chunk | No |
| `/cd top [page]` | List the highest-risk chunks | Yes |
| `/cd info <world> <x> <z>` | Show a detailed result | Yes |
| `/cd teleport <world> <x> <z>` | Teleport to a validated safe location | No |
| `/cd export [world x z]` | Export cached results as JSON | Yes |
| `/cd reload` | Validate and reload configuration | Yes |
| `/cd start` / `/cd stop` | Control passive monitoring | Yes |
| `/cd clear [confirm]` | Clear cached results with confirmation | Yes |
| `/cd notify` | Toggle personal notifications | No |

All permissions default to server operators. Use `chunkdoctor.admin` for every
feature, or grant the individual `chunkdoctor.*` permissions documented in the
[repository](https://github.com/denfry/ChunkDoctor#permissions).

## Configuration

The generated configuration documents every setting. Important controls include:

```yaml
monitoring:
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

Invalid numeric values are clamped to safe ranges. Unsafe export paths, broken
TPS thresholds, and invalid world modes fail closed with clear log messages.

## Anonymous metrics

ChunkDoctor uses [bStats](https://bstats.org/plugin/bukkit/ChunkDoctor/32969) to collect anonymous, aggregated
usage metrics such as the plugin version, Minecraft/server software version,
Java version, operating system type, country, and current player count.
ChunkDoctor does not add custom charts and does not collect player names, chat,
world data, coordinates, scan results, configuration values, or report contents.

Server owners can disable bStats globally in `plugins/bStats/config.yml` by
setting `enabled: false`.

## Support

- [Report a bug](https://github.com/denfry/ChunkDoctor/issues/new?template=bug_report.yml)
- [Report an incorrect score](https://github.com/denfry/ChunkDoctor/issues/new?template=false_score.yml)
- [Request a feature](https://github.com/denfry/ChunkDoctor/issues/new?template=feature_request.yml)
- [Read the Russian documentation](https://github.com/denfry/ChunkDoctor/blob/main/docs/README_RU.md)
- [Review the changelog](https://github.com/denfry/ChunkDoctor/blob/main/CHANGELOG.md)
- [Report a security issue privately](https://github.com/denfry/ChunkDoctor/security/policy)

When reporting a problem, include the ChunkDoctor version, Paper build, Java
version, relevant logs, and the smallest configuration needed to reproduce it.
Do not publish security vulnerabilities in a public issue.

## Open source and modpacks

ChunkDoctor is open source under the [MIT License](https://github.com/denfry/ChunkDoctor/blob/main/LICENSE).
You may include the original, unmodified Modrinth release in Modrinth modpacks.
Please link back to the official project page rather than reuploading the JAR
elsewhere.
