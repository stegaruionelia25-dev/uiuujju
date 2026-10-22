package ro.profitride.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import ro.profitride.R
import ro.profitride.ui.MainActivity

/** Notificarea fixă cu butonul Pornește/Oprește. Nu conține date despre curse. */
object StatusNotification {
    private const val CHANNEL = "status"
    private const val ID = 1

    fun show(context: Context, active: Boolean) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.notif_channel), NotificationManager.IMPORTANCE_LOW),
            )
        }
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), flags)
        val toggle = PendingIntent.getBroadcast(
            context, 1,
            Intent(context, ToggleReceiver::class.java).putExtra(ToggleReceiver.EXTRA_ACTIVE, !active),
            flags,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(context.getString(if (active) R.string.notif_on_title else R.string.notif_off_title))
            .setContentText(context.getString(if (active) R.string.notif_on_text else R.string.notif_off_text))
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .addAction(0, context.getString(if (active) R.string.ctl_stop else R.string.ctl_start), toggle)
            .addAction(0, context.getString(R.string.notif_open), open)
            .build()
        manager.notify(ID, notification)
    }

    fun cancel(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(ID)
    }
}
