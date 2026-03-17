package com.kumhomarine.chartplotter

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import com.kumhomarine.chartplotter.data.datasources.AISDataSource
import com.kumhomarine.chartplotter.data.datasources.AISDataSourceImpl
import com.kumhomarine.chartplotter.data.datasources.TrackLocalDataSource
import com.kumhomarine.chartplotter.data.repositories.AISRepositoryImpl
import com.kumhomarine.chartplotter.data.repositories.TrackRepositoryImpl
import com.kumhomarine.chartplotter.domain.repositories.AISRepository
import com.kumhomarine.chartplotter.domain.repositories.TrackRepository
import com.kumhomarine.chartplotter.recorder.TrackRecordingManager
import com.kumhomarine.chartplotter.service.AISForegroundService
import com.kumhomarine.chartplotter.service.TrackRecordingService
import kotlinx.coroutines.runBlocking

/**
 * ChartPlotter Application
 * 앱 전체에서 AIS 데이터를 유지하기 위해 싱글톤 AIS Repository/DataSource를 관리합니다.
 * - 앱 시작 시 AIS 자동 연결
 * - 백그라운드에서도 AIS 수신 유지 (Foreground Service)
 * - 화면 전환 시에도 AIS 연결 유지
 * - 모든 화면(차트, AIS 등)에서 동일한 AIS 데이터 공유
 * - 항적 기록: 기록을 켠 항적은 백그라운드에서도 계속 저장 (TrackRecordingService)
 */
class ChartPlotterApplication : Application() {

    private var _aisDataSource: AISDataSource? = null
    private var _aisRepository: AISRepository? = null
    private var _trackLocalDataSource: TrackLocalDataSource? = null
    private var _trackRepository: TrackRepository? = null
    private var _trackRecordingManager: TrackRecordingManager? = null

    val aisDataSource: AISDataSource
        get() = _aisDataSource
            ?: AISDataSourceImpl(this).also {
                _aisDataSource = it
                Log.d("[ChartPlotterApplication]", "AISDataSource 싱글톤 생성")
            }

    val aisRepository: AISRepository
        get() = _aisRepository
            ?: AISRepositoryImpl(aisDataSource, this).also {
                _aisRepository = it
                Log.d("[ChartPlotterApplication]", "AISRepository 싱글톤 생성")
            }

    val trackLocalDataSource: TrackLocalDataSource
        get() = _trackLocalDataSource
            ?: TrackLocalDataSource(this).also {
                _trackLocalDataSource = it
            }

    val trackRepository: TrackRepository
        get() = _trackRepository
            ?: TrackRepositoryImpl(trackLocalDataSource).also {
                _trackRepository = it
            }

    val trackRecordingManager: TrackRecordingManager
        get() = _trackRecordingManager
            ?: TrackRecordingManager(this).also {
                _trackRecordingManager = it
                Log.d("[ChartPlotterApplication]", "TrackRecordingManager 싱글톤 생성")
            }

    override fun onCreate() {
        super.onCreate()
        // 앱 시작 시 AIS 연결 시도 (싱글톤 초기화 및 연결)
        aisRepository.connect(38400)
        Log.d("[ChartPlotterApplication]", "앱 시작 - AIS 자동 연결 시도")

        // Foreground Service 시작: 백그라운드에서도 AIS 수신 유지
        startAISForegroundService()

        // 기록 중인 항적이 있으면 항적 백그라운드 기록 서비스 시작
        startTrackRecordingServiceIfNeeded()
    }

    /**
     * 기록 중인 항적이 있으면 TrackRecordingService 시작
     * (앱 재시작 시 이전에 기록 중이던 항적 복원)
     */
    fun startTrackRecordingServiceIfNeeded() {
        runBlocking {
            val recordingTracks = trackRepository.getRecordingTracks()
            if (recordingTracks.isNotEmpty()) {
                val intent = Intent(this@ChartPlotterApplication, TrackRecordingService::class.java).apply {
                    action = TrackRecordingService.ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                Log.d("[ChartPlotterApplication]", "항적 백그라운드 기록 서비스 시작 (기록 중: ${recordingTracks.size}개)")
            }
        }
    }

    /**
     * 기록 중인 항적이 없으면 TrackRecordingService 종료
     */
    fun stopTrackRecordingServiceIfNeeded() {
        runBlocking {
            val recordingTracks = trackRepository.getRecordingTracks()
            if (recordingTracks.isEmpty()) {
                stopService(Intent(this@ChartPlotterApplication, TrackRecordingService::class.java))
                Log.d("[ChartPlotterApplication]", "항적 백그라운드 기록 서비스 종료")
            }
        }
    }

    /**
     * AIS Foreground Service 시작
     * 경량 서비스로 프로세스만 유지하여 성능 영향 최소화
     */
    private fun startAISForegroundService() {
        val intent = Intent(this, AISForegroundService::class.java).apply {
            action = AISForegroundService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Log.d("[ChartPlotterApplication]", "AIS Foreground Service 시작 - 백그라운드 수신 유지")
    }
}
