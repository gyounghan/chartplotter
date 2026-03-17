package com.kumhomarine.chartplotter.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.kumhomarine.chartplotter.MainActivity
import com.kumhomarine.chartplotter.R

/**
 * AIS Foreground Service
 * 백그라운드에서도 AIS 수신을 유지하기 위한 경량 서비스.
 * 실제 AIS 처리(USB, 파싱)는 Application 싱글톤에서 수행되며,
 * 이 서비스는 프로세스 유지만 담당하여 성능 영향 최소화.
 */
class AISForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        Log.d("[AISForegroundService]", "Foreground Service 시작 - AIS 백그라운드 수신 유지")
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.ais_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.ais_service_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.ais_service_title))
            .setContentText(getString(R.string.ais_service_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "ais_foreground_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.kumhomarine.chartplotter.AIS_SERVICE_START"
        const val ACTION_STOP = "com.kumhomarine.chartplotter.AIS_SERVICE_STOP"
    }
}
