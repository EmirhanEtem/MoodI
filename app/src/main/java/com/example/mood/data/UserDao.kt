package com.example.mood.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): UserEntity?

    @Update
    suspend fun update(user: UserEntity)

    @Query("UPDATE users SET profession = :profession WHERE id = :userId")
    suspend fun updateProfession(userId: Long, profession: String?)

    @Query("UPDATE users SET studentExamsJson = :examsJson, developerProjectDetails = :devDetails, doctorShiftDetails = :docDetails, teacherUrgentTaskDetails = :teacherDetails, artistEventDetails = :artistDetails WHERE id = :userId")
    suspend fun updateProfessionDetails(
        userId: Long,
        examsJson: String?,
        devDetails: String?,
        docDetails: String?,
        teacherDetails: String?,
        artistDetails: String?
    )

    @Query("UPDATE users SET spotifyRefreshToken = :token WHERE id = :userId")
    suspend fun updateSpotifyToken(userId: Long, token: String?)

    @Query("UPDATE users SET onboardingCompleted = 1 WHERE id = :userId")
    suspend fun markOnboardingComplete(userId: Long)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int
}
