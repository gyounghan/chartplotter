package com.marineplay.chartplotter.data.database.dao

import androidx.room.*
import com.marineplay.chartplotter.data.database.entities.TrackPointEntity
import kotlinx.coroutines.flow.Flow

/**
 * TrackPoint DAO
 */
@Dao
interface TrackPointDao {
    /**
     * 특정 항적의 모든 포인트 조회 (시간순)
     */
    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    suspend fun getPointsByTrackId(trackId: String): List<TrackPointEntity>
    
    /**
     * 특정 항적의 모든 포인트 Flow 조회
     */
    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    fun getPointsByTrackIdFlow(trackId: String): Flow<List<TrackPointEntity>>
    
    /**
     * 특정 항적의 날짜별 포인트 조회
     */
    @Query("SELECT * FROM track_points WHERE trackId = :trackId AND date = :date ORDER BY timestamp ASC")
    suspend fun getPointsByTrackIdAndDate(trackId: String, date: String): List<TrackPointEntity>
    
    /**
     * 날짜별 포인트 조회 (모든 항적)
     */
    @Query("SELECT * FROM track_points WHERE date = :date ORDER BY timestamp ASC")
    suspend fun getPointsByDate(date: String): List<TrackPointEntity>
    
    /**
     * 특정 항적의 시간 범위별 포인트 조회
     */
    @Query("SELECT * FROM track_points WHERE trackId = :trackId AND timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    suspend fun getPointsByTrackIdAndTimeRange(trackId: String, startTime: Long, endTime: Long): List<TrackPointEntity>
    
    /**
     * 특정 항적의 최근 N개 포인트 조회
     */
    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentPointsByTrackId(trackId: String, limit: Int): List<TrackPointEntity>
    
    /**
     * 포인트 추가 (실시간 저장)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoint(point: TrackPointEntity)
    
    /**
     * 포인트 일괄 추가
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(points: List<TrackPointEntity>)
    
    /**
     * 포인트 삭제
     */
    @Delete
    suspend fun deletePoint(point: TrackPointEntity)
    
    /**
     * 특정 항적의 모든 포인트 삭제
     */
    @Query("DELETE FROM track_points WHERE trackId = :trackId")
    suspend fun deletePointsByTrackId(trackId: String)
    
    /**
     * 특정 항적의 날짜별 포인트 삭제
     */
    @Query("DELETE FROM track_points WHERE trackId = :trackId AND date = :date")
    suspend fun deletePointsByTrackIdAndDate(trackId: String, date: String)
    
    /**
     * 특정 항적의 시간 범위별 포인트 삭제
     */
    @Query("DELETE FROM track_points WHERE trackId = :trackId AND timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun deletePointsByTrackIdAndTimeRange(trackId: String, startTime: Long, endTime: Long)
    
    /**
     * 모든 날짜 목록 조회 (중복 제거)
     */
    @Query("SELECT DISTINCT date FROM track_points ORDER BY date DESC")
    suspend fun getAllDates(): List<String>
    
    /**
     * 특정 항적의 날짜 목록 조회 (중복 제거)
     */
    @Query("SELECT DISTINCT date FROM track_points WHERE trackId = :trackId ORDER BY date DESC")
    suspend fun getDatesByTrackId(trackId: String): List<String>
}

