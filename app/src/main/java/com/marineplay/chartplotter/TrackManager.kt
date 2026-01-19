package com.marineplay.chartplotter

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.ui.graphics.Color
import com.marineplay.chartplotter.data.database.AppDatabase
import com.marineplay.chartplotter.data.database.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 항적 관리 클래스 (Room Database 기반)
 */
class TrackManager(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val trackDao = database.trackDao()
    private val trackRecordDao = database.trackRecordDao()
    private val trackPointDao = database.trackPointDao()
    private val trackSettingsDao = database.trackSettingsDao()
    
    // 기존 SharedPreferences (마이그레이션용)
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("track_prefs", Context.MODE_PRIVATE)
    private val TRACKS_KEY = "tracks"
    private val SETTINGS_KEY = "track_settings"
    private val MIGRATION_KEY = "migrated_to_room"
    
    // 항적 설정 (캐시)
    var settings: TrackSettings = TrackSettings("time", 5000L, 10.0)
        private set
    
    init {
        // 마이그레이션 체크 및 실행
        runBlocking {
            migrateFromSharedPreferencesIfNeeded()
            loadSettings()
        }
    }
    
    /**
     * SharedPreferences에서 Room으로 마이그레이션
     */
    private suspend fun migrateFromSharedPreferencesIfNeeded() {
        val migrated = sharedPreferences.getBoolean(MIGRATION_KEY, false)
        if (migrated) {
            Log.d("[TrackManager]", "이미 Room으로 마이그레이션 완료")
            return
        }
        
        try {
            Log.d("[TrackManager]", "SharedPreferences → Room 마이그레이션 시작")
            
            // TrackSettings 마이그레이션
            val settingsJson = sharedPreferences.getString(SETTINGS_KEY, null)
            if (settingsJson != null) {
                try {
                    val json = JSONObject(settingsJson)
                    val trackSettings = TrackSettingsEntity(
                        id = "default",
                        intervalType = json.getString("intervalType"),
                        timeInterval = json.getLong("timeInterval"),
                        distanceInterval = json.getDouble("distanceInterval")
                    )
                    trackSettingsDao.insertSettings(trackSettings)
                    Log.d("[TrackManager]", "설정 마이그레이션 완료")
                } catch (e: Exception) {
                    Log.e("[TrackManager]", "설정 마이그레이션 실패: ${e.message}")
                }
            }
            
            // Tracks 마이그레이션
            val tracksJson = sharedPreferences.getString(TRACKS_KEY, null)
            if (tracksJson != null) {
                try {
                    val jsonArray = JSONArray(tracksJson)
                    val trackEntities = mutableListOf<TrackEntity>()
                    val recordEntities = mutableListOf<TrackRecordEntity>()
                    val pointEntities = mutableListOf<TrackPointEntity>()
                    
                    for (i in 0 until jsonArray.length()) {
                        val trackJson = jsonArray.getJSONObject(i)
                        val trackEntity = TrackEntity(
                            id = trackJson.getString("id"),
                            name = trackJson.getString("name"),
                            colorValue = trackJson.getLong("colorValue"),
                            isVisible = trackJson.optBoolean("isVisible", true)
                        )
                        trackEntities.add(trackEntity)
                        
                        val recordsArray = trackJson.getJSONArray("records")
                        for (j in 0 until recordsArray.length()) {
                            val recordJson = recordsArray.getJSONObject(j)
                            val recordEntity = TrackRecordEntity(
                                id = recordJson.getString("id"),
                                trackId = trackEntity.id,
                                startTime = recordJson.getLong("startTime"),
                                endTime = recordJson.getLong("endTime"),
                                title = recordJson.getString("title")
                            )
                            recordEntities.add(recordEntity)
                            
                            val pointsArray = recordJson.getJSONArray("points")
                            for (k in 0 until pointsArray.length()) {
                                val pointJson = pointsArray.getJSONObject(k)
                                val pointEntity = TrackPointEntity(
                                    recordId = recordEntity.id,
                                    latitude = pointJson.getDouble("latitude"),
                                    longitude = pointJson.getDouble("longitude"),
                                    timestamp = pointJson.getLong("timestamp"),
                                    sequence = k
                                )
                                pointEntities.add(pointEntity)
                            }
                        }
                    }
                    
                    // 일괄 삽입
                    if (trackEntities.isNotEmpty()) {
                        trackDao.insertTracks(trackEntities)
                    }
                    if (recordEntities.isNotEmpty()) {
                        trackRecordDao.insertRecords(recordEntities)
                    }
                    if (pointEntities.isNotEmpty()) {
                        trackPointDao.insertPoints(pointEntities)
                    }
                    
                    Log.d("[TrackManager]", "항적 마이그레이션 완료: ${trackEntities.size}개 트랙, ${recordEntities.size}개 기록, ${pointEntities.size}개 포인트")
                } catch (e: Exception) {
                    Log.e("[TrackManager]", "항적 마이그레이션 실패: ${e.message}")
                }
            }
            
            // 마이그레이션 완료 표시
            sharedPreferences.edit().putBoolean(MIGRATION_KEY, true).apply()
            Log.d("[TrackManager]", "마이그레이션 완료")
        } catch (e: Exception) {
            Log.e("[TrackManager]", "마이그레이션 중 오류: ${e.message}")
        }
    }
    
    /**
     * 항적 목록 가져오기 (동기)
     */
    fun getTracks(): List<Track> = runBlocking {
        getTracksSuspend()
    }
    
    /**
     * 항적 목록 가져오기 (비동기)
     */
    suspend fun getTracksSuspend(): List<Track> = withContext(Dispatchers.IO) {
        val trackEntities = trackDao.getAllTracks().first()
        val tracks = mutableListOf<Track>()
        
        trackEntities.forEach { trackEntity ->
            val records = trackRecordDao.getRecordsByTrackIdSuspend(trackEntity.id)
            val trackRecords = records.map { recordEntity ->
                val points = trackPointDao.getPointsByRecordIdSuspend(recordEntity.id)
                TrackRecord(
                    id = recordEntity.id,
                    trackId = recordEntity.trackId,
                    startTime = recordEntity.startTime,
                    endTime = recordEntity.endTime,
                    points = points.map { 
                        TrackPoint(
                            latitude = it.latitude,
                            longitude = it.longitude,
                            timestamp = it.timestamp
                        )
                    },
                    title = recordEntity.title
                )
            }
            
            tracks.add(
                Track(
                    id = trackEntity.id,
                    name = trackEntity.name,
                    color = Color(trackEntity.colorValue),
                    records = trackRecords.toMutableList(),
                    isVisible = trackEntity.isVisible
                )
            )
        }
        
        tracks
    }
    
    /**
     * 항적 목록 Flow (실시간 업데이트)
     */
    fun getTracksFlow(): Flow<List<Track>> {
        return trackDao.getAllTracks().map { trackEntities ->
            trackEntities.map { trackEntity ->
                // 각 트랙의 기록과 포인트를 로드
                runBlocking {
                    val records = trackRecordDao.getRecordsByTrackIdSuspend(trackEntity.id)
                    val trackRecords = records.map { recordEntity ->
                        val points = trackPointDao.getPointsByRecordIdSuspend(recordEntity.id)
                        TrackRecord(
                            id = recordEntity.id,
                            trackId = recordEntity.trackId,
                            startTime = recordEntity.startTime,
                            endTime = recordEntity.endTime,
                            points = points.map { 
                                TrackPoint(
                                    latitude = it.latitude,
                                    longitude = it.longitude,
                                    timestamp = it.timestamp
                                )
                            },
                            title = recordEntity.title
                        )
                    }
                    
                    Track(
                        id = trackEntity.id,
                        name = trackEntity.name,
                        color = Color(trackEntity.colorValue),
                        records = trackRecords.toMutableList(),
                        isVisible = trackEntity.isVisible
                    )
                }
            }
        }
    }
    
    /**
     * 항적 추가
     */
    fun addTrack(name: String, color: Color): Track {
        val track = Track(
            id = UUID.randomUUID().toString(),
            name = name,
            color = color
        )
        
        runBlocking {
            trackDao.insertTrack(
                TrackEntity(
                    id = track.id,
                    name = track.name,
                    colorValue = track.color.value,
                    isVisible = track.isVisible
                )
            )
        }
        
        return track
    }
    
    /**
     * 항적 삭제
     */
    fun deleteTrack(trackId: String): Boolean {
        return runBlocking {
            try {
                trackDao.deleteTrackById(trackId)
                true
            } catch (e: Exception) {
                Log.e("[TrackManager]", "항적 삭제 실패: ${e.message}")
                false
            }
        }
    }
    
    /**
     * 항적 기록 추가
     */
    fun addTrackRecord(trackId: String, startTime: Long, endTime: Long, points: List<TrackPoint>): TrackRecord? {
        return runBlocking {
            try {
                // Track 존재 확인
                val track = trackDao.getTrackById(trackId) ?: return@runBlocking null
                
                val recordId = UUID.randomUUID().toString()
                val record = TrackRecord(
                    id = recordId,
                    trackId = trackId,
                    startTime = startTime,
                    endTime = endTime,
                    points = points,
                    title = TrackRecord.generateTitle(startTime, endTime)
                )
                
                // Record 저장
                trackRecordDao.insertRecord(
                    TrackRecordEntity(
                        id = record.id,
                        trackId = record.trackId,
                        startTime = record.startTime,
                        endTime = record.endTime,
                        title = record.title
                    )
                )
                
                // Points 저장
                val pointEntities = points.mapIndexed { index, point ->
                    TrackPointEntity(
                        recordId = recordId,
                        latitude = point.latitude,
                        longitude = point.longitude,
                        timestamp = point.timestamp,
                        sequence = index
                    )
                }
                trackPointDao.insertPoints(pointEntities)
                
                record
            } catch (e: Exception) {
                Log.e("[TrackManager]", "항적 기록 추가 실패: ${e.message}")
                null
            }
        }
    }
    
    /**
     * 항적 기록 삭제
     */
    fun deleteTrackRecord(trackId: String, recordId: String): Boolean {
        return runBlocking {
            try {
                trackRecordDao.deleteRecordById(recordId)
                true
            } catch (e: Exception) {
                Log.e("[TrackManager]", "항적 기록 삭제 실패: ${e.message}")
                false
            }
        }
    }
    
    /**
     * 항적 표시/숨김 설정
     */
    fun setTrackVisibility(trackId: String, isVisible: Boolean) {
        runBlocking {
            trackDao.updateTrackVisibility(trackId, isVisible)
        }
    }
    
    /**
     * 항적 기록 표시/숨김 설정 (개별 항적 기록)
     */
    fun setTrackRecordVisibility(trackId: String, recordId: String, isVisible: Boolean) {
        // 개별 기록 표시는 나중에 구현 가능
    }
    
    /**
     * 설정 저장
     */
    fun saveSettings(newSettings: TrackSettings) {
        settings = newSettings
        runBlocking {
            trackSettingsDao.insertSettings(
                TrackSettingsEntity(
                    id = "default",
                    intervalType = newSettings.intervalType,
                    timeInterval = newSettings.timeInterval,
                    distanceInterval = newSettings.distanceInterval
                )
            )
        }
    }
    
    /**
     * 설정 로드
     */
    private suspend fun loadSettings() {
        try {
            val settingsEntity = trackSettingsDao.getSettingsSuspend()
            if (settingsEntity != null) {
                settings = TrackSettings(
                    intervalType = settingsEntity.intervalType,
                    timeInterval = settingsEntity.timeInterval,
                    distanceInterval = settingsEntity.distanceInterval
                )
            }
        } catch (e: Exception) {
            Log.e("[TrackManager]", "설정 로드 실패: ${e.message}")
        }
    }
}
