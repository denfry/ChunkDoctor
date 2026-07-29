# Modrinth publication guide

This file contains the exact values and copy needed to publish ChunkDoctor on
Modrinth. The public project description is maintained separately in
[`modrinth-description.md`](modrinth-description.md).

## Project form

| Field | Value |
|---|---|
| Project type | Plugin |
| Name | ChunkDoctor |
| Suggested slug | `chunkdoctor` |
| Summary | Find suspicious loaded chunks with bounded Paper monitoring, explainable risk scores, deep diagnostics, and actionable optimization advice. |
| Client-side | Unsupported |
| Server-side | Required |
| Primary categories | Management, Optimization, Utility |
| License | MIT |
| Source | `https://github.com/denfry/ChunkDoctor` |
| Issues | `https://github.com/denfry/ChunkDoctor/issues` |
| Documentation | `https://github.com/denfry/ChunkDoctor/blob/main/docs/README_RU.md` |
| Discord | Leave empty until an official support server exists |
| Donations | Leave empty until an official donation page exists |

If `chunkdoctor` is unavailable, choose a stable slug such as
`chunkdoctor-paper`. Do not change the Gradle configuration: release automation
uses the immutable project ID stored in GitHub rather than assuming a slug.

Paste the complete contents of `docs/modrinth-description.md` into the project
description.

The description uses only GitHub-flavored Markdown plus a small centered header,
provides descriptive alt text for every image, and does not claim compatibility
that has not been tested.

## Branding and gallery

- Icon: `docs/assets/chunkdoctor-icon.png`.
- Featured gallery image: `docs/assets/chunkdoctor-hero.png`.
- Gallery title: `ChunkDoctor risk analysis`.
- Gallery description: `ChunkDoctor highlights a suspicious loaded chunk and explains the factors contributing to its risk score.`

Upload these representative gameplay mockups after the featured image:

| Order | File | Gallery title | Gallery description |
|---:|---|---|---|
| 1 | `docs/assets/gallery/chunkdoctor-risk-analysis.png` | Explainable chunk analysis | Representative gameplay mockup showing a CRITICAL loaded chunk, its 84/100 risk score, confidence, and the observed factors behind the result. |
| 2 | `docs/assets/gallery/chunkdoctor-risk-ranking.png` | Highest-risk loaded chunks | Representative gameplay mockup of `/cd top`, with ranked LOW-to-CRITICAL results and administrator actions for details and safe teleportation. |
| 3 | `docs/assets/gallery/chunkdoctor-deep-scan.png` | Bounded deep scan | Representative gameplay mockup showing a manual deep scan split across ticks with visible block and time budgets. |

The three files are promotional UI mockups based on real ChunkDoctor commands
and defaults. Keep the word **representative** in their gallery descriptions so
they are not presented as captures from a specific production server.

The artwork is original project branding. Do not add screenshots that claim
support for server software or Minecraft versions that have not been tested.

## Initial version

| Field | Value |
|---|---|
| Version name | ChunkDoctor 1.0.0 |
| Version number | `1.0.0` |
| Release channel | Release |
| Minecraft version | `1.21.8` |
| Platform | Paper |
| File | `build/libs/ChunkDoctor-1.0.0.jar` |
| Dependencies | None |

Do not upload `ChunkDoctor-1.0.0-plain.jar`; it does not contain the relocated
Gson dependency and is not the production artifact.

### Initial release changelog

```markdown
### Added

- Fair passive monitoring of loaded chunks with TPS pause/resume hysteresis.
- Tick-sliced quick radius scans and manual deep block scans.
- Configurable nonlinear risk scoring with confidence levels.
- Entity, villager, item, vehicle, hopper, furnace, redstone, spawner,
  container, and block-entity metrics.
- Context-aware explanations and optimization recommendations.
- Interactive Adventure and MiniMessage command interface.
- Administrator notifications with cooldown and persistent opt-out.
- Safe asynchronous JSON report exports.
- Anonymous, globally opt-out bStats usage metrics.
- Unit tests and strict runtime safety limits.
```

## Moderation notes

Use this only if the review form provides a private moderation field:

```text
ChunkDoctor is a server-side Paper plugin. It does not require a client mod or
another plugin. The production JAR is built from the linked public source under
the MIT License. Gson is embedded and relocated under
dev.chunkdoctor.lib.gson. The plugin only analyzes already-loaded chunks;
manual deep scans are bounded and split across ticks.
```

Before submitting for review:

1. Build with Java 21 using `./gradlew clean build`.
2. Confirm the production JAR starts on a staging Paper 1.21.8 server.
3. Upload the icon, featured gallery image, project description, and version.
4. Verify client-side is `Unsupported` and server-side is `Required`.
5. Verify only `Paper` and Minecraft `1.21.8` are selected.
6. Confirm the MIT license, source URL, issue tracker, and installation steps
   are visible.
7. Submit the project for moderation.

## Automated releases

The Gradle `modrinth` task uploads the shaded production JAR and extracts the
matching version section from `CHANGELOG.md`. The tag release workflow publishes
to Modrinth before creating the GitHub release.

Configure the GitHub repository:

1. Create a Modrinth personal access token with the `CREATE_VERSION` scope.
2. In GitHub, open **Settings → Secrets and variables → Actions**.
3. Add repository secret `MODRINTH_TOKEN`.
4. Add repository variable `MODRINTH_PROJECT_ID` containing the Modrinth
   project's immutable ID.
5. Never put the token in `gradle.properties`, workflow YAML, logs, issues, or
   release notes.

For each release:

1. Set the Gradle `version`.
2. Move release notes from `[Unreleased]` into a matching
   `## [x.y.z] - YYYY-MM-DD` section in `CHANGELOG.md`.
3. Commit and push the release changes.
4. Create and push the exact tag `vx.y.z`.
5. The workflow builds and tests, uploads the production JAR to Modrinth, then
   creates the GitHub release.

Use `x.y.z-alpha.n`, `x.y.z-beta.n`, or `x.y.z-rc.n` for prereleases. The
Gradle configuration publishes those versions to Modrinth as `beta`; stable
versions are published as `release`.

Run a local metadata-only check without uploading:

```powershell
$env:MODRINTH_PROJECT_ID = "your-project-id"
$env:MODRINTH_TOKEN = "temporary-token"
.\gradlew.bat modrinth --dry-run
```

`--dry-run` verifies Gradle task wiring but does not contact Modrinth. Remove
the temporary token from the shell after testing:

```powershell
Remove-Item Env:MODRINTH_TOKEN
```

## bStats registration

bStats uses public plugin ID `32969`, registered for **ChunkDoctor** as
Bukkit / Spigot software:

`https://bstats.org/plugin/bukkit/ChunkDoctor/32969`

The build-time property is `bstats_plugin_id=32969` in `gradle.properties`.
Never reuse another project's ID.

The live badge in `docs/modrinth-description.md` uses:

```markdown
[![Servers using ChunkDoctor](https://bstats.org/signatures/bukkit/32969.svg)](https://bstats.org/plugin/bukkit/ChunkDoctor/32969)
```

bStats has no secret token to commit; its plugin ID is intentionally public.
