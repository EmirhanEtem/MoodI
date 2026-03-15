package com.example.mood.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "mood_entries",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class MoodEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val moodName: String,       // WakeMood.name
    val energy: Float,
    val stress: Float,
    val positivity: Float,
    val focus: Float,
    val social: Float,
    val smileProbability: Float? = null,
    val leftEyeOpen: Float? = null,
    val rightEyeOpen: Float? = null,
    val snoozeCount: Int = 0,
    val voiceText: String? = null,
    val voiceAmplitude: Float? = null,
    val voicePauseRatio: Float? = null,
    val voiceSpeakingRate: Float? = null,
    val isNightReview: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
