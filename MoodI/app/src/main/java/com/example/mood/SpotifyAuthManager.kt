package com.example.mood

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import net.openid.appauth.* // AppAuth importları
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object SpotifyAuthManager {

    // Spotify Developer Dashboard'dan alacağın bilgiler
    private const val SPOTIFY_CLIENT_ID = "46e71b3c7cbc407ab25d854a5f7f5e08" // TODO: Kendi Client ID'ni ekle
    private const val SPOTIFY_REDIRECT_URI = "com.example.mood://spotify-login-callback" // TODO: Kendi Redirect URI'ını ekle
    private const val SPOTIFY_AUTH_ENDPOINT = "https://accounts.spotify.com/authorize?client_id=46e71b3c7cbc407ab25d854a5f7f5e08&response_type=code&redirect_uri=http%3A%2F%2Flocalhost%3A8888%2Fcallback&scope=user-library-read"
    private const val SPOTIFY_TOKEN_ENDPOINT = "https://accounts.spotify.com/api/token"

    // İstenen izinler (scope'lar)
    private val SPOTIFY_SCOPES = listOf(
        "streaming", // Eğer Web Playback SDK veya App Remote SDK ile çalma yapacaksan
        "user-read-email",
        "user-read-private",
        "playlist-read-private",
        "playlist-modify-public",
        "playlist-modify-private",
        "user-modify-playback-state", // App Remote SDK için
        "user-read-playback-state"    // App Remote SDK için
    ).joinToString(" ")

    private var authState: AuthState? = null
    private lateinit var authService: AuthorizationService

    // PKCE için
    private var codeVerifier: String? = null

    fun init(context: Context) {
        authService = AuthorizationService(context)
        // Kaydedilmiş bir authState varsa onu yükle (SharedPreferences'tan)
        // loadAuthState(context) // Bu fonksiyonu implemente etmen gerekir
    }

    fun dispose() {
        authService.dispose()
    }

    fun createAuthorizationIntent(): Intent {
        val serviceConfig = AuthorizationServiceConfiguration(
            Uri.parse(SPOTIFY_AUTH_ENDPOINT),
            Uri.parse(SPOTIFY_TOKEN_ENDPOINT)
        )

        // PKCE için code_verifier ve code_challenge oluştur
        val random = SecureRandom()
        val bytes = ByteArray(64)
        random.nextBytes(bytes)
        codeVerifier = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

        val sha256 = MessageDigest.getInstance("SHA-256")
        val challengeBytes = sha256.digest(codeVerifier!!.toByteArray())
        val codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes)

        val authRequestBuilder = AuthorizationRequest.Builder(
            serviceConfig,
            SPOTIFY_CLIENT_ID,
            ResponseTypeValues.CODE,
            Uri.parse(SPOTIFY_REDIRECT_URI)
        )
            .setScope(SPOTIFY_SCOPES)
            .setCodeVerifier(codeVerifier, codeChallenge, "S256")
        // .setState(generateRandomState()) // CSRF için state de ekleyebilirsin

        return authService.getAuthorizationRequestIntent(authRequestBuilder.build())
    }

    fun exchangeToken(
        response: AuthorizationResponse?,
        exception: AuthorizationException?,
        onTokenReceived: (String?) -> Unit
    ) {
        if (exception != null) {
            Log.e("SpotifyAuth", "Authorization failed: ${exception.message}")
            onTokenReceived(null)
            return
        }

        if (response != null) {
            if (codeVerifier == null) {
                Log.e("SpotifyAuth", "Code verifier is null, cannot exchange token.")
                onTokenReceived(null)
                return
            }
            val tokenRequest = response.createTokenExchangeRequest(mapOf("code_verifier" to codeVerifier!!))
            authService.performTokenRequest(tokenRequest) { tokenResponse, tokenEx ->
                if (tokenEx != null) {
                    Log.e("SpotifyAuth", "Token exchange failed: ${tokenEx.message}")
                    onTokenReceived(null)
                    return@performTokenRequest
                }
                if (tokenResponse != null) {
                    // Yeni AuthState oluştur ve kaydet
                    authState = AuthState(response, tokenResponse, tokenEx)
                    // saveAuthState(context, authState) // Bu fonksiyonu implemente etmen gerekir
                    Log.i("SpotifyAuth", "Access Token: ${tokenResponse.accessToken}")
                    onTokenReceived(tokenResponse.accessToken)
                } else {
                    onTokenReceived(null)
                }
            }
        } else {
            onTokenReceived(null)
        }
    }

    // TODO: AuthState'i SharedPreferences'a kaydetme ve yükleme fonksiyonlarını implemente et
    // private fun saveAuthState(context: Context, state: AuthState?) { ... }
    // private fun loadAuthState(context: Context) { ... }
    // Bu, access token süresi dolduğunda refresh token ile yeni token alabilmek için önemlidir.
}