pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // Auto-provision JDK 17 when the machine's default JDK is newer.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "playkiln-capabilities"

// Opt-in building blocks shared by many PlayKiln slots.
// A slot depends on a pinned published version and never on another slot's source.
include(":core", ":ads", ":billing", ":telemetry")
