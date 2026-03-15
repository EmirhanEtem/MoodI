package com.example.mood.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mood.MoodPlannerViewModel
import com.example.mood.WakeMood
import com.example.mood.data.MoodEntryEntity
import com.example.mood.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodAnalyticsScreen(viewModel: MoodPlannerViewModel) {
    val moodHistory by viewModel.moodHistory.collectAsState()
    val totalCount by viewModel.totalAnalysisCount.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mod Analitiği", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = { viewModel.goBack() }) { Icon(Icons.Filled.ArrowBack, "Geri") } }) }
    ) { pv ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pv).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stats overview
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Toplam Analiz", "$totalCount", PrimaryPurple, Modifier.weight(1f))
                    StatCard("Bu Hafta", "${moodHistory.size}", AccentMint, Modifier.weight(1f))
                }
            }

            // Average dimensions
            if (moodHistory.isNotEmpty()) {
                item {
                    val avgEnergy = moodHistory.map { it.energy }.average().toFloat()
                    val avgStress = moodHistory.map { it.stress }.average().toFloat()
                    val avgPositivity = moodHistory.map { it.positivity }.average().toFloat()
                    val avgFocus = moodHistory.map { it.focus }.average().toFloat()
                    val avgSocial = moodHistory.map { it.social }.average().toFloat()

                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(20.dp)) {
                            Text("📊 Haftalık Ortalamalar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            AvgBar("⚡ Enerji", avgEnergy, AccentMint)
                            AvgBar("😰 Stres", avgStress, AccentCoral)
                            AvgBar("😊 Pozitiflik", avgPositivity, AccentGold)
                            AvgBar("🎯 Odak", avgFocus, PrimaryBlue)
                            AvgBar("👥 Sosyallik", avgSocial, MoodCreativeColor)
                        }
                    }
                }

                // Mood distribution
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(20.dp)) {
                            Text("🧠 Mod Dağılımı", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            val distribution = moodHistory.groupBy { it.moodName }.mapValues { it.value.size }.toList().sortedByDescending { it.second }
                            distribution.forEach { (moodName, count) ->
                                val mood = WakeMood.entries.find { it.name == moodName }
                                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("${mood?.emoji ?: "❓"}", fontSize = 20.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(mood?.description ?: moodName, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color(mood?.colorHex ?: 0xFF78909C).copy(alpha = 0.15f))) {
                                        Text("$count", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontWeight = FontWeight.Bold, color = Color(mood?.colorHex ?: 0xFF78909C))
                                    }
                                }
                            }
                        }
                    }
                }

                // History list
                item { Text("📅 Son Analizler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
            }

            if (moodHistory.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📊", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("Henüz analiz kaydı yok", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
                            Text("İlk analizini yaptığında burada görünecek", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            } else {
                items(moodHistory.sortedByDescending { it.timestamp }) { entry ->
                    HistoryEntryCard(entry)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun AvgBar(label: String, value: Float, color: Color) {
    Column(Modifier.padding(vertical = 3.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text("${"%.0f".format(value)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = color)
        }
        LinearProgressIndicator(progress = { (value / 100f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = color, trackColor = color.copy(alpha = 0.12f))
    }
}

@Composable
private fun HistoryEntryCard(entry: MoodEntryEntity) {
    val mood = WakeMood.entries.find { it.name == entry.moodName }
    val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("tr", "TR"))
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(mood?.colorHex ?: 0xFF78909C).copy(alpha = 0.08f))) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(mood?.emoji ?: "❓", fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(mood?.description ?: entry.moodName, fontWeight = FontWeight.Medium)
                Text(dateFormat.format(Date(entry.timestamp)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("⚡${"%.0f".format(entry.energy)}", fontSize = 11.sp, color = AccentMint)
                Text("😊${"%.0f".format(entry.positivity)}", fontSize = 11.sp, color = AccentGold)
            }
        }
    }
}
