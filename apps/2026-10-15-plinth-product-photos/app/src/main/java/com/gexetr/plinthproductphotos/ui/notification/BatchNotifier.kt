package com.gexetr.plinthproductphotos.ui.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.gexetr.plinthproductphotos.MainActivity
import com.gexetr.plinthproductphotos.R

object BatchNotifier {
    const val CHANNEL_ID = "plinth_batch_complete"
    private const val PREFERENCE_FILE = "plinth_batch_notifications"
    private const val PERMISSION_ASKED = "permission_asked"
    private const val ENABLED = "enabled"

    fun notificationsEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE).getBoolean(ENABLED, true)

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        check(
            context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(ENABLED, enabled)
                .commit(),
        ) { "Could not save batch notification preference" }
    }

    fun shouldAskPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED &&
            !context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE)
                .getBoolean(PERMISSION_ASKED, false)

    fun markPermissionAsked(context: Context) {
        check(
            context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(PERMISSION_ASKED, true)
                .commit(),
        ) { "Could not save batch notification permission state" }
    }

    fun postCompletion(context: Context, batchId: String, completed: Int, failed: Int) {
        if (!notificationsEnabled(context)) return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.batch_notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = context.getString(R.string.batch_notification_channel_description)
                },
            )
        }
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(BATCH_ID_EXTRA, batchId)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context,
            batchId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_lucide_image)
            .setContentTitle(context.getString(R.string.batch_notification_title))
            .setContentText(context.getString(R.string.batch_notification_message, completed, failed))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.batch_notification_message, completed, failed)),
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(batchId.hashCode(), notification)
    }

    private const val BATCH_ID_EXTRA = "com.gexetr.plinthproductphotos.extra.BATCH_ID"
}
