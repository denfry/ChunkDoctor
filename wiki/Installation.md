# Installation

ChunkDoctor installs as one server-side JAR on Paper 1.21.8.

## Requirements

- Paper 1.21.8;
- Java 21;
- filesystem write access to the server's `plugins/` directory;
- no client mod and no mandatory plugin dependency.

## Install

1. Stop the server cleanly.
2. Download `ChunkDoctor-<version>.jar` from the
   [GitHub Releases page](https://github.com/denfry/ChunkDoctor/releases).
3. Do not download or install a `-plain.jar` development artifact.
4. Copy the production JAR into `plugins/`.
5. Start the server.
6. Review `plugins/ChunkDoctor/config.yml`.
7. Run `/chunkdoctor status`.

Expected first-start log:

```text
ChunkDoctor <version> enabled. Scores estimate risk; they do not measure per-chunk TPS.
```

## Verify the installation

Run:

```text
/cd status
```

Confirm that:

- ChunkDoctor is enabled;
- the worker queue and runtime budgets are displayed;
- the current world is not excluded by the world filter;
- monitoring is not paused by the configured TPS threshold.

Then stand in a loaded area and run:

```text
/cd scan
/cd top
```

## Upgrade

1. Read the release changelog.
2. Back up `plugins/ChunkDoctor/config.yml`.
3. Stop the server.
4. Replace the old ChunkDoctor JAR with the new production JAR.
5. Start the server and review validation warnings.
6. Compare the generated defaults with your existing configuration.
7. Run `/cd status` and a small `/cd scan`.

Do not keep multiple ChunkDoctor JARs in `plugins/`.

## Reloading

Do not use the server-wide `/reload` command. It is unsafe for many Paper
plugins and can leave tasks or classloaders in an inconsistent state.

Use:

```text
/cd reload
```

This validates ChunkDoctor's configuration and applies reloadable settings.
Changing the worker-pool size requires a full restart.

## Uninstall

1. Stop the server.
2. Remove the ChunkDoctor JAR.
3. Optionally archive or remove `plugins/ChunkDoctor/`.
4. Start the server and confirm no other automation still calls `/cd`.

JSON exports and configuration files are not removed automatically.

## Next

Continue with [[Commands and Permissions]] and [[Configuration]].
