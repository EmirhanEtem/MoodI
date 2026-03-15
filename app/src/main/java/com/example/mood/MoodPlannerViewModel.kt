package com.example.mood

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mood.analysis.*
import com.example.mood.data.AppDatabase
import com.example.mood.data.MoodEntryEntity
import com.example.mood.data.TaskCompletionEntity
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

class MoodPlannerViewModel(application: Application) : AndroidViewModel(application) {

    // --- Repositories & Services ---
    private val userRepository = UserRepository(application)
    private val sessionManager = UserSessionManager(application)
    private val moodAnalysisEngine = MoodAnalysisEngine()
    private val voiceToneAnalyzer = VoiceToneAnalyzer()
    private val db = AppDatabase.getInstance(application)

    // --- Navigation ---
    private val _currentScreen = MutableStateFlow(
        if (sessionManager.isLoggedIn) Screen.MAIN_ANALYSIS
        else if (sessionManager.onboardingComplete) Screen.LOGIN
        else Screen.ONBOARDING
    )
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // --- Auth State ---
    var loginUsername = mutableStateOf("")
    var loginPassword = mutableStateOf("")
    var registerUsername = mutableStateOf("")
    var registerPassword = mutableStateOf("")
    var registerPasswordConfirm = mutableStateOf("")
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()
    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()
    private val _loggedInUsername = MutableStateFlow(sessionManager.username)
    val loggedInUsername: StateFlow<String> = _loggedInUsername.asStateFlow()

    // --- Profession ---
    private val _selectedProfession = MutableStateFlow<String?>(null)
    val selectedProfession: StateFlow<String?> = _selectedProfession.asStateFlow()
    val professions = listOf("Öğrenci", "Yazılımcı", "Doktor", "Öğretmen", "Sanatçı", "Diğer")

    // --- Profession-specific ---
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

    // --- Analysis State ---
    private val _snoozeCount = MutableStateFlow(0)
    val snoozeCount: StateFlow<Int> = _snoozeCount.asStateFlow()
    private val _currentMood = MutableStateFlow<WakeMood?>(null)
    val currentMood: StateFlow<WakeMood?> = _currentMood.asStateFlow()
    private val _currentMoodDimensions = MutableStateFlow(MoodDimension.DEFAULT)
    val currentMoodDimensions: StateFlow<MoodDimension> = _currentMoodDimensions.asStateFlow()
    var smileProbability = mutableStateOf<Float?>(null)
    private val _faceAnalysisResult = MutableStateFlow(FaceAnalysisResult())
    val faceAnalysisResult: StateFlow<FaceAnalysisResult> = _faceAnalysisResult.asStateFlow()
    private val _voiceToneResult = MutableStateFlow(VoiceToneResult())
    val voiceToneResult: StateFlow<VoiceToneResult> = _voiceToneResult.asStateFlow()
    private val _transcribedVoiceInput = MutableStateFlow<String?>(null)
    val transcribedVoiceInput: StateFlow<String?> = _transcribedVoiceInput.asStateFlow()
    private val _isListeningForVoice = MutableStateFlow(false)
    val isListeningForVoice: StateFlow<Boolean> = _isListeningForVoice.asStateFlow()
    private val _isAnalyzingVoiceTone = MutableStateFlow(false)
    val isAnalyzingVoiceTone: StateFlow<Boolean> = _isAnalyzingVoiceTone.asStateFlow()
    private val _needsMoodConfirmation = MutableStateFlow(false)
    val needsMoodConfirmation: StateFlow<Boolean> = _needsMoodConfirmation.asStateFlow()
    private var _preliminaryMoodStore: WakeMood? = null
    val tasks = mutableStateListOf<TaskItem>()
    private val _isLoadingPlan = MutableStateFlow(false)
    val isLoadingPlan: StateFlow<Boolean> = _isLoadingPlan.asStateFlow()

    // --- Spotify ---
    private val _spotifyAccessToken = MutableStateFlow<String?>(sessionManager.spotifyAccessToken)
    val spotifyAccessToken: StateFlow<String?> = _spotifyAccessToken.asStateFlow()
    private val _isSpotifyAuthenticated = MutableStateFlow(sessionManager.isSpotifyTokenValid())
    val isSpotifyAuthenticated: StateFlow<Boolean> = _isSpotifyAuthenticated.asStateFlow()
    private val _createdPlaylistUrl = MutableStateFlow<String?>(null)
    val createdPlaylistUrl: StateFlow<String?> = _createdPlaylistUrl.asStateFlow()
    private val _spotifyMessage = MutableStateFlow<String?>(null)
    val spotifyMessage: StateFlow<String?> = _spotifyMessage.asStateFlow()
    var playlistDurationPreferenceMinutes = mutableStateOf(30)
    private val _isCreatingPlaylist = MutableStateFlow(false)
    val isCreatingPlaylist: StateFlow<Boolean> = _isCreatingPlaylist.asStateFlow()
    private val _spotifyAuthEvent = MutableStateFlow<Unit?>(null)
    val spotifyAuthEvent: StateFlow<Unit?> = _spotifyAuthEvent.asStateFlow()

