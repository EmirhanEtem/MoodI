package com.example.mood.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mood.MoodPlannerViewModel
import com.example.mood.Screen
import com.example.mood.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: MoodPlannerViewModel) {
    val loggedInUsername by viewModel.loggedInUsername.collectAsState()
    val selectedProfession by viewModel.selectedProfession.collectAsState()
    val hasUpcomingExams by viewModel.hasUpcomingExams.collectAsState()
    val developerHasDeadline by viewModel.developerHasProjectDeadline.collectAsState()
    val doctorHasShift by viewModel.doctorHasUpcomingShift.collectAsState()
    val teacherHasTask by viewModel.teacherHasUrgentTask.collectAsState()
    val artistHasEvent by viewModel.artistHasDeadlineOrEvent.collectAsState()
    val totalCount by viewModel.totalAnalysisCount.collectAsState()
    val exams by remember { derivedStateOf { viewModel.studentExams.toList() } }

    var showProfessionPicker by remember { mutableStateOf(false) }

    val professionIcons = mapOf(
        "Öğrenci" to "📚", "Yazılımcı" to "💻", "Doktor" to "🏥",
        "Öğretmen" to "📝", "Sanatçı" to "🎨", "Diğer" to "🌟"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profilim", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.goBack() }) {
                        Icon(Icons.Filled.ArrowBack, "Geri")
                    }
                }
            )
        }
    ) { pv ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pv)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // ===== User Info Card =====
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryPurple.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = PrimaryPurple.copy(alpha = 0.2f)),
                            modifier = Modifier.size(60.dp)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    loggedInUsername.take(1).uppercase(),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PrimaryPurple
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                loggedInUsername,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${professionIcons[selectedProfession] ?: "🌟"} ${selectedProfession ?: "Meslek seçilmedi"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$totalCount",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AccentMint
                            )
                            Text("analiz", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }

            // ===== Profession Section =====
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("👔", fontSize = 24.sp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Meslek",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { showProfessionPicker = !showProfessionPicker }) {
                                Icon(
                                    if (showProfessionPicker) Icons.Filled.ExpandLess else Icons.Filled.Edit,
                                    null, modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(if (showProfessionPicker) "Kapat" else "Değiştir")
                            }
                        }

                        if (selectedProfession != null && !showProfessionPicker) {
                            Spacer(Modifier.height(8.dp))
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = PrimaryPurple.copy(alpha = 0.08f)
                                )
                            ) {
                                Row(
                                    Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        professionIcons[selectedProfession] ?: "🌟",
                                        fontSize = 24.sp
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        selectedProfession ?: "",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = showProfessionPicker) {
                            Column(
                                modifier = Modifier.padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                viewModel.professions.forEach { profession ->
                                    val isSelected = selectedProfession == profession
                                    Card(
                                        onClick = {
                                            viewModel.selectProfessionFromProfile(profession)
                                            showProfessionPicker = false
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected)
                                                PrimaryPurple.copy(alpha = 0.15f)
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        border = if (isSelected)
                                            CardDefaults.outlinedCardBorder().copy(
                                                brush = androidx.compose.ui.graphics.SolidColor(PrimaryPurple)
                                            )
                                        else null
                                    ) {
                                        Row(
                                            Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(professionIcons[profession] ?: "🌟", fontSize = 22.sp)
                                            Spacer(Modifier.width(12.dp))
                                            Text(profession, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                            Spacer(Modifier.weight(1f))
                                            if (isSelected) {
                                                Icon(Icons.Filled.CheckCircle, null, tint = PrimaryPurple)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ===== Profession-Specific Details =====
            when (selectedProfession) {
                "Öğrenci" -> {
                    // Student exams
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("📝", fontSize = 24.sp)
                                    Spacer(Modifier.width(12.dp))
                                    Text("Sınavlarım", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(12.dp))

                                // Add exam form
                                OutlinedTextField(
                                    value = viewModel.tempExamCourseName.value,
                                    onValueChange = { viewModel.tempExamCourseName.value = it },
                                    label = { Text("Ders Adı") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = viewModel.tempExamDate.value,
                                    onValueChange = { viewModel.tempExamDate.value = it },
                                    label = { Text("Sınav Tarihi (örn: 25 Mart Salı)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.addStudentExam() },
                                    modifier = Modifier.align(Alignment.End),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = viewModel.tempExamCourseName.value.isNotBlank() && viewModel.tempExamDate.value.isNotBlank()
                                ) {
                                    Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Sınav Ekle")
                                }
                            }
                        }
                    }
                    // Existing exams list
                    if (exams.isNotEmpty()) {
                        item {
                            Text(
                                "Eklenen Sınavlar (${exams.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                        items(exams, key = { it.id }) { exam ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = AccentGold.copy(alpha = 0.08f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📖", fontSize = 20.sp)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(exam.courseName, fontWeight = FontWeight.Medium)
                                        Text(
                                            exam.examDate,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    IconButton(onClick = { viewModel.removeStudentExam(exam) }) {
                                        Icon(Icons.Filled.Delete, "Sil", tint = AccentCoral)
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    "Henüz sınav eklenmedi",
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.outline,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                "Yazılımcı" -> {
                    item {
                        ProfessionDetailCard(
                            title = "Proje Bilgileri",
                            emoji = "💻",
                            hasDetail = developerHasDeadline,
                            detailValue = viewModel.developerProjectDetails.value,
                            label = "Proje Detayları",
                            placeholder = "Proje adı, deadline, teknolojiler...",
                            onDetailChange = { viewModel.developerProjectDetails.value = it },
                            onToggle = { viewModel.setProfessionSpecificYesNoFromProfile("Yazılımcı", it) }
                        )
                    }
                }
                "Doktor" -> {
                    item {
                        ProfessionDetailCard(
                            title = "Nöbet Bilgileri",
                            emoji = "🏥",
                            hasDetail = doctorHasShift,
                            detailValue = viewModel.doctorShiftDetails.value,
                            label = "Nöbet Detayları",
                            placeholder = "Nöbet günleri, saatleri...",
                            onDetailChange = { viewModel.doctorShiftDetails.value = it },
                            onToggle = { viewModel.setProfessionSpecificYesNoFromProfile("Doktor", it) }
                        )
                    }
                }
                "Öğretmen" -> {
                    item {
                        ProfessionDetailCard(
                            title = "Görev Bilgileri",
                            emoji = "📝",
                            hasDetail = teacherHasTask,
                            detailValue = viewModel.teacherUrgentTaskDetails.value,
                            label = "Görev Detayları",
                            placeholder = "Acil görevler, toplantılar...",
                            onDetailChange = { viewModel.teacherUrgentTaskDetails.value = it },
                            onToggle = { viewModel.setProfessionSpecificYesNoFromProfile("Öğretmen", it) }
                        )
                    }
                }
                "Sanatçı" -> {
                    item {
                        ProfessionDetailCard(
                            title = "Etkinlik Bilgileri",
                            emoji = "🎨",
                            hasDetail = artistHasEvent,
                            detailValue = viewModel.artistEventDetails.value,
                            label = "Etkinlik Detayları",
                            placeholder = "Sergi, konser, teslim tarihi...",
                            onDetailChange = { viewModel.artistEventDetails.value = it },
                            onToggle = { viewModel.setProfessionSpecificYesNoFromProfile("Sanatçı", it) }
                        )
                    }
                }
            }

            // ===== Actions =====
            item {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.fullResetToLogin() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCoral)
                ) {
                    Icon(Icons.Filled.Logout, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Çıkış Yap", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ProfessionDetailCard(
    title: String,
    emoji: String,
    hasDetail: Boolean?,
    detailValue: String,
    label: String,
    placeholder: String,
    onDetailChange: (String) -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 24.sp)
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))

            // Toggle
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Yaklaşan bir plan var mı?",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = hasDetail == true,
                    onCheckedChange = { onToggle(it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = PrimaryPurple)
                )
            }

            AnimatedVisibility(visible = hasDetail == true) {
                Column(Modifier.padding(top = 12.dp)) {
                    OutlinedTextField(
                        value = detailValue,
                        onValueChange = onDetailChange,
                        label = { Text(label) },
                        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.outline) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (detailValue.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Kaydedildi", style = MaterialTheme.typography.bodySmall, color = SuccessGreen)
                        }
                    }
                }
            }
        }
    }
}
