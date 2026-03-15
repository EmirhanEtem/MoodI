package com.example.mood

import android.content.Context
import com.example.mood.data.AppDatabase
import com.example.mood.data.UserEntity
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

class UserRepository(context: Context) {
    private val userDao = AppDatabase.getInstance(context).userDao()

    suspend fun register(username: String, password: String): Result<Long> {
        return try {
            val existing = userDao.findByUsername(username)
            if (existing != null) {
                Result.failure(Exception("Bu kullanıcı adı zaten kullanılıyor"))
            } else {
                val salt = generateSalt()
                val hashedPassword = hashPassword(password, salt)
                val user = UserEntity(
                    username = username,
                    hashedPassword = hashedPassword,
                    salt = salt
                )
                val id = userDao.insert(user)
                Result.success(id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(username: String, password: String): Result<UserEntity> {
        return try {
            val user = userDao.findByUsername(username)
                ?: return Result.failure(Exception("Kullanıcı bulunamadı"))
            val hashedInput = hashPassword(password, user.salt)
            if (hashedInput == user.hashedPassword) {
                Result.success(user)
            } else {
                Result.failure(Exception("Şifre yanlış"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserById(id: Long): UserEntity? = userDao.findById(id)

    suspend fun updateProfession(userId: Long, profession: String?) {
        userDao.updateProfession(userId, profession)
    }

    suspend fun updateProfessionDetails(
        userId: Long,
        examsJson: String?,
        devDetails: String?,
        docDetails: String?,
        teacherDetails: String?,
        artistDetails: String?
    ) {
        userDao.updateProfessionDetails(
            userId, examsJson, devDetails, docDetails, teacherDetails, artistDetails
        )
    }

    suspend fun markOnboardingComplete(userId: Long) {
        userDao.markOnboardingComplete(userId)
    }

    suspend fun updateSpotifyToken(userId: Long, token: String?) {
        userDao.updateSpotifyToken(userId, token)
    }

    suspend fun hasUsers(): Boolean = userDao.getUserCount() > 0

    private fun generateSalt(): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(32)
        random.nextBytes(saltBytes)
        return Base64.getEncoder().encodeToString(saltBytes)
    }

    private fun hashPassword(password: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(Base64.getDecoder().decode(salt))
        val hashedBytes = md.digest(password.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(hashedBytes)
    }
}
