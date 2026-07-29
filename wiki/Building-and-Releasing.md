# Building and Releasing

ChunkDoctor uses Gradle Kotlin DSL and Java 21.

## Build from source

Linux or macOS:

```bash
./gradlew clean build
```

Windows:

```powershell
.\gradlew.bat clean build
```

The installable artifact is:

```text
build/libs/ChunkDoctor-<version>.jar
```

Do not deploy the `-plain.jar`; it excludes relocated runtime dependencies.

## Dependency packaging

The production JAR relocates:

- Gson into `dev.chunkdoctor.lib.gson`;
- bStats into `dev.chunkdoctor.lib.bstats`.

This avoids classpath collisions with other server plugins.

## Continuous integration

Every push to `main` and every pull request:

- validates the Gradle wrapper;
- installs Java 21;
- runs a clean build and tests;
- uploads the production JAR as a short-lived workflow artifact.

GitHub Actions are pinned to commit SHAs.

## Release checklist

1. Choose the semantic version.
2. Set `version` in `build.gradle.kts`.
3. Move notes from `[Unreleased]` into a matching changelog section:

   ```text
   ## [x.y.z] - YYYY-MM-DD
   ```

4. Build and test locally.
5. Commit the release changes.
6. Create the exact tag `vx.y.z`.
7. Push the commit and tag.

## Automated publishing

The tag workflow:

1. verifies that the tag matches the Gradle version;
2. performs a clean build and test run;
3. uploads the shaded JAR to Modrinth;
4. creates the GitHub release.

Required GitHub configuration:

- repository secret `MODRINTH_TOKEN`;
- repository variable `MODRINTH_PROJECT_ID`.

The token requires the Modrinth `CREATE_VERSION` scope. Never commit it.

## Version channels

- stable versions publish as `release`;
- versions containing `alpha`, `beta`, or `rc` publish to Modrinth as `beta`.

The Modrinth changelog is extracted from the matching `CHANGELOG.md` section.
Publishing fails if that section is missing or empty.
