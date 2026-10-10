package com.gexetr.lumintralightplanner.ui.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * The receiver `AndroidManifest.xml` declares for `android.appwidget.action.APPWIDGET_UPDATE`, and the
 * only entry point the platform has into the night glance.
 *
 * It carries no state of its own. The tile is renderable from the persisted snapshot at any moment, so
 * the receiver has nothing to restore, nothing to fetch and nothing to schedule: the platform's own
 * update request after a reboot is enough to redraw it, which is why no `BOOT_COMPLETED` receiver of
 * ours exists for the widget and no such permission is declared.
 *
 * It lives in its own file rather than beside the widget because the capability names it by path
 * (`widget/NightGlanceWidgetReceiver.kt`), so a reader looking for what the manifest declares finds
 * the class rather than a second class hidden in the widget's own file.
 */
class NightGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NightGlanceWidget()
}
