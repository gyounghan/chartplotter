package com.marineplay.chartplotter.data.database.dao

import androidx.room.*
import com.marineplay.chartplotter.data.database.entities.TrackEntity
import kotlinx.coroutines.flow.Flow

/**
 * Track DAO
 */
@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY createdAt DESC")
    fun getAllTracks(): Flow<List<TrackEntity>>
    
    @Query("SELECT * FROM tracks WHERE id = :trackId")
    suspend fun getTrackById(trackId: String): TrackEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackEntity>)
    
    @Update
    suspend fun updateTrack(track: TrackEntity)
    
    @Delete
    suspend fun deleteTrack(track: TrackEntity)
    
    @Query("DELETE FROM tracks WHERE id = :trackId")
    suspend fun deleteTrackById(trackId: String)
    
    @Query("UPDATE tracks SET isVisible = :isVisible WHERE id = :trackId")
    suspend fun updateTrackVisibility(trackId: String, isVisible: Boolean)
    
    /**
     * 항적 기록 상태 업데이트
     */
    @Query("UPDATE tracks SET isRecording = :isRecording WHERE id = :trackId")
    suspend fun updateTrackRecording(trackId: String, isRecording: Boolean)
    
    /**
     * 항적 설정 업데이트
     */
    @Query("UPDATE tracks SET intervalType = :intervalType, timeInterval = :timeInterval, distanceInterval = :distanceInterval WHERE id = :trackId")
    suspend fun updateTrackSettings(trackId: String, intervalType: String, timeInterval: Long, distanceInterval: Double)
    
    /**
     * 현재 기록 중인 항적 목록 조회
     */
    @Query("SELECT * FROM tracks WHERE isRecording = 1")
    suspend fun getRecordingTracks(): List<TrackEntity>
}

