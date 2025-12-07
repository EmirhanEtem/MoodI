package com.example.mood

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

// ExamDetail, Screen, WakeMood, TaskItem data class/enum'ları ayrı dosyalarda olmalı.

class MoodPlannerViewModel : ViewModel() {
    private val _currentScreen = MutableStateFlow(Screen.LOGIN)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    var username = mutableStateOf("")
    var password = mutableStateOf("")

    private val _selectedProfession = MutableStateFlow<String?>(null)
    val selectedProfession: StateFlow<String?> = _selectedProfession.asStateFlow()
    val professions = listOf("Öğrenci", "Yazılımcı", "Doktor", "Öğretmen", "Sanatçı", "Diğer")

    // --- Mesleğe Özel State'ler ---
    private val _hasUpcomingExams = MutableStateFlow<Boolean?>(null)
    val hasUpcomingExams: StateFlow<Boolean?> = _hasUpcomingExams.asStateFlow()
    val studentExams = mutableStateListOf<ExamDetail>()
    var tempExamCourseName = mutableStateOf("")
    var tempExamDate = mutableStateOf("")

    private val _developerHasProjectDeadline = MutableStateFlow<Boolean?>(null)
    val developerHasProjectDeadline: StateFlow<Boolean?> = _developerHasProjectDeadline.asStateFlow()
    var developerProjectDetails = mutableStateOf("")

    private val _doctorHasUpcomingShift = MutableStateFlow<Boolean?>(null)
    val doctorHasUpcomingShift: StateFlow<Boolean?> = _doctorHasUpcomingShift.asStateFlow()
    var doctorShiftDetails = mutableStateOf("")

    private val _teacherHasUrgentTask = MutableStateFlow<Boolean?>(null)
    val teacherHasUrgentTask: StateFlow<Boolean?> = _teacherHasUrgentTask.asStateFlow()
    var teacherUrgentTaskDetails = mutableStateOf("")

    private val _artistHasDeadlineOrEvent = MutableStateFlow<Boolean?>(null)
    val artistHasDeadlineOrEvent: StateFlow<Boolean?> = _artistHasDeadlineOrEvent.asStateFlow()
    var artistEventDetails = mutableStateOf("")

    // --- Genel Günlük State'ler ---
    private val _snoozeCount = MutableStateFlow(0)
    val snoozeCount: StateFlow<Int> = _snoozeCount.asStateFlow()
    private val _currentMood = MutableStateFlow<WakeMood?>(null)
    val currentMood: StateFlow<WakeMood?> = _currentMood.asStateFlow()
    var smileProbability = mutableStateOf<Float?>(null)
    private val _transcribedVoiceInput = MutableStateFlow<String?>(null)
    val transcribedVoiceInput: StateFlow<String?> = _transcribedVoiceInput.asStateFlow()
    private val _isListeningForVoice = MutableStateFlow(false)
    val isListeningForVoice: StateFlow<Boolean> = _isListeningForVoice.asStateFlow()
    private val _needsMoodConfirmation = MutableStateFlow(false)
    val needsMoodConfirmation: StateFlow<Boolean> = _needsMoodConfirmation.asStateFlow()
    private var _preliminaryMoodStore: WakeMood? = null
    val tasks = mutableStateListOf<TaskItem>()
    private val _isLoadingPlan = MutableStateFlow(false)
    val isLoadingPlan: StateFlow<Boolean> = _isLoadingPlan.asStateFlow()

    // --- Spotify ile İlgili State'ler ---
    private val _spotifyAccessToken = MutableStateFlow<String?>(null)
    val spotifyAccessToken: StateFlow<String?> = _spotifyAccessToken.asStateFlow()
    private val _isSpotifyAuthenticated = MutableStateFlow(false)
    val isSpotifyAuthenticated: StateFlow<Boolean> = _isSpotifyAuthenticated.asStateFlow()
    private val _createdPlaylistUrl = MutableStateFlow<String?>(null)
    val createdPlaylistUrl: StateFlow<String?> = _createdPlaylistUrl.asStateFlow()
    private val _spotifyMessage = MutableStateFlow<String?>(null)
    val spotifyMessage: StateFlow<String?> = _spotifyMessage.asStateFlow()
    var playlistDurationPreferenceMinutes = mutableStateOf(30)
    private val _isCreatingPlaylist = MutableStateFlow(false)
    val isCreatingPlaylist: StateFlow<Boolean> = _isCreatingPlaylist.asStateFlow()

    private val _spotifyAuthEvent = MutableStateFlow<Unit?>(null) // Event Flow
    val spotifyAuthEvent: StateFlow<Unit?> = _spotifyAuthEvent.asStateFlow()


    private val fallbackTasks = listOf(
        TaskItem(title="Gününü planla", difficulty=1, estimatedTimeMinutes=10, tags=listOf("planning"), detailPromptInstruction = "Kullanıcının \"Gününü planla\" görevi için detaylı adımlar oluştur."),
        TaskItem(title="Bir bardak su iç", difficulty=1, estimatedTimeMinutes=2, tags=listOf("health"), detailPromptInstruction = "Kullanıcının \"Bir bardak su iç\" eyleminin faydalarını ve nasıl alışkanlık haline getirebileceğini açıkla."),
        TaskItem(title="Kısa bir yürüyüş yap", difficulty=1, estimatedTimeMinutes=15, tags=listOf("physical", "break"), detailPromptInstruction = "Kullanıcının \"Kısa bir yürüyüş yap\" görevi için motive edici ipuçları ve faydalarını listele.")
    )
    private var generativeModel: GenerativeModel? = null

