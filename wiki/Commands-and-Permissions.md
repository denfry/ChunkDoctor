# Commands and Permissions

The primary command is `/chunkdoctor`; `/cd` is its short alias.

## Commands

| Command | Purpose | Permission | Console |
|---|---|---|:---:|
| `/cd help` | Show command help | `chunkdoctor.use` | Yes |
| `/cd status` | Show monitor, queue, and budget status | `chunkdoctor.use` | Yes |
| `/cd scan [radius]` | Scan loaded chunks around the player | `chunkdoctor.scan` | No |
| `/cd deep` | Deep-scan the current loaded chunk | `chunkdoctor.deep` | No |
| `/cd top [page]` | Show the highest-risk cached chunks | `chunkdoctor.top` | Yes |
| `/cd info <world> <x> <z>` | Show one detailed result | `chunkdoctor.info` | Yes |
| `/cd teleport <world> <x> <z>` | Teleport to a validated safe location | `chunkdoctor.teleport` | No |
| `/cd export` | Export all cached results | `chunkdoctor.export` | Yes |
| `/cd export <world> <x> <z>` | Export one cached result | `chunkdoctor.export` | Yes |
| `/cd reload` | Validate and reload configuration | `chunkdoctor.reload` | Yes |
| `/cd start` | Start passive monitoring | `chunkdoctor.control` | Yes |
| `/cd stop` | Stop passive monitoring | `chunkdoctor.control` | Yes |
| `/cd clear [confirm]` | Clear cached results using two-step confirmation | `chunkdoctor.clear` | Yes |
| `/cd notify` | Toggle personal risk notifications | `chunkdoctor.notify` | No |

## Permission model

Every permission defaults to server operators.

| Permission | Grants |
|---|---|
| `chunkdoctor.use` | Base command, help, and status |
| `chunkdoctor.scan` | Quick loaded-chunk scans |
| `chunkdoctor.deep` | Manual deep scans |
| `chunkdoctor.top` | Risk ranking |
| `chunkdoctor.info` | Detailed cached results |
| `chunkdoctor.teleport` | Safe teleport action |
| `chunkdoctor.export` | JSON exports |
| `chunkdoctor.reload` | Configuration reload |
| `chunkdoctor.control` | Passive monitor start/stop |
| `chunkdoctor.clear` | Cache clearing |
| `chunkdoctor.notify` | Notifications and personal toggle |
| `chunkdoctor.admin` | Every permission above |

Grant only the capabilities an administrative role needs. In particular,
`export`, `teleport`, `clear`, `reload`, and `control` should not be given to
ordinary players.

## Scan behavior

`/cd scan [radius]` uses Chebyshev distance, so the requested radius forms a
square around the player. Two independent limits apply:

- maximum radius;
- maximum number of accepted chunks.

Unloaded chunks are skipped. The command never loads them to satisfy a scan.

## Teleport safety

Teleportation requires:

- the target world to exist;
- the target chunk to remain loaded;
- a validated safe destination;
- the caller to be a player with `chunkdoctor.teleport`.

The command fails closed if any condition changes between selection and
execution.

## Cache clearing

`/cd clear` does not immediately delete results. It returns a confirmation
instruction. Only the matching `/cd clear confirm` action clears the in-memory
cache.

Export important results first. See [[Reports and Exports]].
