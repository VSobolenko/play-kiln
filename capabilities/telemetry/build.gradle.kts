// capability:telemetry — logging, crash hooks and product-event gates.
//
// Ships a no-op implementation as the default so a slot has zero tracking unless it
// explicitly opts in, and so Data safety answers stay honest.
plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

group = "com.gexetr.capability"
version = providers.gradleProperty("playkiln.capabilityVersion").get()

android {
    namespace = "com.gexetr.capability.telemetry"
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

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    api(project(":core"))

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
