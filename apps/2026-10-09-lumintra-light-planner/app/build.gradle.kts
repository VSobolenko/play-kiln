import java.util.Properties

// Factory commands:
// ./gradlew assembleDebug                    (sideload APK)
// ./gradlew bundleRelease                    (Play AAB)
// ./gradlew lint
// ./gradlew :app:generateBaselineProfile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    // Navigation 3's `rememberNavBackStack` stores the back stack through its own serializer, so the
    // destination keys are `@Serializable`. The configuration is the one the published reference slot
    // already runs; see `.cursor/rules/ui-ux-baseline.mdc`.
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.baselineprofile)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
val hasReleaseKeystore = keystorePropertiesFile.isFile

if (hasReleaseKeystore) {
    keystorePropertiesFile.inputStream().use(keystoreProperties::load)
}

android {
    namespace = "com.gexetr.lumintralightplanner"
    compileSdk = 37
    // Pinned by AGENTS.md "Required toolchain" and declared here so the pin is read from the
    // artifact rather than left as prose no program enforces.
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.gexetr.lumintralightplanner"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        // The User-Agent sent to api.met.no and CelesTrak reads BuildConfig.VERSION_NAME.
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                // Gradle's file(...) resolves relative paths against this module directory.
                storeFile = file(requireNotNull(keystoreProperties.getProperty("storeFile")) {
                    "keystore.properties must define storeFile"
                })
                storePassword = requireNotNull(keystoreProperties.getProperty("storePassword")) {
                    "keystore.properties must define storePassword"
                }
                keyAlias = requireNotNull(keystoreProperties.getProperty("keyAlias")) {
                    "keystore.properties must define keyAlias"
                }
                keyPassword = requireNotNull(keystoreProperties.getProperty("keyPassword")) {
                    "keystore.properties must define keyPassword"
                }
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )

            // Play uploads require a real upload key configured in keystore.properties.
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                // Development only: Play requires a real upload key for release uploads.
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // With AGP 9 built-in Kotlin, jvmTarget follows compileOptions.targetCompatibility.

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            // Every Orekit/Hipparchus jar carries its own META-INF/LICENSE.txt and NOTICE.txt, so the
            // merge collides on them. The Apache-2.0 texts still ship, verbatim, in
            // src/main/assets/licenses/ (orekit-*.txt, hipparchus-*.txt).
            // com.sun.source.util.Plugin is a javac plugin service Orekit never uses at run time; it
            // was the one R8 warning in docs/G-android-build-proof.md.
            excludes += setOf(
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "META-INF/*.kotlin_module",
                "META-INF/services/com.sun.source.util.Plugin",
            )
        }
        jniLibs {
            // useLegacyPackaging = false keeps 16 KB page-size compatibility for native libraries.
            useLegacyPackaging = false
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = false
        lintConfig = file("../lint.xml")
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    // AppCompatDelegate.setApplicationLocales — in-app language picker host (AppCompatActivity).
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.core)
    // Parses the api.met.no forecast, the CelesTrak GP JSON and the scout export/import document.
    implementation(libs.kotlinx.serialization.json)
    // Network, engine and geocoder work off the main thread.
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.profileinstaller)
    // Soft rating prompts: ReviewManagerFactory → requestReviewFlow → launchReviewFlow.
    implementation(libs.play.review)
    implementation(libs.play.review.ktx)
    // Night glance home-screen widget (GlanceAppWidget + GlanceAppWidgetReceiver).
    implementation(libs.androidx.glance.appwidget)
    // Glance's Material 3 colour providers, so the widget takes the slot's own palette.
    implementation(libs.androidx.glance.material3)
    // Dark-window reminder: inexact OneTimeWorkRequest that WorkManager reschedules after reboot.
    implementation(libs.androidx.work.runtime.ktx)
    // Satellite passes: SGP4 propagation and TEME → ITRF → topocentric frames (Apache-2.0).
    implementation(libs.orekit)
    // Orekit's maths dependencies, declared explicitly at the version Orekit 13.1.9 pins.
    implementation(libs.hipparchus.core)
    implementation(libs.hipparchus.filtering)
    implementation(libs.hipparchus.fitting)
    implementation(libs.hipparchus.geometry)
    implementation(libs.hipparchus.ode)
    implementation(libs.hipparchus.optim)
    implementation(libs.hipparchus.stat)

    constraints {
        // Compose app: no Fragment UI. review-ktx 2.0.2 (and GMS basement) request fragment 1.1.0,
        // which Play flags as outdated; this only raises the transitive to the floor.
        api(libs.androidx.fragment) {
            because("Play outdated-SDK floor: review-ktx 2.0.2 pulls androidx.fragment:fragment:1.1.0")
        }
    }

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    baselineProfile(project(":baselineprofile"))
}

baselineProfile {
    // Generation needs an attached emulator/device and is not part of a normal build.
    saveInSrc = true
    mergeIntoMain = true
}
