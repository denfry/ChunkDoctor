plugins {
    java
    id("com.gradleup.shadow") version "9.6.1"
    id("com.modrinth.minotaur") version "2.9.0"
}

group = "dev.chunkdoctor"
version = "1.0.0"
val pluginVersionForResources = version.toString()
val bStatsPluginId = providers.gradleProperty("bstats_plugin_id").orElse("0")

fun releaseNotesFor(version: String): String {
    val changelog = rootProject.file("CHANGELOG.md").readText()
    val versionHeader = Regex("""(?m)^## \[${Regex.escape(version)}]\s+-\s+.+$""")
    val match = versionHeader.find(changelog)
        ?: throw GradleException("CHANGELOG.md has no release section for version $version")
    val nextHeader = Regex("""(?m)^## \[""").find(changelog, match.range.last + 1)
    return changelog
        .substring(match.range.last + 1, nextHeader?.range?.first ?: changelog.length)
        .trim()
        .ifEmpty {
            throw GradleException("CHANGELOG.md release section for version $version is empty")
        }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.87-stable")
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("org.bstats:bstats-bukkit:3.2.1")

    testImplementation("org.junit.jupiter:junit-jupiter:6.1.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    processResources {
        filesMatching(listOf("plugin.yml", "bstats.properties")) {
            expand(
                "version" to pluginVersionForResources,
                "bstatsPluginId" to bStatsPluginId.get()
            )
        }
    }

    test {
        useJUnitPlatform()
    }

    shadowJar {
        archiveClassifier.set("")
        relocate("com.google.gson", "dev.chunkdoctor.lib.gson")
        relocate("org.bstats", "dev.chunkdoctor.lib.bstats")
    }

    jar {
        archiveClassifier.set("plain")
    }

    build {
        dependsOn(shadowJar)
    }
}

modrinth {
    token.set(providers.environmentVariable("MODRINTH_TOKEN"))
    projectId.set(providers.environmentVariable("MODRINTH_PROJECT_ID"))
    versionNumber.set(project.version.toString())
    versionName.set("ChunkDoctor ${project.version}")
    versionType.set(
        if (project.version.toString().contains(Regex("""(?i)(alpha|beta|rc)"""))) {
            "beta"
        } else {
            "release"
        }
    )
    uploadFile.set(tasks.shadowJar)
    gameVersions.add("1.21.8")
    loaders.add("paper")
    changelog.set(providers.provider { releaseNotesFor(project.version.toString()) })
}
