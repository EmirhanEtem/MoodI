package com.example.mood.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mood.MoodPlannerViewModel
import com.example.mood.WakeMood
import com.example.mood.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodConfirmationScreen(viewModel: MoodPlannerViewModel) {
    val currentMood by viewModel.currentMood.collectAsState()
    val dimensions by viewModel.currentMoodDimensions.collectAsState()
    val mood = currentMood ?: return

    val infiniteTransition = rememberInfiniteTransition(label = "emoji")
    val emojiScale by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = EaseInOutSine), RepeatMode.Reverse), label = "s"
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Ruh Halin") }, navigationIcon = { IconButton(onClick = { viewModel.goBack() }) { Icon(Icons.Filled.ArrowBack, "Geri") } }) }
    ) { pv ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pv).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Main mood display
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(mood.colorHex).copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(mood.emoji, fontSize = 72.sp, modifier = Modifier.scale(emojiScale))
                        Spacer(Modifier.height(12.dp))
                        Text(mood.description, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text("Bu senin bugünkü ruh halin olarak belirlendi", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
                    }
                }
            }
            // Dimension scores
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Boyut Analizi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        DimensionBar("⚡ Enerji", dimensions.energy, AccentMint)
                        DimensionBar("😰 Stres", dimensions.stress, AccentCoral)
                        DimensionBar("😊 Pozitiflik", dimensions.positivity, AccentGold)
                        DimensionBar("🎯 Odak", dimensions.focus, PrimaryBlue)
                        DimensionBar("👥 Sosyallik", dimensions.social, MoodCreativeColor)
                    }
                }
            }
            // Alternative moods
            item {
                Text("Farklı mı hissediyorsun?", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(WakeMood.entries.filter { it != mood }) { alt ->
                        FilterChip(
                            onClick = { viewModel.userSelectsAlternativeMood(alt) },
                            label = { Text("${alt.emoji} ${alt.description}", fontSize = 12.sp) },
                            selected = false,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
            // Confirm buttons
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.userConfirmsMood(true) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(mood.colorHex))
                ) {
                    Icon(Icons.Filled.CheckCircle, null)
                    Spacer(Modifier.width(8.dp))
                    Text("${mood.emoji} Evet, bu benim modumu!", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.userConfirmsMood(false) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Tekrar Analiz Et") }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DimensionBar(label: String, value: Float, color: Color) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text("${"%.0f".format(value)}/100", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (value / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}
