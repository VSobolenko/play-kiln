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
    namespace = "com.gexetr.scaffold"
    compileSdk = 37
    // Pinned by AGENTS.md "Required toolchain" and declared here so the pin is read from the
    // artifact rather than left as prose no program enforces.
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.gexetr.scaffold"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
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
            excludes += setOf(
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "META-INF/*.kotlin_module",
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
    implementation(libs.androidx.profileinstaller)
    // Soft rating prompts: ReviewManagerFactory → requestReviewFlow → launchReviewFlow.
    implementation(libs.play.review.ktx)

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
