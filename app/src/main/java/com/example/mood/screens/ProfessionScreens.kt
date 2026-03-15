package com.example.mood.screens

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
fun ProfessionSelectionScreen(viewModel: MoodPlannerViewModel) {
    val professionIcons = mapOf(
        "Öğrenci" to "📚", "Yazılımcı" to "💻", "Doktor" to "🏥",
        "Öğretmen" to "📝", "Sanatçı" to "🎨", "Diğer" to "🌟"
    )
    Scaffold(
        topBar = { TopAppBar(title = { Text("Mesleğini Seç", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = { viewModel.goBack() }) { Icon(Icons.Filled.ArrowBack, "Geri") } }) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(viewModel.professions) { profession ->
                Card(
                    onClick = { viewModel.selectProfession(profession) },
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(professionIcons[profession] ?: "🌟", fontSize = 28.sp)
                        Spacer(Modifier.width(16.dp))
                        Text(profession, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentExamPromptScreen(viewModel: MoodPlannerViewModel) {
    Scaffold(topBar = { TopAppBar(title = { Text("Öğrenci Bilgileri") }, navigationIcon = { IconButton(onClick = { viewModel.goBack() }) { Icon(Icons.Filled.ArrowBack, "Geri") } }) }) { pv ->
        Column(modifier = Modifier.fillMaxSize().padding(pv).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("📝", fontSize = 56.sp)
            Spacer(Modifier.height(16.dp))
            Text("Yakın zamanda sınavın var mı?", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(32.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { viewModel.setProfessionSpecificYesNo("Öğrenci", true) }, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Evet") }
                OutlinedButton(onClick = { viewModel.setProfessionSpecificYesNo("Öğrenci", false) }, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Hayır") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentExamInputScreen(viewModel: MoodPlannerViewModel) {
    val exams by remember { derivedStateOf { viewModel.studentExams.toList() } }
    Scaffold(topBar = { TopAppBar(title = { Text("Sınav Takvimi") }, navigationIcon = { IconButton(onClick = { viewModel.goBack() }) { Icon(Icons.Filled.ArrowBack, "Geri") } }) }) { pv ->
        Column(modifier = Modifier.fillMaxSize().padding(pv).padding(16.dp)) {
            OutlinedTextField(value = viewModel.tempExamCourseName.value, onValueChange = { viewModel.tempExamCourseName.value = it }, label = { Text("Ders Adı") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = viewModel.tempExamDate.value, onValueChange = { viewModel.tempExamDate.value = it }, label = { Text("Sınav Tarihi (örn: 25 Aralık Cuma)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done))
            Spacer(Modifier.height(12.dp))
            Button(onClick = { viewModel.addStudentExam() }, modifier = Modifier.align(Alignment.End), shape = RoundedCornerShape(12.dp)) { Text("Sınav Ekle") }
            Spacer(Modifier.height(16.dp))
            if (exams.isNotEmpty()) {
                Text("Eklenen Sınavlar:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(exams, key = { it.id }) { exam ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) { Text(exam.courseName, fontWeight = FontWeight.Medium); Text(exam.examDate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) }
                                IconButton(onClick = { viewModel.removeStudentExam(exam) }) { Icon(Icons.Filled.Delete, "Sil", tint = AccentCoral) }
                            }
                        }
                    }
                }
            } else { Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Text("Henüz sınav eklenmedi", color = MaterialTheme.colorScheme.outline) } }
            Button(onClick = { viewModel.finishProfessionSetup() }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Devam Et", fontWeight = FontWeight.Bold) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionDetailPromptScreen(viewModel: MoodPlannerViewModel) {
    val selectedProfession by viewModel.selectedProfession.collectAsState()
    val promptQ = when (selectedProfession) {
        "Yazılımcı" -> "Yakında yetiştirmen gereken bir proje var mı?"
        "Doktor" -> "Yakında önemli bir nöbetin var mı?"
        "Öğretmen" -> "Yakında acil bir görevin var mı?"
        "Sanatçı" -> "Yakında bir etkinlik/teslim tarihin var mı?"
        else -> "Bugün özel bir durumun var mı?"
    }
    val emoji = when (selectedProfession) { "Yazılımcı" -> "💻"; "Doktor" -> "🏥"; "Öğretmen" -> "📝"; "Sanatçı" -> "🎨"; else -> "🌟" }
    Scaffold(topBar = { TopAppBar(title = { Text("${selectedProfession ?: ""} Detayları") }, navigationIcon = { IconButton(onClick = { viewModel.goBack() }) { Icon(Icons.Filled.ArrowBack, "Geri") } }) }) { pv ->
        Column(modifier = Modifier.fillMaxSize().padding(pv).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(emoji, fontSize = 56.sp); Spacer(Modifier.height(16.dp))
            Text(promptQ, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold); Spacer(Modifier.height(32.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { selectedProfession?.let { viewModel.setProfessionSpecificYesNo(it, true) } }, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Evet") }
                OutlinedButton(onClick = { selectedProfession?.let { viewModel.setProfessionSpecificYesNo(it, false) } }, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Hayır") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionDetailInputScreen(viewModel: MoodPlannerViewModel) {
    val selectedProfession by viewModel.selectedProfession.collectAsState()
    var detailValue by remember(selectedProfession) { mutableStateOf(when (selectedProfession) { "Yazılımcı" -> viewModel.developerProjectDetails.value; "Doktor" -> viewModel.doctorShiftDetails.value; "Öğretmen" -> viewModel.teacherUrgentTaskDetails.value; "Sanatçı" -> viewModel.artistEventDetails.value; else -> "" }) }
    val label = when (selectedProfession) { "Yazılımcı" -> "Proje Detayları"; "Doktor" -> "Nöbet Detayları"; "Öğretmen" -> "Görev Detayları"; "Sanatçı" -> "Etkinlik Detayları"; else -> "Detaylar" }

    Scaffold(topBar = { TopAppBar(title = { Text("$selectedProfession - Detay") }, navigationIcon = { IconButton(onClick = { viewModel.goBack() }) { Icon(Icons.Filled.ArrowBack, "Geri") } }) }) { pv ->
        Column(modifier = Modifier.fillMaxSize().padding(pv).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Detaylarını gir:", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = detailValue, onValueChange = { detailValue = it
                    when (selectedProfession) { "Yazılımcı" -> viewModel.developerProjectDetails.value = it; "Doktor" -> viewModel.doctorShiftDetails.value = it; "Öğretmen" -> viewModel.teacherUrgentTaskDetails.value = it; "Sanatçı" -> viewModel.artistEventDetails.value = it }
                },
                label = { Text(label) }, modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp), maxLines = 5, shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.weight(1f))
            Button(onClick = { viewModel.finishProfessionSetup() }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Devam Et", fontWeight = FontWeight.Bold) }
        }
    }
}
