package com.gexetr.coverkit.domain

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Shared JSON codec for [CoverProject] trees. Kept pure so unit tests can round-trip
 * without Android Context.
 */
object ProjectCodec {
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
        prettyPrint = false
    }

    fun encode(project: CoverProject): String = json.encodeToString(project)

    fun decode(raw: String): CoverProject = json.decodeFromString(raw)
}
