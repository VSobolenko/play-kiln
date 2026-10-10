package com.gexetr.lumintralightplanner

import android.app.Application
import android.content.ComponentCallbacks2
import com.gexetr.lumintralightplanner.ui.night.SceneMemoryPressure

/**
 * Process-wide entry for memory pressure.
 *
 * The night scene keeps large decoded bitmaps (Milky Way WebP, sky LUT raster strips). When the
 * system asks via [onTrimMemory], those caches are dropped through [SceneMemoryPressure] rather than
 * left until the process is killed. The Activity also registers the same callback so a host that
 * never receives Application callbacks still releases.
 */
class LumintraApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        SceneMemoryPressure.install(this)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        SceneMemoryPressure.onTrimMemory(level)
    }
}
