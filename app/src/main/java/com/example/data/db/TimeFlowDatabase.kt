package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.DailyReviewEntity
import com.example.data.model.FocusSessionEntity
import com.example.data.model.TaskEntity

@Database(
    entities = [TaskEntity::class, FocusSessionEntity::class, DailyReviewEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TimeFlowDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun dailyReviewDao(): DailyReviewDao

    companion object {
        @Volatile
        private var INSTANCE: TimeFlowDatabase? = null

        fun getDatabase(context: Context): TimeFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TimeFlowDatabase::class.java,
                    "timeflow_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
