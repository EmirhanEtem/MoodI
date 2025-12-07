package com.example.mood

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mood.ui.theme.MoodİİTheme
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.Locale

object SpotifyAuthHelper {
    const val SPOTIFY_CLIENT_ID = "46e71b3c7cbc407ab25d854a5f7f5e08" // TODO: KENDİ CLIENT ID'Nİ GİR
    const val SPOTIFY_REDIRECT_URI_SCHEME = "com.example.mood"
    const val SPOTIFY_REDIRECT_URI_HOST = "spotify-login-callback"
    val SPOTIFY_REDIRECT_URI: Uri = Uri.parse("$SPOTIFY_REDIRECT_URI_SCHEME://$SPOTIFY_REDIRECT_URI_HOST")
    private const val SPOTIFY_AUTH_ENDPOINT = "https://accounts.spotify.com/authorize"
    private const val SPOTIFY_TOKEN_ENDPOINT = "https://accounts.spotify.com/api/token"
    private val SPOTIFY_SCOPES = listOf("streaming", "user-read-email", "user-read-private", "playlist-read-private", "playlist-modify-public", "playlist-modify-private", "user-modify-playback-state", "user-read-playback-state").joinToString(" ")
    var codeVerifier: String? = null

    fun createAuthorizationIntent(context: Context): Intent {
        val serviceConfig = AuthorizationServiceConfiguration(Uri.parse(SPOTIFY_AUTH_ENDPOINT), Uri.parse(SPOTIFY_TOKEN_ENDPOINT))
        val random = SecureRandom(); val bytes = ByteArray(64); random.nextBytes(bytes)
        codeVerifier = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        val sha256 = MessageDigest.getInstance("SHA-256")
        val challengeBytes = sha256.digest(codeVerifier!!.toByteArray(Charsets.UTF_8))
        val codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes)
        val authRequestBuilder = AuthorizationRequest.Builder(serviceConfig, SPOTIFY_CLIENT_ID, ResponseTypeValues.CODE, SPOTIFY_REDIRECT_URI).setScope(SPOTIFY_SCOPES).setCodeVerifier(codeVerifier, codeChallenge, "S256")
        return AuthorizationService(context).getAuthorizationRequestIntent(authRequestBuilder.build())
    }

    fun exchangeTokenUsingResponse(
        context: Context,
        response: AuthorizationResponse?,
        exception: AuthorizationException?,
        onTokenReceived: (String?) -> Unit
    ) {
        if (exception != null) {
            Log.e("SpotifyAuth", "Authorization (callback) failed: ${exception.error} - ${exception.errorDescription}", exception)
            onTokenReceived(null)
            return
        }

        if (response != null) {
            // codeVerifier'ı AuthorizationRequest oluştururken zaten set etmiştik.
            // AuthorizationResponse.createTokenExchangeRequest() bunu kendisi kullanmalı.
            // Bu yüzden mapOf("code_verifier" to codeVerifier!!) kısmını kaldırmalıyız.
            // AppAuth, PKCE için code_verifier'ı AuthorizationResponse'tan alıp TokenRequest'e
            // doğru şekilde ekler.
            val tokenRequest = response.createTokenExchangeRequest() // EK PARAMETRELER OLMADAN

            AuthorizationService(context).performTokenRequest(tokenRequest) { tokenResponse, tokenEx ->
                if (tokenEx != null) {
                    Log.e("SpotifyAuth", "Token exchange failed: ${tokenEx.error} - ${tokenEx.errorDescription}", tokenEx)
                    onTokenReceived(null)
                } else if (tokenResponse?.accessToken != null) {
                    Log.i("SpotifyAuth", "Access Token Acquired: ${tokenResponse.accessToken?.take(10)}...")
                    // TODO: Token'ları (accessToken, refreshToken, expiresAt) güvenli sakla
                    // Örneğin: AuthStateManager(context).updateAuthState(tokenResponse, tokenEx)
                    // ve AuthStateManager(context).getCurrent().accessToken
                    onTokenReceived(tokenResponse.accessToken)
                } else {
                    Log.w("SpotifyAuth", "Token exchange succeeded but tokenResponse or accessToken is null.")
                    onTokenReceived(null)
                }
            }
        } else {
            Log.e("SpotifyAuth", "Authorization response is null in exchangeTokenUsingResponse.")
            onTokenReceived(null)
        }
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var spotifyAuthLauncher: ActivityResultLauncher<Intent>
    private lateinit var viewModel: MoodPlannerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        spotifyAuthLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val response = result.data?.let { AuthorizationResponse.fromIntent(it) }
                val ex = result.data?.let { AuthorizationException.fromIntent(it) }
                SpotifyAuthHelper.exchangeTokenUsingResponse(this, response, ex) { accessToken ->
                    viewModel.setSpotifyAccessToken(accessToken) // ViewModel'i burada güncelle
                    if (accessToken != null) {
                        Toast.makeText(this, "Spotify ile başarıyla bağlandı!", Toast.LENGTH_SHORT).show()
                        if(viewModel.currentScreen.value == Screen.PLAN_DISPLAY || viewModel.currentScreen.value == Screen.SPOTIFY_PLAYLIST_DURATION_PROMPT) {
                            viewModel.navigateTo(Screen.SPOTIFY_PLAYLIST_DURATION_PROMPT)
                        }
                    } else {
                        Toast.makeText(this, "Spotify bağlantısı kurulamadı.", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(this, "Spotify bağlantı akışı iptal edildi/başarısız.", Toast.LENGTH_LONG).show()
                if(::viewModel.isInitialized) { // ViewModel'in başlatıldığından emin ol
                    viewModel.setSpotifyAccessToken(null)
                }
            }
        }
        enableEdgeToEdge()
        setContent {
            val composeViewModel: MoodPlannerViewModel = viewModel()
            this.viewModel = composeViewModel // Activity seviyesindeki değişkene ata
            val spotifyAuthEvent by viewModel.spotifyAuthEvent.collectAsState()
            LaunchedEffect(spotifyAuthEvent) {
                if (spotifyAuthEvent != null) {
                    initiateSpotifyLogin()
                    viewModel.consumeSpotifyAuthEvent()
                }
            }
            MoodİİTheme { MainAppNavigation(viewModel = composeViewModel) }
        }
    }

    fun initiateSpotifyLogin() {
        if (SpotifyAuthHelper.SPOTIFY_CLIENT_ID == "SENIN_SPOTIFY_CLIENT_ID_BURAYA" || SpotifyAuthHelper.SPOTIFY_CLIENT_ID.isBlank()) {
            Toast.makeText(this, "Lütfen Spotify Client ID'nizi ayarlayın!", Toast.LENGTH_LONG).show(); return
        }
        try {
            val authIntent = SpotifyAuthHelper.createAuthorizationIntent(this)
            spotifyAuthLauncher.launch(authIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Spotify login başlatılamadı: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            Log.e("SpotifyLoginError", "Error initiating Spotify login", e)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("MainActivity", "onNewIntent received: $intent")
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri != null &&
                uri.scheme.equals(SpotifyAuthHelper.SPOTIFY_REDIRECT_URI_SCHEME) &&
                uri.host.equals(SpotifyAuthHelper.SPOTIFY_REDIRECT_URI_HOST)) {
                Log.d("MainActivity", "Spotify callback URI received in onNewIntent: $uri")
                // Burada AppAuth'un intent'i işlemesi için bir yol gerekebilir,
                // ancak ActivityResultLauncher genellikle bu durumu yakalar.
                // Gerekirse, AuthorizationResponse.fromIntent(intent) ve
                // AuthorizationException.fromIntent(intent) ile burada da token exchange tetiklenebilir.
                // Şimdilik launcher'a güveniyoruz.
            }
        }
    }
}

