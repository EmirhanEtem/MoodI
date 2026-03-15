package com.example.mood

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class UserSessionManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "moodii_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_SPOTIFY_ACCESS_TOKEN = "spotify_access_token"
        private const val KEY_SPOTIFY_TOKEN_EXPIRES = "spotify_token_expires"
    }

    var userId: Long
        get() = prefs.getLong(KEY_USER_ID, -1L)
        set(value) = prefs.edit().putLong(KEY_USER_ID, value).apply()

    var username: String
        get() = prefs.getString(KEY_USERNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var onboardingComplete: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, value).apply()

    var spotifyAccessToken: String?
        get() = prefs.getString(KEY_SPOTIFY_ACCESS_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_SPOTIFY_ACCESS_TOKEN, value).apply()

    var spotifyTokenExpires: Long
        get() = prefs.getLong(KEY_SPOTIFY_TOKEN_EXPIRES, 0L)
        set(value) = prefs.edit().putLong(KEY_SPOTIFY_TOKEN_EXPIRES, value).apply()

    fun saveLoginSession(userId: Long, username: String) {
        this.userId = userId
        this.username = username
        this.isLoggedIn = true
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun isSpotifyTokenValid(): Boolean {
        val token = spotifyAccessToken
        val expires = spotifyTokenExpires
        return !token.isNullOrBlank() && System.currentTimeMillis() < expires
    }
}
