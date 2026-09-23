// PlayKiln capabilities — thin, versioned building blocks over official SDKs.
//
// These are NOT a replacement for the official libraries (Play Billing, Google Mobile Ads,
// UMP consent). They own the *integration* that every app otherwise re-invents: correct
// initialisation order, consent sequencing, entitlement state, test doubles, and a
// machine-readable compliance manifest that the store pack is generated from.
//
// Rules for anything added here live in capabilities/AGENTS.md.
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction

plugins {
    alias(libs.plugins.android.library) apply false
}

/**
 * Validates every capability.json against the rules in schema/capability.schema.json.
 *
 * Written as a typed task rather than a `doLast` closure on purpose. A closure that reads a
 * script-level value captures the build script itself, and the configuration cache refuses to
 * serialize script objects — which made this task fail on every run while the cache is on
 * (and the cache is on by default here, for build speed). Typed inputs cannot capture the
 * script, so the defect cannot come back.
 */
abstract class CheckCapabilityManifestsTask : DefaultTask() {
    @get:Input
    abstract val capabilityModules: ListProperty<String>

    @get:Input
    abstract val capabilityGroup: Property<String>

    @get:Input
    abstract val expectedVersion: Property<String>

    @get:InputFiles
    abstract val manifests: ConfigurableFileCollection

    @get:InputFile
    abstract val schemaFile: RegularFileProperty

    @TaskAction
    fun verify() {
        val modules = capabilityModules.get()
        val group = capabilityGroup.get()
        val expected = expectedVersion.get()

        val requiredKeys = listOf(
            "schemaVersion", "id", "version", "artifact", "summary", "sdkDependencies",
            "permissions", "dataCollected", "requiresConsent", "networkRequired", "testDoubles",
        )
        val allowedDataTypes = setOf(
            "Location", "Personal info", "Financial info", "Health and fitness", "Messages",
            "Photos and videos", "Audio", "Files and docs", "Calendar", "Contacts",
            "App activity", "Web browsing", "App info and performance", "Device or other IDs",
        )

        val byModule = manifests.files.associateBy { it.parentFile.name }
        var failures = 0

        for (module in modules) {
            var moduleFailures = 0
            val file = byModule[module]

            if (file == null || !file.isFile) {
                logger.error("FAIL  $module/capability.json is missing")
                failures++
                continue
            }

            @Suppress("UNCHECKED_CAST")
            val json = JsonSlurper().parseText(file.readText()) as Map<String, Any?>

            val missing = requiredKeys.filter { !json.containsKey(it) }
            if (missing.isNotEmpty()) {
                logger.error("FAIL  $module/capability.json lacks keys: $missing")
                failures++
                continue
            }

            if (json["id"] != module) {
                logger.error("FAIL  $module/capability.json id='${json["id"]}' must equal the module name")
                moduleFailures++
            }

            if (json["version"] != expected) {
                logger.error("FAIL  $module/capability.json version must match playkiln.capabilityVersion=$expected")
                moduleFailures++
            }

            if (json["artifact"] != "$group:$module") {
                logger.error("FAIL  $module/capability.json artifact must be $group:$module")
                moduleFailures++
            }

            @Suppress("UNCHECKED_CAST")
            val collected = json["dataCollected"] as List<Map<String, Any?>>
            for (entry in collected) {
                val type = entry["type"] as String
                if (type !in allowedDataTypes) {
                    logger.error("FAIL  $module: dataCollected type '$type' is not a Play Data safety category")
                    moduleFailures++
                }
                for (key in listOf("purpose", "shared", "optional")) {
                    if (!entry.containsKey(key)) {
                        logger.error("FAIL  $module: dataCollected entry needs '$key'")
                        moduleFailures++
                    }
                }
            }

            @Suppress("UNCHECKED_CAST")
            val doubles = json["testDoubles"] as List<String>
            if (json["networkRequired"] == true && doubles.isEmpty()) {
                logger.error("FAIL  $module: a networked capability must ship a test double")
                moduleFailures++
            }

            failures += moduleFailures
            if (moduleFailures == 0) logger.lifecycle("PASS  $module/capability.json")
        }

        failures += verifySchemaAgrees(schemaFile.get().asFile.readText(), group)

        if (failures > 0) throw GradleException("$failures capability manifest problem(s)")
    }

    /**
     * The JSON schema restates the artifact group as a regex and imports it from nowhere, so a
     * stale copy there stays invisible until someone validates against the schema. Compare the
     * two instead of trusting them to stay in step.
     */
    private fun verifySchemaAgrees(schemaText: String, group: String): Int {
        @Suppress("UNCHECKED_CAST")
        val schema = JsonSlurper().parseText(schemaText) as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val pattern = ((schema["properties"] as? Map<String, Any?>)
            ?.get("artifact") as? Map<String, Any?>)
            ?.get("pattern") as? String

        val escapedGroup = group.replace(".", "\\.")
        return when {
            pattern == null -> {
                logger.error("FAIL  schema/capability.schema.json has no properties.artifact.pattern")
                1
            }

            !pattern.contains("^$escapedGroup:") -> {
                logger.error(
                    "FAIL  schema artifact pattern '$pattern' must start with ^$escapedGroup: " +
                        "to agree with playkiln.capabilityGroup=$group"
                )
                1
            }

            else -> 0
        }
    }
}

val capabilityModuleNames = listOf("core", "ads", "billing", "telemetry", "remoteconfig")

tasks.register<CheckCapabilityManifestsTask>("checkCapabilityManifests") {
    group = "verification"
    description = "Validate every capability.json against schema/capability.schema.json rules."

    capabilityModules.set(capabilityModuleNames)
    capabilityGroup.set(providers.gradleProperty("playkiln.capabilityGroup"))
    expectedVersion.set(providers.gradleProperty("playkiln.capabilityVersion"))
    manifests.setFrom(capabilityModuleNames.map { layout.projectDirectory.file("$it/capability.json") })
    schemaFile.set(layout.projectDirectory.file("schema/capability.schema.json"))
}
