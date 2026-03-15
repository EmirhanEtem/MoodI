package com.example.mood.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val hashedPassword: String,
    val salt: String,
    val profession: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val spotifyRefreshToken: String? = null,
    val onboardingCompleted: Boolean = false,
    
    // Profession-specific saved details
    val studentExamsJson: String? = null,
    val developerProjectDetails: String? = null,
    val doctorShiftDetails: String? = null,
    val teacherUrgentTaskDetails: String? = null,
    val artistEventDetails: String? = null
)
