package com.marineplay.chartplotter

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 항적 관리 클래스
 */
class TrackManager(private val context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("track_prefs", Context.MODE_PRIVATE)
    
    private val TRACKS_KEY = "tracks"
    private val SETTINGS_KEY = "track_settings"
    
    // 항적 목록
    private val tracks = mutableListOf<Track>()
    
    // 항적 설정
    var settings: TrackSettings = TrackSettings("time", 5000L, 10.0)
        private set
    
    init {
        loadTracks()
        loadSettings()
    }
    
    /**
     * 항적 목록 가져오기
     */
    fun getTracks(): List<Track> = tracks.toList()
    
    /**
     * 항적 추가
     */
    fun addTrack(name: String, color: Color): Track {
        val track = Track(
            id = UUID.randomUUID().toString(),
            name = name,
            color = color
        )
        tracks.add(track)
        saveTracks()
        return track
    }
    
    /**
     * 항적 삭제
     */
    fun deleteTrack(trackId: String): Boolean {
        val removed = tracks.removeAll { it.id == trackId }
        if (removed) {
            saveTracks()
        }
        return removed
    }
    
    /**
     * 항적 기록 추가
     */
    fun addTrackRecord(trackId: String, startTime: Long, endTime: Long, points: List<TrackPoint>): TrackRecord? {
        val track = tracks.find { it.id == trackId } ?: return null
        
        val record = TrackRecord(
            id = UUID.randomUUID().toString(),
            trackId = trackId,
            startTime = startTime,
            endTime = endTime,
            points = points,
            title = TrackRecord.generateTitle(startTime, endTime)
        )
        
        track.records.add(record)
        saveTracks()
        return record
    }
    
    /**
     * 항적 기록 삭제
     */
    fun deleteTrackRecord(trackId: String, recordId: String): Boolean {
        val track = tracks.find { it.id == trackId } ?: return false
        val removed = track.records.removeAll { it.id == recordId }
        if (removed) {
            saveTracks()
        }
        return removed
    }
    
    /**
     * 항적 표시/숨김 설정
     */
    fun setTrackVisibility(trackId: String, isVisible: Boolean) {
        tracks.find { it.id == trackId }?.isVisible = isVisible
        saveTracks()
    }
    
    /**
     * 항적 기록 표시/숨김 설정 (개별 항적 기록)
     */
    fun setTrackRecordVisibility(trackId: String, recordId: String, isVisible: Boolean) {
        val track = tracks.find { it.id == trackId } ?: return
        // 개별 기록 표시는 나중에 구현 가능
        saveTracks()
    }
    
    /**
     * 설정 저장
     */
    fun saveSettings(newSettings: TrackSettings) {
        settings = newSettings
        val json = JSONObject().apply {
            put("intervalType", newSettings.intervalType)
            put("timeInterval", newSettings.timeInterval)
            put("distanceInterval", newSettings.distanceInterval)
        }
        sharedPreferences.edit().putString(SETTINGS_KEY, json.toString()).apply()
    }
    
    /**
     * 항적 저장
     */
    private fun saveTracks() {
        val jsonArray = JSONArray()
        tracks.forEach { track ->
            val trackJson = JSONObject().apply {
                put("id", track.id)
                put("name", track.name)
                put("colorValue", track.color.value.toLong())
                put("isVisible", track.isVisible)
                
                val recordsArray = JSONArray()
                track.records.forEach { record ->
                    val recordJson = JSONObject().apply {
                        put("id", record.id)
                        put("startTime", record.startTime)
                        put("endTime", record.endTime)
                        put("title", record.title)
                        
                        val pointsArray = JSONArray()
                        record.points.forEach { point ->
                            val pointJson = JSONObject().apply {
                                put("latitude", point.latitude)
                                put("longitude", point.longitude)
                                put("timestamp", point.timestamp)
                            }
                            pointsArray.put(pointJson)
                        }
                        put("points", pointsArray)
                    }
                    recordsArray.put(recordJson)
                }
                put("records", recordsArray)
            }
            jsonArray.put(trackJson)
        }
        sharedPreferences.edit().putString(TRACKS_KEY, jsonArray.toString()).apply()
    }
    
    /**
     * 항적 로드
     */
    private fun loadTracks() {
        tracks.clear()
        val jsonString = sharedPreferences.getString(TRACKS_KEY, null) ?: return
        
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val trackJson = jsonArray.getJSONObject(i)
                val track = Track(
                    id = trackJson.getString("id"),
                    name = trackJson.getString("name"),
                    color = Color(trackJson.getLong("colorValue")),
                    isVisible = trackJson.optBoolean("isVisible", true)
                )
                
                val recordsArray = trackJson.getJSONArray("records")
                for (j in 0 until recordsArray.length()) {
                    val recordJson = recordsArray.getJSONObject(j)
                    val pointsList = mutableListOf<TrackPoint>()
                    val pointsArray = recordJson.getJSONArray("points")
                    for (k in 0 until pointsArray.length()) {
                        val pointJson = pointsArray.getJSONObject(k)
                        pointsList.add(
                            TrackPoint(
                                latitude = pointJson.getDouble("latitude"),
                                longitude = pointJson.getDouble("longitude"),
                                timestamp = pointJson.getLong("timestamp")
                            )
                        )
                    }
                    
                    track.records.add(
                        TrackRecord(
                            id = recordJson.getString("id"),
                            trackId = track.id,
                            startTime = recordJson.getLong("startTime"),
                            endTime = recordJson.getLong("endTime"),
                            points = pointsList,
                            title = recordJson.getString("title")
                        )
                    )
                }
                
                tracks.add(track)
            }
        } catch (e: Exception) {
            Log.e("[TrackManager]", "항적 로드 실패: ${e.message}")
        }
    }
    
    /**
     * 설정 로드
     */
    private fun loadSettings() {
        val jsonString = sharedPreferences.getString(SETTINGS_KEY, null) ?: return
        
        try {
            val json = JSONObject(jsonString)
            settings = TrackSettings(
                intervalType = json.getString("intervalType"),
                timeInterval = json.getLong("timeInterval"),
                distanceInterval = json.getDouble("distanceInterval")
            )
        } catch (e: Exception) {
            Log.e("[TrackManager]", "설정 로드 실패: ${e.message}")
        }
    }
}