    // --- Analytics ---
    private val _moodHistory = MutableStateFlow<List<MoodEntryEntity>>(emptyList())
    val moodHistory: StateFlow<List<MoodEntryEntity>> = _moodHistory.asStateFlow()
    private val _totalAnalysisCount = MutableStateFlow(0)
    val totalAnalysisCount: StateFlow<Int> = _totalAnalysisCount.asStateFlow()

    // --- Gemini ---
    private val fallbackTasks = listOf(
        TaskItem(title="Gününü planla", difficulty=1, estimatedTimeMinutes=10, tags=listOf("planning"), detailPromptInstruction = "Kullanıcının \"Gününü planla\" görevi için detaylı adımlar oluştur."),
        TaskItem(title="Bir bardak su iç", difficulty=1, estimatedTimeMinutes=2, tags=listOf("health"), detailPromptInstruction = "Kullanıcının \"Bir bardak su iç\" eyleminin faydalarını açıkla."),
        TaskItem(title="Kısa bir yürüyüş yap", difficulty=1, estimatedTimeMinutes=15, tags=listOf("physical"), detailPromptInstruction = "Kullanıcının \"Kısa bir yürüyüş yap\" görevi için ipuçları listele.")
    )
    private var generativeModel: GenerativeModel? = null
    private val gson = Gson()

