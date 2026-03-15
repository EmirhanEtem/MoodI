package com.example.mood.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mood.MoodPlannerViewModel
import com.example.mood.Screen
import com.example.mood.ui.theme.*

@Composable
fun LoginScreen(viewModel: MoodPlannerViewModel) {
    val authError by viewModel.authError.collectAsState()
    val isLoading by viewModel.isAuthLoading.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PrimaryDeep, DarkBg)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo area
            val infiniteTransition = rememberInfiniteTransition(label = "glow")
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.5f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse), label = "a"
            )
            Text("🧠", fontSize = 64.sp)
            Spacer(Modifier.height(8.dp))
            Text("Moodİİ", style = MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
            Text("WakeMood Planner", style = MaterialTheme.typography.titleSmall, color = AccentMint.copy(alpha = glowAlpha))
            Spacer(Modifier.height(48.dp))

            // Login card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.7f)),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Giriş Yap", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(24.dp))

                    OutlinedTextField(
                        value = viewModel.loginUsername.value,
                        onValueChange = { viewModel.loginUsername.value = it; viewModel.clearAuthError() },
                        label = { Text("Kullanıcı Adı") },
                        leadingIcon = { Icon(Icons.Filled.Person, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentMint,
                            unfocusedBorderColor = DarkTextTertiary,
                            focusedLabelColor = AccentMint,
                            cursorColor = AccentMint
                        )
                    )
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = viewModel.loginPassword.value,
                        onValueChange = { viewModel.loginPassword.value = it; viewModel.clearAuthError() },
                        label = { Text("Şifre") },
                        leadingIcon = { Icon(Icons.Filled.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null)
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentMint,
                            unfocusedBorderColor = DarkTextTertiary,
                            focusedLabelColor = AccentMint,
                            cursorColor = AccentMint
                        )
                    )

                    authError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = AccentCoral, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.processLogin() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
                        else Text("Giriş Yap", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Hesabın yok mu?", color = DarkTextSecondary, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(4.dp))
                Text(
                    "Kayıt Ol",
                    color = AccentMint,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { viewModel.clearAuthError(); viewModel.navigateTo(Screen.REGISTER) }
                )
            }
        }
    }
}

@Composable
fun RegisterScreen(viewModel: MoodPlannerViewModel) {
    val authError by viewModel.authError.collectAsState()
    val isLoading by viewModel.isAuthLoading.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PrimaryDeep, DarkBg)))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("✨", fontSize = 56.sp)
            Spacer(Modifier.height(8.dp))
            Text("Hesap Oluştur", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedTextField(
                        value = viewModel.registerUsername.value,
                        onValueChange = { viewModel.registerUsername.value = it; viewModel.clearAuthError() },
                        label = { Text("Kullanıcı Adı") },
                        leadingIcon = { Icon(Icons.Filled.Person, null) },
                        singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentMint, unfocusedBorderColor = DarkTextTertiary, focusedLabelColor = AccentMint, cursorColor = AccentMint)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = viewModel.registerPassword.value,
                        onValueChange = { viewModel.registerPassword.value = it; viewModel.clearAuthError() },
                        label = { Text("Şifre") },
                        leadingIcon = { Icon(Icons.Filled.Lock, null) },
                        trailingIcon = { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null) } },
                        singleLine = true, visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentMint, unfocusedBorderColor = DarkTextTertiary, focusedLabelColor = AccentMint, cursorColor = AccentMint)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = viewModel.registerPasswordConfirm.value,
                        onValueChange = { viewModel.registerPasswordConfirm.value = it; viewModel.clearAuthError() },
                        label = { Text("Şifre Tekrar") },
                        leadingIcon = { Icon(Icons.Filled.Lock, null) },
                        singleLine = true, visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentMint, unfocusedBorderColor = DarkTextTertiary, focusedLabelColor = AccentMint, cursorColor = AccentMint)
                    )
                    authError?.let { Spacer(Modifier.height(8.dp)); Text(it, color = AccentCoral, style = MaterialTheme.typography.bodySmall) }
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.processRegister() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp), enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentMint)
                    ) {
                        if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = DarkBg)
                        else Text("Kayıt Ol", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkBg)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Zaten hesabın var mı?", color = DarkTextSecondary)
                Spacer(Modifier.width(4.dp))
                Text("Giriş Yap", color = AccentMint, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { viewModel.clearAuthError(); viewModel.navigateTo(Screen.LOGIN) })
            }
        }
    }
}
