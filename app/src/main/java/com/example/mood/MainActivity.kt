package com.example.mood

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mood.screens.*
import com.example.mood.ui.theme.MoodİİTheme
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse

class MainActivity : ComponentActivity() {

    private lateinit var spotifyAuthLauncher: ActivityResultLauncher<android.content.Intent>
    private var pendingViewModel: MoodPlannerViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        SpotifyAuthManager.init(this)

        spotifyAuthLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (data != null) {
                val response = AuthorizationResponse.fromIntent(data)
                val exception = AuthorizationException.fromIntent(data)
                SpotifyAuthManager.exchangeToken(response, exception) { token ->
                    pendingViewModel?.setSpotifyAccessToken(token)
                }
            } else {
                pendingViewModel?.setSpotifyAccessToken(null)
            }
        }

        setContent {
            MoodİİTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val vm: MoodPlannerViewModel = viewModel()
                    pendingViewModel = vm
                    MoodİİApp(viewModel = vm, onSpotifyAuthRequest = {
                        try {
                            val intent = SpotifyAuthManager.createAuthorizationIntent()
                            spotifyAuthLauncher.launch(intent)
                        } catch (e: Exception) {
                            Log.e("SpotifyAuth", "Error launching auth: ${e.message}", e)
                        }
                    })
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { SpotifyAuthManager.dispose() } catch (_: Exception) {}
    }
}

@Composable
fun MoodİİApp(viewModel: MoodPlannerViewModel, onSpotifyAuthRequest: () -> Unit) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    // Listen for Spotify auth events
    val spotifyAuthEvent by viewModel.spotifyAuthEvent.collectAsState()
    if (spotifyAuthEvent != null) {
        viewModel.consumeSpotifyAuthEvent()
        onSpotifyAuthRequest()
    }

    when (currentScreen) {
        Screen.ONBOARDING -> OnboardingScreen(viewModel)
        Screen.LOGIN -> LoginScreen(viewModel)
        Screen.REGISTER -> RegisterScreen(viewModel)
        Screen.PROFESSION_SELECTION -> ProfessionSelectionScreen(viewModel)
        Screen.STUDENT_EXAM_PROMPT -> StudentExamPromptScreen(viewModel)
        Screen.STUDENT_EXAM_INPUT -> StudentExamInputScreen(viewModel)
        Screen.PROFESSION_DETAIL_PROMPT -> ProfessionDetailPromptScreen(viewModel)
        Screen.PROFESSION_DETAIL_INPUT -> ProfessionDetailInputScreen(viewModel)
        Screen.MAIN_ANALYSIS -> MainAnalysisScreen(viewModel)
        Screen.MOOD_CONFIRMATION -> MoodConfirmationScreen(viewModel)
        Screen.PLAN_DISPLAY -> PlanDisplayScreen(viewModel)
        Screen.SPOTIFY_PLAYLIST_DURATION_PROMPT -> SpotifyDurationPromptScreen(viewModel)
        Screen.MOOD_ANALYTICS -> MoodAnalyticsScreen(viewModel)
        Screen.NIGHT_REVIEW -> MoodAnalyticsScreen(viewModel) // Reuse analytics for now
        Screen.PROFILE -> ProfileScreen(viewModel)
    }
}