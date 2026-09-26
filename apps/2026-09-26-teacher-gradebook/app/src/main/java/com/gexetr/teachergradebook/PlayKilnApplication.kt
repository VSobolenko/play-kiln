package com.gexetr.teachergradebook

import android.app.Application
import com.gexetr.teachergradebook.monetization.PlayKilnContainer

/**
 * The process-level owner of the app's dependencies.
 *
 * The container is built here rather than in the activity so the record and the billing connection
 * outlive an activity restart: a rotation, a split-screen resize, or a system-initiated activity
 * recreation must not reconnect to Play or re-read the record from disk.
 */
class PlayKilnApplication : Application() {

    lateinit var container: PlayKilnContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = PlayKilnContainer(this)
        container.start()
    }

    override fun onTerminate() {
        container.stop()
        super.onTerminate()
    }
}
