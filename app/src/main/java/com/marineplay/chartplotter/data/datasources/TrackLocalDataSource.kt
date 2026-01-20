package com.marineplay.chartplotter.data.datasources

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.ui.graphics.Color
import com.marineplay.chartplotter.data.database.AppDatabase
import com.marineplay.chartplotter.data.database.entities.*
import com.marineplay.chartplotter.domain.entities.Track
import com.marineplay.chartplotter.domain.entities.TrackPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

/**
 * 항적 로컬 데이터 소스 (Room Database 기반)
 * TrackPoint를 Track에 직접 저장 (TrackRecord 제거)
 */
class TrackLocalDataSource(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val trackDao = database.trackDao()
    private val trackPointDao = database.trackPointDao()
    
    // 기존 SharedPreferences (마이그레이션용)
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("track_prefs", Context.MODE_PRIVATE)
    private val TRACKS_KEY = "tracks"
    private val MIGRATION_KEY = "migrated_to_room_v4" // 버전 4로 변경
    
    init {
        // 마이그레이션 체크 및 실행
        runBlocking {
            migrateFromSharedPreferencesIfNeeded()
        }
    }
    
    /**
     * SharedPreferences에서 Room으로 마이그레이션 (TrackRecord -> TrackPoint 직접 저장)
     */
    private suspend fun migrateFromSharedPreferencesIfNeeded() {
        val migrated = sharedPreferences.getBoolean(MIGRATION_KEY, false)
        if (migrated) {
            Log.d("[TrackLocalDataSource]", "이미 Room v4로 마이그레이션 완료")
            return
        }
        
        try {
            Log.d("[TrackLocalDataSource]", "SharedPreferences → Room v4 마이그레이션 시작")
            
            // Tracks 마이그레이션
            val tracksJson = sharedPreferences.getString(TRACKS_KEY, null)
            if (tracksJson != null) {
                try {
                    val jsonArray = JSONArray(tracksJson)
                    val trackEntities = mutableListOf<TrackEntity>()
                    val pointEntities = mutableListOf<TrackPointEntity>()
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    
                    for (i in 0 until jsonArray.length()) {
                        val trackJson = jsonArray.getJSONObject(i)
                        val trackEntity = TrackEntity(
                            id = trackJson.getString("id"),
                            name = trackJson.getString("name"),
                            colorValue = trackJson.getLong("colorValue"),
                            isVisible = trackJson.optBoolean("isVisible", true),
                            intervalType = trackJson.optString("intervalType", "time"),
                            timeInterval = trackJson.optLong("timeInterval", 5000L),
                            distanceInterval = trackJson.optDouble("distanceInterval", 10.0),
                            isRecording = trackJson.optBoolean("isRecording", false)
                        )
                        trackEntities.add(trackEntity)
                        
                        // TrackRecord를 거치지 않고 TrackPoint를 직접 저장
                        val recordsArray = trackJson.getJSONArray("records")
                        for (j in 0 until recordsArray.length()) {
                            val recordJson = recordsArray.getJSONObject(j)
                            val pointsArray = recordJson.getJSONArray("points")
                            
                            for (k in 0 until pointsArray.length()) {
                                val pointJson = pointsArray.getJSONObject(k)
                                val timestamp = pointJson.getLong("timestamp")
                                val date = dateFormat.format(java.util.Date(timestamp))
                                
                                val pointEntity = TrackPointEntity(
                                    trackId = trackEntity.id, // recordId 대신 trackId 직접 사용
                                    latitude = pointJson.getDouble("latitude"),
                                    longitude = pointJson.getDouble("longitude"),
                                    timestamp = timestamp,
                                    date = date
                                )
                                pointEntities.add(pointEntity)
                            }
                        }
                    }
                    
                    // 일괄 삽입
                    if (trackEntities.isNotEmpty()) {
                        trackDao.insertTracks(trackEntities)
                    }
                    if (pointEntities.isNotEmpty()) {
                        trackPointDao.insertPoints(pointEntities)
                    }
                    
                    Log.d("[TrackLocalDataSource]", "항적 마이그레이션 완료: ${trackEntities.size}개 트랙, ${pointEntities.size}개 포인트")
                } catch (e: Exception) {
                    Log.e("[TrackLocalDataSource]", "항적 마이그레이션 실패: ${e.message}")
                }
            }
            
            // 마이그레이션 완료 표시
            sharedPreferences.edit().putBoolean(MIGRATION_KEY, true).apply()
            Log.d("[TrackLocalDataSource]", "마이그레이션 완료")
        } catch (e: Exception) {
            Log.e("[TrackLocalDataSource]", "마이그레이션 중 오류: ${e.message}")
        }
    }
    
    /**
     * 항적 목록 가져오기 (비동기)
     */
    suspend fun getTracksSuspend(): List<Track> = withContext(Dispatchers.IO) {
        val trackEntities = trackDao.getAllTracks().first()
        val tracks = mutableListOf<Track>()
        
        trackEntities.forEach { trackEntity ->
            // TrackPoint를 직접 로드
            val pointEntities = trackPointDao.getPointsByTrackId(trackEntity.id)
            val points = pointEntities.map { 
                TrackPoint(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    timestamp = it.timestamp
                )
            }
            
            tracks.add(
                Track(
                    id = trackEntity.id,
                    name = trackEntity.name,
                    color = Color(trackEntity.colorValue),
                    points = points.toMutableList(),
                    isVisible = trackEntity.isVisible,
                    intervalType = trackEntity.intervalType,
                    timeInterval = trackEntity.timeInterval,
                    distanceInterval = trackEntity.distanceInterval,
                    isRecording = trackEntity.isRecording
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
                // 각 트랙의 포인트를 로드
                runBlocking {
                    val pointEntities = trackPointDao.getPointsByTrackId(trackEntity.id)
                    val points = pointEntities.map { 
                        TrackPoint(
                            latitude = it.latitude,
                            longitude = it.longitude,
                            timestamp = it.timestamp
                        )
                    }
                    
                    Track(
                        id = trackEntity.id,
                        name = trackEntity.name,
                        color = Color(trackEntity.colorValue),
                        points = points.toMutableList(),
                        isVisible = trackEntity.isVisible,
                        intervalType = trackEntity.intervalType,
                        timeInterval = trackEntity.timeInterval,
                        distanceInterval = trackEntity.distanceInterval,
                        isRecording = trackEntity.isRecording
                    )
                }
            }
        }
    }
    
    /**
     * 항적 추가
     */
    suspend fun addTrack(
        name: String, 
        color: Color,
        intervalType: String = "time",
        timeInterval: Long = 5000L,
        distanceInterval: Double = 10.0
    ): Track {
        val trackId = UUID.randomUUID().toString()
        
        trackDao.insertTrack(
            TrackEntity(
                id = trackId,
                name = name,
                colorValue = color.value.toLong(),
                isVisible = true,
                intervalType = intervalType,
                timeInterval = timeInterval,
                distanceInterval = distanceInterval,
                isRecording = false
            )
        )
        
        return Track(
            id = trackId,
            name = name,
            color = color,
            intervalType = intervalType,
            timeInterval = timeInterval,
            distanceInterval = distanceInterval,
            isRecording = false
        )
    }
    
    /**
     * 항적 설정 업데이트
     */
    suspend fun updateTrackSettings(
        trackId: String,
        intervalType: String? = null,
        timeInterval: Long? = null,
        distanceInterval: Double? = null
    ): Boolean {
        return try {
            val track = trackDao.getTrackById(trackId) ?: return false
            trackDao.updateTrackSettings(
                trackId = trackId,
                intervalType = intervalType ?: track.intervalType,
                timeInterval = timeInterval ?: track.timeInterval,
                distanceInterval = distanceInterval ?: track.distanceInterval
            )
            true
        } catch (e: Exception) {
            Log.e("[TrackLocalDataSource]", "항적 설정 업데이트 실패: ${e.message}")
            false
        }
    }
    
    /**
     * 항적 기록 상태 업데이트
     */
    suspend fun setTrackRecording(trackId: String, isRecording: Boolean): Boolean {
        return try {
            trackDao.updateTrackRecording(trackId, isRecording)
            true
        } catch (e: Exception) {
            Log.e("[TrackLocalDataSource]", "항적 기록 상태 업데이트 실패: ${e.message}")
            false
        }
    }
    
    /**
     * 현재 기록 중인 항적 목록 가져오기
     */
    suspend fun getRecordingTracks(): List<Track> {
        val recordingTrackEntities = trackDao.getRecordingTracks()
        return recordingTrackEntities.map { trackEntity ->
            val pointEntities = trackPointDao.getPointsByTrackId(trackEntity.id)
            val points = pointEntities.map { 
                TrackPoint(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    timestamp = it.timestamp
                )
            }
            
            Track(
                id = trackEntity.id,
                name = trackEntity.name,
                color = Color(trackEntity.colorValue),
                points = points.toMutableList(),
                isVisible = trackEntity.isVisible,
                intervalType = trackEntity.intervalType,
                timeInterval = trackEntity.timeInterval,
                distanceInterval = trackEntity.distanceInterval,
                isRecording = trackEntity.isRecording
            )
        }
    }
    
    /**
     * 항적 삭제
     */
    suspend fun deleteTrack(trackId: String): Boolean {
        return try {
            trackDao.deleteTrackById(trackId)
            true
        } catch (e: Exception) {
            Log.e("[TrackLocalDataSource]", "항적 삭제 실패: ${e.message}")
            false
        }
    }
    
    /**
     * TrackPoint 실시간 저장 (앱 종료 시에도 데이터 손실 없음)
     */
    suspend fun addTrackPoint(trackId: String, point: TrackPoint): Boolean {
        return try {
            // Track 존재 확인
            val track = trackDao.getTrackById(trackId) ?: return false
            
            // 날짜 계산 (yyyy-MM-dd 형식)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = dateFormat.format(java.util.Date(point.timestamp))
            
            // TrackPoint 저장
            val pointEntity = TrackPointEntity(
                trackId = trackId,
                latitude = point.latitude,
                longitude = point.longitude,
                timestamp = point.timestamp,
                date = date
            )
            trackPointDao.insertPoint(pointEntity)
            
            true
        } catch (e: Exception) {
            Log.e("[TrackLocalDataSource]", "TrackPoint 저장 실패: ${e.message}")
            false
        }
    }
    
    /**
     * TrackPoint 일괄 저장
     */
    suspend fun addTrackPoints(trackId: String, points: List<TrackPoint>): Boolean {
        return try {
            // Track 존재 확인
            val track = trackDao.getTrackById(trackId) ?: return false
            
            // 날짜 계산 (yyyy-MM-dd 형식)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val pointEntities = points.map { point ->
                TrackPointEntity(
                    trackId = trackId,
                    latitude = point.latitude,
                    longitude = point.longitude,
                    timestamp = point.timestamp,
                    date = dateFormat.format(java.util.Date(point.timestamp))
                )
            }
            trackPointDao.insertPoints(pointEntities)
            
            true
        } catch (e: Exception) {
            Log.e("[TrackLocalDataSource]", "TrackPoint 일괄 저장 실패: ${e.message}")
            false
        }
    }
    
    /**
     * 항적 표시/숨김 설정
     */
    suspend fun setTrackVisibility(trackId: String, isVisible: Boolean) {
        trackDao.updateTrackVisibility(trackId, isVisible)
    }
    
    /**
     * 항적의 기록 상태 확인
     */
    suspend fun isTrackRecording(trackId: String): Boolean {
        return try {
            val track = trackDao.getTrackById(trackId)
            track?.isRecording ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 특정 항적의 포인트 가져오기
     */
    suspend fun getTrackPoints(trackId: String): List<TrackPoint> {
        return try {
            val pointEntities = trackPointDao.getPointsByTrackId(trackId)
            pointEntities.map { 
                TrackPoint(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    timestamp = it.timestamp
                )
            }
        } catch (e: Exception) {
            Log.e("[TrackLocalDataSource]", "항적 포인트 가져오기 실패: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * 특정 항적의 날짜별 포인트 가져오기
     */
    suspend fun getTrackPointsByDate(trackId: String, date: String): List<TrackPoint> {
        return try {
            val pointEntities = trackPointDao.getPointsByTrackIdAndDate(trackId, date)
            pointEntities.map { 
                TrackPoint(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    timestamp = it.timestamp
                )
            }
        } catch (e: Exception) {
            Log.e("[TrackLocalDataSource]", "날짜별 항적 포인트 가져오기 실패: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * 날짜별 포인트 가져오기 (모든 항적)
     */
    suspend fun getPointsByDate(date: String): List<Pair<String, TrackPoint>> {
        return try {
            val pointEntities = trackPointDao.getPointsByDate(date)
            pointEntities.map { 
                Pair(it.trackId, TrackPoint(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    timestamp = it.timestamp
                ))
            }
        } catch (e: Exception) {
            Log.e("[TrackLocalDataSource]", "날짜별 포인트 가져오기 실패: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * 특정 항적의 시간 범위별 포인트 가져오기
     */
    suspend fun getTrackPointsByTimeRange(trackId: String, startTime: Long, endTime: Long): List<TrackPoint> {
        return try {
            val pointEntities = trackPointDao.getPointsByTrackIdAndTimeRange(trackId, startTime, endTime)
            pointEntities.map { 
                TrackPoint(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    timestamp = it.timestamp
                )
            }
        } catch (e: Exception) {
            Log.e("[TrackLocalDataSource]", "시간 범위별 항적 포인트 가져오기 실패: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * 특정 항적의 최근 N개 포인트 가져오기
     */
    suspend fun getRecentTrackPoints(trackId: String, limit: Int = 2000): List<TrackPoint> {
        return try {
            val pointEntities = trackPointDao.getRecentPointsByTrackId(trackId, limit)
            pointEntities.reversed().map { // 최신순이므로 역순으로 정렬
                TrackPoint(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    timestamp = it.timestamp
                )
            }
        } catch (e: Exception) {
            Log.e("[TrackLocalDataSource]", "최근 항적 포인트 가져오기 실패: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * 특정 항적의 포인트 삭제
     */
    suspend fun deleteTrackPoints(trackId: String): Boolean {
        return try {
            trackPointDao.deletePointsByTrackId(trackId)
            true
        } catch (e: Exception) {
            Log.e("[TrackLocalDataSource]", "항적 포인트 삭제 실패: ${e.message}")
            false
        }
    }
    
    /**
     * 특정 항적의 날짜별 포인트 삭제
     */
    suspend fun deleteTrackPointsByDate(trackId: String, date: String): Boolean {
        return try {
            trackPointDao.deletePointsByTrackIdAndDate(trackId, date)
            true
        } catch (e: Exception) {
            Log.e("[TrackLocalDataSource]", "날짜별 항적 포인트 삭제 실패: ${e.message}")
            false
        }
    }
    
    /**
     * 특정 항적의 시간 범위별 포인트 삭제
     */
    suspend fun deleteTrackPointsByTimeRange(trackId: String, startTime: Long, endTime: Long): Boolean {
        return try {
            trackPointDao.deletePointsByTrackIdAndTimeRange(trackId, startTime, endTime)
            true
        } catch (e: Exception) {
            Log.e("[TrackLocalDataSource]", "시간 범위별 항적 포인트 삭제 실패: ${e.message}")
            false
        }
    }
    
    /**
     * 모든 날짜 목록 가져오기
     */
    suspend fun getAllDates(): List<String> {
        return try {
            trackPointDao.getAllDates()
        } catch (e: Exception) {
            Log.e("[TrackLocalDataSource]", "날짜 목록 가져오기 실패: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * 특정 항적의 날짜 목록 가져오기
     */
    suspend fun getDatesByTrackId(trackId: String): List<String> {
        return try {
            trackPointDao.getDatesByTrackId(trackId)
        } catch (e: Exception) {
            Log.e("[TrackLocalDataSource]", "항적별 날짜 목록 가져오기 실패: ${e.message}")
            emptyList()
        }
    }
}
