package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DailyReviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyReviewDao {
    @Query("SELECT * FROM daily_reviews ORDER BY date DESC")
    fun getAllReviews(): Flow<List<DailyReviewEntity>>

    @Query("SELECT * FROM daily_reviews WHERE date = :date")
    fun getReviewForDate(date: String): Flow<DailyReviewEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: DailyReviewEntity)

    @Query("DELETE FROM daily_reviews")
    suspend fun clearAllReviews()
}
