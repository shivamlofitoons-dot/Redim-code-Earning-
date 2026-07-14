package com.example.model

import androidx.compose.runtime.Immutable

@Immutable
data class UserProfile(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val referralCode: String = "",
    val referredBy: String = "",
    val streakDays: Int = 0,
    val lastCheckInTimestamp: Long = 0L,
    val points: Int = 0
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "username" to username,
        "email" to email,
        "referralCode" to referralCode,
        "referredBy" to referredBy,
        "streakDays" to streakDays,
        "lastCheckInTimestamp" to lastCheckInTimestamp,
        "points" to points
    )
}

@Immutable
data class WalletState(
    val totalPoints: Int = 0,
    val earnedToday: Int = 0,
    val redeemedToday: Int = 0,
    val transactions: List<Transaction> = emptyList()
)

@Immutable
data class Transaction(
    val id: String = "",
    val title: String = "",
    val points: Int = 0,
    val type: String = "Earned", // "Earned" or "Redeemed"
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "points" to points,
        "type" to type,
        "timestamp" to timestamp
    )
}

@Immutable
data class TaskItem(
    val id: String = "",
    val title: String = "",
    val points: Int = 0,
    val type: String = "", // "playtime", "survey", "gd_playtime", "video", "subscribe", "app_install"
    val status: String = "Pending", // "Pending" or "Completed"
    val completionTime: Long = 0L
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "points" to points,
        "type" to type,
        "status" to status,
        "completionTime" to completionTime
    )
}

@Immutable
data class RedeemRequest(
    val id: String = "",
    val userEmail: String = "",
    val gmail: String = "",
    val rewardName: String = "",
    val points: Int = 0,
    val status: String = "Pending", // "Pending", "Approved", "Rejected", "Sent"
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userEmail" to userEmail,
        "gmail" to gmail,
        "rewardName" to rewardName,
        "points" to points,
        "status" to status,
        "timestamp" to timestamp
    )
}

@Immutable
data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "", // "task_completed", "reward_approved", "reward_rejected", "new_task"
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "message" to message,
        "type" to type,
        "timestamp" to timestamp
    )
}

@Immutable
data class AppSettings(
    val referralBonus: Int = 500,
    val dailyCheckInPoints: Int = 100,
    val streakBonusMultiplier: Int = 10
)
