package com.marineplay.chartplotter.data.database.dao

import androidx.room.*
import com.marineplay.chartplotter.data.database.entities.TrackPointEntity
import kotlinx.coroutines.flow.Flow

/**
 * TrackPoint DAO
 */
@Dao
interface TrackPointDao {
    @Query("SELECT * FROM track_points WHERE recordId = :recordId ORDER BY sequence ASC")
    fun getPointsByRecordId(recordId: String): Flow<List<TrackPointEntity>>
    
    @Query("SELECT * FROM track_points WHERE recordId = :recordId ORDER BY sequence ASC")
    suspend fun getPointsByRecordIdSuspend(recordId: String): List<TrackPointEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoint(point: TrackPointEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(points: List<TrackPointEntity>)
    
    @Delete
    suspend fun deletePoint(point: TrackPointEntity)
    
    @Query("DELETE FROM track_points WHERE recordId = :recordId")
    suspend fun deletePointsByRecordId(recordId: String)
    
    @Query("DELETE FROM track_points WHERE recordId IN (SELECT id FROM track_records WHERE trackId = :trackId)")
    suspend fun deletePointsByTrackId(trackId: String)
}

