package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Transaction
import com.example.ui.components.*
import com.example.viewmodel.RewardsViewModel

@Composable
fun WalletScreen(
    viewModel: RewardsViewModel,
    modifier: Modifier = Modifier
) {
    val wallet by viewModel.wallet.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBackground)
            .neonGlow(color = NeonCyan, alpha = 0.04f)
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Main Wallet Title
            Text(
                text = "MY WALLET",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp,
                modifier = Modifier.fillMaxWidth().maxSizeLimit()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Total Points Display Card
            GlassCard(
                modifier = Modifier.fillMaxWidth().maxSizeLimit(),
                hasNeonBorder = true,
                neonColors = listOf(NeonCyan, NeonMagenta, NeonCyan)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("TOTAL BALANCE", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Stars, contentDescription = null, tint = NeonYellow, modifier = Modifier.size(32.dp).padding(end = 8.dp))
                            Text(
                                text = "${wallet.totalPoints}",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0x0CFFFFFF))
                            .border(1.dp, Color(0x1FFFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(28.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Sub-grid (Earned Today vs Redeemed Today)
            Row(
                modifier = Modifier.fillMaxWidth().maxSizeLimit(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Earned Today
                GlassCard(
                    modifier = Modifier.weight(1f),
                    hasNeonBorder = false
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0x1A2AFFB0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("EARNED TODAY", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text("+${wallet.earnedToday}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                        }
                    }
                }

                // Redeemed Today
                GlassCard(
                    modifier = Modifier.weight(1f),
                    hasNeonBorder = false
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0x1AFF3366)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = NeonRed, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("REDEEMED TODAY", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text("-${wallet.redeemedToday}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeonRed)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Transaction History Title
            Text(
                text = "TRANSACTION LOGS",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp,
                modifier = Modifier.fillMaxWidth().maxSizeLimit().padding(bottom = 12.dp)
            )

            // Transactions list
            Box(modifier = Modifier.fillMaxWidth().maxSizeLimit().weight(1f)) {
                if (wallet.transactions.isEmpty()) {
                    // Empty state (Following frontend-design requirements)
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "Empty History",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Transactions Found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Start completing playtime or survey tasks to earn reward points!",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(wallet.transactions) { transaction ->
                            TransactionRow(transaction)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionRow(transaction: Transaction) {
    val isEarned = transaction.type == "Earned"
    val indicatorColor = if (isEarned) NeonGreen else NeonRed
    val sign = if (isEarned) "+" else "-"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x08FFFFFF))
            .border(1.dp, Color(0x10FFFFFF), RoundedCornerShape(12.dp))
            .padding(14.dp)
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
                        .background(indicatorColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isEarned) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = indicatorColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = transaction.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatTimestamp(transaction.timestamp),
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
            Text(
                text = "$sign${transaction.points} PTS",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = indicatorColor
            )
        }
    }
}