@Composable
fun MainAppNavigation(viewModel: MoodPlannerViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentMoodForDisplay by viewModel.currentMood.collectAsState()
    val isLoadingPlan by viewModel.isLoadingPlan.collectAsState()

    val dynamicBackgroundColor = when {
        (currentScreen == Screen.PLAN_DISPLAY || currentScreen == Screen.MOOD_CONFIRMATION) && currentMoodForDisplay != null -> {
            when (currentMoodForDisplay) {
                WakeMood.ENERGETIC_READY -> Color(0xFFFFF9C4); WakeMood.SLEEPY_TIRED -> Color(0xFFE3F2FD)
                WakeMood.GRUMPY_STRESSED -> Color(0xFFE8F5E9); WakeMood.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            }
        }
        else -> MaterialTheme.colorScheme.background
    }
    BackHandler(enabled = currentScreen != Screen.LOGIN) {
        if (currentScreen == Screen.PLAN_DISPLAY && viewModel.needsMoodConfirmation.value) viewModel.navigateTo(Screen.MOOD_CONFIRMATION)
        else if (currentScreen == Screen.MOOD_CONFIRMATION) viewModel.navigateTo(Screen.MAIN_ANALYSIS)
        else viewModel.goBack()
    }
    Surface(modifier = Modifier.fillMaxSize().background(dynamicBackgroundColor)) {
        AnimatedContent(targetState = currentScreen, label = "ScreenTransition") { screenState ->
            when (screenState) {
                Screen.LOGIN -> LoginScreen(viewModel = viewModel)
                Screen.PROFESSION_SELECTION -> ProfessionSelectionScreen(viewModel = viewModel)
                Screen.STUDENT_EXAM_PROMPT -> StudentExamPromptScreen(viewModel = viewModel)
                Screen.STUDENT_EXAM_INPUT -> StudentExamInputScreen(viewModel = viewModel)
                Screen.PROFESSION_DETAIL_PROMPT -> ProfessionDetailPromptScreen(viewModel = viewModel)
                Screen.PROFESSION_DETAIL_INPUT -> ProfessionDetailInputScreen(viewModel = viewModel)
                Screen.MAIN_ANALYSIS -> MainAnalysisInputsScreen(viewModel = viewModel)
                Screen.MOOD_CONFIRMATION -> MoodConfirmationScreen(viewModel = viewModel)
                Screen.SPOTIFY_PLAYLIST_DURATION_PROMPT -> SpotifyPlaylistDurationPromptScreen(viewModel = viewModel)
                Screen.PLAN_DISPLAY -> PlanDisplayScreen(viewModel = viewModel)
            }
        }
        if (isLoadingPlan) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).zIndex(3f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Text("Planınız hızlıca oluşturuluyor...", color = Color.White, modifier = Modifier.padding(top = 16.dp), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: MoodPlannerViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("WakeMood Planner", style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(bottom = 32.dp))
        OutlinedTextField(value = viewModel.username.value, onValueChange = { viewModel.username.value = it }, label = { Text("Kullanıcı Adı") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = viewModel.password.value, onValueChange = { viewModel.password.value = it }, label = { Text("Şifre") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { viewModel.processLogin() }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Giriş Yap") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionSelectionScreen(viewModel: MoodPlannerViewModel) {
    Scaffold(topBar = { TopAppBar(title = { Text("Mesleğinizi Seçin") }, navigationIcon = { IconButton(onClick = { viewModel.goBack() }) { Icon(Icons.Filled.ArrowBack, "Geri") } }) }) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
            items(viewModel.professions) { profession -> Button(onClick = { viewModel.selectProfession(profession) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) { Text(profession, fontSize = 18.sp) } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentExamPromptScreen(viewModel: MoodPlannerViewModel) {
    Scaffold(topBar = { TopAppBar(title = { Text("Öğrenci Bilgileri") }, navigationIcon = { IconButton(onClick = { viewModel.goBack() }) { Icon(Icons.Filled.ArrowBack, "Geri") } }) }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Yakın zamanda bir sınav takviminiz var mı?", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = { viewModel.setProfessionSpecificYesNo("Öğrenci", true) }, modifier = Modifier.weight(1f).height(50.dp)) { Text("Evet") }; Spacer(modifier = Modifier.width(16.dp))
                OutlinedButton(onClick = { viewModel.setProfessionSpecificYesNo("Öğrenci", false) }, modifier = Modifier.weight(1f).height(50.dp)) { Text("Hayır") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentExamInputScreen(viewModel: MoodPlannerViewModel) {
    val exams by remember { derivedStateOf { viewModel.studentExams.toList() } }
    Scaffold(topBar = { TopAppBar(title = { Text("Sınav Takvimi Girişi") }, navigationIcon = { IconButton(onClick = { viewModel.goBack() }) { Icon(Icons.Filled.ArrowBack, "Geri") } }) }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            Text("Sınavlarınızı Ekleyin:", style = MaterialTheme.typography.titleLarge); Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = viewModel.tempExamCourseName.value, onValueChange = { viewModel.tempExamCourseName.value = it }, label = { Text("Ders Adı") }, modifier = Modifier.fillMaxWidth(), singleLine = true); Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = viewModel.tempExamDate.value, onValueChange = { viewModel.tempExamDate.value = it }, label = { Text("Sınav Tarihi (örn: 25 Aralık Cuma)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)); Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { viewModel.addStudentExam() }, modifier = Modifier.align(Alignment.End)) { Text("Sınav Ekle") }; Spacer(modifier = Modifier.height(16.dp))
            if (exams.isNotEmpty()) {
                Text("Eklenen Sınavlar:", style = MaterialTheme.typography.titleMedium)
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(exams, key = { it.id }) { exam -> Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text("${exam.courseName} - ${exam.examDate}", modifier = Modifier.weight(1f)); IconButton(onClick = { viewModel.removeStudentExam(exam) }) { Icon(Icons.Filled.Delete, "Sınavı Sil") } }; Divider() }
                }
            } else { Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { Text("Henüz sınav eklenmedi.", style = MaterialTheme.typography.bodyLarge) } }
            Button(onClick = { viewModel.navigateTo(Screen.MAIN_ANALYSIS) }, modifier = Modifier.fillMaxWidth().height(50.dp).padding(top = 16.dp)) { Text("Devam Et") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionDetailPromptScreen(viewModel: MoodPlannerViewModel) {
    val selectedProfession by viewModel.selectedProfession.collectAsState()
    val promptQuestion = when (selectedProfession) {
        "Yazılımcı" -> "Yakın zamanda yetiştirmeniz gereken bir projeniz veya önemli bir teslim tarihiniz var mı?"
        "Doktor" -> "Yakın zamanda önemli bir nöbetiniz, yoğun bir ameliyat takviminiz veya dikkat etmeniz gereken özel bir hasta durumunuz var mı?"
        "Öğretmen" -> "Yakın zamanda hazırlamanız gereken bir ders planı, sınav okuma, veli toplantısı veya önemli bir okul etkinliğiniz var mı?"
        "Sanatçı" -> "Üzerinde çalıştığınız bir eser için teslim tarihi, bir sergi, performans hazırlığı veya ilham gerektiren önemli bir projeniz var mı?"
        else -> "Bu mesleğinizle ilgili bugün için dikkate almamız gereken özel bir durumunuz, projeniz veya göreviniz var mı?"
    }
    Scaffold(topBar = { TopAppBar(title = { Text("${selectedProfession ?: ""} Detayları") }, navigationIcon = { IconButton(onClick = { viewModel.goBack() }) { Icon(Icons.Filled.ArrowBack, "Geri") } }) }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(promptQuestion, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = { selectedProfession?.let { viewModel.setProfessionSpecificYesNo(it, true) } }, modifier = Modifier.weight(1f).height(50.dp)) { Text("Evet, Var") }; Spacer(modifier = Modifier.width(16.dp))
                OutlinedButton(onClick = { selectedProfession?.let { viewModel.setProfessionSpecificYesNo(it, false) } }, modifier = Modifier.weight(1f).height(50.dp)) { Text("Hayır / Yok") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionDetailInputScreen(viewModel: MoodPlannerViewModel) {
    val selectedProfession by viewModel.selectedProfession.collectAsState()
    var detailValue by remember(selectedProfession) { // Key'e selectedProfession ekledik, meslek değişince input sıfırlansın/güncellensin
        mutableStateOf(
            when (selectedProfession) {
                "Yazılımcı" -> viewModel.developerProjectDetails.value
                "Doktor" -> viewModel.doctorShiftDetails.value
                "Öğretmen" -> viewModel.teacherUrgentTaskDetails.value
                "Sanatçı" -> viewModel.artistEventDetails.value
                else -> ""
            }
        )
    }
    val (label, hint) = when (selectedProfession) {
        "Yazılımcı" -> "Proje Detayları" to "örn: X uygulaması backend, Cuma'ya yetişmeli"; "Doktor" -> "Nöbet/Yoğunluk Detayları" to "örn: Yarın gece acil nöbeti, 12 saat"
        "Öğretmen" -> "Acil Görev Detayları" to "örn: Yarınki Edebiyat dersi için sunum"; "Sanatçı" -> "Etkinlik/Proje Detayları" to "örn: Haftaya sergi, son 2 tablo kaldı"
        else -> "Ek Detaylar" to "Bugünkü özel durumunuzu kısaca belirtin"
    }

    Scaffold(topBar = { TopAppBar(title = { Text("$selectedProfession - Ek Bilgi") }, navigationIcon = { IconButton(onClick = { viewModel.goBack() }) { Icon(Icons.Filled.ArrowBack, "Geri") } }) }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Lütfen $selectedProfession mesleğinizle ilgili aşağıdaki detayı girin:", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            OutlinedTextField(value = detailValue, onValueChange = { detailValue = it
                when (selectedProfession) {
                    "Yazılımcı" -> viewModel.developerProjectDetails.value = it; "Doktor" -> viewModel.doctorShiftDetails.value = it
                    "Öğretmen" -> viewModel.teacherUrgentTaskDetails.value = it; "Sanatçı" -> viewModel.artistEventDetails.value = it
                }
            }, label = { Text(label) }, placeholder = { Text(hint) }, modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp), maxLines = 5)
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = { viewModel.navigateTo(Screen.MAIN_ANALYSIS) }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Devam Et") }
        }
    }
}
@Composable
fun TaskRow(task: TaskItem, viewModel: MoodPlannerViewModel) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable {
                expanded = !expanded
                if (expanded && task.subTasks.isEmpty() && !task.isLoadingSubTasks && task.detailPromptInstruction != null) {
                    viewModel.fetchSubTasksFor(task)
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.isCompleted) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.toggleTaskCompletion(task) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Filled.CheckCircleOutline else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = if (task.isCompleted) "Tamamlandı" else "Tamamlanmadı",
                        tint = if (task.isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontSize = 18.sp,
                        fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.Medium,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (task.detailPromptInstruction != null) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Daralt" else "Genişlet",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded && task.detailPromptInstruction != null,
                enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
            ) {
                Column(modifier = Modifier.padding(start = 58.dp, end = 16.dp, bottom = 16.dp, top = 0.dp)) {
                    Divider(modifier = Modifier.padding(bottom = 8.dp))
                    if (task.isLoadingSubTasks) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally).padding(vertical = 8.dp))
                    } else if (task.subTasks.isNotEmpty()) {
                        Text("Detaylar/Alt Görevler:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom=4.dp))
                        task.subTasks.forEach { subTask ->
                            Text(
                                text = "• $subTask",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    } else if (!task.isLoadingSubTasks) {
                        Text(
                            "Bu görev için ek detay bulunamadı.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun InputCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
fun MoodIcon(mood: WakeMood, modifier: Modifier = Modifier) {
    val icon = when (mood) { WakeMood.ENERGETIC_READY -> Icons.Filled.WbSunny; WakeMood.SLEEPY_TIRED -> Icons.Filled.Bedtime; WakeMood.GRUMPY_STRESSED -> Icons.Filled.SentimentVeryDissatisfied; WakeMood.NEUTRAL -> Icons.Filled.SentimentSatisfied }
    val tint = when (mood) { WakeMood.ENERGETIC_READY -> Color(0xFFFFC107); WakeMood.SLEEPY_TIRED -> Color(0xFF64B5F6); WakeMood.GRUMPY_STRESSED -> Color(0xFF4DB6AC); WakeMood.NEUTRAL -> MaterialTheme.colorScheme.primary }
    Icon(imageVector = icon, contentDescription = mood.description, modifier = modifier, tint = tint)
}

@Composable
fun MoodConfirmationCard(mood: WakeMood, onConfirm: () -> Unit, onReject: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp), elevation = CardDefaults.cardElevation(4.dp), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Tahminimizce modun:", style = MaterialTheme.typography.titleLarge); Row(verticalAlignment = Alignment.CenterVertically) { MoodIcon(mood = mood, modifier = Modifier.size(60.dp)); Spacer(modifier = Modifier.width(16.dp)); Text(mood.description, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
            Text("Bu şekilde devam edip bir plan oluşturalım mı?", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f).height(48.dp)) { Text("Hayır, Değiştir") }; Spacer(modifier = Modifier.width(16.dp)); Button(onClick = onConfirm, modifier = Modifier.weight(1f).height(48.dp)) { Text("Evet, Planla") } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAnalysisInputsScreen(viewModel: MoodPlannerViewModel) {
    val context = LocalContext.current
    var showCameraPreview by remember { mutableStateOf(false) }
    var faceGraphicInfoState by remember { mutableStateOf(FaceGraphicInfo()) }
    val smileProbState by viewModel.smileProbability
    val transcribedVoiceInput by viewModel.transcribedVoiceInput.collectAsState()
    val isListeningForVoice by viewModel.isListeningForVoice.collectAsState()
    val snoozeCount by viewModel.snoozeCount.collectAsState()
    val preliminaryMoodForButtonText by viewModel.currentMood.collectAsState()

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted -> if (isGranted) { faceGraphicInfoState = FaceGraphicInfo(); viewModel.smileProbability.value = null; showCameraPreview = true } else { Toast.makeText(context, "Kamera izni reddedildi.", Toast.LENGTH_LONG).show() } }
    val speechRecognizerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> viewModel.stopListening(); if (result.resultCode == Activity.RESULT_OK && result.data != null) { val speechResult: ArrayList<String>? = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS); viewModel.setTranscribedText(speechResult?.get(0) ?: "Ses anlaşılamadı.") } else { viewModel.setTranscribedText("Ses tanıma iptal edildi veya hata oluştu.") } }
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted -> if (isGranted) { val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR"); putExtra(RecognizerIntent.EXTRA_PROMPT, "Nasıl hissettiğini söyle...") }; try { viewModel.startListening(); speechRecognizerLauncher.launch(intent) } catch (e: ActivityNotFoundException) { Toast.makeText(context, "Cihazınızda ses tanıma özelliği bulunmuyor.", Toast.LENGTH_SHORT).show(); viewModel.setTranscribedText("Ses tanıma desteklenmiyor."); viewModel.stopListening() } } else { Toast.makeText(context, "Ses kaydı izni reddedildi.", Toast.LENGTH_SHORT).show(); viewModel.stopListening() } }

    Scaffold(topBar = { TopAppBar(title = { Text("Günlük Analiz") }, navigationIcon = { IconButton(onClick = { viewModel.goBack() }) { Icon(Icons.Filled.ArrowBack, "Geri") } }) }) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(modifier = Modifier.weight(1f).padding(paddingValues).padding(horizontal = 16.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item { Column(horizontalAlignment = Alignment.CenterHorizontally) { viewModel.username.value.takeIf { it.isNotBlank() }?.let { Text("Merhaba, $it!", style = MaterialTheme.typography.headlineSmall) } ?: Text("Merhaba!", style = MaterialTheme.typography.headlineSmall); Text("Güne nasıl başladığını analiz edelim:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)) } }
                    item { InputCard { Text("1. Alarm Erteleme", style = MaterialTheme.typography.titleLarge); Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) { IconButton(onClick = { viewModel.decrementSnooze() }) { Icon(Icons.Filled.Remove, "Azalt", Modifier.size(36.dp)) }; Text("$snoozeCount", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 20.dp)); IconButton(onClick = { viewModel.incrementSnooze() }) { Icon(Icons.Filled.Add, "Arttır", Modifier.size(36.dp)) } } } }
                    item { InputCard { Text("2. Yüz İfadesi", style = MaterialTheme.typography.titleLarge); FilledTonalButton(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.fillMaxWidth(0.9f).height(56.dp)) { Icon(Icons.Filled.Face, null, Modifier.padding(end = 8.dp)); Text("Yüz İfadesini Tara", fontSize = 16.sp) }; smileProbState?.let { Text("Gülümseme: ${"%.1f".format(it * 100)}%", color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(top = 8.dp)) } } }
                    item { InputCard { Text("3. Sesli Ruh Hali", style = MaterialTheme.typography.titleLarge); FilledTonalButton(onClick = { recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }, modifier = Modifier.fillMaxWidth(0.9f).height(56.dp), enabled = !isListeningForVoice) { Icon(if (isListeningForVoice) Icons.Filled.VolumeUp else Icons.Filled.Mic, null, Modifier.padding(end = 8.dp)); Text(if (isListeningForVoice) "Dinleniyor..." else "Sesli Giriş Yap", fontSize = 16.sp) }; transcribedVoiceInput?.let { text -> if (text.isNotBlank()) { val textColor = if (text.containsAny("iptal", "anlaşılamadı", "desteklenmiyor")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary; Text("Söyledin: \"$text\"", color = textColor, modifier = Modifier.padding(top = 8.dp)) } } } }
                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
                Column(modifier = Modifier.fillMaxWidth().padding(paddingValues).padding(bottom = 16.dp, start = 16.dp, end = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(onClick = { viewModel.startInitialAnalysis() }, modifier = Modifier.fillMaxWidth(0.9f).height(60.dp), shape = RoundedCornerShape(16.dp)) { Text(if (preliminaryMoodForButtonText != null && !viewModel.needsMoodConfirmation.collectAsState().value) "Tekrar Değerlendir ve Planla" else "Analiz Et ve Planla", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                    Text("Sıfırla (Girişe Dön)", modifier = Modifier.padding(top = 16.dp).clickable { viewModel.fullResetToLogin() }, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (showCameraPreview) { Box(modifier = Modifier.fillMaxSize().zIndex(1f)) { CameraPreview(modifier = Modifier.fillMaxSize(), onSmileDetectedAndSaved = { showCameraPreview = false }, onFacesDetected = { newFaceInfo -> faceGraphicInfoState = newFaceInfo; viewModel.smileProbability.value = newFaceInfo.faces.firstOrNull()?.smilingProbability }); FaceOverlay(faceGraphicInfo = faceGraphicInfoState, modifier = Modifier.fillMaxSize()) } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodConfirmationScreen(viewModel: MoodPlannerViewModel) {
    val preliminaryMood by viewModel.currentMood.collectAsState()
    if (preliminaryMood == null) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Ruh hali belirleniyor...") }; return }
    Scaffold(topBar = { TopAppBar(title = { Text("Ruh Hali Onayı") }, navigationIcon = { IconButton(onClick = { viewModel.goBack() }) { Icon(Icons.Filled.ArrowBack, "Geri") } }) }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            MoodConfirmationCard(mood = preliminaryMood!!, onConfirm = { viewModel.userConfirmsMood(true) }, onReject = { viewModel.userConfirmsMood(false) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDisplayScreen(viewModel: MoodPlannerViewModel) {
    val currentMood by viewModel.currentMood.collectAsState()
    val snoozeCount by viewModel.snoozeCount.collectAsState()
    val smileProbState by viewModel.smileProbability
    val transcribedVoiceInput by viewModel.transcribedVoiceInput.collectAsState()
    val tasks = viewModel.tasks
    val isCreatingPlaylist by viewModel.isCreatingPlaylist.collectAsState()
    val createdPlaylistUrl by viewModel.createdPlaylistUrl.collectAsState()
    val spotifyMessage by viewModel.spotifyMessage.collectAsState()
    val isSpotifyAuthenticated by viewModel.isSpotifyAuthenticated.collectAsState()
    val context = LocalContext.current

    if (currentMood == null) { Log.d("PlanDisplayScreen", "Current mood is null"); Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Plan yükleniyor...") }; return }
    Scaffold(topBar = { TopAppBar(title = { Text("Günlük Planın") }, navigationIcon = { IconButton(onClick = { viewModel.goBack() }) { Icon(Icons.Filled.ArrowBack, "Geri") } }) }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) { MoodIcon(mood = currentMood!!, modifier = Modifier.size(48.dp)); Spacer(modifier = Modifier.width(12.dp)); Text(currentMood!!.description, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
            Text("Alarm Erteleme: $snoozeCount kez", style = MaterialTheme.typography.titleMedium); smileProbState?.let { Text("Gülümseme Oranı: ${"%.1f".format(it * 100)}%", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp)) }; transcribedVoiceInput?.let { if (it.isNotBlank() && !it.containsAny("iptal", "anlaşılamadı", "desteklenmiyor")) { Text("Sesli Giriş: \"$it\"", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp)) } }; Spacer(modifier = Modifier.height(24.dp))
            Text("Önerilen Görevler:", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (tasks.isEmpty()) { Text("Bugün için özel bir görev önerisi bulunamadı veya Gemini yanıt vermedi.", modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge) }
            else { LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) { items(tasks, key = { task -> task.id }) { task -> TaskRow(task = task, viewModel = viewModel) } } }
            Spacer(modifier = Modifier.height(16.dp))

            spotifyMessage?.let { message -> Text(message, color = if (message.contains("başarıyla",true) || message.contains("başarılı",true)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 8.dp)) }
            createdPlaylistUrl?.let { url -> Button(onClick = { try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: ActivityNotFoundException) { Toast.makeText(context, "Spotify uygulaması bulunamadı.", Toast.LENGTH_LONG).show() } }, modifier = Modifier.fillMaxWidth(0.9f).height(50.dp).padding(bottom = 8.dp)) { Icon(Icons.Filled.OpenInBrowser, null, Modifier.padding(end=8.dp)); Text("Listeyi Spotify'da Aç") } }
            if (!isSpotifyAuthenticated) { Button(onClick = { viewModel.triggerSpotifyAuthRequest() }, modifier = Modifier.fillMaxWidth(0.9f).height(50.dp).padding(bottom = 8.dp)) { Icon(Icons.Filled.Link, null, Modifier.padding(end=8.dp)); Text("Spotify Hesabına Bağlan") } }
            else { Button(onClick = { viewModel.navigateTo(Screen.SPOTIFY_PLAYLIST_DURATION_PROMPT) }, enabled = !isCreatingPlaylist, modifier = Modifier.fillMaxWidth(0.9f).height(50.dp)) { if (isCreatingPlaylist) { CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary); Spacer(Modifier.width(8.dp)); Text("Oluşturuluyor...") } else { Icon(Icons.Filled.MusicNote, null, Modifier.padding(end=8.dp)); Text("Spotify Çalma Listesi Oluştur") } }; OutlinedButton(onClick = { viewModel.clearSpotifyAuth() }, modifier = Modifier.fillMaxWidth(0.9f).height(50.dp).padding(top = 8.dp)) { Icon(Icons.Filled.LinkOff, null, Modifier.padding(end=8.dp)); Text("Spotify Bağlantısını Kes") } }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.resetForNewDay() }, modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)) { Text("Yeni Gün Analizi Başlat") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyPlaylistDurationPromptScreen(viewModel: MoodPlannerViewModel) {
    var durationSliderPosition by remember { mutableFloatStateOf(viewModel.playlistDurationPreferenceMinutes.value.toFloat()) }
    val context = LocalContext.current
    Scaffold(topBar = { TopAppBar(title = { Text("Çalma Listesi Süresi") }, navigationIcon = { IconButton(onClick = { viewModel.goBack() }) { Icon(Icons.Filled.ArrowBack, "Geri") } }) }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Çalma listeniz yaklaşık kaç dakika olsun?", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(16.dp))
            Text("${durationSliderPosition.toInt()} dakika", style = MaterialTheme.typography.titleLarge)
            Slider(value = durationSliderPosition, onValueChange = { durationSliderPosition = it }, valueRange = 15f..120f, steps = (120-15)/5 - 1, modifier = Modifier.padding(vertical = 16.dp)); Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { viewModel.playlistDurationPreferenceMinutes.value = durationSliderPosition.toInt(); viewModel.createSpotifyPlaylistForMood(durationSliderPosition.toInt()); Toast.makeText(context, "İstek gönderildi...", Toast.LENGTH_SHORT).show(); viewModel.navigateTo(Screen.PLAN_DISPLAY) }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Oluştur") }
        }
    }
}

// --- Previews ---
@SuppressLint("ComposableNaming", "LocalViewModelStoreOwner", "ViewModelConstructorInComposable")
@Preview(showBackground = true, name = "Login Screen")
@Composable
fun LoginScreenPreview() { MoodİİTheme { LoginScreen(viewModel = MoodPlannerViewModel()) } }

@SuppressLint("ComposableNaming", "LocalViewModelStoreOwner", "ViewModelConstructorInComposable")
@Preview(showBackground = true, name = "Profession Selection")
@Composable
fun ProfessionSelectionScreenPreview() { MoodİİTheme { ProfessionSelectionScreen(viewModel = MoodPlannerViewModel()) } }

@SuppressLint("ComposableNaming", "LocalViewModelStoreOwner", "ViewModelConstructorInComposable")
@Preview(showBackground = true, name = "Student Exam Prompt")
@Composable
fun StudentExamPromptScreenPreview() { MoodİİTheme { StudentExamPromptScreen(viewModel = MoodPlannerViewModel()) } }

@SuppressLint("ComposableNaming", "LocalViewModelStoreOwner", "ViewModelConstructorInComposable")
@Preview(showBackground = true, name = "Student Exam Input")
@Composable
fun StudentExamInputScreenPreview() { MoodİİTheme { StudentExamInputScreen(viewModel = MoodPlannerViewModel()) } }

@SuppressLint("ComposableNaming", "LocalViewModelStoreOwner", "ViewModelConstructorInComposable")
@Preview(showBackground = true, name = "Profession Detail Prompt")
@Composable
fun ProfessionDetailPromptScreenPreview() { MoodİİTheme { val vm = MoodPlannerViewModel(); vm.selectProfession("Yazılımcı"); ProfessionDetailPromptScreen(viewModel = vm) } }

@SuppressLint("ComposableNaming", "LocalViewModelStoreOwner", "ViewModelConstructorInComposable")
@Preview(showBackground = true, name = "Profession Detail Input")
@Composable
fun ProfessionDetailInputScreenPreview() { MoodİİTheme { val vm = MoodPlannerViewModel(); vm.selectProfession("Doktor"); vm.setProfessionSpecificYesNo("Doktor", true); ProfessionDetailInputScreen(viewModel = vm) } }

@SuppressLint("ComposableNaming", "LocalViewModelStoreOwner", "ViewModelConstructorInComposable")
@Preview(showBackground = true, name = "Main Analysis Inputs")
@Composable
fun MainAnalysisInputsScreenPreview() { MoodİİTheme { MainAnalysisInputsScreen(viewModel = MoodPlannerViewModel()) } }

@SuppressLint("ComposableNaming", "LocalViewModelStoreOwner", "ViewModelConstructorInComposable")
@Preview(showBackground = true, name = "Mood Confirmation")
@Composable
fun MoodConfirmationScreenPreview() { MoodİİTheme { val vm = MoodPlannerViewModel(); LaunchedEffect(Unit) { vm.setSnoozeCount(2); vm.startInitialAnalysis() }; if (vm.needsMoodConfirmation.collectAsState().value && vm.currentMood.collectAsState().value != null) { MoodConfirmationScreen(viewModel = vm) } else { Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()){ Text("Hazırlanıyor...") } } } }

@SuppressLint("ComposableNaming", "LocalViewModelStoreOwner", "ViewModelConstructorInComposable")
@Preview(showBackground = true, name = "Plan Display")
@Composable
fun PlanDisplayScreenPreview() { MoodİİTheme { val vm = MoodPlannerViewModel(); LaunchedEffect(Unit) { vm.setSnoozeCount(0); vm.smileProbability.value = 0.9f; vm.setTranscribedText("Harika bir gün!"); vm.startInitialAnalysis(); vm.userConfirmsMood(true) }; PlanDisplayScreen(viewModel = vm) } }

@SuppressLint("ComposableNaming", "LocalViewModelStoreOwner", "ViewModelConstructorInComposable")
@Preview(showBackground = true, name = "Spotify Duration Prompt")
@Composable
fun SpotifyDurationPromptPreview() { MoodİİTheme { SpotifyPlaylistDurationPromptScreen(viewModel = MoodPlannerViewModel()) } }

@SuppressLint("ComposableNaming")
@Preview(showBackground = true, name = "Isolated MoodConfirmationCard")
@Composable
fun IsolatedMoodConfirmationCardPreview() { MoodİİTheme { MoodConfirmationCard(mood = WakeMood.SLEEPY_TIRED, onConfirm = { }, onReject = { }) } }