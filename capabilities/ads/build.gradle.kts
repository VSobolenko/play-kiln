// capability:ads — consent-gated ad loading over the official Google Mobile Ads SDK.
//
// The SDK types stay behind `implementation`: a slot talks to AdsController, not to AdMob,
// so switching networks or adding a test double never rewrites app code.
plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

group = "com.gexetr.capability"
version = providers.gradleProperty("playkiln.capabilityVersion").get()

android {
    namespace = "com.gexetr.capability.ads"
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

    // Official SDKs, deliberately not exposed through this module's public API.
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)

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
