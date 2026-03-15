package com.example.mood.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.mood.*
import com.example.mood.analysis.FaceAnalysisResult
import com.example.mood.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAnalysisScreen(viewModel: MoodPlannerViewModel) {
    val context = LocalContext.current
    val snoozeCount by viewModel.snoozeCount.collectAsState()
    val transcribedText by viewModel.transcribedVoiceInput.collectAsState()
    val isListening by viewModel.isListeningForVoice.collectAsState()
    val isAnalyzingTone by viewModel.isAnalyzingVoiceTone.collectAsState()
    val faceResult by viewModel.faceAnalysisResult.collectAsState()
    val voiceResult by viewModel.voiceToneResult.collectAsState()
    val loggedInUsername by viewModel.loggedInUsername.collectAsState()
    val totalCount by viewModel.totalAnalysisCount.collectAsState()

    var showCamera by remember { mutableStateOf(false) }
    var faceAnalyzer by remember { mutableStateOf<FaceAnalyzer?>(null) }
    var faceGraphicInfo by remember { mutableStateOf(FaceGraphicInfo()) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) showCamera = true
        else Toast.makeText(context, "Kamera izni gerekli", Toast.LENGTH_SHORT).show()
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) viewModel.startVoiceToneAnalysisFromSentence()
        else Toast.makeText(context, "Ses analizi için mikrofon izni gerekli", Toast.LENGTH_SHORT).show()
    }

    if (showCamera) {
        CameraScreen(
            onAnalysisComplete = { result -> viewModel.setFaceAnalysisResult(result); showCamera = false },
            onDismiss = { showCamera = false }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Column { Text("Günaydın, $loggedInUsername! 👋", fontWeight = FontWeight.Bold); Text("Bugünü analiz edelim", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) } },
                    actions = {
                        if (totalCount > 0) {
                            IconButton(onClick = { viewModel.navigateTo(Screen.MOOD_ANALYTICS) }) { Icon(Icons.Filled.Analytics, "Analitik", tint = PrimaryPurple) }
                        }
                        IconButton(onClick = { viewModel.navigateTo(Screen.PROFILE) }) { Icon(Icons.Filled.Person, "Profil", tint = AccentMint) }
                        IconButton(onClick = { viewModel.fullResetToLogin() }) { Icon(Icons.Filled.Logout, "Çıkış", tint = AccentCoral) }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- Snooze Card ---
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⏰", fontSize = 24.sp); Spacer(Modifier.width(12.dp))
                            Text("Alarm Erteleme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            FilledIconButton(onClick = { viewModel.decrementSnooze() }, shape = CircleShape, colors = IconButtonDefaults.filledIconButtonColors(containerColor = AccentCoral.copy(alpha = 0.2f))) { Icon(Icons.Filled.Remove, "Azalt", tint = AccentCoral) }
                            Spacer(Modifier.width(24.dp))
                            Text("$snoozeCount", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryPurple)
                            Text(" kez", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 4.dp, top = 10.dp))
                            Spacer(Modifier.width(24.dp))
                            FilledIconButton(onClick = { viewModel.incrementSnooze() }, shape = CircleShape, colors = IconButtonDefaults.filledIconButtonColors(containerColor = AccentMint.copy(alpha = 0.2f))) { Icon(Icons.Filled.Add, "Artır", tint = AccentMint) }
                        }
                    }
                }

                // --- Camera Card ---
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📸", fontSize = 24.sp); Spacer(Modifier.width(12.dp))
                            Text("Yüz Analizi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))
                        if (faceResult.faceDetected) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                AnalysisChip("😊 ${"%.0f".format(faceResult.smileProbability * 100)}%", AccentMint)
                                AnalysisChip("👁 ${"%.0f".format(faceResult.averageEyeOpen * 100)}%", PrimaryBlue)
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("✅ Yüz analizi tamamlandı", color = SuccessGreen, style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("Anlık fotoğrafınız analiz edilecek", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) showCamera = true
                                else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (faceResult.faceDetected) SuccessGreen.copy(alpha = 0.3f) else PrimaryPurple)
                        ) {
                            Icon(if (faceResult.faceDetected) Icons.Filled.CheckCircle else Icons.Filled.CameraAlt, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (faceResult.faceDetected) "Tekrar Çek" else "Fotoğraf Çek", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // --- Voice Card ---
                val voiceTimer by viewModel.voiceTimer.collectAsState()
                
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎤", fontSize = 24.sp); Spacer(Modifier.width(12.dp))
                            Text("Sesli Analiz", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))
                        
                        // State 1: Active recording (Timer + Sentence)
                        if (isListening || isAnalyzingTone) {
                            if (voiceTimer > 0) {
                                Text(
                                    "Lütfen 10 saniye boyunca aşağıdaki metni sesli okuyun:",
                                    color = AccentMint, 
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = PrimaryPurple.copy(alpha = 0.15f)), border = BorderStroke(1.dp, PrimaryPurple)) {
                                    Text("\"$transcribedText\"", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Spacer(Modifier.height(16.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Timer, null, tint = AccentCoral)
                                    Spacer(Modifier.width(8.dp))
                                    Text("$voiceTimer saniye kaldı", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AccentCoral)
                                }
                            } else if (isAnalyzingTone) {
                                Text("Sesiniz yapay zeka tarafından analiz ediliyor...", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(Modifier.fillMaxWidth(), color = PrimaryPurple)
                            }
                        } 
                        // State 2: Analysis finished (Results)
                        else if (voiceResult.isAnalyzed && transcribedText != null) {
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = PrimaryPurple.copy(alpha = 0.1f))) {
                                Text("\"$transcribedText\"", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                AnalysisChip("⚡ ${"%.0f".format(voiceResult.estimatedEnergy)}", AccentMint)
                                AnalysisChip("😰 ${"%.0f".format(voiceResult.estimatedStress)}", AccentCoral)
                                AnalysisChip("😊 ${"%.0f".format(voiceResult.estimatedPositivity)}", AccentGold)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("✅ Ses analizi tamamlandı", color = SuccessGreen, style = MaterialTheme.typography.bodySmall)
                        } 
                        // State 3: Ready to start
                        else {
                            Text("Senin için rastgele bir metin seçeceğiz. Kayda başlamak için butona tıkla.", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodySmall)
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                if (isListening) return@Button
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    viewModel.startVoiceToneAnalysisFromSentence()
                                } else { 
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp),
                            enabled = !isAnalyzingTone && voiceTimer == 0,
                            colors = ButtonDefaults.buttonColors(containerColor = if (voiceResult.isAnalyzed) SuccessGreen.copy(alpha = 0.3f) else PrimaryPurple)
                        ) {
                            if (isListening) {
                                val pulseTransition = rememberInfiniteTransition(label = "mic")
                                val pulseScale by pulseTransition.animateFloat(1f, 1.3f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "p")
                                Icon(Icons.Filled.Mic, null, Modifier.scale(pulseScale), tint = AccentCoral)
                                Spacer(Modifier.width(8.dp)); Text("Dinleniyor...", fontWeight = FontWeight.Bold, color = AccentCoral)
                            } else {
                                Icon(if (voiceResult.isAnalyzed) Icons.Filled.CheckCircle else Icons.Filled.Mic, null)
                                Spacer(Modifier.width(8.dp)); Text(if (voiceResult.isAnalyzed) "Tekrar Kaydet" else "Sesli Analize Başla", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // --- Analyze Button ---
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.startInitialAnalysis() },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Icon(Icons.Filled.Psychology, null, Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Modumu Analiz Et", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AnalysisChip(text: String, color: Color) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f))) {
        Text(text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(onAnalysisComplete: (FaceAnalysisResult) -> Unit, onDismiss: () -> Unit) {
    var faceGraphicInfo by remember { mutableStateOf(FaceGraphicInfo()) }
    val context = LocalContext.current
    val faceAnalyzer = remember {
        FaceAnalyzer(context, onAnalysisComplete = onAnalysisComplete, onFacesDetected = { faceGraphicInfo = it })
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Anlık Fotoğraf") }, navigationIcon = { IconButton(onClick = { faceAnalyzer.release(); onDismiss() }) { Icon(Icons.Filled.Close, "Kapat") } }) }
    ) { pv ->
        Box(modifier = Modifier.fillMaxSize().padding(pv)) {
            CameraPreview(faceAnalyzer = faceAnalyzer, onFaceDetected = { })
            FaceOverlay(faceGraphicInfo = faceGraphicInfo, modifier = Modifier.fillMaxSize())
            // Capture button
            Box(modifier = Modifier.fillMaxSize().padding(bottom = 32.dp), contentAlignment = Alignment.BottomCenter) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Yüzünü kameraya göster", color = Color.White, fontWeight = FontWeight.Bold,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(horizontal = 16.dp, vertical = 8.dp))
                    Spacer(Modifier.height(16.dp))
                    FilledIconButton(
                        onClick = { faceAnalyzer.requestCapture() },
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = PrimaryPurple)
                    ) {
                        Icon(Icons.Filled.CameraAlt, "Çek", modifier = Modifier.size(32.dp), tint = Color.White)
                    }
                }
            }
        }
    }
}
