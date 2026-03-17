package com.kumhomarine.chartplotter.alerts

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
import com.kumhomarine.chartplotter.MainActivity
import com.kumhomarine.chartplotter.R

class AISAlertNotifier(
    private val context: Context
) {
    private val lastNotifiedAt = mutableMapOf<String, Long>()

    fun notify(payload: AISAlertPayload) {
        if (!canNotify()) return
        if (!shouldNotify(payload)) return

        ensureChannel()
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = when (payload.type) {
            AISAlertContract.TYPE_CPA_WARNING -> "CPA 경보"
            AISAlertContract.TYPE_GUARD_INTRUSION -> "경계 침범"
            AISAlertContract.TYPE_FAVORITE -> "즐겨찾기 알림"
            else -> "AIS 경보"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText("${payload.vesselName} (${payload.mmsi}) - ${payload.message}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(getNotificationId(payload), notification)
        lastNotifiedAt["${payload.type}:${payload.mmsi}"] = payload.timestamp
    }

    private fun shouldNotify(payload: AISAlertPayload): Boolean {
        val key = "${payload.type}:${payload.mmsi}"
        val last = lastNotifiedAt[key] ?: return true
        return (payload.timestamp - last) >= 60_000L
    }

    private fun getNotificationId(payload: AISAlertPayload): Int {
        return ("${payload.type}:${payload.mmsi}").hashCode()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AIS Alert",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "AIS CPA/경계/즐겨찾기 경보"
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun canNotify(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val CHANNEL_ID = "ais_alert_channel"
    }
}

