// capability:billing — entitlement state over the official Google Play Billing library.
//
// Owns the parts every app gets wrong: connection retry, product/offer caching, purchase
// acknowledgement, pending-purchase resolution on cold start, and a deterministic fake.
plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

group = providers.gradleProperty("playkiln.capabilityGroup").get()
version = providers.gradleProperty("playkiln.capabilityVersion").get()

android {
    namespace = "com.gexetr.capability.billing"
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

    implementation(libs.billing.ktx)

    // Compose apps do not use Fragment; this only upgrades the transitive so Play stops flagging 1.1.0.
    // Path measured 2026-10-05: billing:9.1.0 → play-services-basement → fragment:1.1.0.
    constraints {
        api(libs.androidx.fragment) {
            because("Play notice: Billing→GMS basement pulled androidx.fragment:fragment:1.1.0; pin latest stable")
        }
    }

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
