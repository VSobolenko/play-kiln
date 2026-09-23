// capability:remoteconfig — safe defaults and deterministic fetches over Firebase Remote Config.
//
// Firebase types stay behind `implementation`: slots depend only on the PlayKiln controller.
plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

group = providers.gradleProperty("playkiln.capabilityGroup").get()
version = providers.gradleProperty("playkiln.capabilityVersion").get()

android {
    namespace = "com.gexetr.capability.remoteconfig"
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

    // Official SDK, deliberately not exposed through this module's public API.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.remote.config)

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
