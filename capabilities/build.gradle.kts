// PlayKiln capabilities — thin, versioned building blocks over official SDKs.
//
// These are NOT a replacement for the official libraries (Play Billing, Google Mobile Ads,
// UMP consent). They own the *integration* that every app otherwise re-invents: correct
// initialisation order, consent sequencing, entitlement state, test doubles, and a
// machine-readable compliance manifest that the store pack is generated from.
//
// Rules for anything added here live in capabilities/AGENTS.md.
import groovy.json.JsonSlurper

plugins {
    alias(libs.plugins.android.library) apply false
}

// Every capability module must ship a capability.json. It is the single source of truth for
// what the capability collects, which permissions and SDKs it brings, and which test double
// replaces the real implementation. Tooling (scripts/capability-registry.mjs, store-kit.py)
// reads these files to build the Play Data safety answers.
val capabilityModules = listOf("core", "ads", "billing", "telemetry")

tasks.register("checkCapabilityManifests") {
    group = "verification"
    description = "Validate every capability.json against schema/capability.schema.json rules."
    val rootDir = layout.projectDirectory
    inputs.files(capabilityModules.map { rootDir.file("$it/capability.json") })
    doLast {
        val required = listOf(
            "schemaVersion", "id", "version", "artifact", "summary", "sdkDependencies",
            "permissions", "dataCollected", "requiresConsent", "networkRequired", "testDoubles"
        )
        val allowedDataTypes = setOf(
            "Location", "Personal info", "Financial info", "Health and fitness", "Messages",
            "Photos and videos", "Audio", "Files and docs", "Calendar", "Contacts",
            "App activity", "Web browsing", "App info and performance", "Device or other IDs"
        )
        var failures = 0
        for (module in capabilityModules) {
            var moduleFailures = 0
            val file = rootDir.file("$module/capability.json").asFile
            if (!file.isFile) {
                logger.error("FAIL  $module/capability.json is missing")
                failures++
                continue
            }
            val text = file.readText()
            @Suppress("UNCHECKED_CAST")
            val json = JsonSlurper().parseText(text) as Map<String, Any?>
            val missing = required.filter { !json.containsKey(it) }
            if (missing.isNotEmpty()) {
                logger.error("FAIL  $module/capability.json lacks keys: $missing")
                failures++
                continue
            }
            val id = json["id"] as String
            if (id != module) {
                logger.error("FAIL  $module/capability.json id='$id' must equal the module name")
                moduleFailures++
            }
            if (json["version"] != providers.gradleProperty("playkiln.capabilityVersion").get()) {
                logger.error("FAIL  $module/capability.json version must match playkiln.capabilityVersion")
                moduleFailures++
            }
            val artifact = json["artifact"] as String
            if (artifact != "com.gexetr.capability:$module") {
                logger.error("FAIL  $module/capability.json artifact must be com.gexetr.capability:$module")
                moduleFailures++
            }
            @Suppress("UNCHECKED_CAST")
            val collected = json["dataCollected"] as List<Map<String, Any?>>
            for (entry in collected) {
                val type = entry["type"] as String
                if (type !in allowedDataTypes) {
                    logger.error("FAIL  $module/capability.json dataCollected type '$type' is not a Play Data safety category")
                    moduleFailures++
                }
                for (key in listOf("purpose", "shared", "optional")) {
                    if (!entry.containsKey(key)) {
                        logger.error("FAIL  $module/capability.json dataCollected entry needs '$key'")
                        moduleFailures++
                    }
                }
            }
            @Suppress("UNCHECKED_CAST")
            val doubles = json["testDoubles"] as List<String>
            if (json["networkRequired"] == true && doubles.isEmpty()) {
                logger.error("FAIL  $module/capability.json requires a test double for a networked capability")
                moduleFailures++
            }
            failures += moduleFailures
            if (moduleFailures == 0) logger.lifecycle("PASS  $module/capability.json")
        }
        if (failures > 0) throw GradleException("$failures capability manifest problem(s)")
    }
}
