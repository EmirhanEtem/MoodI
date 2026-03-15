package com.example.mood.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.example.mood.MoodPlannerViewModel
import com.example.mood.Screen
import com.example.mood.TaskItem
import com.example.mood.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDisplayScreen(viewModel: MoodPlannerViewModel) {
    val context = LocalContext.current
    val currentMood by viewModel.currentMood.collectAsState()
    val isLoadingPlan by viewModel.isLoadingPlan.collectAsState()
    val spotifyMsg by viewModel.spotifyMessage.collectAsState()
    val isSpotifyAuth by viewModel.isSpotifyAuthenticated.collectAsState()
    val playlistUrl by viewModel.createdPlaylistUrl.collectAsState()
    val isCreatingPlaylist by viewModel.isCreatingPlaylist.collectAsState()
    val mood = currentMood
    val tasks = viewModel.tasks

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text("Günlük Planın", fontWeight = FontWeight.Bold); mood?.let { Text("${it.emoji} ${it.description}", style = MaterialTheme.typography.bodySmall, color = Color(it.colorHex)) } } },
                actions = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.MOOD_ANALYTICS) }) { Icon(Icons.Filled.Analytics, "Analitik") }
                    IconButton(onClick = { viewModel.resetForNewDay() }) { Icon(Icons.Filled.Refresh, "Yeni Analiz") }
                }
            )
        }
    ) { paddingValues ->
        if (isLoadingPlan) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryPurple, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("AI planını oluşturuyor...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text("Ruh haline özel görevler hazırlanıyor", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // Recommended activities
                mood?.let { m ->
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(m.colorHex).copy(alpha = 0.1f))) {
                            Column(Modifier.padding(16.dp)) {
                                Text("💡 Önerilen Aktiviteler", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(8.dp))
                                m.recommendedActivities.forEach { act -> Text("• $act", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                    }
                }
                // Tasks
                item { Text("📋 Görevlerin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp)) }
                itemsIndexed(tasks, key = { _, t -> t.id }) { index, task ->
                    TaskCard(task = task, onToggle = { viewModel.toggleTaskCompletion(task) }, onExpand = { viewModel.fetchSubTasksFor(task) }, onPriorityChange = { p -> viewModel.setTaskPriority(task, p) })
                }
                // Spotify section
                item {
                    Spacer(Modifier.height(16.dp))
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1DB954).copy(alpha = 0.1f))) {
                        Column(Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🎵", fontSize = 24.sp)
                                Spacer(Modifier.width(12.dp))
                                Text("Spotify Çalma Listesi", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(Modifier.height(12.dp))
                            spotifyMsg?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) }
                            playlistUrl?.let { url ->
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))) {
                                    Text("Spotify'da Aç", fontWeight = FontWeight.Bold)
                                }
                            }
                            if (playlistUrl == null) {
                                Spacer(Modifier.height(8.dp))
                                if (isSpotifyAuth) {
                                    Button(
                                        onClick = { viewModel.navigateTo(Screen.SPOTIFY_PLAYLIST_DURATION_PROMPT) },
                                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                                        enabled = !isCreatingPlaylist,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))
                                    ) { Text("Liste Oluştur", fontWeight = FontWeight.Bold) }
                                } else {
                                    OutlinedButton(
                                        onClick = { viewModel.triggerSpotifyAuthRequest() },
                                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                                    ) { Text("Spotify'a Bağlan") }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun TaskCard(task: TaskItem, onToggle: () -> Unit, onExpand: () -> Unit, onPriorityChange: (Int) -> Unit) {
    val priorityColor = when (task.priority) { 1 -> AccentCoral; 2 -> AccentGold; else -> AccentMint }
    val priorityLabel = when (task.priority) { 1 -> "Yüksek"; 2 -> "Orta"; else -> "Düşük" }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle() }, colors = CheckboxDefaults.colors(checkedColor = SuccessGreen))
                Spacer(Modifier.width(8.dp))
                Text(
                    task.title, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f),
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                )
                Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = priorityColor.copy(alpha = 0.15f))) {
                    Text(priorityLabel, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 10.sp, color = priorityColor, fontWeight = FontWeight.Bold)
                }
            }
            // Sub-tasks
            if (task.subTasks.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                task.subTasks.forEach { sub -> 
                    Text(
                        text = parseMarkdownToAnnotatedString("  • $sub"), 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            if (task.subTasks.isEmpty() && !task.isLoadingSubTasks) {
                TextButton(onClick = onExpand, modifier = Modifier.align(Alignment.End)) { Text("Detay Göster", fontSize = 12.sp) }
            }
            if (task.isLoadingSubTasks) { Spacer(Modifier.height(4.dp)); LinearProgressIndicator(Modifier.fillMaxWidth(), color = PrimaryPurple) }
        }
    }
}


@Composable
fun parseMarkdownToAnnotatedString(text: String): AnnotatedString {
    return buildAnnotatedString {
        val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
        var lastCursor = 0
        boldRegex.findAll(text).forEach { matchResult ->
            append(text.substring(lastCursor, matchResult.range.first))
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(matchResult.groupValues[1])
            }
            lastCursor = matchResult.range.last + 1
        }
        append(text.substring(lastCursor, text.length))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyDurationPromptScreen(viewModel: MoodPlannerViewModel) {
    val isCreating by viewModel.isCreatingPlaylist.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("Çalma Listesi Süresi") }, navigationIcon = { IconButton(onClick = { viewModel.goBack() }) { Icon(Icons.Filled.ArrowBack, "Geri") } }) }) { pv ->
        Column(Modifier.fillMaxSize().padding(pv).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("🎵", fontSize = 56.sp)
            Spacer(Modifier.height(16.dp))
            Text("Kaç dakikalık liste?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            Text("${viewModel.playlistDurationPreferenceMinutes.value} dakika", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryPurple)
            Spacer(Modifier.height(16.dp))
            Slider(
                value = viewModel.playlistDurationPreferenceMinutes.value.toFloat(),
                onValueChange = { viewModel.playlistDurationPreferenceMinutes.value = it.toInt() },
                valueRange = 10f..120f, steps = 10,
                colors = SliderDefaults.colors(thumbColor = PrimaryPurple, activeTrackColor = PrimaryPurple)
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { viewModel.createSpotifyPlaylistForMood(viewModel.playlistDurationPreferenceMinutes.value) },
                modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp),
                enabled = !isCreating, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))
            ) {
                if (isCreating) { CircularProgressIndicator(Modifier.size(24.dp), color = Color.White) }
                else { Text("Liste Oluştur!", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
