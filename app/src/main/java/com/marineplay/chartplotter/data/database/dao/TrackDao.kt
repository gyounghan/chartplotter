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
}

