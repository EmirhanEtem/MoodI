package com.example.mood.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodEntryDao {
    @Insert
    suspend fun insert(entry: MoodEntryEntity): Long

    @Query("SELECT * FROM mood_entries WHERE userId = :userId ORDER BY timestamp DESC")
    fun getEntriesByUser(userId: Long): Flow<List<MoodEntryEntity>>

    @Query("SELECT * FROM mood_entries WHERE userId = :userId AND timestamp >= :since ORDER BY timestamp DESC")
    fun getEntriesSince(userId: Long, since: Long): Flow<List<MoodEntryEntity>>

    @Query("SELECT * FROM mood_entries WHERE userId = :userId AND timestamp >= :weekAgo ORDER BY timestamp ASC")
    suspend fun getWeeklyEntries(userId: Long, weekAgo: Long): List<MoodEntryEntity>

    @Query("SELECT * FROM mood_entries WHERE userId = :userId AND timestamp >= :monthAgo ORDER BY timestamp ASC")
    suspend fun getMonthlyEntries(userId: Long, monthAgo: Long): List<MoodEntryEntity>

    @Query("SELECT * FROM mood_entries WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestEntry(userId: Long): MoodEntryEntity?

    @Query("SELECT COUNT(*) FROM mood_entries WHERE userId = :userId")
    suspend fun getEntryCount(userId: Long): Int

    @Query("SELECT moodName, COUNT(*) as count FROM mood_entries WHERE userId = :userId GROUP BY moodName ORDER BY count DESC")
    suspend fun getMoodDistribution(userId: Long): List<MoodCount>

    @Query("SELECT AVG(energy) FROM mood_entries WHERE userId = :userId AND timestamp >= :since")
    suspend fun getAverageEnergy(userId: Long, since: Long): Float?

    @Query("SELECT AVG(stress) FROM mood_entries WHERE userId = :userId AND timestamp >= :since")
    suspend fun getAverageStress(userId: Long, since: Long): Float?

    @Query("SELECT AVG(positivity) FROM mood_entries WHERE userId = :userId AND timestamp >= :since")
    suspend fun getAveragePositivity(userId: Long, since: Long): Float?
}

data class MoodCount(
    val moodName: String,
    val count: Int
)
