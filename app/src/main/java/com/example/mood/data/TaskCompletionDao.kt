package com.example.mood.data

import androidx.room.*

@Dao
interface TaskCompletionDao {
    @Insert
    suspend fun insert(task: TaskCompletionEntity): Long

    @Insert
    suspend fun insertAll(tasks: List<TaskCompletionEntity>)

    @Update
    suspend fun update(task: TaskCompletionEntity)

    @Query("SELECT * FROM task_completions WHERE moodEntryId = :moodEntryId ORDER BY sortOrder ASC")
    suspend fun getByMoodEntry(moodEntryId: Long): List<TaskCompletionEntity>

    @Query("UPDATE task_completions SET isCompleted = :completed, completedAt = :completedAt WHERE id = :taskId")
    suspend fun updateCompletion(taskId: Long, completed: Boolean, completedAt: Long?)

    @Query("UPDATE task_completions SET sortOrder = :newOrder WHERE id = :taskId")
    suspend fun updateSortOrder(taskId: Long, newOrder: Int)

    @Query("UPDATE task_completions SET priority = :priority WHERE id = :taskId")
    suspend fun updatePriority(taskId: Long, priority: Int)

    @Query("SELECT COUNT(*) FROM task_completions WHERE moodEntryId = :moodEntryId AND isCompleted = 1")
    suspend fun getCompletedCount(moodEntryId: Long): Int

    @Query("SELECT COUNT(*) FROM task_completions WHERE moodEntryId = :moodEntryId")
    suspend fun getTotalCount(moodEntryId: Long): Int
}
