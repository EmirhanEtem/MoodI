package com.example.mood.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mood.MoodPlannerViewModel
import com.example.mood.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(viewModel: MoodPlannerViewModel) {
    val pages = listOf(
        Triple("🧠", "Akıllı Mod Analizi", "Yüz ifaden, ses tonun ve davranışların ile çok boyutlu ruh hali analizi"),
        Triple("🎯", "Kişisel Günlük Plan", "AI destekli, mesleğine ve moduna özel günlük eylem planları"),
        Triple("🎵", "Spotify Entegrasyonu", "Moduna uygun çalma listesi oluştur ve güne enerjik başla")
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PrimaryDeep, DarkBg)))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))
            Text("Moodİİ", style = MaterialTheme.typography.displayMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Text("WakeMood Planner", style = MaterialTheme.typography.titleMedium, color = AccentMint)
            Spacer(Modifier.height(48.dp))

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                val (emoji, title, desc) = pages[page]
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.95f, targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "s"
                )
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(emoji, fontSize = 80.sp, modifier = Modifier.scale(scale))
                    Spacer(Modifier.height(32.dp))
                    Text(title, style = MaterialTheme.typography.headlineMedium, color = Color.White, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Text(desc, style = MaterialTheme.typography.bodyLarge, color = DarkTextSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            // Page indicators
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(16.dp)) {
                repeat(pages.size) { i ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(if (pagerState.currentPage == i) 28.dp else 8.dp, 8.dp)
                            .background(
                                if (pagerState.currentPage == i) AccentMint else DarkTextTertiary,
                                RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            if (pagerState.currentPage == pages.size - 1) {
                Button(
                    onClick = { viewModel.completeOnboarding() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentMint)
                ) {
                    Text("Başlayalım!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkBg)
                }
            } else {
                Button(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text("İleri", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
