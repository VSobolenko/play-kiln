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
        // Capabilities are published locally during factory development. Keep this only in
        // dependency resolution (not pluginManagement), and after trusted remotes: it enables
        // com.gexetr.capability artifacts without letting mavenLocal shadow Google plugins.
        mavenLocal()
    }
}

rootProject.name = "__APP_SLUG__"
include(":app", ":baselineprofile")
