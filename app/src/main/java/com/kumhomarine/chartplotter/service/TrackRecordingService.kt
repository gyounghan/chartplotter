package com.kumhomarine.chartplotter.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.kumhomarine.chartplotter.ChartPlotterApplication
import com.kumhomarine.chartplotter.MainActivity
import com.kumhomarine.chartplotter.R

/**
 * 차트플로터 항적 백그라운드 기록 Foreground Service
 * 기록을 켠 항적만 백그라운드에서도 계속 저장
 * 항해일지 VoyageRecordingService와 동일 패턴
 */
class TrackRecordingService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    private val app by lazy { application as ChartPlotterApplication }
    private val trackRecordingManager by lazy { app.trackRecordingManager }
    private val handler = Handler(Looper.getMainLooper())
    private var lastLatitude: Double = 0.0
    private var lastLongitude: Double = 0.0
    private var hasValidLocation = false
    private var timerRunnable: Runnable? = null
    private var locationCallback: LocationCallback? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "항적 백그라운드 기록 시작")
        startLocationUpdates()
        startTimeIntervalTimer()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimeIntervalTimer()
        locationCallback?.let { callback ->
            LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(callback)
        }
        locationCallback = null
        Log.d(TAG, "항적 백그라운드 기록 종료")
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        val client = LocationServices.getFusedLocationProviderClient(this)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L)
            .setWaitForAccurateLocation(false)
            .build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    lastLatitude = loc.latitude
                    lastLongitude = loc.longitude
                    hasValidLocation = true
                    trackRecordingManager.onLocationUpdate(loc.latitude, loc.longitude, false)
                }
            }
        }
        client.requestLocationUpdates(request, locationCallback!!, mainLooper)
    }

    /**
     * 시간 간격 기준 항적을 위한 주기적 타이머
     * 가장 짧은 timeInterval을 사용하여 마지막 위치로 포인트 추가
     */
    private fun startTimeIntervalTimer() {
        stopTimeIntervalTimer()
        kotlinx.coroutines.runBlocking {
            val recordingTracks = app.trackRepository.getRecordingTracks()
            val timeIntervalTracks = recordingTracks.filter { it.intervalType == "time" }
            if (timeIntervalTracks.isEmpty()) return@runBlocking

            val minInterval = timeIntervalTracks.minOfOrNull { it.timeInterval } ?: 5000L

            timerRunnable = object : Runnable {
                override fun run() {
                    if (hasValidLocation) {
                        trackRecordingManager.onLocationUpdate(lastLatitude, lastLongitude, true)
                    }
                    handler.postDelayed(this, minInterval)
                }
            }
            handler.postDelayed(timerRunnable!!, minInterval)
            Log.d(TAG, "시간 간격 타이머 시작: ${minInterval}ms")
        }
    }

    private fun stopTimeIntervalTimer() {
        timerRunnable?.let { handler.removeCallbacks(it) }
        timerRunnable = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.track_recording_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.track_recording_service_channel_desc)
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
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
            .setContentTitle(getString(R.string.track_recording_service_title))
            .setContentText(getString(R.string.track_recording_service_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val TAG = "TrackRecordingService"
        private const val CHANNEL_ID = "track_recording_channel"
        private const val NOTIFICATION_ID = 2001

        const val ACTION_START = "com.kumhomarine.chartplotter.TRACK_RECORDING_START"
        const val ACTION_STOP = "com.kumhomarine.chartplotter.TRACK_RECORDING_STOP"
    }
}
