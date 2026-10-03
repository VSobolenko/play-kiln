package com.gexetr.laneset.data

import android.content.Context

/**
 * The app's stores and its billing manager, built once and read from anywhere.
 *
 * There is no dependency-injection framework here and none is warranted for four objects: the whole
 * graph is three DataStore files and one `BillingClient`. There is deliberately also no `Application`
 * subclass and no `ContentProvider` — the code that needs the graph is the activity, and an
 * initialiser that runs before the activity is a startup cost and a crash surface paid for nothing.
 * This is the shape the factory's other slots already use, so it is the shape a reader will
 * recognise; a second container pattern invented per slot is a second thing to learn.
 *
 * [init] is idempotent. It is called from the host's `onCreate`, which runs again after a
 * configuration change and after the process is recreated, and asserting the stores exist on every
 * one of those calls would crash the app on a rotation. An already-initialised graph is left exactly
 * as it is, so the same store instances keep serving the same files.
 *
 * The application context is what is kept, never the activity's: a store that holds an activity
 * context outlives the activity it was created from.
 *
 * **The billing lifecycle is not started here.** [billing] is constructed, not connected: the host
 * calls `connect()` when the app comes to the foreground and `disconnect()` when it leaves, which
 * keeps a `BillingClient` from being opened for a process that is only showing a notification.
 * Constructing the client is local and free; connecting it talks to the Play Store.
 *
 * Note that `DataStoreSettingsStore` built here and one built by a `ViewModel`'s factory are two
 * wrappers over **one** DataStore: `preferencesDataStore` is a property delegate that caches a single
 * instance per file, so the file has one writer and the two wrappers cannot disagree about it.
 */
object AppGraph {

    lateinit var settings: SettingsStore
        private set

    lateinit var pace: PaceStore
        private set

    lateinit var session: SessionStore
        private set

    lateinit var billing: BillingManager
        private set

    /** Whether [init] has completed, so a caller can say so instead of catching a `lateinit` error. */
    val isReady: Boolean
        get() = this::settings.isInitialized &&
            this::pace.isInitialized &&
            this::session.isInitialized &&
            this::billing.isInitialized

    /**
     * Builds the graph for this process. Idempotent, and safe to call on every `onCreate`.
     *
     * @param context any context; the application context is what is kept.
     */
    @Synchronized
    fun init(context: Context) {
        if (isReady) return
        val applicationContext = context.applicationContext
        settings = DataStoreSettingsStore(applicationContext)
        pace = DataStorePaceStore(applicationContext)
        session = DataStoreSessionStore(applicationContext)
        billing = PlayBillingManager(applicationContext)
    }
}
