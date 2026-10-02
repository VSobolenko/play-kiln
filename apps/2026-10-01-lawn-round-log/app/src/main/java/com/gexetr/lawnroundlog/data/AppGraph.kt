package com.gexetr.lawnroundlog.data

import android.content.Context
import java.io.File

/**
 * The app's two stores, built once and read from anywhere.
 *
 * There is no dependency-injection framework here and none is warranted for two objects: the whole
 * graph is the record file and the preferences file. There is deliberately also no `Application`
 * subclass and no `ContentProvider` — the code that needs the stores is the activity, and an
 * initialiser that runs before the activity is a startup cost and a crash surface paid for nothing.
 *
 * [init] is idempotent. It is called from `MainActivity.onCreate`, which runs again after a
 * configuration change and after the process is recreated, and asserting the stores exist on every
 * one of those calls would crash the app on a rotation. An already-initialised graph is left
 * exactly as it is, so the same [RoundStore] instance keeps serving the same file.
 *
 * The application context is what is kept, never the activity's: a store that holds an activity
 * context outlives the activity it was created from.
 */
object AppGraph {

    private const val ROUND_DOCUMENT_FILE_NAME = "round-log.json"

    lateinit var round: RoundStore

    lateinit var settings: SettingsStore

    /** Whether [init] has completed, so callers can say so instead of catching a `lateinit` error. */
    val isReady: Boolean
        get() = this::round.isInitialized && this::settings.isInitialized

    @Synchronized
    fun init(context: Context) {
        if (isReady) return
        val applicationContext = context.applicationContext
        round = RoundStore(File(applicationContext.filesDir, ROUND_DOCUMENT_FILE_NAME))
        settings = SettingsStore(applicationContext)
    }
}
