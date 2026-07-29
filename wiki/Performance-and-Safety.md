# Performance and Safety

ChunkDoctor follows one rule: a lag diagnostic must not become the source of
lag.

## Thread model

### Server thread

- reads already-loaded chunks;
- collects counters into immutable snapshots;
- accesses Bukkit and Paper objects;
- updates the result repository;
- sends messages and notifications.

### Bounded worker pool

- calculates risk scores;
- sorts explanations;
- builds recommendations.

### Export executor

- serializes JSON;
- writes a temporary file;
- atomically replaces the destination when supported.

Bukkit objects are never passed into worker tasks.

## Passive monitoring

Passive monitoring:

- rotates fairly through worlds and loaded chunks;
- limits chunks per cycle;
- stops collection when the per-tick deadline is reached;
- respects per-chunk rescan cooldowns;
- pauses below the configured TPS threshold;
- resumes only above the higher recovery threshold.

It does not iterate every block.

## Bounded overload behavior

The analysis queue has a hard capacity. When full, new work is rejected rather
than allowing an unbounded backlog.

This protects:

- heap memory;
- shutdown time;
- result freshness;
- recovery after a server load spike.

## Deep scans

Deep scans are intentionally more expensive and therefore:

- require a player command and permission;
- operate on the current loaded chunk;
- split block work across ticks;
- enforce both block and time budgets;
- have an overall timeout;
- limit concurrent scans;
- cancel when the world unloads or the plugin disables.

## Filesystem boundary

Exports resolve beneath the plugin data directory. Unsafe relative paths,
traversal sequences, separators, and drive syntax are rejected before any file
is written.

## Administrative boundaries

State-changing or sensitive operations use separate permissions:

- monitor control;
- configuration reload;
- cache clearing;
- teleportation;
- export.

Cache clearing uses explicit confirmation. Teleportation revalidates the world,
loaded chunk, and safe destination.

## Operational guidance

- Keep default budgets until you have measured peak-time MSPT.
- Test changes on staging before production.
- Increase one budget at a time.
- Use a world whitelist on networks with many utility worlds.
- Export before clearing the cache.
- Compare ChunkDoctor results with spark rather than treating score as timing.