    init {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey != "NO_KEY_FOUND_IN_PROPERTIES" && apiKey.isNotBlank() && apiKey != "NO_KEY_FOUND") {
                val config = generationConfig { temperature = 0.75f; topK = 1; topP = 0.95f; maxOutputTokens = 4096 }
                val safetySettings = listOf(
                    SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.MEDIUM_AND_ABOVE),
                    SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE),
                    SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.MEDIUM_AND_ABOVE),
                    SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.MEDIUM_AND_ABOVE),
                )
                generativeModel = GenerativeModel(
                    modelName = "gemini-2.0-flash", // Veya "gemini-1.0-pro-latest"
                    apiKey = apiKey,
                    generationConfig = config,
                    safetySettings = safetySettings
                )
                Log.i("GeminiInit", "Gemini Model Initialized successfully.")
            } else {
                Log.e("GeminiInit", "GEMINI_API_KEY not found. Using fallback. Value: '$apiKey'")
            }
        } catch (e: Exception) {
            Log.e("GeminiInit", "Error initializing Gemini: ${e.message}", e)
        }
    }

    fun setSpotifyAccessToken(token: String?) {
        _spotifyAccessToken.value = token
        _isSpotifyAuthenticated.value = !token.isNullOrBlank()
        if (token.isNullOrBlank()) {
            _spotifyMessage.value = "Spotify bağlantısı kurulamadı."
        } else {
            _spotifyMessage.value = "Spotify ile başarıyla bağlandı!"
            // Token alındıktan sonra, eğer çalma listesi oluşturma ekranına gitmek gerekiyorsa yönlendir
            if (_currentScreen.value == Screen.PLAN_DISPLAY || _currentScreen.value == Screen.SPOTIFY_PLAYLIST_DURATION_PROMPT) {
                navigateTo(Screen.SPOTIFY_PLAYLIST_DURATION_PROMPT)
            }
        }
    }

    fun clearSpotifyAuth() {
        _spotifyAccessToken.value = null
        _isSpotifyAuthenticated.value = false
        _createdPlaylistUrl.value = null
        _spotifyMessage.value = "Spotify bağlantısı kesildi."
    }

    fun consumeSpotifyAuthEvent() { _spotifyAuthEvent.value = null }

    fun triggerSpotifyAuthRequest() {
        Log.d("SpotifyAuth", "Spotify authentication request triggered from ViewModel.")
        _spotifyAuthEvent.value = Unit // Event'i tetikle, MainActivity bunu dinleyecek
    }

    fun navigateTo(screen: Screen) {
        Log.d("ViewModelNav", "Navigating to: ${screen.name} from ${_currentScreen.value.name}")
        _currentScreen.value = screen
    }

    fun goBack() {
        val previousScreen = when (_currentScreen.value) {
            Screen.PROFESSION_SELECTION -> Screen.LOGIN
            Screen.STUDENT_EXAM_PROMPT -> Screen.PROFESSION_SELECTION
            Screen.STUDENT_EXAM_INPUT -> Screen.STUDENT_EXAM_PROMPT
            Screen.PROFESSION_DETAIL_PROMPT -> Screen.PROFESSION_SELECTION
            Screen.PROFESSION_DETAIL_INPUT -> Screen.PROFESSION_DETAIL_PROMPT
            Screen.MAIN_ANALYSIS -> {
                when (_selectedProfession.value) {
                    "Öğrenci" -> if (_hasUpcomingExams.value == true) Screen.STUDENT_EXAM_INPUT else Screen.STUDENT_EXAM_PROMPT
                    "Yazılımcı" -> if (_developerHasProjectDeadline.value != null) Screen.PROFESSION_DETAIL_PROMPT else Screen.PROFESSION_SELECTION
                    "Doktor" -> if (_doctorHasUpcomingShift.value != null) Screen.PROFESSION_DETAIL_PROMPT else Screen.PROFESSION_SELECTION
                    "Öğretmen" -> if (_teacherHasUrgentTask.value != null) Screen.PROFESSION_DETAIL_PROMPT else Screen.PROFESSION_SELECTION
                    "Sanatçı" -> if (_artistHasDeadlineOrEvent.value != null) Screen.PROFESSION_DETAIL_PROMPT else Screen.PROFESSION_SELECTION
                    else -> Screen.PROFESSION_SELECTION
                }
            }
            Screen.MOOD_CONFIRMATION -> Screen.MAIN_ANALYSIS
            Screen.SPOTIFY_PLAYLIST_DURATION_PROMPT -> Screen.PLAN_DISPLAY
            Screen.PLAN_DISPLAY -> Screen.MOOD_CONFIRMATION
            else -> null
        }
        previousScreen?.let { navigateTo(it) }
    }

    fun processLogin() {
        if (username.value.isNotBlank() && password.value.isNotBlank()) navigateTo(Screen.PROFESSION_SELECTION)
    }

    fun selectProfession(profession: String) {
        _selectedProfession.value = profession
        resetAllProfessionSpecificStatesExcept(profession)
        when (profession) {
            "Öğrenci" -> navigateTo(Screen.STUDENT_EXAM_PROMPT)
            "Yazılımcı", "Doktor", "Öğretmen", "Sanatçı" -> navigateTo(Screen.PROFESSION_DETAIL_PROMPT)
            else -> navigateTo(Screen.MAIN_ANALYSIS)
        }
    }

    fun setProfessionSpecificYesNo(profession: String, hasSpecificEvent: Boolean) {
        when (profession) {
            "Öğrenci" -> { _hasUpcomingExams.value = hasSpecificEvent; if (hasSpecificEvent) navigateTo(Screen.STUDENT_EXAM_INPUT) else { studentExams.clear(); navigateTo(Screen.MAIN_ANALYSIS) } }
            "Yazılımcı" -> { _developerHasProjectDeadline.value = hasSpecificEvent; if (hasSpecificEvent) navigateTo(Screen.PROFESSION_DETAIL_INPUT) else { developerProjectDetails.value = ""; navigateTo(Screen.MAIN_ANALYSIS) } }
            "Doktor" -> { _doctorHasUpcomingShift.value = hasSpecificEvent; if (hasSpecificEvent) navigateTo(Screen.PROFESSION_DETAIL_INPUT) else { doctorShiftDetails.value = ""; navigateTo(Screen.MAIN_ANALYSIS) } }
            "Öğretmen" -> { _teacherHasUrgentTask.value = hasSpecificEvent; if (hasSpecificEvent) navigateTo(Screen.PROFESSION_DETAIL_INPUT) else { teacherUrgentTaskDetails.value = ""; navigateTo(Screen.MAIN_ANALYSIS) } }
            "Sanatçı" -> { _artistHasDeadlineOrEvent.value = hasSpecificEvent; if (hasSpecificEvent) navigateTo(Screen.PROFESSION_DETAIL_INPUT) else { artistEventDetails.value = ""; navigateTo(Screen.MAIN_ANALYSIS) } }
        }
    }

    fun addStudentExam() {
        if (tempExamCourseName.value.isNotBlank() && tempExamDate.value.isNotBlank()) {
            studentExams.add(ExamDetail(courseName = tempExamCourseName.value, examDate = tempExamDate.value))
            tempExamCourseName.value = ""; tempExamDate.value = ""
        }
    }
    fun removeStudentExam(exam: ExamDetail) { studentExams.remove(exam) }

    private fun resetAllProfessionSpecificStatesExcept(currentProfession: String? = null) {
        if (currentProfession != "Öğrenci") { _hasUpcomingExams.value = null; studentExams.clear(); tempExamCourseName.value = ""; tempExamDate.value = "" }
        if (currentProfession != "Yazılımcı") { _developerHasProjectDeadline.value = null; developerProjectDetails.value = "" }
        if (currentProfession != "Doktor") { _doctorHasUpcomingShift.value = null; doctorShiftDetails.value = "" }
        if (currentProfession != "Öğretmen") { _teacherHasUrgentTask.value = null; teacherUrgentTaskDetails.value = "" }
        if (currentProfession != "Sanatçı") { _artistHasDeadlineOrEvent.value = null; artistEventDetails.value = "" }
    }

    fun setSnoozeCount(count: Int) { _snoozeCount.value = count.coerceAtLeast(0) }
    fun incrementSnooze() { _snoozeCount.value++ }
    fun decrementSnooze() { if (_snoozeCount.value > 0) _snoozeCount.value-- }
    fun setTranscribedText(text: String?) { _transcribedVoiceInput.value = text }
    fun startListening() { _isListeningForVoice.value = true; _transcribedVoiceInput.value = null }
    fun stopListening() { _isListeningForVoice.value = false }

    fun startInitialAnalysis() {
        viewModelScope.launch {
            val mood = determineWakeMood(smileProbability.value, _snoozeCount.value, _transcribedVoiceInput.value)
            _preliminaryMoodStore = mood
            _currentMood.value = mood
            _needsMoodConfirmation.value = true
            navigateTo(Screen.MOOD_CONFIRMATION)
        }
    }

    fun userConfirmsMood(confirmed: Boolean) {
        _needsMoodConfirmation.value = false
        if (confirmed && _preliminaryMoodStore != null) {
            _currentMood.value = _preliminaryMoodStore
            generatePlanWithGemini(_preliminaryMoodStore!!)
        } else {
            _currentMood.value = null; _preliminaryMoodStore = null; navigateTo(Screen.MAIN_ANALYSIS)
        }
    }

    private fun generatePlanWithGemini(mood: WakeMood) {
        if (generativeModel == null) {
            Log.w("Gemini", "GenerativeModel not initialized. Using fallback tasks for main plan.")
            tasks.clear(); tasks.addAll(fallbackTasks.map { it.copy(detailPromptInstruction = "Kullanıcının \"${it.title}\" görevi için detaylı adımlar oluştur.") }); navigateTo(Screen.PLAN_DISPLAY)
            return
        }
        _isLoadingPlan.value = true
        viewModelScope.launch {
            try {
                val prompt = buildGeminiPrompt(mood)
                Log.d("GeminiPrompt", "Sending to Gemini for main plan: $prompt")
                val response = generativeModel!!.generateContent(prompt)
                val geminiTextResponse = response.text ?: ""
                Log.d("GeminiResponse", "Main plan response: $geminiTextResponse")
                val generatedTasks = parseGeminiResponseToTasks(geminiTextResponse)
                if (generatedTasks.isNotEmpty()) { tasks.clear(); tasks.addAll(generatedTasks) }
                else { Log.w("Gemini", "Gemini returned no main tasks. Using fallback."); tasks.clear(); tasks.addAll(fallbackTasks.map { it.copy(detailPromptInstruction = "Kullanıcının \"${it.title}\" görevi için detaylı adımlar oluştur.") }) }
            } catch (e: Exception) {
                Log.e("GeminiCall", "Error calling Gemini for main plan: ${e.message}", e)
                tasks.clear(); tasks.addAll(fallbackTasks.map { it.copy(detailPromptInstruction = "Kullanıcının \"${it.title}\" görevi için detaylı adımlar oluştur.") })
            } finally { _isLoadingPlan.value = false; navigateTo(Screen.PLAN_DISPLAY) }
        }
    }

    fun fetchSubTasksFor(taskItem: TaskItem) {
        if (taskItem.detailPromptInstruction == null || generativeModel == null) {
            Log.w("GeminiSubTask", "No detail prompt or model not initialized for task: ${taskItem.title}")
            val taskIndex = tasks.indexOfFirst { it.id == taskItem.id }
            if (taskIndex != -1 && tasks.indices.contains(taskIndex)) {
                tasks[taskIndex] = tasks[taskIndex].copy(subTasks = listOf("Detay prompt'u veya model bulunamadı."), isLoadingSubTasks = false)
            }
            return
        }
        val taskIndex = tasks.indexOfFirst { it.id == taskItem.id }
        if (taskIndex == -1) return
        tasks[taskIndex] = taskItem.copy(isLoadingSubTasks = true, subTasks = emptyList())
        viewModelScope.launch {
            try {
                val prompt = taskItem.detailPromptInstruction
                Log.d("GeminiSubTask", "Requesting sub-tasks for: ${taskItem.title} with prompt: $prompt")
                val response = generativeModel!!.generateContent(prompt)
                val subTaskText = response.text ?: ""
                Log.d("GeminiSubTask", "Response for sub-tasks for '${taskItem.title}':\n$subTaskText")
                val parsedSubTasks = subTaskText.lines().map { it.trim() }.filter { it.startsWith("* ") || it.startsWith("- ") }.map { it.removePrefix("* ").removePrefix("- ").trim() }.filter { it.isNotBlank() }
                if (tasks.indices.contains(taskIndex)) {
                    tasks[taskIndex] = tasks[taskIndex].copy(subTasks = parsedSubTasks, isLoadingSubTasks = false)
                }
            } catch (e: Exception) {
                Log.e("GeminiSubTask", "Error fetching sub-tasks for ${taskItem.title}: ${e.message}", e)
                if (tasks.indices.contains(taskIndex)) {
                    tasks[taskIndex] = tasks[taskIndex].copy(subTasks = listOf("Detaylar alınırken bir hata oluştu."), isLoadingSubTasks = false)
                }
            }
        }
    }

    fun createSpotifyPlaylistForMood(durationMinutes: Int) {
        val mood = currentMood.value
        val token = _spotifyAccessToken.value
        if (mood == null) { _spotifyMessage.value = "Önce ruh haliniz analiz edilmeli."; return }
        if (token.isNullOrBlank()) { _spotifyMessage.value = "Lütfen önce Spotify hesabınıza bağlanın."; triggerSpotifyAuthRequest(); return }
        if (generativeModel == null) { _spotifyMessage.value = "Yapay zeka modeli hazır değil."; return }

        _isCreatingPlaylist.value = true
        _createdPlaylistUrl.value = null
        _spotifyMessage.value = "Şarkılar Gemini'den isteniyor..."
        viewModelScope.launch {
            try {
                val promptForSongs = buildGeminiPromptForSongs(mood, durationMinutes)
                Log.d("GeminiSongPrompt", promptForSongs)
                val geminiResponse = generativeModel!!.generateContent(promptForSongs)
                val songSuggestionsText = geminiResponse.text ?: ""
                Log.d("GeminiSongResponse", songSuggestionsText)
                val suggestedSongs = parseGeminiResponseToSongList(songSuggestionsText)

                if (suggestedSongs.isEmpty()) { _spotifyMessage.value = "Modunuza uygun şarkı önerisi bulunamadı."; throw Exception("Gemini returned no song suggestions.") }
                _spotifyMessage.value = "Şarkılar Spotify'da aranıyor..."
                val userId = getSpotifyUserId(token)
                if (userId.isNullOrBlank()) { _spotifyMessage.value = "Spotify kullanıcı ID'si alınamadı."; throw Exception("Failed to get Spotify User ID.") }

                val trackUris = mutableListOf<String>()
                for (songPair in suggestedSongs) {
                    val trackUri = searchSpotifyTrackUri(token, songPair.first, songPair.second)
                    if (trackUri != null) trackUris.add(trackUri)
                    else Log.w("SpotifySearch", "Track not found: ${songPair.first} - ${songPair.second}")
                    if (trackUris.size >= 20) break // Max 20 şarkı şimdilik
                    delay(250) // API rate limitleri için küçük bir gecikme
                }

                if (trackUris.isEmpty()) { _spotifyMessage.value = "Önerilen şarkılar Spotify'da bulunamadı."; throw Exception("No track URIs found.") }
                _spotifyMessage.value = "Çalma listesi oluşturuluyor..."
                val playlistName = "${SimpleDateFormat("dd MMMM yyyy", Locale("tr", "TR")).format(Calendar.getInstance().time)} Mod Planı"
                val playlistDescription = "WakeMood Planner tarafından ${mood.description} modunuz için oluşturuldu."
                val newPlaylist = createSpotifyPlaylist(token, userId, playlistName, playlistDescription)
                val playlistId = newPlaylist?.first
                val playlistUrl = newPlaylist?.second
                if (playlistId.isNullOrBlank() || playlistUrl.isNullOrBlank()) { _spotifyMessage.value = "Spotify çalma listesi oluşturulamadı."; throw Exception("Failed to create playlist.") }
                _spotifyMessage.value = "Şarkılar listeye ekleniyor..."
                addTracksToSpotifyPlaylist(token, playlistId, trackUris)
                _createdPlaylistUrl.value = playlistUrl
                _spotifyMessage.value = "'$playlistName' listeniz başarıyla oluşturuldu!"
                Log.i("Spotify", "Playlist '$playlistId' created. URL: $playlistUrl")
            } catch (e: Exception) {
                Log.e("Spotify", "Error in createSpotifyPlaylistForMood: ${e.message}", e)
                if (_spotifyMessage.value == null || !_spotifyMessage.value!!.contains("başarıyla", ignoreCase = true)) {
                    _spotifyMessage.value = "Çalma listesi hatası: ${e.localizedMessage?.take(100)}"
                }
            } finally { _isCreatingPlaylist.value = false }
        }
    }

    private suspend fun getSpotifyUserId(token: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.spotify.com/v1/me")
            (url.openConnection() as? HttpURLConnection)?.run {
                requestMethod = "GET"; setRequestProperty("Authorization", "Bearer $token"); setRequestProperty("Content-Type", "application/json")
                if (responseCode == HttpURLConnection.HTTP_OK) JSONObject(BufferedReader(InputStreamReader(inputStream)).readText()).optString("id")
                else { Log.e("SpotifyAPI", "Get User ID Error: $responseCode - ${errorStream?.bufferedReader()?.readText()}"); null }
            }
        } catch (e: Exception) { Log.e("SpotifyAPI", "Get User ID Exception: ${e.message}", e); null }
    }

    private suspend fun searchSpotifyTrackUri(token: String, trackName: String, artistName: String): String? = withContext(Dispatchers.IO) {
        try {
            val query = URLEncoder.encode("track:\"$trackName\" artist:\"$artistName\"", "UTF-8")
            val url = URL("https://api.spotify.com/v1/search?q=$query&type=track&limit=1")
            (url.openConnection() as? HttpURLConnection)?.run {
                requestMethod = "GET"; setRequestProperty("Authorization", "Bearer $token"); setRequestProperty("Accept", "application/json")
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val itemsArray = JSONObject(BufferedReader(InputStreamReader(inputStream)).readText()).optJSONObject("tracks")?.optJSONArray("items")
                    if (itemsArray != null && itemsArray.length() > 0) itemsArray.getJSONObject(0).optString("uri").ifBlank { null } else null
                } else { Log.e("SpotifyAPI_Search", "Search Error '$trackName - $artistName': $responseCode - ${errorStream?.bufferedReader()?.readText()}"); null }
            }
        } catch (e: Exception) { Log.e("SpotifyAPI_Search", "Search Exception '$trackName - $artistName': ${e.message}", e); null }
    }

    private suspend fun createSpotifyPlaylist(token: String, userId: String, name: String, description: String): Pair<String?, String?>? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.spotify.com/v1/users/$userId/playlists")
            val payload = JSONObject().apply { put("name", name); put("description", description); put("public", false) }.toString()
            (url.openConnection() as? HttpURLConnection)?.run {
                requestMethod = "POST"; setRequestProperty("Authorization", "Bearer $token"); setRequestProperty("Content-Type", "application/json"); doOutput = true
                OutputStreamWriter(outputStream).use { it.write(payload) }
                if (responseCode == HttpURLConnection.HTTP_CREATED) {
                    val json = JSONObject(BufferedReader(InputStreamReader(inputStream)).readText())
                    Pair(json.optString("id"), json.optJSONObject("external_urls")?.optString("spotify"))
                } else { Log.e("SpotifyAPI", "Create Playlist Error: $responseCode - ${errorStream?.bufferedReader()?.readText()}"); null }
            }
        } catch (e: Exception) { Log.e("SpotifyAPI", "Create Playlist Exception: ${e.message}", e); null }
    }

    private suspend fun addTracksToSpotifyPlaylist(token: String, playlistId: String, trackUris: List<String>): Boolean = withContext(Dispatchers.IO) {
        if (trackUris.isEmpty()) return@withContext true
        val chunks = trackUris.chunked(100); var allSuccessful = true
        for (chunk in chunks) { if (!addChunkToPlaylist(token, playlistId, chunk)) { allSuccessful = false; break }; if (chunks.size > 1) delay(500) }
        return@withContext allSuccessful
    }

    private suspend fun addChunkToPlaylist(token: String, playlistId: String, chunkUris: List<String>): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.spotify.com/v1/playlists/$playlistId/tracks")
            val payload = JSONObject().apply { put("uris", JSONArray(chunkUris)) }.toString()
            (url.openConnection() as? HttpURLConnection)?.run {
                requestMethod = "POST"; setRequestProperty("Authorization", "Bearer $token"); setRequestProperty("Content-Type", "application/json"); doOutput = true
                OutputStreamWriter(outputStream).use { it.write(payload) }
                val success = responseCode == HttpURLConnection.HTTP_CREATED
                if(!success) Log.e("SpotifyAPI_AddTracks", "Add Tracks Chunk Error: $responseCode - ${errorStream?.bufferedReader()?.readText()}")
                success
            } ?: false
        } catch (e: Exception) { Log.e("SpotifyAPI_AddTracks", "Add Tracks Chunk Exception: ${e.message}", e); false }
    }

    private fun buildGeminiPrompt(mood: WakeMood): String {
        val calendar = Calendar.getInstance()
        val currentDayName = SimpleDateFormat("EEEE", Locale("tr", "TR")).format(calendar.time)
        val currentTime = SimpleDateFormat("HH:mm", Locale("tr", "TR")).format(calendar.time)
        val todayFormatted = SimpleDateFormat("dd MMMM EEEE", Locale("tr", "TR")).format(calendar.time)
        var prompt = "Sen, kullanıcıların sabahki ruh hallerine, mesleklerine, günlük programlarına ve özel durumlarına (örneğin sınav takvimleri) göre son derece kişiselleştirilmiş, detaylı, motive edici ve uygulanabilir günlük eylem planları oluşturan bir yapay zeka asistanısın. Amacın, kullanıcının gününü en verimli ve pozitif şekilde geçirmesine yardımcı olmak.\n\n"
        prompt += "**KULLANICI PROFİLİ VE GÜNCEL DURUMU:**\n"; prompt += "------------------------------------\n"
        prompt += "- **Sabahki Ruh Hali:** ${mood.description}\n"; prompt += "- **Alarm Erteleme Sayısı:** ${snoozeCount.value} kez\n"
        smileProbability.value?.let { prompt += "- **Yüz İfadesi (Gülümseme Olasılığı):** ${"%.1f".format(it * 100)}%\n" }
        transcribedVoiceInput.value?.let { if (it.isNotBlank() && !it.containsAny("iptal", "anlaşılamadı", "desteklenmiyor")) { prompt += "- **Kullanıcının Sabahki Sesli Notu:** \"$it\"\n" } }
        selectedProfession.value?.let { prof ->
            prompt += "- **Meslek:** $prof\n"
            when (prof) {
                "Öğrenci" -> {
                    prompt += "- **Öğrenci Detayları:**\n"
                    if (hasUpcomingExams.value == true && studentExams.isNotEmpty()) {
                        prompt += "  - **Yaklaşan Sınavlar:**\n"; studentExams.forEach { exam -> prompt += "    - ${exam.courseName} (Tarih: ${exam.examDate})\n" }
                        var needsUrgentStudyPlan = false; var urgentExamCourse = ""
                        studentExams.forEach { exam -> if (exam.examDate.contains(currentDayName, true) || exam.examDate.contains("bugün", true) || exam.examDate.equals(todayFormatted, true) ) { needsUrgentStudyPlan = true; urgentExamCourse = exam.courseName; prompt += "  - **DİKKAT:** Bugün ($currentDayName, $todayFormatted) $urgentExamCourse sınavı var!\n"} }
                        if (needsUrgentStudyPlan) prompt += "  - **KRİTİK İSTEK (Öğrenci - Sınav Günü):** Bugün $urgentExamCourse sınavı olduğu için, bu sınava özel **ANA BAŞLIKLAR** içeren bir çalışma programı oluştur. Her ana başlık, üzerine tıklandığında daha fazla detay (saatlik plan, ipuçları vb.) alınabilecek şekilde genel bir eylemi ifade etmelidir. Örnek ana başlıklar: '$urgentExamCourse Sabah Hızlı Tekrarı', '$urgentExamCourse Öğleden Sonra Yoğun Soru Çözümü', 'Sınav Öncesi Son Kontroller'. Bu ana planın kendisi saatlik detay içermesin, sadece genel eylem başlıklarını versin.\n"
                        else prompt += "  - **İSTEK (Öğrenci - Normal Gün):** Bugün acil bir sınavı görünmüyor. Sınavlarına yönelik daha genel hazırlık başlıkları ve diğer günlük görevler öner. Her başlık genel bir eylemi ifade etsin ve üzerine tıklandığında detaylandırılabilsin.\n"
                    } else if (hasUpcomingExams.value == false) prompt += "  - **BİLGİ (Öğrenci):** Yakın zamanda önemli bir sınavı yok. Genel öğrenci verimlilik ve kişisel gelişim için ana görev başlıkları oluştur.\n"
                    else prompt += "  - **BİLGİ (Öğrenci):** Sınav durumu belirtilmedi. Genel bir öğrenci için ana görev başlıkları oluştur.\n"
                }
                "Yazılımcı" -> {
                    prompt += "- **Yazılımcı Detayları:**\n"
                    if (developerHasProjectDeadline.value == true && developerProjectDetails.value.isNotBlank()) { prompt += "  - **Proje Yetiştirme Durumu:** Evet, yetiştirmesi gereken bir projesi var. Detaylar: \"${developerProjectDetails.value}\"\n"; prompt += "  - **İSTEK (Yazılımcı - Proje):** Bu projeye odaklanmasını sağlayacak, zaman yönetimi teknikleri içeren (örn: Pomodoro) ve mola zamanlamalarını içeren görev başlıkları oluştur. Proje teslim tarihini dikkate al.\n" }
                    else if (developerHasProjectDeadline.value == false) prompt += "  - **BİLGİ (Yazılımcı):** Yakın zamanda yetiştirmesi gereken acil bir projesi yok. Kişisel gelişim (yeni teknoloji öğrenme), kod refactoring veya genel kodlama pratiği için görev başlıkları oluştur.\n"
                    else prompt += "  - **BİLGİ (Yazılımcı):** Proje durumu belirtilmedi. Genel bir yazılımcı için üretkenlik ve öğrenme odaklı görev başlıkları oluştur.\n"
                }
                "Doktor" -> {
                    prompt += "- **Doktor Detayları:**\n"
                    if (doctorHasUpcomingShift.value == true && doctorShiftDetails.value.isNotBlank()) { prompt += "  - **Nöbet/Yoğunluk Durumu:** Evet. Detaylar: \"${doctorShiftDetails.value}\"\n"; prompt += "  - **İSTEK (Doktor - Yoğun Gün):** Bu yoğunluğa hazırlık, nöbet sırasında/sonrasında yapılabilecekler ve dinlenme için görev başlıkları oluştur.\n" }
                    else if (doctorHasUpcomingShift.value == false) prompt += "  - **BİLGİ (Doktor):** Yakın zamanda özel bir yoğunluk/nöbet durumu yok. Mesleki gelişim (makale okuma), hasta takibi veya kişisel dinlenme/hobi için görev başlıkları oluştur.\n"
                    else prompt += "  - **BİLGİ (Doktor):** Özel durum belirtilmedi. Genel bir doktor için üretkenlik, öğrenme ve kişisel refah odaklı görev başlıkları oluştur.\n"
                }
                "Öğretmen" -> {
                    prompt += "- **Öğretmen Detayları:**\n"
                    if (teacherHasUrgentTask.value == true && teacherUrgentTaskDetails.value.isNotBlank()) { prompt += "  - **Acil Görev Durumu:** Evet. Detaylar: \"${teacherUrgentTaskDetails.value}\"\n"; prompt += "  - **İSTEK (Öğretmen - Acil Görev):** Belirtilen \"${teacherUrgentTaskDetails.value}\" görevini tamamlamasına yardımcı olacak adımlar ve diğer günlük işlerini dengelemesi için görev başlıkları oluştur.\n" }
                    else if (teacherHasUrgentTask.value == false) prompt += "  - **BİLGİ (Öğretmen):** Yakın zamanda özel bir acil görevi yok. Ders materyali geliştirme, öğrenci değerlendirme, mesleki gelişim veya kişisel dinlenme için görev başlıkları oluştur.\n"
                    else prompt += "  - **BİLGİ (Öğretmen):** Özel görev belirtilmedi. Genel bir öğretmen için ders hazırlığı, öğrenci etkileşimi ve kişisel refah odaklı görev başlıkları oluştur.\n"
                }
                "Sanatçı" -> {
                    prompt += "- **Sanatçı Detayları:**\n"
                    if (artistHasDeadlineOrEvent.value == true && artistEventDetails.value.isNotBlank()) { prompt += "  - **Etkinlik/Teslim Tarihi Durumu:** Evet. Detaylar: \"${artistEventDetails.value}\"\n"; prompt += "  - **İSTEK (Sanatçı - Etkinlik/Proje):** Belirtilen \"${artistEventDetails.value}\" için hazırlık adımları, yaratıcılığı artırıcı aktiviteler ve diğer günlük işlerini dengelemesi için görev başlıkları oluştur.\n" }
                    else if (artistHasDeadlineOrEvent.value == false) prompt += "  - **BİLGİ (Sanatçı):** Yakın zamanda özel bir etkinlik veya teslim tarihi yok. İlham toplama, yeni teknikler deneme, portfolyo güncelleme veya kişisel bir sanat projesi geliştirme için görev başlıkları oluştur.\n"
                    else prompt += "  - **BİLGİ (Sanatçı):** Özel durum belirtilmedi. Genel bir sanatçı için yaratıcılık, pratik ve ilham odaklı görev başlıkları oluştur.\n"
                }
                "Diğer" -> { prompt += "  - **İSTEK (Genel - Diğer):** Kullanıcının 'Diğer' olarak belirttiği mesleği için genel üretkenlik, kişisel gelişim ve refah odaklı görev başlıkları oluştur.\n"}
            }
        } ?: run {
            prompt += "- **Meslek:** Belirtilmemiş.\n  - **İSTEK (Genel):** Kullanıcı için genel üretkenlik ve iyi hissetme odaklı görev başlıkları oluştur.\n"
        }
        prompt += "------------------------------------\n\n"
        prompt += "**İSTENEN ANA GÖREV BAŞLIKLARI FORMATI VE DETAYLARI:**\n"; prompt += "------------------------------------\n"
        prompt += "1. Plan, kullanıcının mevcut ruh halini (**${mood.description}**) iyileştirmeye veya bu ruh halinden en iyi şekilde faydalanmaya yönelik olmalıdır.\n"
        prompt += "2. **ANA GÖREV BAŞLIKLARI** olarak **4 ila 6 madde** listele. Bu başlıklar genel eylemleri ifade etmelidir (örn: 'Sabah Egzersizi', 'Proje X Üzerinde Çalışma', 'Akşam Yemeği Hazırlığı'). Detayları bu aşamada verme.\n"
        prompt += "3. Her ana görev başlığı yeni satırda ve '-' ile başlamalıdır. Başka hiçbir ek bilgi (parantez, süre, açıklama) bu ana başlık satırında OLMAMALIDIR.\n"
        prompt += "   Kesinlikle Uyulması Gereken Ana Görev Başlığı Formatı: - [ANA GÖREV BAŞLIĞI]\n"
        prompt += "4. **ÖRNEK ANA GÖREV BAŞLIKLARI (Bu örnekleri doğrudan kullanma, sadece format için referans al. Parantez içi detayları KESİNLİKLE EKLEME!):**\n"
        prompt += "   - Güne Başlangıç Rutini\n   - Odaklanmış Çalışma Seansı\n   - Öğle Yemeği ve Kısa Mola\n   - Matematik Sınavı Hazırlığı\n   - Akşam Dinlenme Aktivitesi\n"
        prompt += "5. Bugün $currentDayName, saat şu an $currentTime. Planı bu zamanı dikkate alarak oluştur.\n"
        prompt += "6. Eğer kullanıcı öğrenci ve sınavı varsa, ana görev başlıklarının bir veya birkaçı sınav hazırlığına yönelik olmalı. Diğer durumlarda, mesleğine ve genel üretkenliğe uygun başlıklar öner.\n"
        prompt += "------------------------------------\n"
        prompt += "**ANA GÖREV BAŞLIKLARINI ŞİMDİ OLUŞTUR (SADECE '-' İLE BAŞLAYAN, YALIN ANA BAŞLIKLARI İÇERMELİ. PARANTEZ, SÜRE, AÇIKLAMA YOK!):**\n"
        return prompt
    }

    private fun parseGeminiResponseToTasks(response: String): List<TaskItem> {
        Log.d("GeminiParse", "Raw response from Gemini for main plan:\n$response")
        val parsedTasks = response.lines().map { it.trim() }.filter { it.startsWith("- ") }.map { line ->
            val mainTitle = line.removePrefix("- ").trim()
            var detailPrompt = "Kullanıcının \"$mainTitle\" adlı ana görevi için 3-5 maddelik, uygulanabilir alt adımlar veya detaylı bir eylem planı oluştur. Cevabın sadece bu alt adımları/detayları içermeli ve her birini '*' veya '-' ile başlatmalısın. Kullanıcının mesleği: ${selectedProfession.value ?: "Belirtilmemiş"}. "
            when (selectedProfession.value) {
                "Öğrenci" -> { if (hasUpcomingExams.value == true && studentExams.isNotEmpty()) { detailPrompt += "Yaklaşan sınavları var: ${studentExams.joinToString { exam -> "${exam.courseName} (${exam.examDate})" }}. "; val matchingExam = studentExams.firstOrNull { mainTitle.contains(it.courseName, ignoreCase = true) }; if (matchingExam != null) { detailPrompt += "\"$mainTitle\" görevi ${matchingExam.courseName} sınavıyla ilgili görünüyor. Bu sınav için saatlik çalışma blokları, mola zamanları ve spesifik çalışma teknikleri (Pomodoro, Feynman vb.) içeren detaylı bir alt plan öner. " } else { detailPrompt += "Bu genel bir görev gibi görünüyor, öğrencinin sınavlarını da göz önünde bulundurarak detaylandır. " } } else { detailPrompt += "Öğrencinin yakın zamanda sınavı yok. Bu görevi genel öğrenci verimliliği ve kişisel gelişim bağlamında detaylandır. " } }
                "Yazılımcı" -> { if (developerHasProjectDeadline.value == true && developerProjectDetails.value.isNotBlank()) { detailPrompt += "Yetiştirmesi gereken projesi var: \"${developerProjectDetails.value}\". Bu görevi proje hedefleri ve teslim tarihi bağlamında detaylandır. Teknik adımlar, araştırma veya test süreçleri gibi alt başlıklar ekle. " } else { detailPrompt += "Yazılımcının acil bir projesi yok. Bu görevi kişisel gelişim, yeni bir teknoloji öğrenme veya bir yan proje geliştirme bağlamında detaylandır. " } }
                "Doktor" -> { if (doctorHasUpcomingShift.value == true && doctorShiftDetails.value.isNotBlank()) { detailPrompt += "Yakın zamanda bir nöbeti/yoğunluğu var: \"${doctorShiftDetails.value}\". Bu görevi bu yoğunluk öncesi hazırlık, yoğunluk sırası veya sonrası dinlenme bağlamında detaylandır. " } else { detailPrompt += "Doktorun acil bir nöbeti/yoğunluğu yok. Bu görevi mesleki gelişim (makale okuma), hasta iletişimi veya kişisel refah (stres yönetimi, hobi) bağlamında detaylandır. " } }
                "Öğretmen" -> { if (teacherHasUrgentTask.value == true && teacherUrgentTaskDetails.value.isNotBlank()) { detailPrompt += "Acil bir görevi var: \"${teacherUrgentTaskDetails.value}\". Bu görevi, belirtilen acil işi tamamlamaya yönelik adımlar veya ders materyali hazırlığı, öğrenci değerlendirmesi gibi bağlamlarda detaylandır. " } else { detailPrompt += "Öğretmenin acil bir görevi yok. Bu görevi ders planı geliştirme, yeni öğretim teknikleri araştırma veya mesleki gelişim bağlamında detaylandır. " } }
                "Sanatçı" -> { if (artistHasDeadlineOrEvent.value == true && artistEventDetails.value.isNotBlank()) { detailPrompt += "Bir etkinlik/teslim tarihi var: \"${artistEventDetails.value}\". Bu görevi, belirtilen etkinliğe yönelik yaratıcı süreç adımları, pratik yapma veya eser tamamlama bağlamında detaylandır. " } else { detailPrompt += "Sanatçının acil bir etkinliği/teslim tarihi yok. Bu görevi ilham arama, yeni teknikler deneme, portfolyo oluşturma veya kişisel bir sanat projesi geliştirme bağlamında detaylandır. " } }
                else -> { detailPrompt += "Bu genel bir görev. Kullanıcının üretkenliğini artıracak veya iyi hissetmesini sağlayacak şekilde detaylandır. " }
            }
            detailPrompt += "Kullanıcının genel ruh hali: ${currentMood.value?.description ?: "Belirtilmemiş"}. Bu detayı da alt görevleri oluştururken göz önünde bulundur."
            TaskItem(title = mainTitle, detailPromptInstruction = detailPrompt)
        }.filter { it.title.isNotBlank() }
        Log.d("GeminiParse", "Parsed ${parsedTasks.size} main tasks with updated detail prompts.")
        if (parsedTasks.isEmpty() && response.isNotBlank()) {
            Log.w("GeminiParse", "Could not parse main tasks. Using fallback.")
            val fallbackParsed = response.lines().map{it.trim()}.filter{it.isNotBlank() && !it.startsWith("*") && !it.startsWith("#") && it.length > 5 && !it.contains(":")}.map{TaskItem(title=it, detailPromptInstruction = "Kullanıcının \"$it\" görevi için detaylı adımlar oluştur.")}
            if(fallbackParsed.isNotEmpty()) return fallbackParsed
            return fallbackTasks.map { it.copy(detailPromptInstruction = "Kullanıcının \"${it.title}\" görevi için detaylı adımlar oluştur.") }
        }
        return if (parsedTasks.isEmpty()) fallbackTasks.map { it.copy(detailPromptInstruction = "Kullanıcının \"${it.title}\" görevi için detaylı adımlar oluştur.") } else parsedTasks
    }

    private fun buildGeminiPromptForSongs(mood: WakeMood, durationMinutes: Int): String {
        var prompt = "Kullanıcının sabahki ruh hali: ${mood.description}. "
        selectedProfession.value?.let { prompt += "Mesleği: $it. " }
        prompt += "Bu ruh haline ve yaklaşık $durationMinutes dakikalık bir dinleme süresine uygun, bir Spotify çalma listesi için şarkı önerileri yap. Her öneri 'Şarkı Adı - Sanatçı Adı' formatında olmalı ve her biri yeni bir satırda listelenmelidir. Amacımız, kullanıcının moduna uygun bir atmosfer yaratmak. Örneğin, '${WakeMood.ENERGETIC_READY.description}' modu için tempolu ve motive edici şarkılar; '${WakeMood.GRUMPY_STRESSED.description}' modu için sakinleştirici ve rahatlatıcı enstrümantal veya düşük tempolu şarkılar; '${WakeMood.SLEEPY_TIRED.description}' modu için yumuşak ve huzurlu melodiler öner.\n"
        prompt += "Lütfen sadece şarkı ve sanatçı listesini ver, başka bir yorum veya giriş cümlesi ekleme.\nÖrnek:\nBohemian Rhapsody - Queen\nImagine - John Lennon\nİşte şarkı listesi (en az ${durationMinutes / 5} şarkı, en fazla ${durationMinutes / 3} şarkı):\n"
        return prompt
    }

    private fun parseGeminiResponseToSongList(response: String): List<Pair<String, String>> {
        return response.lines().map { it.trim() }.filter { it.contains(" - ") && it.length > 5 && (it.startsWith("- ") || it.firstOrNull()?.isLetterOrDigit() == true) }.mapNotNull { line ->
            val cleanLine = line.removePrefix("- ").trim()
            val parts = cleanLine.split(" - ", limit = 2)
            if (parts.size == 2) { Pair(parts[0].trim(), parts[1].trim()) } else { null }
        }
    }

    private fun determineWakeMood(smileProb: Float?, snoozeCount: Int, voiceText: String?): WakeMood {
        val actualSmileProb = smileProb ?: 0f; var moodScore = 0
        if (actualSmileProb > 0.65f) moodScore += 2 else if (actualSmileProb > 0.35f) moodScore += 1 else if (actualSmileProb < 0.15f && smileProb != null) moodScore -=1
        if (snoozeCount == 0) moodScore += 1 else if (snoozeCount >= 3) moodScore -= 2 else if (snoozeCount >= 1) moodScore -= 1
        voiceText?.let { text -> val lowerText = text.lowercase(Locale("tr", "TR")); if (lowerText.containsAny("harika", "mükemmel", "çok iyi", "enerjiğiyim", "süper")) moodScore += 2 else if (lowerText.containsAny("yorgunum", "kötüyüm", "uykulu", "istemiyorum", "berbat")) moodScore -= 2 else if (lowerText.containsAny("iyiyim", "fena değil", "normal")) moodScore += 1 else if (lowerText.containsAny("sinirliyim", "stresliyim", "gerginim", "kızgınım")) moodScore -= 1 }
        Log.d("MoodDetermination", "Smile: $actualSmileProb, Snooze: $snoozeCount, Voice: \"$voiceText\", Score: $moodScore")
        return when { moodScore >= 3 -> WakeMood.ENERGETIC_READY; moodScore <= -2 -> WakeMood.SLEEPY_TIRED; (moodScore <= 0 && snoozeCount >= 2) || (voiceText?.lowercase(Locale("tr", "TR"))?.containsAny("sinirli", "stresli", "gergin") == true) -> WakeMood.GRUMPY_STRESSED; else -> WakeMood.NEUTRAL }
    }

    fun toggleTaskCompletion(task: TaskItem) {
        val index = tasks.indexOfFirst { it.id == task.id }
        if (index != -1) { tasks[index] = tasks[index].copy(isCompleted = !tasks[index].isCompleted) }
    }

    fun resetForNewDay() {
        _snoozeCount.value = 0; smileProbability.value = null; _transcribedVoiceInput.value = null; _isListeningForVoice.value = false
        _currentMood.value = null; _preliminaryMoodStore = null; _needsMoodConfirmation.value = false; tasks.clear()
        navigateTo(Screen.MAIN_ANALYSIS); Log.d("ViewModel", "Daily state reset, navigating to MAIN_ANALYSIS.")
    }

    fun fullResetToLogin() {
        username.value = ""; password.value = ""; _selectedProfession.value = null; resetAllProfessionSpecificStatesExcept(null)
        _snoozeCount.value = 0; smileProbability.value = null; _transcribedVoiceInput.value = null; _isListeningForVoice.value = false
        _currentMood.value = null; _preliminaryMoodStore = null; _needsMoodConfirmation.value = false; tasks.clear()
        navigateTo(Screen.LOGIN); Log.d("ViewModel", "Full reset, navigating to LOGIN.")
    }
}

fun String.containsAny(vararg keywords: String, ignoreCase: Boolean = true): Boolean {
    val locale = Locale("tr", "TR")
    val currentString = if (ignoreCase) this.lowercase(locale) else this
    return keywords.any {
        val currentKeyword = if (ignoreCase) it.lowercase(locale) else it
        currentString.contains(currentKeyword)
    }
}