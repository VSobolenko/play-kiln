package com.gexetr.plinthproductphotos.data.look

import android.content.Context
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.domain.look.LookJsonCodec
import com.gexetr.plinthproductphotos.domain.model.BackgroundSpec
import com.gexetr.plinthproductphotos.domain.model.Look
import com.gexetr.plinthproductphotos.domain.model.ShadowSpec
import java.io.File
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

class LookRepository(context: Context) {
    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, "backup/looks.json")

    fun all(): List<Look> = synchronized(LOCK) {
        val existing = if (file.exists()) {
            LookJsonCodec.decode(file.readText(Charsets.UTF_8))
        } else {
            emptyList()
        }
        val looks = looksWithDefault(existing, appContext.getString(R.string.home_nav_default_look))
        if (existing.isEmpty()) write(looks)
        looks
    }

    fun get(id: String): Look? = all().firstOrNull { it.id == id }

    fun save(look: Look) = synchronized(LOCK) {
        require(look.id.isNotBlank())
        require(look.name.isNotBlank()) { "Look name cannot be blank" }
        val existing = all()
        write(existing.filterNot { it.id == look.id } + look)
    }

    fun rename(id: String, name: String) = synchronized(LOCK) {
        val normalizedName = name.trim()
        require(normalizedName.isNotEmpty()) { "Look name cannot be blank" }
        val existing = all()
        val found = existing.firstOrNull { it.id == id } ?: return@synchronized
        write(existing.map { if (it.id == id) found.copy(name = normalizedName) else it })
    }

    fun duplicate(id: String, name: String): Look? = synchronized(LOCK) {
        val source = all().firstOrNull { it.id == id } ?: return@synchronized null
        val duplicate = source.copy(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifEmpty { source.name },
            createdAt = System.currentTimeMillis(),
        )
        write(all() + duplicate)
        duplicate
    }

    fun delete(id: String) = synchronized(LOCK) {
        val existing = all()
        if (existing.any { it.id == id }) write(existing.filterNot { it.id == id })
    }

    private fun write(looks: List<Look>) {
        val parent = requireNotNull(file.parentFile)
        if (!parent.isDirectory && !parent.mkdirs()) {
            throw IllegalStateException("Could not create saved Looks storage")
        }
        val temporary = File(parent, "${file.name}.tmp")
        try {
            FileOutputStream(temporary).use { output ->
                output.write(LookJsonCodec.encode(looks).toByteArray(Charsets.UTF_8))
                output.fd.sync()
            }
            try {
                Files.move(
                    temporary.toPath(),
                    file.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            if (temporary.exists() && !temporary.delete()) {
                throw IllegalStateException("Could not remove temporary saved Looks file")
            }
        }
    }

    private companion object {
        val LOCK = Any()
    }
}

internal fun looksWithDefault(existing: List<Look>, name: String): List<Look> =
    existing.ifEmpty {
        listOf(
            Look(
                id = "plinth-default",
                name = name,
                background = BackgroundSpec(
                    kind = BackgroundSpec.Kind.SOLID,
                    colorArgb = 0xffffffff.toInt(),
                    color2Argb = null,
                    angleDeg = null,
                    sceneId = null,
                    sceneAssetPath = null,
                    prompt = null,
                    seed = null,
                ),
                shadow = ShadowSpec(kind = ShadowSpec.Kind.NONE),
                createdAt = 0L,
            ),
        )
    }
