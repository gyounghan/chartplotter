package com.marineplay.chartplotter.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.marineplay.chartplotter.data.database.dao.*
import com.marineplay.chartplotter.data.database.entities.*

/**
 * Room Database
 */
@Database(
    entities = [
        TrackEntity::class,
        TrackPointEntity::class
    ],
    version = 4, // TrackRecordEntity 제거, TrackPointEntity 구조 변경
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun trackPointDao(): TrackPointDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chartplotter_database"
                )
                    .fallbackToDestructiveMigration() // 개발 중이므로 데이터 손실 허용
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

