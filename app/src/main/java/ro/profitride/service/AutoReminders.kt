package ro.profitride.service

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import java.time.LocalDate
import java.util.Calendar
import ro.profitride.ProfitRideApp
import ro.profitride.R
import ro.profitride.core.AutoLogic
import ro.profitride.ui.MainActivity
import ro.profitride.util.AutoNames

/** Reamintiri pentru documente și întreținere. Se verifică o dată pe zi și la deschiderea aplicației. */
object AutoReminders {
    private const val CHANNEL = "auto_reminders"

    /** Programează verificarea zilnică (în jur de 09:00). Alarma rămâne și după ce aplicația e închisă din recente. */
    fun schedule(context: Context) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val first = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        val pending = PendingIntent.getBroadcast(
            context, 7, Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP, first, AlarmManager.INTERVAL_DAY, pending)
    }

    fun notificationsAllowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Trimite notificările care trebuie trimise azi și le marchează, ca să nu se repete. */
    fun check(context: Context) {
        if (!notificationsAllowed(context)) return
        val app = ProfitRideApp.instance
        val data = app.autoStore.state.value
        val notified = app.autoStore.notifiedKeys()
        val today = LocalDate.now().toEpochDay()
        val docs = AutoLogic.dueDocs(data.docs, today, notified)
        val maint = AutoLogic.dueMaintenance(data.maintenance, data.odometer, notified)
        if (docs.isEmpty() && maint.isEmpty()) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.auto_notif_channel), NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        fun post(key: String, title: String) {
            val n = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(title)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            manager.notify(key.hashCode(), n)
        }
        docs.forEach { d ->
            val name = context.getString(AutoNames.doc(d.doc.type))
            post(
                d.key,
                when {
                    d.daysLeft < 0 -> context.getString(R.string.auto_notif_doc_expired, name)
                    d.daysLeft == 0L -> context.getString(R.string.auto_notif_doc_today, name)
                    else -> context.getString(R.string.auto_notif_doc_days, name, d.daysLeft.toInt())
                },
            )
        }
        maint.forEach { m ->
            val name = context.getString(AutoNames.maint(m.item.kind))
            val km = AutoLogic.groupThousands(kotlin.math.abs(m.kmLeft))
            post(
                m.key,
                if (m.expired) context.getString(R.string.auto_notif_maint_late, name, km)
                else context.getString(R.string.auto_notif_maint_left, name, km),
            )
        }
        app.autoStore.markNotified(docs.map { it.key } + maint.map { it.key })
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) AutoReminders.schedule(context)
        AutoReminders.check(context)
    }
}
