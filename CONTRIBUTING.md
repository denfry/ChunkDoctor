# Contributing to ChunkDoctor

Thanks for helping make server diagnostics safer and more useful.

## Before opening an issue

- Search existing issues.
- Confirm the server is Paper 1.21.8 on Java 21.
- Reproduce on a staging server with the latest release.
- Include the ChunkDoctor config with world names and sensitive values removed.
- For risk-quality reports, include observed metrics, expected level, actual
  level, and relevant spark evidence.

Use private security reporting for vulnerabilities; see [SECURITY.md](SECURITY.md).

## Development

```bash
./gradlew clean build
```

Every pull request must:

- keep all Bukkit/Paper access on the main thread;
- pass only immutable Bukkit-free snapshots to workers;
- never load an unloaded chunk for analysis;
- preserve bounded queues and tick budgets;
- add tests for scoring or configuration changes;
- avoid NMS, CraftBukkit, reflection, and mandatory integrations;
- update README/config comments when behavior changes.

## Pull requests

1. Fork the repository and create a focused branch.
2. Keep changes scoped to one concern.
3. Use clear commit messages, preferably Conventional Commits.
4. Run `./gradlew clean build`.
5. Explain performance and thread-safety implications in the PR.

By contributing, you agree that your work is licensed under the MIT License.
