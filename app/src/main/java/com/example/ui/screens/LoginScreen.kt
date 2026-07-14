package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.viewmodel.RewardsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: RewardsViewModel,
    modifier: Modifier = Modifier
) {
    var isSignUp by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var referralCode by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsState()
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBackground)
            .neonGlow(color = NeonCyan, alpha = 0.08f)
            .neonGlow(color = NeonMagenta, alpha = 0.06f)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .maxSizeLimit()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Branding Title
            Text(
                text = "FUN EARNING",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = "by SHIVAM",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = NeonCyan,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Dynamic Glass Card for Form
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                hasNeonBorder = true,
                neonColors = listOf(NeonCyan, NeonMagenta, NeonGreen, NeonCyan)
            ) {
                Text(
                    text = if (isSignUp) "Create Account" else "Welcome Back",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                if (isSignUp) {
                    // Username Field
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = NeonCyan) },
                        colors = outlinedTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )
                }

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = NeonCyan) },
                    colors = outlinedTextFieldColors(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NeonCyan) },
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(icon, contentDescription = "Toggle password visibility", tint = Color.Gray)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = outlinedTextFieldColors(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth().padding(bottom = if (isSignUp) 16.dp else 24.dp)
                )

                if (isSignUp) {
                    // Referral Code Field
                    OutlinedTextField(
                        value = referralCode,
                        onValueChange = { referralCode = it },
                        label = { Text("Referral Code (Optional)", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = NeonGreen) },
                        colors = outlinedTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                    )
                }

                // Action Button with Loading check
                if (isLoading) {
                    CircularProgressIndicator(
                        color = NeonMagenta,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 8.dp)
                    )
                } else {
                    NeonButton(
                        onClick = {
                            if (isSignUp) {
                                viewModel.signUp(username, email, password, referralCode) { success ->
                                    if (success) {
                                        username = ""
                                        email = ""
                                        password = ""
                                        referralCode = ""
                                    }
                                }
                            } else {
                                viewModel.login(email, password) { success ->
                                    if (success) {
                                        email = ""
                                        password = ""
                                    }
                                }
                            }
                        },
                        colors = listOf(NeonCyan, NeonMagenta)
                    ) {
                        Text(
                            text = if (isSignUp) "SIGN UP" else "LOG IN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Toggle Mode button
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSignUp) "Already have an account?" else "New to Fun Earning?",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                    TextButton(onClick = { isSignUp = !isSignUp }) {
                        Text(
                            text = if (isSignUp) "Log In" else "Sign Up",
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NeonCyan,
    unfocusedBorderColor = Color(0x33FFFFFF),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = NeonCyan,
    unfocusedLabelColor = Color.Gray,
    focusedContainerColor = Color(0x05FFFFFF),
    unfocusedContainerColor = Color(0x05FFFFFF)
)

fun Modifier.maxSizeLimit() = this.widthIn(max = 480.dp)
