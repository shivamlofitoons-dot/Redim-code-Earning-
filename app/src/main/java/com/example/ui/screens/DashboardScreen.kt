package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TaskItem
import com.example.ui.components.*
import com.example.viewmodel.RewardsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: RewardsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val scrollState = rememberScrollState()

    // Spin Wheel Sheet / Dialog State
    var showSpinDialog by remember { mutableStateOf(false) }

    // Active Task Simulator State
    var activeSimulatorTask by remember { mutableStateOf<TaskItem?>(null) }

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

            // Points Balance Card
            GlassCard(
                modifier = Modifier.fillMaxWidth().maxSizeLimit(),
                hasNeonBorder = true,
                neonColors = listOf(NeonCyan, NeonMagenta, NeonCyan)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Welcome, ${currentUser?.username ?: "User"}",
                            fontSize = 14.sp,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Stars,
                                contentDescription = null,
                                tint = NeonYellow,
                                modifier = Modifier.size(28.dp).padding(end = 6.dp)
                            )
                            Text(
                                text = "${currentUser?.points ?: 0}",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "Reward Points Balance",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // Streak Display
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x1F2AFFB0))
                            .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                contentDescription = "Streak",
                                tint = NeonMagenta,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "${currentUser?.streakDays ?: 0} Days",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Streak",
                                fontSize = 9.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Row: Daily Check-in & Lucky Spin
            Row(
                modifier = Modifier.fillMaxWidth().maxSizeLimit(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Daily Check-In
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x0CFFFFFF))
                        .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(16.dp))
                        .clickable { viewModel.claimDailyCheckIn() }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = "Daily Check-in",
                            tint = NeonCyan,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Daily Check-In",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Claim daily reward",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Lucky Spin
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x0CFFFFFF))
                        .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(16.dp))
                        .clickable { showSpinDialog = true }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Casino,
                            contentDescription = "Lucky Spin",
                            tint = NeonMagenta,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Lucky Spin",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Spin neon wheel",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tasks Section Title
            Row(
                modifier = Modifier.fillMaxWidth().maxSizeLimit(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TASK CENTER",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "6 Tasks Available",
                    fontSize = 12.sp,
                    color = NeonCyan
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tasks list (Custom grid-like column layout for responsiveness)
            Column(
                modifier = Modifier.fillMaxWidth().maxSizeLimit(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                tasks.forEach { task ->
                    TaskCard(
                        task = task,
                        onAction = {
                            activeSimulatorTask = task
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(100.dp)) // padding for floating bottom navigation bar
        }

        // LUCKY SPIN WHEEL DIALOG OVERLAY
        if (showSpinDialog) {
            LuckySpinDialog(
                onDismiss = { showSpinDialog = false },
                onSpinResult = { points ->
                    viewModel.spinWheelAndReward(points)
                }
            )
        }

        // ACTIVE TASK SIMULATION OVERLAY
        activeSimulatorTask?.let { task ->
            TaskSimulatorOverlay(
                task = task,
                onDismiss = { activeSimulatorTask = null },
                onComplete = {
                    viewModel.completeTask(task.id, task.title, task.points)
                    activeSimulatorTask = null
                }
            )
        }
    }
}

@Composable
fun TaskCard(
    task: TaskItem,
    onAction: () -> Unit
) {
    val isCompleted = task.status == "Completed"
    val accentColor = when (task.type) {
        "playtime" -> NeonCyan
        "survey" -> NeonGreen
        "gd_playtime" -> NeonYellow
        "video" -> NeonMagenta
        "subscribe" -> NeonRed
        else -> NeonCyan
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x0CFFFFFF))
            .border(
                width = 1.dp,
                color = if (isCompleted) Color(0x1F2AFFB0) else Color(0x1FFFFFFF),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = task.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Stars,
                        contentDescription = null,
                        tint = NeonYellow,
                        modifier = Modifier.size(14.dp).padding(end = 4.dp)
                    )
                    Text(
                        text = "+${task.points} Points",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = NeonYellow
                    )
                }
                if (isCompleted) {
                    Text(
                        text = "Completed: ${formatTimestamp(task.completionTime)}",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Task State Button
            Button(
                onClick = { if (!isCompleted) onAction() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCompleted) Color(0x1F2AFFB0) else accentColor,
                    contentColor = if (isCompleted) NeonGreen else Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                enabled = !isCompleted,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                if (isCompleted) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Completed",
                        modifier = Modifier.size(16.dp).padding(end = 4.dp)
                    )
                    Text("Done", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("Start", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// LUCKY NEON SPIN WHEEL DIALOG
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LuckySpinDialog(
    onDismiss: () -> Unit,
    onSpinResult: (Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var rotationAngle by remember { mutableStateOf(0f) }
    var isSpinning by remember { mutableStateOf(false) }
    
    // Custom options
    val segments = listOf(
        Pair("10 PTS", 10),
        Pair("50 PTS", 50),
        Pair("0 PTS", 0),
        Pair("100 PTS", 100),
        Pair("20 PTS", 20),
        Pair("200 PTS", 200),
        Pair("5 PTS", 5),
        Pair("500 PTS", 500)
    )

    val animatedRotation by animateFloatAsState(
        targetValue = rotationAngle,
        animationSpec = tween(
            durationMillis = 4000,
            easing = EaseOutQuart
        ),
        label = "spin_rotation"
    )

    AlertDialog(
        onDismissRequest = { if (!isSpinning) onDismiss() },
        modifier = Modifier
            .fillMaxWidth()
            .maxSizeLimit()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF060608))
            .border(1.5.dp, Brush.linearGradient(listOf(NeonCyan, NeonMagenta)), RoundedCornerShape(24.dp)),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        content = {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LUCKY SPIN WHEEL",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Test your luck once per session!",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Neon Wheel Canvas Container
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Wheel segments drawn via custom canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(animatedRotation)
                    ) {
                        val strokeWidth = 2.dp.toPx()
                        val diameter = size.minDimension
                        val rect = Size(diameter, diameter)
                        
                        // Segment Colors
                        val colors = listOf(
                            NeonCyan, Color(0xFF1C1B1F), NeonMagenta, Color(0xFF1C1B1F),
                            NeonGreen, Color(0xFF1C1B1F), NeonYellow, Color(0xFF1C1B1F)
                        )

                        for (i in segments.indices) {
                            val startAngle = i * 45f
                            drawArc(
                                color = colors[i % colors.size],
                                startAngle = startAngle,
                                sweepAngle = 45f,
                                useCenter = true,
                                size = size
                            )
                            drawArc(
                                color = Color(0x33FFFFFF),
                                startAngle = startAngle,
                                sweepAngle = 45f,
                                useCenter = true,
                                size = size,
                                style = Stroke(width = strokeWidth)
                            )
                        }

                        // Draw outer circle neon ring
                        drawCircle(
                            color = NeonCyan,
                            radius = diameter / 2f,
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }

                    // Pointer (Arrow at the top pointing down)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 220.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = NeonRed,
                            modifier = Modifier.size(52.dp)
                        )
                    }

                    // Inner circle core
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .border(2.dp, NeonYellow, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Stars,
                            contentDescription = null,
                            tint = NeonYellow,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isSpinning) {
                    CircularProgressIndicator(color = NeonMagenta)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x1CFFFFFF)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close", color = Color.White)
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isSpinning = true
                                    // Generate a random multiplier of 45 deg for segments
                                    val randomSegmentIndex = Random.nextInt(segments.size)
                                    // Total angle: spin around 5 times (1800 deg) + center of selected segment
                                    val finalAngle = 1800f + (360f - (randomSegmentIndex * 45f) - 22.5f)
                                    rotationAngle = finalAngle
                                    
                                    // Wait for animation to finish
                                    delay(4200)
                                    
                                    val reward = segments[randomSegmentIndex]
                                    onSpinResult(reward.second)
                                    isSpinning = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Spin Wheel", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    )
}

// TASK SIMULATION SHEETS
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSimulatorOverlay(
    task: TaskItem,
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var simulationProgress by remember { mutableFloatStateOf(0f) }
    var currentStep by remember { mutableStateOf("") }
    
    // For Survey
    var surveyQuestionIndex by remember { mutableIntStateOf(0) }
    val surveyQuestions = listOf(
        "Q1: What is your favorite way to earn reward points?",
        "Q2: How likely are you to recommend this app to a friend?",
        "Q3: Rate SHIVAM's app design on a scale of 1-5 stars!"
    )
    val surveyAnswers = listOf(
        listOf("Playing Mini Games", "Watching Video Tasks", "Referrals", "Lucky Spin"),
        listOf("Very Likely", "Maybe Later", "Definitely!", "Unsure"),
        listOf("⭐⭐⭐⭐⭐ (Excellent)", "⭐⭐⭐⭐ (Very Good)", "⭐⭐⭐ (Average)", "⭐ (Need Fix)")
    )

    // For GD Playtime (Mini Clicking Game)
    var clickCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(task.type) {
        when (task.type) {
            "playtime" -> {
                currentStep = "Launching playtime background thread..."
                for (i in 1..100) {
                    delay(80)
                    simulationProgress = i / 100f
                    if (i == 30) currentStep = "Tracking active play minutes..."
                    if (i == 70) currentStep = "Syncing logs with SHIVAM backend..."
                }
            }
            "video" -> {
                currentStep = "Buffering video content..."
                for (i in 1..100) {
                    delay(50)
                    simulationProgress = i / 100f
                    if (i == 20) currentStep = "Streaming reward sponsor clip..."
                    if (i == 80) currentStep = "Verifying watch completion status..."
                }
            }
            "app_install" -> {
                currentStep = "Opening Play Store link simulator..."
                for (i in 1..100) {
                    delay(60)
                    simulationProgress = i / 100f
                    if (i == 25) currentStep = "Downloading APK packages..."
                    if (i == 65) currentStep = "Installing client-side services..."
                    if (i == 90) currentStep = "Verifying unique install fingerprint..."
                }
            }
            "subscribe" -> {
                currentStep = "Routing to YouTube channel..."
                for (i in 1..100) {
                    delay(40)
                    simulationProgress = i / 100f
                    if (i == 40) currentStep = "Please hit SUBSCRIBE & click back..."
                    if (i == 80) currentStep = "Checking subscription webhook..."
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .maxSizeLimit()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF060608))
            .border(1.dp, NeonCyan, RoundedCornerShape(24.dp)),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        content = {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TASK SIMULATOR",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = task.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                // DYNAMIC CONTENTS BASED ON TYPE
                when (task.type) {
                    "survey" -> {
                        // Survey dialog flow
                        Text(
                            text = surveyQuestions[surveyQuestionIndex],
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            surveyAnswers[surveyQuestionIndex].forEach { option ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (surveyQuestionIndex < surveyQuestions.size - 1) {
                                                surveyQuestionIndex++
                                            } else {
                                                onComplete()
                                            }
                                        },
                                    colors = CardDefaults.cardColors(containerColor = Color(0x0CFFFFFF)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1FFFFFFF))
                                ) {
                                    Text(
                                        text = option,
                                        color = Color.LightGray,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        }
                    }
                    "gd_playtime" -> {
                        // Game clicking flow
                        Text(
                            text = "Tap the Neon target 5 times as fast as possible!",
                            fontSize = 14.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(if (clickCount % 2 == 0) NeonMagenta.copy(alpha = 0.2f) else NeonCyan.copy(alpha = 0.2f))
                                .border(2.dp, if (clickCount % 2 == 0) NeonMagenta else NeonCyan, CircleShape)
                                .clickable {
                                    clickCount++
                                    if (clickCount >= 5) {
                                        onComplete()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Gamepad,
                                    contentDescription = null,
                                    tint = NeonYellow,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "$clickCount / 5",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    else -> {
                        // Loading simulator
                        LinearProgressIndicator(
                            progress = { simulationProgress },
                            color = NeonCyan,
                            trackColor = Color(0x15FFFFFF),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = currentStep,
                            fontSize = 14.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        if (simulationProgress >= 1f) {
                            Button(
                                onClick = onComplete,
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Claim Reward Points", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    )
}

fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return "Pending"
    val sdf = java.text.SimpleDateFormat("hh:mm a, dd MMM", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
