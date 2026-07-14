package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.RewardsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(dynamicColor = false, darkTheme = true) {
                MainAppContainer()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer() {
    val context = LocalContext.current
    val viewModel: RewardsViewModel = viewModel()

    val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isFirebaseConnected by viewModel.isFirebaseConnected.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    var activeScreen by remember { mutableStateOf("dashboard") }
    var showNotifsInboxInMainActivity by remember { mutableStateOf(false) }

    // Toast status message listener
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearStatusMessage()
        }
    }

    if (!isUserLoggedIn) {
        LoginScreen(viewModel = viewModel)
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = AmoledBackground,
            topBar = {
                // Customized Glassmorphic Header TopBar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .background(AmoledBackground)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Title + Subtitle
                        Column {
                            Text(
                                text = "FUN EARNING",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isFirebaseConnected) NeonGreen else NeonCyan)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isFirebaseConnected) "Cloud Synced" else "Offline Simulation",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Right side icons: Balance Pill + Notification bell
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Points Balance Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x0CFFFFFF))
                                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Stars,
                                        contentDescription = null,
                                        tint = NeonYellow,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${currentUser?.points ?: 0} PTS",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Notification inbox shortcut bell
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x0CFFFFFF))
                                    .clickable { showNotifsInboxInMainActivity = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = NeonMagenta,
                                    modifier = Modifier.size(20.dp)
                                )
                                // Simple active notification bubble
                                if (notifications.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .align(Alignment.TopEnd)
                                            .padding(top = 4.dp, end = 4.dp)
                                            .clip(CircleShape)
                                            .background(NeonRed)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                FloatingBottomBar(
                    activeScreen = activeScreen,
                    onNavigate = { activeScreen = it }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Screen layout state manager
                when (activeScreen) {
                    "dashboard" -> DashboardScreen(viewModel = viewModel)
                    "wallet" -> WalletScreen(viewModel = viewModel)
                    "redeem" -> RedeemScreen(viewModel = viewModel)
                    "profile" -> ProfileScreen(viewModel = viewModel)
                }
            }
        }
    }

    // MAIN HEADER BELL POPUP NOTIFICATION INBOX
    if (showNotifsInboxInMainActivity) {
        NotificationsInboxDialog(
            notifications = notifications,
            onDismiss = { showNotifsInboxInMainActivity = false }
        )
    }
}

@Composable
fun FloatingBottomBar(
    activeScreen: String,
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
            .windowInsetsPadding(WindowInsets.navigationBars) // Safeguards gesture navigation overlaps as required by frontend-design SKILL!
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xE5030304)) // Deep AMOLED translucent glass
            .neonBorder(
                strokeWidth = 1.2.dp,
                shape = RoundedCornerShape(24.dp),
                colors = listOf(NeonCyan, NeonMagenta, NeonGreen, NeonCyan)
            )
            .padding(vertical = 4.dp, horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = listOf(
                Triple("dashboard", Icons.Default.Dashboard, "Task Center"),
                Triple("wallet", Icons.Default.AccountBalanceWallet, "Wallet"),
                Triple("redeem", Icons.Default.Redeem, "Redeem"),
                Triple("profile", Icons.Default.Person, "Profile")
            )

            items.forEach { (route, icon, label) ->
                val isActive = activeScreen == route
                val tintColor = if (isActive) NeonCyan else Color.Gray

                Column(
                    modifier = Modifier
                        .clickable { onNavigate(route) }
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = tintColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        color = tintColor,
                        fontSize = 10.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
