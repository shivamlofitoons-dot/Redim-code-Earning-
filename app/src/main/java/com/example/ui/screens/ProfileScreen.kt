package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NotificationItem
import com.example.ui.components.*
import com.example.viewmodel.RewardsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: RewardsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val isFirebaseConnected by viewModel.isFirebaseConnected.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    var referralInput by remember { mutableStateOf("") }
    var showNotifsSheet by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBackground)
            .neonGlow(color = NeonMagenta, alpha = 0.04f)
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Main Title
            Text(
                text = "USER PROFILE",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp,
                modifier = Modifier.fillMaxWidth().maxSizeLimit()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // User Profile Details Card
            GlassCard(
                modifier = Modifier.fillMaxWidth().maxSizeLimit(),
                hasNeonBorder = true,
                neonColors = listOf(NeonMagenta, NeonCyan, NeonMagenta)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Avatar (Mock)
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(NeonCyan, NeonMagenta)))
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (currentUser?.username?.take(2)?.uppercase()) ?: "SH",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = currentUser?.username ?: "User",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = currentUser?.email ?: "user@gmail.com",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action: View Notifications Inbox Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .maxSizeLimit()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x0CFFFFFF))
                    .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(16.dp))
                    .clickable { showNotifsSheet = true }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonMagenta.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = NeonMagenta)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Notifications Inbox", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("${notifications.size} total messages", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Referral System Card
            GlassCard(
                modifier = Modifier.fillMaxWidth().maxSizeLimit(),
                hasNeonBorder = false
            ) {
                Text(
                    text = "REFERRAL SYSTEM",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Share referral code block
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x0CFFFFFF))
                        .border(1.dp, Color(0x10FFFFFF), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("YOUR REFERRAL CODE", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text(
                                text = currentUser?.referralCode ?: "FUN1234",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeonGreen,
                                letterSpacing = 1.sp
                            )
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Copy button
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Referral Code", currentUser?.referralCode ?: "")
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Code Copied!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0x15FFFFFF))
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White)
                            }

                            // Share button
                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Join Fun Earning App")
                                        putExtra(Intent.EXTRA_TEXT, "Earn daily reward points on Fun Earning App! Use my referral code to get 500 bonus points: ${currentUser?.referralCode}")
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Code"))
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0x15FFFFFF))
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // If referredBy is already entered, show code. Else show input field
                if (currentUser?.referredBy?.isNotEmpty() == true) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x0CFFFFFF))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Referred By Code Applied: ${currentUser?.referredBy}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Text(
                        text = "Have a referral code? Enter it below to earn +500 PTS bonus!",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = referralInput,
                            onValueChange = { referralInput = it },
                            placeholder = { Text("Enter referral code", color = Color.Gray, fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color(0x22FFFFFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f).height(48.dp)
                        )

                        Button(
                            onClick = {
                                viewModel.enterReferralCode(referralInput)
                                referralInput = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("Apply", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Firebase System Connection indicator card
            GlassCard(
                modifier = Modifier.fillMaxWidth().maxSizeLimit(),
                hasNeonBorder = false
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("FIREBASE INTEGRATION", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isFirebaseConnected) "CONNECTED TO CLOUD DATABASE" else "LOCAL SIMULATION ENGINE ACTIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFirebaseConnected) NeonGreen else NeonCyan
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isFirebaseConnected) NeonGreen else NeonCyan)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Logout Button
            Button(
                onClick = { viewModel.logout() },
                colors = ButtonDefaults.buttonColors(containerColor = NeonRed.copy(alpha = 0.2f), contentColor = NeonRed),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().maxSizeLimit().height(48.dp)
            ) {
                Icon(Icons.Default.Logout, contentDescription = "Log out", modifier = Modifier.size(18.dp).padding(end = 8.dp))
                Text("LOG OUT OF SESSION", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            Spacer(modifier = Modifier.height(120.dp)) // bottom navigation spacer
        }

        // NOTIFICATIONS INBOX DIALOG SHEET
        if (showNotifsSheet) {
            NotificationsInboxDialog(
                notifications = notifications,
                onDismiss = { showNotifsSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsInboxDialog(
    notifications: List<NotificationItem>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .maxSizeLimit()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF060608))
            .border(1.dp, NeonMagenta, RoundedCornerShape(24.dp)),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        content = {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = NeonMagenta, modifier = Modifier.size(24.dp).padding(end = 8.dp))
                        Text(
                            text = "NOTIFICATIONS INBOX",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    if (notifications.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("No recent notifications", color = Color.Gray, fontSize = 14.sp)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(notifications) { notif ->
                                val notifColor = when (notif.type) {
                                    "task_completed" -> NeonGreen
                                    "reward_approved" -> NeonCyan
                                    "reward_rejected" -> NeonRed
                                    "new_task" -> NeonYellow
                                    else -> Color.Gray
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x0CFFFFFF))
                                        .border(1.dp, Color(0x10FFFFFF), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(notifColor.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(notifColor)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = notif.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = notif.message,
                                                fontSize = 11.sp,
                                                color = Color.LightGray
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = formatTimestamp(notif.timestamp),
                                                fontSize = 9.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}
