package com.gexetr.coverkit.data

import android.content.Context
import com.gexetr.coverkit.domain.CoverProject
import com.gexetr.coverkit.domain.ProjectCodec
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Durable cover projects on device storage (JSON under filesDir). */
interface ProjectStore {
    /** Live list for Projects UI (newest first). */
    val projects: StateFlow<List<CoverProject>>

    suspend fun list(): List<CoverProject>
    suspend fun open(id: String): CoverProject?
    /** Alias for [open] used by the editor. */
    suspend fun load(id: String): CoverProject?
    suspend fun save(project: CoverProject): CoverProject
    suspend fun autosave(project: CoverProject): CoverProject
    suspend fun duplicate(id: String): CoverProject?
    suspend fun delete(id: String): Boolean
    /** Reload [projects] from disk. */
    suspend fun refresh()
}

class FileProjectStore(
    private val context: Context,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : ProjectStore {

    private val mutex = Mutex()
    private val mutableProjects = MutableStateFlow<List<CoverProject>>(emptyList())
    override val projects: StateFlow<List<CoverProject>> = mutableProjects.asStateFlow()

    private fun projectsDir(): File {
        val dir = File(context.filesDir, PROJECTS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun fileFor(id: String): File = File(projectsDir(), "$id.json")

    override suspend fun refresh() {
        mutableProjects.value = list()
    }

    override suspend fun list(): List<CoverProject> = withContext(Dispatchers.IO) {
        mutex.withLock {
            projectsDir()
                .listFiles { file -> file.isFile && file.extension == "json" }
                .orEmpty()
                .mapNotNull { file -> runCatching { ProjectCodec.decode(file.readText()) }.getOrNull() }
                .sortedByDescending { it.updatedAtMs }
        }
    }

    override suspend fun open(id: String): CoverProject? = withContext(Dispatchers.IO) {
        mutex.withLock { openUnlocked(id) }
    }

    override suspend fun load(id: String): CoverProject? = open(id)

    override suspend fun save(project: CoverProject): CoverProject {
        val stamped = write(project)
        refresh()
        return stamped
    }

    override suspend fun autosave(project: CoverProject): CoverProject = save(project)

    override suspend fun duplicate(id: String): CoverProject? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val original = openUnlocked(id) ?: return@withLock null
            val copy = original.copy(
                id = UUID.randomUUID().toString(),
                name = "${original.name} copy",
                updatedAtMs = clock(),
            )
            writeUnlocked(copy)
            copy
        }.also { refresh() }
    }

    override suspend fun delete(id: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val file = fileFor(id)
            if (!file.isFile) return@withLock false
            file.delete()
        }.also { refresh() }
    }

    private suspend fun write(project: CoverProject): CoverProject = withContext(Dispatchers.IO) {
        mutex.withLock {
            val stamped = project.copy(updatedAtMs = clock())
            writeUnlocked(stamped)
            stamped
        }
    }

    private fun openUnlocked(id: String): CoverProject? {
        val file = fileFor(id)
        if (!file.isFile) return null
        return runCatching { ProjectCodec.decode(file.readText()) }.getOrNull()
    }

    private fun writeUnlocked(project: CoverProject) {
        fileFor(project.id).writeText(ProjectCodec.encode(project))
    }

    private companion object {
        const val PROJECTS_DIR = "projects"
    }
}
