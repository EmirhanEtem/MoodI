package com.example.mood.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_completions",
    foreignKeys = [
        ForeignKey(
            entity = MoodEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["moodEntryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("moodEntryId")]
)
data class TaskCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val moodEntryId: Long,
    val taskTitle: String,
    val isCompleted: Boolean = false,
    val priority: Int = 2, // 1=high, 2=medium, 3=low
    val sortOrder: Int = 0,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
