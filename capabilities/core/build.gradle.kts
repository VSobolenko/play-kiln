// capability:core — contracts shared by every capability and every slot.
//
// Deliberately tiny and dependency-light: only coroutines. No Android UI, no SDK types.
plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

group = "com.gexetr.capability"
version = providers.gradleProperty("playkiln.capabilityVersion").get()

android {
    namespace = "com.gexetr.capability.core"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }

    // Publish one release variant; the factory pins the version, never a branch.
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    // Public API exposes Flow/StateFlow, so coroutines belong on the consumer compile classpath.
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                artifactId = project.name
            }
        }
    }
}