    init {
        viewModelScope.launch(Dispatchers.IO) {
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
                    generativeModel = GenerativeModel(modelName = "gemini-2.5-flash", apiKey = apiKey, generationConfig = config, safetySettings = safetySettings)
                    Log.i("GeminiInit", "Gemini Model Initialized successfully.")
                } else {
                    Log.e("GeminiInit", "GEMINI_API_KEY not found.")
                }
            } catch (e: Exception) {
                Log.e("GeminiInit", "Error initializing Gemini: ${e.message}", e)
            }
        }
        if (sessionManager.isLoggedIn) {
            loadMoodHistory()
            loadUserDetails()
        }
    }

    // ==================== AUTH ====================
    fun processLogin() {
        if (loginUsername.value.isBlank() || loginPassword.value.isBlank()) {
            _authError.value = "Lütfen tüm alanları doldurun"; return
        }
        _isAuthLoading.value = true; _authError.value = null
        viewModelScope.launch {
            val result = userRepository.login(loginUsername.value.trim(), loginPassword.value)
            result.onSuccess { user ->
                sessionManager.saveLoginSession(user.id, user.username)
                _loggedInUsername.value = user.username
                _selectedProfession.value = user.profession
                loadMoodHistory()
                if (user.profession != null) {
                    loadUserDetails()
                    navigateTo(Screen.MAIN_ANALYSIS)
                } else {
                    navigateTo(Screen.PROFESSION_SELECTION)
                }
            }.onFailure { e ->
                _authError.value = e.message ?: "Giriş başarısız"
            }
            _isAuthLoading.value = false
        }
    }

    fun processRegister() {
        if (registerUsername.value.isBlank() || registerPassword.value.isBlank()) {
            _authError.value = "Lütfen tüm alanları doldurun"; return
        }
        if (registerPassword.value.length < 4) {
            _authError.value = "Şifre en az 4 karakter olmalı"; return
        }
        if (registerPassword.value != registerPasswordConfirm.value) {
            _authError.value = "Şifreler eşleşmiyor"; return
        }
        _isAuthLoading.value = true; _authError.value = null
        viewModelScope.launch {
            val result = userRepository.register(registerUsername.value.trim(), registerPassword.value)
            result.onSuccess { userId ->
                sessionManager.saveLoginSession(userId, registerUsername.value.trim())
                _loggedInUsername.value = registerUsername.value.trim()
                navigateTo(Screen.PROFESSION_SELECTION)
            }.onFailure { e ->
                _authError.value = e.message ?: "Kayıt başarısız"
            }
            _isAuthLoading.value = false
        }
    }

    fun completeOnboarding() {
        sessionManager.onboardingComplete = true
        navigateTo(Screen.LOGIN)
    }

    fun clearAuthError() { _authError.value = null }

    // ==================== NAVIGATION ====================
    fun navigateTo(screen: Screen) {
        Log.d("ViewModelNav", "Navigating to: ${screen.name} from ${_currentScreen.value.name}")
        _currentScreen.value = screen
    }

    fun finishProfessionSetup() {
        saveProfessionDetails()
        navigateTo(Screen.MAIN_ANALYSIS)
    }

    fun goBack() {
        val previousScreen = when (_currentScreen.value) {
            Screen.REGISTER -> Screen.LOGIN
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
            Screen.MOOD_ANALYTICS -> Screen.PLAN_DISPLAY
            Screen.NIGHT_REVIEW -> Screen.PLAN_DISPLAY
            Screen.PROFILE -> { saveProfessionDetails(); Screen.MAIN_ANALYSIS }
            else -> null
        }
        previousScreen?.let { navigateTo(it) }
    }

    // ==================== PROFESSION ====================
    fun selectProfession(profession: String) {
        _selectedProfession.value = profession
        resetAllProfessionSpecificStatesExcept(profession)
        viewModelScope.launch { userRepository.updateProfession(sessionManager.userId, profession) }
        when (profession) {
            "Öğrenci" -> navigateTo(Screen.STUDENT_EXAM_PROMPT)
            "Yazılımcı", "Doktor", "Öğretmen", "Sanatçı" -> navigateTo(Screen.PROFESSION_DETAIL_PROMPT)
            else -> navigateTo(Screen.MAIN_ANALYSIS)
        }
    }

    /** Profile screen: change profession without navigating away */
    fun selectProfessionFromProfile(profession: String) {
        _selectedProfession.value = profession
        resetAllProfessionSpecificStatesExcept(profession)
        viewModelScope.launch { userRepository.updateProfession(sessionManager.userId, profession) }
    }

    /** Profile screen: toggle profession detail without navigating away */
    fun setProfessionSpecificYesNoFromProfile(profession: String, hasSpecificEvent: Boolean) {
        when (profession) {
            "Öğrenci" -> { _hasUpcomingExams.value = hasSpecificEvent; if (!hasSpecificEvent) studentExams.clear() }
            "Yazılımcı" -> { _developerHasProjectDeadline.value = hasSpecificEvent; if (!hasSpecificEvent) developerProjectDetails.value = "" }
            "Doktor" -> { _doctorHasUpcomingShift.value = hasSpecificEvent; if (!hasSpecificEvent) doctorShiftDetails.value = "" }
            "Öğretmen" -> { _teacherHasUrgentTask.value = hasSpecificEvent; if (!hasSpecificEvent) teacherUrgentTaskDetails.value = "" }
            "Sanatçı" -> { _artistHasDeadlineOrEvent.value = hasSpecificEvent; if (!hasSpecificEvent) artistEventDetails.value = "" }
        }
    }

    fun setProfessionSpecificYesNo(profession: String, hasSpecificEvent: Boolean) {
        when (profession) {
            "Öğrenci" -> { _hasUpcomingExams.value = hasSpecificEvent; if (hasSpecificEvent) navigateTo(Screen.STUDENT_EXAM_INPUT) else { studentExams.clear(); saveProfessionDetails(); navigateTo(Screen.MAIN_ANALYSIS) } }
            "Yazılımcı" -> { _developerHasProjectDeadline.value = hasSpecificEvent; if (hasSpecificEvent) navigateTo(Screen.PROFESSION_DETAIL_INPUT) else { developerProjectDetails.value = ""; saveProfessionDetails(); navigateTo(Screen.MAIN_ANALYSIS) } }
            "Doktor" -> { _doctorHasUpcomingShift.value = hasSpecificEvent; if (hasSpecificEvent) navigateTo(Screen.PROFESSION_DETAIL_INPUT) else { doctorShiftDetails.value = ""; saveProfessionDetails(); navigateTo(Screen.MAIN_ANALYSIS) } }
            "Öğretmen" -> { _teacherHasUrgentTask.value = hasSpecificEvent; if (hasSpecificEvent) navigateTo(Screen.PROFESSION_DETAIL_INPUT) else { teacherUrgentTaskDetails.value = ""; saveProfessionDetails(); navigateTo(Screen.MAIN_ANALYSIS) } }
            "Sanatçı" -> { _artistHasDeadlineOrEvent.value = hasSpecificEvent; if (hasSpecificEvent) navigateTo(Screen.PROFESSION_DETAIL_INPUT) else { artistEventDetails.value = ""; saveProfessionDetails(); navigateTo(Screen.MAIN_ANALYSIS) } }
        }
    }

    fun addStudentExam() {
        if (tempExamCourseName.value.isNotBlank() && tempExamDate.value.isNotBlank()) {
            studentExams.add(ExamDetail(courseName = tempExamCourseName.value, examDate = tempExamDate.value))
            tempExamCourseName.value = ""; tempExamDate.value = ""
            saveProfessionDetails()
        }
    }
    fun removeStudentExam(exam: ExamDetail) { studentExams.remove(exam); saveProfessionDetails() }

    private fun resetAllProfessionSpecificStatesExcept(currentProfession: String? = null) {
        if (currentProfession != "Öğrenci") { _hasUpcomingExams.value = null; studentExams.clear(); tempExamCourseName.value = ""; tempExamDate.value = "" }
        if (currentProfession != "Yazılımcı") { _developerHasProjectDeadline.value = null; developerProjectDetails.value = "" }
        if (currentProfession != "Doktor") { _doctorHasUpcomingShift.value = null; doctorShiftDetails.value = "" }
        if (currentProfession != "Öğretmen") { _teacherHasUrgentTask.value = null; teacherUrgentTaskDetails.value = "" }
        if (currentProfession != "Sanatçı") { _artistHasDeadlineOrEvent.value = null; artistEventDetails.value = "" }
    }

    // Voice Capture Timer State
    private val _voiceTimer = MutableStateFlow(0)
    val voiceTimer = _voiceTimer.asStateFlow()

    private val voiceAnalysisSentences = listOf(
        "Bugün her şeyin yolunda gideceğine ve güzel bir gün olacağına inanıyorum.",
        "Hedeflerime ulaşmak için gereken enerjiye ve motivasyona sahibim.",
        "Derin bir nefes alıyorum ve bugünün bana getireceği fırsatları kucaklıyorum.",
        "Zorlukların üstesinden gelebileceğimi biliyorum, kendime güveniyorum.",
        "Bugün verimli çalışacak ve yapmak istediklerimi tamamlayacağım."
    )

    // ==================== PERSISTENCE ====================
    private fun loadUserDetails() {
        if (!sessionManager.isLoggedIn) return
        viewModelScope.launch {
            val user = userRepository.getUserById(sessionManager.userId)
            if (user != null) {
                _selectedProfession.value = user.profession
                
                if (!user.studentExamsJson.isNullOrBlank()) {
                    try {
                        val type = object : TypeToken<List<ExamDetail>>() {}.type
                        val list: List<ExamDetail> = gson.fromJson(user.studentExamsJson, type)
                        studentExams.clear()
                        studentExams.addAll(list)
                        _hasUpcomingExams.value = list.isNotEmpty()
                    } catch (e: Exception) { Log.e("LoadUser", "Error parsing exams", e) }
                }

                if (!user.developerProjectDetails.isNullOrBlank()) { developerProjectDetails.value = user.developerProjectDetails; _developerHasProjectDeadline.value = true }
                if (!user.doctorShiftDetails.isNullOrBlank()) { doctorShiftDetails.value = user.doctorShiftDetails; _doctorHasUpcomingShift.value = true }
                if (!user.teacherUrgentTaskDetails.isNullOrBlank()) { teacherUrgentTaskDetails.value = user.teacherUrgentTaskDetails; _teacherHasUrgentTask.value = true }
                if (!user.artistEventDetails.isNullOrBlank()) { artistEventDetails.value = user.artistEventDetails; _artistHasDeadlineOrEvent.value = true }
            }
        }
    }

    fun saveProfessionDetails() {
        if (!sessionManager.isLoggedIn) return
        val examsJson = if (studentExams.isNotEmpty()) gson.toJson(studentExams.toList()) else null
        viewModelScope.launch {
            userRepository.updateProfessionDetails(
                userId = sessionManager.userId,
                examsJson = examsJson,
                devDetails = if (_developerHasProjectDeadline.value == true) developerProjectDetails.value else null,
                docDetails = if (_doctorHasUpcomingShift.value == true) doctorShiftDetails.value else null,
                teacherDetails = if (_teacherHasUrgentTask.value == true) teacherUrgentTaskDetails.value else null,
                artistDetails = if (_artistHasDeadlineOrEvent.value == true) artistEventDetails.value else null
            )
        }
    }

    // ==================== ANALYSIS INPUTS ====================
    fun setSnoozeCount(count: Int) { _snoozeCount.value = count.coerceAtLeast(0) }
    fun incrementSnooze() { _snoozeCount.value++ }
    fun decrementSnooze() { if (_snoozeCount.value > 0) _snoozeCount.value-- }
    fun setTranscribedText(text: String?) { _transcribedVoiceInput.value = text }
    fun startListening() { _isListeningForVoice.value = true; _transcribedVoiceInput.value = null }
    fun stopListening() { _isListeningForVoice.value = false; _voiceTimer.value = 0 }

    fun setFaceAnalysisResult(result: FaceAnalysisResult) {
        _faceAnalysisResult.value = result
        smileProbability.value = result.smileProbability
    }

    fun startVoiceToneAnalysisFromSentence() {
        if (_isAnalyzingVoiceTone.value || _isListeningForVoice.value) return
        
        _isListeningForVoice.value = true
        _isAnalyzingVoiceTone.value = true
        val randomSentence = voiceAnalysisSentences.random()
        _transcribedVoiceInput.value = randomSentence
        
        viewModelScope.launch {
            // Timer coroutine
            launch {
                for (i in 10 downTo 1) {
                    if (!_isListeningForVoice.value) break
                    _voiceTimer.value = i
                    delay(1000)
                }
                _voiceTimer.value = 0
                _isListeningForVoice.value = false
            }
            
            // Analyzer coroutine
            try {
                val result = voiceToneAnalyzer.analyzeVoiceTone(
                    durationMs = 10000L,
                    transcribedText = randomSentence
                )
                if (_voiceTimer.value <= 1) { // Only save if not cancelled early
                    _voiceToneResult.value = result
                }
                Log.d("VoiceTone", "Analysis complete: energy=${result.estimatedEnergy}, stress=${result.estimatedStress}")
            } catch (e: Exception) {
                Log.e("VoiceTone", "Error: ${e.message}", e)
            } finally {
                _isAnalyzingVoiceTone.value = false
                _isListeningForVoice.value = false
                _voiceTimer.value = 0
            }
        }
    }

    // ==================== MOOD ANALYSIS ====================
    fun startInitialAnalysis() {
        viewModelScope.launch {
            val input = MoodAnalysisInput(
                faceResult = _faceAnalysisResult.value,
                voiceToneResult = _voiceToneResult.value,
                transcribedText = _transcribedVoiceInput.value,
                snoozeCount = _snoozeCount.value,
                hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            )
            val (mood, dimensions) = moodAnalysisEngine.analyze(input)
            _preliminaryMoodStore = mood
            _currentMood.value = mood
            _currentMoodDimensions.value = dimensions
            _needsMoodConfirmation.value = true
            navigateTo(Screen.MOOD_CONFIRMATION)
        }
    }

    fun userConfirmsMood(confirmed: Boolean) {
        _needsMoodConfirmation.value = false
        if (confirmed && _preliminaryMoodStore != null) {
            _currentMood.value = _preliminaryMoodStore
            saveMoodEntry()
            generatePlanWithGemini(_preliminaryMoodStore!!)
        } else {
            _currentMood.value = null; _preliminaryMoodStore = null
            navigateTo(Screen.MAIN_ANALYSIS)
        }
    }

    fun userSelectsAlternativeMood(mood: WakeMood) {
        _preliminaryMoodStore = mood
        _currentMood.value = mood
        // Update dimensions to match selected mood
        _currentMoodDimensions.value = MoodDimension(
            energy = mood.energyLevel.toFloat(),
            stress = mood.stressLevel.toFloat(),
            positivity = mood.positivityLevel.toFloat(),
            focus = mood.focusLevel.toFloat(),
            social = mood.socialLevel.toFloat()
        )
    }

    private fun saveMoodEntry() {
        viewModelScope.launch {
            try {
                val dims = _currentMoodDimensions.value
                val face = _faceAnalysisResult.value
                val voice = _voiceToneResult.value
                val entry = MoodEntryEntity(
                    userId = sessionManager.userId,
                    moodName = _currentMood.value?.name ?: "NEUTRAL",
                    energy = dims.energy,
                    stress = dims.stress,
                    positivity = dims.positivity,
                    focus = dims.focus,
                    social = dims.social,
                    smileProbability = face.smileProbability,
                    leftEyeOpen = face.leftEyeOpenProbability,
                    rightEyeOpen = face.rightEyeOpenProbability,
                    snoozeCount = _snoozeCount.value,
                    voiceText = _transcribedVoiceInput.value,
                    voiceAmplitude = voice.averageAmplitude,
                    voicePauseRatio = voice.pauseRatio,
                    voiceSpeakingRate = voice.speakingRateWPM
                )
                db.moodEntryDao().insert(entry)
                loadMoodHistory()
                Log.d("MoodEntry", "Saved mood entry: ${entry.moodName}")
            } catch (e: Exception) {
                Log.e("MoodEntry", "Error saving: ${e.message}", e)
            }
        }
    }

    fun loadMoodHistory() {
        viewModelScope.launch {
            try {
                val weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
                val entries = db.moodEntryDao().getWeeklyEntries(sessionManager.userId, weekAgo)
                _moodHistory.value = entries
                _totalAnalysisCount.value = db.moodEntryDao().getEntryCount(sessionManager.userId)
            } catch (e: Exception) {
                Log.e("MoodHistory", "Error loading: ${e.message}", e)
            }
        }
    }

    // ==================== PLAN GENERATION ====================
    private fun generatePlanWithGemini(mood: WakeMood) {
        if (generativeModel == null) {
            tasks.clear(); tasks.addAll(fallbackTasks); navigateTo(Screen.PLAN_DISPLAY); return
        }
        _isLoadingPlan.value = true
        viewModelScope.launch {
            try {
                val prompt = buildGeminiPrompt(mood)
                val response = generativeModel!!.generateContent(prompt)
                val geminiTextResponse = response.text ?: ""
                val generatedTasks = parseGeminiResponseToTasks(geminiTextResponse)
                if (generatedTasks.isNotEmpty()) { tasks.clear(); tasks.addAll(generatedTasks) }
                else { tasks.clear(); tasks.addAll(fallbackTasks) }
            } catch (e: Exception) {
                Log.e("GeminiCall", "Error: ${e.message}", e)
                tasks.clear(); tasks.addAll(fallbackTasks)
            } finally { _isLoadingPlan.value = false; navigateTo(Screen.PLAN_DISPLAY) }
        }
    }

    fun fetchSubTasksFor(taskItem: TaskItem) {
        if (taskItem.detailPromptInstruction == null || generativeModel == null) {
            val taskIndex = tasks.indexOfFirst { it.id == taskItem.id }
            if (taskIndex != -1) { tasks[taskIndex] = tasks[taskIndex].copy(subTasks = listOf("Detay bulunamadı."), isLoadingSubTasks = false) }
            return
        }
        val taskIndex = tasks.indexOfFirst { it.id == taskItem.id }
        if (taskIndex == -1) return
        tasks[taskIndex] = taskItem.copy(isLoadingSubTasks = true, subTasks = emptyList())
        viewModelScope.launch {
            try {
                val response = generativeModel!!.generateContent(taskItem.detailPromptInstruction)
                val subTaskText = response.text ?: ""
                var parsedSubTasks = subTaskText.lines().map { it.trim() }
                    .filter { it.startsWith("* ") || it.startsWith("- ") }
                    .map { it.removePrefix("* ").removePrefix("- ").trim() }
                    .filter { it.isNotBlank() }
                
                if (parsedSubTasks.isEmpty()) {
                    parsedSubTasks = subTaskText.lines().map { it.trim() }.filter { it.isNotBlank() }
                }
                
                if (parsedSubTasks.isEmpty()) {
                    parsedSubTasks = listOf("Detay alınamadı.")
                }
                
                if (tasks.indices.contains(taskIndex)) { tasks[taskIndex] = tasks[taskIndex].copy(subTasks = parsedSubTasks, isLoadingSubTasks = false) }
            } catch (e: Exception) {
                Log.e("GeminiSubTask", "Error: ${e.message}", e)
                if (tasks.indices.contains(taskIndex)) { tasks[taskIndex] = tasks[taskIndex].copy(subTasks = listOf("Hata oluştu."), isLoadingSubTasks = false) }
            }
        }
    }

    // ==================== SPOTIFY ====================
    fun setSpotifyAccessToken(token: String?) {
        _spotifyAccessToken.value = token
        _isSpotifyAuthenticated.value = !token.isNullOrBlank()
        sessionManager.spotifyAccessToken = token
        sessionManager.spotifyTokenExpires = if (token != null) System.currentTimeMillis() + 3600000L else 0L
        if (token.isNullOrBlank()) _spotifyMessage.value = "Spotify bağlantısı kurulamadı."
        else {
            _spotifyMessage.value = "Spotify bağlandı!"
            if (_currentScreen.value == Screen.PLAN_DISPLAY || _currentScreen.value == Screen.SPOTIFY_PLAYLIST_DURATION_PROMPT) {
                navigateTo(Screen.SPOTIFY_PLAYLIST_DURATION_PROMPT)
            }
        }
    }

    fun clearSpotifyAuth() {
        _spotifyAccessToken.value = null; _isSpotifyAuthenticated.value = false
        _createdPlaylistUrl.value = null; _spotifyMessage.value = "Spotify bağlantısı kesildi."
        sessionManager.spotifyAccessToken = null; sessionManager.spotifyTokenExpires = 0L
    }

    fun consumeSpotifyAuthEvent() { _spotifyAuthEvent.value = null }
    fun triggerSpotifyAuthRequest() { _spotifyAuthEvent.value = Unit }

    fun createSpotifyPlaylistForMood(durationMinutes: Int) {
        val mood = currentMood.value
        val token = _spotifyAccessToken.value
        if (mood == null) { _spotifyMessage.value = "Önce ruh haliniz analiz edilmeli."; return }
        if (token.isNullOrBlank()) { _spotifyMessage.value = "Lütfen Spotify hesabınıza bağlanın."; triggerSpotifyAuthRequest(); return }
        if (generativeModel == null) { _spotifyMessage.value = "AI modeli hazır değil."; return }
        _isCreatingPlaylist.value = true; _createdPlaylistUrl.value = null; _spotifyMessage.value = "Şarkılar aranıyor..."
        viewModelScope.launch {
            try {
                val promptForSongs = buildGeminiPromptForSongs(mood, durationMinutes)
                val geminiResponse = generativeModel!!.generateContent(promptForSongs)
                val songSuggestionsText = geminiResponse.text ?: ""
                val suggestedSongs = parseGeminiResponseToSongList(songSuggestionsText)
                if (suggestedSongs.isEmpty()) { _spotifyMessage.value = "Şarkı önerisi bulunamadı."; throw Exception("No songs") }
                _spotifyMessage.value = "Spotify'da aranıyor..."
                val userId = getSpotifyUserId(token) ?: throw Exception("User ID failed")
                val trackUris = mutableListOf<String>()
                for (songPair in suggestedSongs) {
                    val trackUri = searchSpotifyTrackUri(token, songPair.first, songPair.second)
                    if (trackUri != null) trackUris.add(trackUri)
                    if (trackUris.size >= 20) break
                    delay(250)
                }
                if (trackUris.isEmpty()) { _spotifyMessage.value = "Şarkılar Spotify'da bulunamadı."; throw Exception("No URIs") }
                _spotifyMessage.value = "Liste oluşturuluyor..."
                val playlistName = "${SimpleDateFormat("dd MMMM yyyy", Locale("tr", "TR")).format(Calendar.getInstance().time)} Mod Planı"
                val playlistDescription = "WakeMood Planner — ${mood.emoji} ${mood.description}"
                val newPlaylist = createSpotifyPlaylist(token, userId, playlistName, playlistDescription)
                val playlistId = newPlaylist?.first ?: throw Exception("Playlist creation failed")
                val playlistUrl = newPlaylist.second
                addTracksToSpotifyPlaylist(token, playlistId, trackUris)
                _createdPlaylistUrl.value = playlistUrl
                _spotifyMessage.value = "'$playlistName' oluşturuldu!"
                navigateTo(Screen.PLAN_DISPLAY)
            } catch (e: Exception) {
                Log.e("Spotify", "Error: ${e.message}", e)
                if (_spotifyMessage.value?.contains("başarı") != true) {
                    _spotifyMessage.value = "Hata: ${e.localizedMessage?.take(80)}"
                }
            } finally { _isCreatingPlaylist.value = false }
        }
    }

    // ==================== TASK MANAGEMENT ====================
    fun toggleTaskCompletion(task: TaskItem) {
        val index = tasks.indexOfFirst { it.id == task.id }
        if (index != -1) { tasks[index] = tasks[index].copy(isCompleted = !tasks[index].isCompleted) }
    }

    fun setTaskPriority(task: TaskItem, priority: Int) {
        val index = tasks.indexOfFirst { it.id == task.id }
        if (index != -1) { tasks[index] = tasks[index].copy(priority = priority) }
    }

    fun moveTask(from: Int, to: Int) {
        if (from in tasks.indices && to in tasks.indices) {
            val item = tasks.removeAt(from)
            tasks.add(to, item)
        }
    }

    // ==================== SESSION ====================
    fun resetForNewDay() {
        _snoozeCount.value = 0; smileProbability.value = null; _transcribedVoiceInput.value = null
        _isListeningForVoice.value = false; _currentMood.value = null; _preliminaryMoodStore = null
        _needsMoodConfirmation.value = false; tasks.clear()
        _faceAnalysisResult.value = FaceAnalysisResult(); _voiceToneResult.value = VoiceToneResult()
        navigateTo(Screen.MAIN_ANALYSIS)
    }

    fun fullResetToLogin() {
        if (sessionManager.isLoggedIn) {
            saveProfessionDetails() 
        }
        sessionManager.clearSession()
        loginUsername.value = ""; loginPassword.value = ""
        _selectedProfession.value = null; resetAllProfessionSpecificStatesExcept(null)
        _snoozeCount.value = 0; smileProbability.value = null; _transcribedVoiceInput.value = null
        _currentMood.value = null; tasks.clear()
        _faceAnalysisResult.value = FaceAnalysisResult(); _voiceToneResult.value = VoiceToneResult()
        navigateTo(Screen.LOGIN)
    }

    // ==================== GEMINI PROMPTS ====================
    private fun buildGeminiPrompt(mood: WakeMood): String {
        val calendar = Calendar.getInstance()
        val currentDayName = SimpleDateFormat("EEEE", Locale("tr", "TR")).format(calendar.time)
        val currentTime = SimpleDateFormat("HH:mm", Locale("tr", "TR")).format(calendar.time)
        val todayFormatted = SimpleDateFormat("dd MMMM EEEE", Locale("tr", "TR")).format(calendar.time)
        val dims = _currentMoodDimensions.value

        var prompt = "Sen kişiselleştirilmiş günlük planlar oluşturan bir AI asistanısın.\n\n"
        prompt += "**KULLANICI PROFİLİ:**\n"
        prompt += "- Ruh Hali: ${mood.emoji} ${mood.description}\n"
        prompt += "- Enerji: ${dims.energy.toInt()}/100, Stres: ${dims.stress.toInt()}/100, Pozitiflik: ${dims.positivity.toInt()}/100\n"
        prompt += "- Alarm Erteleme: ${snoozeCount.value} kez\n"
        smileProbability.value?.let { prompt += "- Gülümseme: ${"%.0f".format(it * 100)}%\n" }
        transcribedVoiceInput.value?.let { if (it.isNotBlank()) prompt += "- Sesli Not: \"$it\"\n" }

        selectedProfession.value?.let { prof ->
            prompt += "- Meslek: $prof\n"
            when (prof) {
                "Öğrenci" -> if (hasUpcomingExams.value == true && studentExams.isNotEmpty()) {
                    prompt += "- Sınavlar: ${studentExams.joinToString { "${it.courseName} (${it.examDate})" }}\n"
                }
                "Yazılımcı" -> if (developerHasProjectDeadline.value == true) prompt += "- Proje: \"${developerProjectDetails.value}\"\n"
                "Doktor" -> if (doctorHasUpcomingShift.value == true) prompt += "- Nöbet: \"${doctorShiftDetails.value}\"\n"
                "Öğretmen" -> if (teacherHasUrgentTask.value == true) prompt += "- Görev: \"${teacherUrgentTaskDetails.value}\"\n"
                "Sanatçı" -> if (artistHasDeadlineOrEvent.value == true) prompt += "- Etkinlik: \"${artistEventDetails.value}\"\n"
            }
        }

        prompt += "\nBugün: $currentDayName, $todayFormatted, saat $currentTime\n\n"
        prompt += "**FORMAT:** 5-7 görev başlığı üret. Her biri yeni satırda '- ' ile başlasın.\n"
        prompt += "Parantez, süre, açıklama EKLEME. Sadece yalın başlıklar.\n"
        prompt += "Kullanıcının ruh haline ve mesleğine uygun olsun.\n"
        return prompt
    }

    private fun parseGeminiResponseToTasks(response: String): List<TaskItem> {
        val parsedTasks = response.lines().map { it.trim() }
            .filter { it.startsWith("- ") }
            .map { line ->
                val mainTitle = line.removePrefix("- ").trim()
                    .replace(Regex("\\(.*?\\)"), "").trim()
                    .replace(Regex("\\[.*?]"), "").trim()
                val detailPrompt = "Kullanıcının \"$mainTitle\" görevi için 3-5 alt adım oluştur. Her birini '- ' ile başlat. Meslek: ${selectedProfession.value ?: "Belirtilmemiş"}. Ruh hali: ${currentMood.value?.description ?: "Normal"}."
                TaskItem(title = mainTitle, detailPromptInstruction = detailPrompt)
            }
            .filter { it.title.isNotBlank() && it.title.length > 3 }

        return if (parsedTasks.isEmpty()) fallbackTasks else parsedTasks
    }

    private fun buildGeminiPromptForSongs(mood: WakeMood, durationMinutes: Int): String {
        var prompt = "Kullanıcının ruh hali: ${mood.emoji} ${mood.description}. "
        selectedProfession.value?.let { prompt += "Mesleği: $it. " }
        prompt += "Bu ruh haline uygun, yaklaşık $durationMinutes dakikalık Spotify çalma listesi için şarkı öner. "
        prompt += "Her öneri 'Şarkı Adı - Sanatçı Adı' formatında, yeni satırda olsun. "
        prompt += "En az ${durationMinutes / 5} şarkı, en fazla ${durationMinutes / 3} şarkı öner.\n"
        return prompt
    }

    private fun parseGeminiResponseToSongList(response: String): List<Pair<String, String>> {
        return response.lines().map { it.trim() }.filter { it.contains(" - ") && it.length > 5 }.mapNotNull { line ->
            val cleanLine = line.removePrefix("- ").removePrefix("* ").trim()
            val parts = cleanLine.split(" - ", limit = 2)
            if (parts.size == 2) Pair(parts[0].trim(), parts[1].trim()) else null
        }
    }

    // ==================== SPOTIFY API ====================
    private suspend fun getSpotifyUserId(token: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.spotify.com/v1/me")
            (url.openConnection() as? HttpURLConnection)?.run {
                requestMethod = "GET"; setRequestProperty("Authorization", "Bearer $token"); setRequestProperty("Content-Type", "application/json")
                if (responseCode == HttpURLConnection.HTTP_OK) JSONObject(BufferedReader(InputStreamReader(inputStream)).readText()).optString("id")
                else null
            }
        } catch (e: Exception) { Log.e("SpotifyAPI", "User ID error: ${e.message}"); null }
    }

    private suspend fun searchSpotifyTrackUri(token: String, trackName: String, artistName: String): String? = withContext(Dispatchers.IO) {
        try {
            val query = URLEncoder.encode("track:\"$trackName\" artist:\"$artistName\"", "UTF-8")
            val url = URL("https://api.spotify.com/v1/search?q=$query&type=track&limit=1")
            (url.openConnection() as? HttpURLConnection)?.run {
                requestMethod = "GET"; setRequestProperty("Authorization", "Bearer $token")
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    JSONObject(BufferedReader(InputStreamReader(inputStream)).readText()).optJSONObject("tracks")?.optJSONArray("items")?.let {
                        if (it.length() > 0) it.getJSONObject(0).optString("uri").ifBlank { null } else null
                    }
                } else null
            }
        } catch (e: Exception) { null }
    }

    private suspend fun createSpotifyPlaylist(token: String, userId: String, name: String, description: String): Pair<String?, String?>? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.spotify.com/v1/users/$userId/playlists")
            val payload = JSONObject().apply { put("name", name); put("description", description); put("public", false) }.toString()
            (url.openConnection() as? HttpURLConnection)?.run {
                requestMethod = "POST"; setRequestProperty("Authorization", "Bearer $token"); setRequestProperty("Content-Type", "application/json"); doOutput = true
                OutputStreamWriter(outputStream).use { it.write(payload) }
                if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                    val json = JSONObject(BufferedReader(InputStreamReader(inputStream)).readText())
                    Pair(json.optString("id"), json.optJSONObject("external_urls")?.optString("spotify"))
                } else {
                    val error = errorStream?.let { BufferedReader(InputStreamReader(it)).readText() }
                    Log.e("SpotifyAPI", "Playlist creation failed: $responseCode - $error")
                    null
                }
            }
        } catch (e: Exception) { Log.e("SpotifyAPI", "Playlist exception: ${e.message}", e); null }
    }

    private suspend fun addTracksToSpotifyPlaylist(token: String, playlistId: String, trackUris: List<String>): Boolean = withContext(Dispatchers.IO) {
        if (trackUris.isEmpty()) return@withContext true
        try {
            val url = URL("https://api.spotify.com/v1/playlists/$playlistId/tracks")
            val payload = JSONObject().apply { put("uris", JSONArray(trackUris)) }.toString()
            (url.openConnection() as? HttpURLConnection)?.run {
                requestMethod = "POST"; setRequestProperty("Authorization", "Bearer $token"); setRequestProperty("Content-Type", "application/json"); doOutput = true
                OutputStreamWriter(outputStream).use { it.write(payload) }
                if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) true
                else {
                    val error = errorStream?.let { BufferedReader(InputStreamReader(it)).readText() }
                    Log.e("SpotifyAPI", "Add tracks failed: $responseCode - $error")
                    false
                }
            } ?: false
        } catch (e: Exception) { Log.e("SpotifyAPI", "Add tracks exception: ${e.message}", e); false }
    }
}

fun String.containsAny(vararg keywords: String, ignoreCase: Boolean = true): Boolean {
    val locale = Locale("tr", "TR")
    val currentString = if (ignoreCase) this.lowercase(locale) else this
    return keywords.any { val kw = if (ignoreCase) it.lowercase(locale) else it; currentString.contains(kw) }
}