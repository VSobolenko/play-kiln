import java.net.URI
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
    alias(libs.plugins.ksp)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
val hasReleaseKeystore = keystorePropertiesFile.isFile

// Public Cloud project NUMBER from the project linked to this app's Play Integrity settings.
val integrityCloudProjectNumber = providers.gradleProperty("PLINTH_INTEGRITY_CLOUD_PROJECT_NUMBER")
    .orElse(providers.environmentVariable("PLINTH_INTEGRITY_CLOUD_PROJECT_NUMBER"))
    .map { value ->
        requireNotNull(value.toLongOrNull()?.takeIf { it > 0 }) {
            "PLINTH_INTEGRITY_CLOUD_PROJECT_NUMBER must be a positive Google Cloud project number"
        }
    }
    .orElse(0L)

val debugApiBaseUrl = providers.gradleProperty("PLINTH_DEBUG_API_BASE_URL")
    .orElse(providers.environmentVariable("PLINTH_DEBUG_API_BASE_URL"))
    .orElse("https://pikupo.com/plinth-debug/v1")
    .map { value ->
        val uri = URI(value)
        require(uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()) {
            "PLINTH_DEBUG_API_BASE_URL must be an absolute HTTP or HTTPS URL"
        }
        // Owner rule: debug defaults to the remote debug API. HTTP is only for
        // an explicit loopback override — never the default review path.
        if (uri.scheme == "http") {
            require(uri.host in setOf("127.0.0.1", "localhost")) {
                "PLINTH_DEBUG_API_BASE_URL HTTP is only allowed for 127.0.0.1/localhost"
            }
        }
        value
    }

if (hasReleaseKeystore) {
    keystorePropertiesFile.inputStream().use(keystoreProperties::load)
}

android {
    namespace = "com.gexetr.plinthproductphotos"
    compileSdk = 37
    // Pinned by AGENTS.md "Required toolchain" and declared here so the pin is read from the
    // artifact rather than left as prose no program enforces.
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.gexetr.plinthproductphotos"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("long", "INTEGRITY_CLOUD_PROJECT_NUMBER", "${integrityCloudProjectNumber.get()}L")

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
            buildConfigField("String", "API_BASE_URL", "\"${debugApiBaseUrl.get()}\"")
        }

        release {
            buildConfigField("String", "API_BASE_URL", "\"https://pikupo.com/plinth/v1\"")
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
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.telephoto.zoomable)
    implementation(libs.capability.core)
    implementation(libs.capability.billing)
    implementation(libs.billing.ktx)
    implementation(libs.androidx.profileinstaller)
    // Soft rating prompts: ReviewManagerFactory → requestReviewFlow → launchReviewFlow.
    implementation(libs.play.review.ktx)
    implementation(libs.play.integrity)

    // Compose apps do not use Fragment; this constraint only raises the Billing transitive above Play's floor.
    constraints {
        api(libs.androidx.fragment) {
            because("Play's dependency floor requires androidx.fragment:fragment 1.9.1 or newer")
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

composeCompiler {
    reportsDestination = rootProject.layout.projectDirectory.dir(".kiln/scratch/w34/compose-compiler/reports")
    metricsDestination = rootProject.layout.projectDirectory.dir(".kiln/scratch/w34/compose-compiler/metrics")
}
