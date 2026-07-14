package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.firebase.FirebaseConfig
import com.example.model.*
import com.example.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class RewardsViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "RewardsViewModel"
    private val prefs: SharedPreferences = application.getSharedPreferences("fun_earning_prefs", Context.MODE_PRIVATE)

    // UI States
    private val _isUserLoggedIn = MutableStateFlow(false)
    val isUserLoggedIn = _isUserLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _wallet = MutableStateFlow(WalletState())
    val wallet = _wallet.asStateFlow()

    private val _tasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val tasks = _tasks.asStateFlow()

    private val _redeemRequests = MutableStateFlow<List<RedeemRequest>>(emptyList())
    val redeemRequests = _redeemRequests.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage = _statusMessage.asStateFlow()

    private val _isFirebaseConnected = MutableStateFlow(false)
    val isFirebaseConnected = _isFirebaseConnected.asStateFlow()

    // Database listeners
    private var userListener: ValueEventListener? = null
    private var tasksListener: ValueEventListener? = null
    private var walletListener: ValueEventListener? = null
    private var redeemListener: ValueEventListener? = null
    private var notificationsListener: ValueEventListener? = null

    init {
        // Initialize Firebase
        FirebaseConfig.initialize(application)
        _isFirebaseConnected.value = FirebaseConfig.isFirebaseActive
        
        // Load initial session from SharedPreferences
        checkLocalSession()
    }

    private fun checkLocalSession() {
        val savedUserId = prefs.getString("current_user_id", null)
        val savedEmail = prefs.getString("current_user_email", null)
        
        if (savedUserId != null && savedEmail != null) {
            _isLoading.value = true
            setupSession(savedUserId, savedEmail)
        } else {
            // Setup default empty lists for logged-out state
            setupDefaultTasks()
        }
    }

    private fun setupSession(userId: String, email: String) {
        _isUserLoggedIn.value = true
        
        // Store in prefs
        prefs.edit()
            .putString("current_user_id", userId)
            .putString("current_user_email", email)
            .apply()

        if (FirebaseConfig.isFirebaseActive) {
            setupFirebaseListeners(userId)
        } else {
            setupLocalSimulation(userId, email)
        }
    }

    private fun setupDefaultTasks() {
        _tasks.value = listOf(
            TaskItem("task_playtime", "Playtime Task", 150, "playtime"),
            TaskItem("task_survey", "Survey Task", 200, "survey"),
            TaskItem("task_gd_playtime", "GD Playtime Task", 300, "gd_playtime"),
            TaskItem("task_video", "Video Task", 100, "video"),
            TaskItem("task_subscribe", "Subscribe Task", 80, "subscribe"),
            TaskItem("task_app_install", "App Install Task", 250, "app_install")
        )
    }

    private fun setupLocalSimulation(userId: String, email: String) {
        Log.d(TAG, "Setting up offline simulation for user: $email")
        
        // Retrieve local profile or create new one
        val username = prefs.getString("local_user_${userId}_username", "User_${email.substringBefore("@")}") ?: "User"
        val referralCode = prefs.getString("local_user_${userId}_ref", "FUN${userId.takeLast(4).uppercase()}") ?: "FUN1234"
        val referredBy = prefs.getString("local_user_${userId}_referredBy", "") ?: ""
        val points = prefs.getInt("local_user_${userId}_points", 0)
        val streak = prefs.getInt("local_user_${userId}_streak", 0)
        val lastCheckIn = prefs.getLong("local_user_${userId}_lastCheckIn", 0L)

        val profile = UserProfile(
            id = userId,
            username = username,
            email = email,
            referralCode = referralCode,
            referredBy = referredBy,
            streakDays = streak,
            lastCheckInTimestamp = lastCheckIn,
            points = points
        )
        _currentUser.value = profile

        // Setup local tasks
        val completedTasksStr = prefs.getString("local_user_${userId}_completed_tasks", "") ?: ""
        val completedIds = completedTasksStr.split(",").filter { it.isNotEmpty() }.toSet()
        
        _tasks.value = listOf(
            TaskItem("task_playtime", "Playtime Task", 150, "playtime", if ("task_playtime" in completedIds) "Completed" else "Pending", if ("task_playtime" in completedIds) System.currentTimeMillis() else 0L),
            TaskItem("task_survey", "Survey Task", 200, "survey", if ("task_survey" in completedIds) "Completed" else "Pending", if ("task_survey" in completedIds) System.currentTimeMillis() else 0L),
            TaskItem("task_gd_playtime", "GD Playtime Task", 300, "gd_playtime", if ("task_gd_playtime" in completedIds) "Completed" else "Pending", if ("task_gd_playtime" in completedIds) System.currentTimeMillis() else 0L),
            TaskItem("task_video", "Video Task", 100, "video", if ("task_video" in completedIds) "Completed" else "Pending", if ("task_video" in completedIds) System.currentTimeMillis() else 0L),
            TaskItem("task_subscribe", "Subscribe Task", 80, "subscribe", if ("task_subscribe" in completedIds) "Completed" else "Pending", if ("task_subscribe" in completedIds) System.currentTimeMillis() else 0L),
            TaskItem("task_app_install", "App Install Task", 250, "app_install", if ("task_app_install" in completedIds) "Completed" else "Pending", if ("task_app_install" in completedIds) System.currentTimeMillis() else 0L)
        )

        // Load Wallet Transactions
        val transStr = prefs.getString("local_user_${userId}_transactions", "") ?: ""
        val transactionsList = parseTransactions(transStr)
        
        // Calculate earned and redeemed today
        val todayStart = getStartOfDay()
        val earnedToday = transactionsList.filter { it.type == "Earned" && it.timestamp >= todayStart }.sumOf { it.points }
        val redeemedToday = transactionsList.filter { it.type == "Redeemed" && it.timestamp >= todayStart }.sumOf { it.points }

        _wallet.value = WalletState(
            totalPoints = points,
            earnedToday = earnedToday,
            redeemedToday = redeemedToday,
            transactions = transactionsList
        )

        // Load Redeem Requests
        val redeemStr = prefs.getString("local_user_${userId}_redeems", "") ?: ""
        _redeemRequests.value = parseRedeems(redeemStr)

        // Load Notifications
        val notifStr = prefs.getString("local_user_${userId}_notifications", "") ?: ""
        val parsedNotifs = parseNotifications(notifStr)
        if (parsedNotifs.isEmpty()) {
            // Add a default welcome notification
            val welcome = NotificationItem(
                id = UUID.randomUUID().toString(),
                title = "Welcome to Fun Earning App!",
                message = "Complete playtime, video and survey tasks to earn reward points daily!",
                type = "new_task",
                timestamp = System.currentTimeMillis()
            )
            _notifications.value = listOf(welcome)
            saveNotifications(userId, listOf(welcome))
        } else {
            _notifications.value = parsedNotifs
        }

        _isLoading.value = false
    }

    private fun setupFirebaseListeners(userId: String) {
        Log.d(TAG, "Setting up real-time Firebase listeners for user: $userId")
        
        // Remove existing listeners if any
        removeListeners()

        // 1. User profile listener
        userListener = FirebaseConfig.getUsersReference()?.child(userId)?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val profile = snapshot.getValue(UserProfile::class.java)
                if (profile != null) {
                    _currentUser.value = profile
                    // Sync points with wallet
                    _wallet.value = _wallet.value.copy(totalPoints = profile.points)
                } else {
                    // Create basic profile in Firebase if doesn't exist
                    val email = FirebaseConfig.auth?.currentUser?.email ?: "user@example.com"
                    val newProfile = UserProfile(
                        id = userId,
                        username = email.substringBefore("@"),
                        email = email,
                        referralCode = "FUN${userId.takeLast(4).uppercase()}",
                        points = 0
                    )
                    FirebaseConfig.getUsersReference()?.child(userId)?.setValue(newProfile)
                }
                _isLoading.value = false
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "User listener cancelled", error.toException())
                _isLoading.value = false
            }
        })

        // 2. Wallet/Transaction listener
        walletListener = FirebaseConfig.getWalletsReference()?.child(userId)?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val transactionsList = mutableListOf<Transaction>()
                snapshot.child("transactions").children.forEach { child ->
                    val trans = child.getValue(Transaction::class.java)
                    if (trans != null) transactionsList.add(trans)
                }
                transactionsList.sortByDescending { it.timestamp }

                val todayStart = getStartOfDay()
                val earnedToday = transactionsList.filter { it.type == "Earned" && it.timestamp >= todayStart }.sumOf { it.points }
                val redeemedToday = transactionsList.filter { it.type == "Redeemed" && it.timestamp >= todayStart }.sumOf { it.points }

                _wallet.value = WalletState(
                    totalPoints = _currentUser.value?.points ?: 0,
                    earnedToday = earnedToday,
                    redeemedToday = redeemedToday,
                    transactions = transactionsList
                )
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Wallet listener cancelled", error.toException())
            }
        })

        // 3. Task completion listener
        tasksListener = FirebaseConfig.getTaskHistoryReference()?.child(userId)?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val completedTaskIds = mutableMapOf<String, Long>()
                snapshot.children.forEach { child ->
                    val taskId = child.key ?: ""
                    val compTime = child.getValue(Long::class.java) ?: 0L
                    completedTaskIds[taskId] = compTime
                }

                _tasks.value = listOf(
                    TaskItem("task_playtime", "Playtime Task", 150, "playtime", if ("task_playtime" in completedTaskIds) "Completed" else "Pending", completedTaskIds["task_playtime"] ?: 0L),
                    TaskItem("task_survey", "Survey Task", 200, "survey", if ("task_survey" in completedTaskIds) "Completed" else "Pending", completedTaskIds["task_survey"] ?: 0L),
                    TaskItem("task_gd_playtime", "GD Playtime Task", 300, "gd_playtime", if ("task_gd_playtime" in completedTaskIds) "Completed" else "Pending", completedTaskIds["task_gd_playtime"] ?: 0L),
                    TaskItem("task_video", "Video Task", 100, "video", if ("task_video" in completedTaskIds) "Completed" else "Pending", completedTaskIds["task_video"] ?: 0L),
                    TaskItem("task_subscribe", "Subscribe Task", 80, "subscribe", if ("task_subscribe" in completedTaskIds) "Completed" else "Pending", completedTaskIds["task_subscribe"] ?: 0L),
                    TaskItem("task_app_install", "App Install Task", 250, "app_install", if ("task_app_install" in completedTaskIds) "Completed" else "Pending", completedTaskIds["task_app_install"] ?: 0L)
                )
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Tasks listener cancelled", error.toException())
            }
        })

        // 4. Redeem requests listener
        redeemListener = FirebaseConfig.getRedeemRequestsReference()?.orderByChild("userEmail")?.equalTo(_currentUser.value?.email ?: "")
            ?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<RedeemRequest>()
                    snapshot.children.forEach { child ->
                        val req = child.getValue(RedeemRequest::class.java)
                        if (req != null) list.add(req)
                    }
                    list.sortByDescending { it.timestamp }
                    _redeemRequests.value = list
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Redeem requests listener cancelled", error.toException())
                }
            })

        // 5. Notifications listener
        notificationsListener = FirebaseConfig.getNotificationsReference()?.child(userId)?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<NotificationItem>()
                snapshot.children.forEach { child ->
                    val notif = child.getValue(NotificationItem::class.java)
                    if (notif != null) list.add(notif)
                }
                list.sortByDescending { it.timestamp }
                if (list.isEmpty()) {
                    val welcome = NotificationItem(
                        id = UUID.randomUUID().toString(),
                        title = "Welcome to Fun Earning App!",
                        message = "Complete playtime, video and survey tasks to earn reward points daily!",
                        type = "new_task",
                        timestamp = System.currentTimeMillis()
                    )
                    _notifications.value = listOf(welcome)
                    FirebaseConfig.getNotificationsReference()?.child(userId)?.child(welcome.id)?.setValue(welcome)
                } else {
                    _notifications.value = list
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Notifications listener cancelled", error.toException())
            }
        })
    }

    private fun removeListeners() {
        userListener?.let { FirebaseConfig.getUsersReference()?.removeEventListener(it) }
        tasksListener?.let { FirebaseConfig.getTaskHistoryReference()?.removeEventListener(it) }
        walletListener?.let { FirebaseConfig.getWalletsReference()?.removeEventListener(it) }
        redeemListener?.let { FirebaseConfig.getRedeemRequestsReference()?.removeEventListener(it) }
        notificationsListener?.let { FirebaseConfig.getNotificationsReference()?.removeEventListener(it) }
    }

    // AUTH ACTIONS
    fun login(email: String, pword: String, onResult: (Boolean) -> Unit) {
        if (email.isEmpty() || pword.isEmpty()) {
            _statusMessage.value = "Email and Password cannot be empty."
            onResult(false)
            return
        }

        _isLoading.value = true
        _statusMessage.value = null

        if (FirebaseConfig.isFirebaseActive && FirebaseConfig.auth != null) {
            viewModelScope.launch {
                try {
                    val authResult = FirebaseConfig.auth!!.signInWithEmailAndPassword(email, pword).await()
                    val user = authResult.user
                    if (user != null) {
                        setupSession(user.uid, email)
                        _statusMessage.value = "Login Successful!"
                        onResult(true)
                    } else {
                        _statusMessage.value = "Login Failed."
                        onResult(false)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Firebase login error, falling back to local auth", e)
                    // Attempt local auth fallback so they can still test
                    performLocalLogin(email, pword, onResult)
                } finally {
                    _isLoading.value = false
                }
            }
        } else {
            // Run pure local session auth
            viewModelScope.launch {
                performLocalLogin(email, pword, onResult)
                _isLoading.value = false
            }
        }
    }

    private fun performLocalLogin(email: String, pword: String, onResult: (Boolean) -> Unit) {
        val savedPword = prefs.getString("local_user_pw_${email}", null)
        if (savedPword == pword) {
            val userId = prefs.getString("local_user_id_${email}", UUID.randomUUID().toString()) ?: UUID.randomUUID().toString()
            setupSession(userId, email)
            _statusMessage.value = "Login Successful! (Offline Mode)"
            onResult(true)
        } else {
            _statusMessage.value = "Incorrect password or account does not exist offline."
            onResult(false)
        }
    }

    fun signUp(username: String, email: String, pword: String, referralCode: String, onResult: (Boolean) -> Unit) {
        if (username.isEmpty() || email.isEmpty() || pword.isEmpty()) {
            _statusMessage.value = "All fields are required."
            onResult(false)
            return
        }

        _isLoading.value = true
        _statusMessage.value = null

        if (FirebaseConfig.isFirebaseActive && FirebaseConfig.auth != null) {
            viewModelScope.launch {
                try {
                    val authResult = FirebaseConfig.auth!!.createUserWithEmailAndPassword(email, pword).await()
                    val user = authResult.user
                    if (user != null) {
                        val userId = user.uid
                        val myRefCode = "FUN${userId.takeLast(4).uppercase()}"
                        
                        // Setup the profile
                        val newProfile = UserProfile(
                            id = userId,
                            username = username,
                            email = email,
                            referralCode = myRefCode,
                            referredBy = referralCode,
                            points = if (referralCode.isNotEmpty()) 500 else 0 // 500 bonus for using a ref
                        )

                        // Save to Realtime Database
                        FirebaseConfig.getUsersReference()?.child(userId)?.setValue(newProfile)?.await()

                        // Create wallet entry
                        val initialTrans = mutableListOf<Transaction>()
                        if (referralCode.isNotEmpty()) {
                            val bonusTrans = Transaction(
                                id = UUID.randomUUID().toString(),
                                title = "Referral Bonus Received",
                                points = 500,
                                type = "Earned"
                            )
                            initialTrans.add(bonusTrans)
                            FirebaseConfig.getWalletsReference()?.child(userId)?.child("transactions")?.child(bonusTrans.id)?.setValue(bonusTrans)
                            
                            // Generate referral welcome notification
                            val bonusNotif = NotificationItem(
                                id = UUID.randomUUID().toString(),
                                title = "Referral Bonus Claimed",
                                message = "You received 500 points using referral code: $referralCode",
                                type = "task_completed"
                            )
                            FirebaseConfig.getNotificationsReference()?.child(userId)?.child(bonusNotif.id)?.setValue(bonusNotif)
                        }

                        // Also store locally for fallback persistence
                        saveLocalProfile(userId, newProfile, pword, initialTrans, referralCode)

                        setupSession(userId, email)
                        _statusMessage.value = "Registration Successful!"
                        onResult(true)
                    } else {
                        _statusMessage.value = "Registration failed."
                        onResult(false)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Firebase signup error, falling back to local signup", e)
                    performLocalSignUp(username, email, pword, referralCode, onResult)
                } finally {
                    _isLoading.value = false
                }
            }
        } else {
            // Run pure local sign up
            viewModelScope.launch {
                performLocalSignUp(username, email, pword, referralCode, onResult)
                _isLoading.value = false
            }
        }
    }

    private fun performLocalSignUp(username: String, email: String, pword: String, referralCode: String, onResult: (Boolean) -> Unit) {
        val alreadyExists = prefs.contains("local_user_pw_${email}")
        if (alreadyExists) {
            _statusMessage.value = "Account with this email already exists locally."
            onResult(false)
            return
        }

        val userId = UUID.randomUUID().toString()
        val myRefCode = "FUN${userId.takeLast(4).uppercase()}"
        val hasReferral = referralCode.isNotEmpty()
        val initialPoints = if (hasReferral) 500 else 0

        val newProfile = UserProfile(
            id = userId,
            username = username,
            email = email,
            referralCode = myRefCode,
            referredBy = referralCode,
            points = initialPoints
        )

        val initialTrans = mutableListOf<Transaction>()
        if (hasReferral) {
            initialTrans.add(
                Transaction(
                    id = UUID.randomUUID().toString(),
                    title = "Referral Bonus Received",
                    points = 500,
                    type = "Earned"
                )
            )
        }

        saveLocalProfile(userId, newProfile, pword, initialTrans, referralCode)
        setupSession(userId, email)
        _statusMessage.value = "Registration Successful! (Offline Mode)"
        onResult(true)
    }

    private fun saveLocalProfile(userId: String, profile: UserProfile, pword: String, transList: List<Transaction>, referredBy: String) {
        prefs.edit()
            .putString("local_user_pw_${profile.email}", pword)
            .putString("local_user_id_${profile.email}", userId)
            .putString("local_user_${userId}_username", profile.username)
            .putString("local_user_${userId}_ref", profile.referralCode)
            .putString("local_user_${userId}_referredBy", referredBy)
            .putInt("local_user_${userId}_points", profile.points)
            .putInt("local_user_${userId}_streak", profile.streakDays)
            .putLong("local_user_${userId}_lastCheckIn", profile.lastCheckInTimestamp)
            .putString("local_user_${userId}_transactions", serializeTransactions(transList))
            .apply()
    }

    fun logout() {
        _isLoading.value = true
        removeListeners()
        
        if (FirebaseConfig.isFirebaseActive && FirebaseConfig.auth != null) {
            FirebaseConfig.auth!!.signOut()
        }

        prefs.edit()
            .remove("current_user_id")
            .remove("current_user_email")
            .apply()

        _currentUser.value = null
        _wallet.value = WalletState()
        setupDefaultTasks()
        _redeemRequests.value = emptyList()
        _notifications.value = emptyList()
        _isUserLoggedIn.value = false
        _isLoading.value = false
        _statusMessage.value = "Logged Out Successfully."
    }

    // CORE ACTIONS

    // 1. Daily Check-in
    fun claimDailyCheckIn() {
        val user = _currentUser.value ?: return
        val now = System.currentTimeMillis()
        val lastCheckIn = user.lastCheckInTimestamp
        
        // Cooldown check (24 hours in MS)
        val cooldown = 24 * 60 * 60 * 1000L
        if (now - lastCheckIn < cooldown && lastCheckIn != 0L) {
            val hoursLeft = ((cooldown - (now - lastCheckIn)) / (1000 * 60 * 60)).toInt()
            val minsLeft = (((cooldown - (now - lastCheckIn)) / (1000 * 60)) % 60).toInt()
            _statusMessage.value = "Already claimed today! Please try again in $hoursLeft h $minsLeft m."
            return
        }

        // Calculate streak
        val isConsecutive = (now - lastCheckIn) < (48 * 60 * 60 * 1000L) || lastCheckIn == 0L
        val newStreak = if (isConsecutive) user.streakDays + 1 else 1
        
        // Reward: base (100) + streak bonus (streak * 10)
        val rewardAmount = 100 + (newStreak * 10)
        val newPoints = user.points + rewardAmount

        val updatedProfile = user.copy(
            points = newPoints,
            streakDays = newStreak,
            lastCheckInTimestamp = now
        )

        val trans = Transaction(
            id = UUID.randomUUID().toString(),
            title = "Daily Check-in (Day $newStreak)",
            points = rewardAmount,
            type = "Earned"
        )

        val notif = NotificationItem(
            id = UUID.randomUUID().toString(),
            title = "Daily Check-In Claimed!",
            message = "You earned $rewardAmount points! Streak: $newStreak days.",
            type = "task_completed"
        )

        _isLoading.value = true
        if (FirebaseConfig.isFirebaseActive) {
            viewModelScope.launch {
                try {
                    val userRef = FirebaseConfig.getUsersReference()?.child(user.id)
                    userRef?.child("points")?.setValue(newPoints)
                    userRef?.child("streakDays")?.setValue(newStreak)
                    userRef?.child("lastCheckInTimestamp")?.setValue(now)

                    // Write transaction
                    FirebaseConfig.getWalletsReference()?.child(user.id)?.child("transactions")?.child(trans.id)?.setValue(trans)
                    // Write notification
                    FirebaseConfig.getNotificationsReference()?.child(user.id)?.child(notif.id)?.setValue(notif)
                    
                    _statusMessage.value = "Claimed successfully! +$rewardAmount Points."
                } catch (e: Exception) {
                    Log.e(TAG, "Daily check-in firebase error, falling back locally", e)
                    applyLocalDailyCheckIn(user.id, updatedProfile, trans, notif, rewardAmount)
                } finally {
                    _isLoading.value = false
                }
            }
        } else {
            applyLocalDailyCheckIn(user.id, updatedProfile, trans, notif, rewardAmount)
            _isLoading.value = false
        }
    }

    private fun applyLocalDailyCheckIn(userId: String, profile: UserProfile, trans: Transaction, notif: NotificationItem, amount: Int) {
        _currentUser.value = profile
        
        // Update local prefs
        prefs.edit()
            .putInt("local_user_${userId}_points", profile.points)
            .putInt("local_user_${userId}_streak", profile.streakDays)
            .putLong("local_user_${userId}_lastCheckIn", profile.lastCheckInTimestamp)
            .apply()

        // Update local wallet
        val updatedTransactions = listOf(trans) + _wallet.value.transactions
        _wallet.value = _wallet.value.copy(
            totalPoints = profile.points,
            earnedToday = _wallet.value.earnedToday + amount,
            transactions = updatedTransactions
        )
        prefs.edit().putString("local_user_${userId}_transactions", serializeTransactions(updatedTransactions)).apply()

        // Update local notifications
        val updatedNotifs = listOf(notif) + _notifications.value
        _notifications.value = updatedNotifs
        saveNotifications(userId, updatedNotifs)

        _statusMessage.value = "Claimed successfully! +$amount Points. (Offline Mode)"
    }

    // 2. Complete a Task
    fun completeTask(taskId: String, taskTitle: String, points: Int) {
        val user = _currentUser.value ?: return
        
        // Check if already completed
        val task = _tasks.value.find { it.id == taskId }
        if (task?.status == "Completed") {
            _statusMessage.value = "This task has already been completed!"
            return
        }

        val newPoints = user.points + points
        val compTime = System.currentTimeMillis()

        val trans = Transaction(
            id = UUID.randomUUID().toString(),
            title = "Completed $taskTitle",
            points = points,
            type = "Earned",
            timestamp = compTime
        )

        val notif = NotificationItem(
            id = UUID.randomUUID().toString(),
            title = "Task Completed!",
            message = "You earned $points points for completing the $taskTitle.",
            type = "task_completed",
            timestamp = compTime
        )

        _isLoading.value = true
        if (FirebaseConfig.isFirebaseActive) {
            viewModelScope.launch {
                try {
                    // Update points in Firebase profile
                    FirebaseConfig.getUsersReference()?.child(user.id)?.child("points")?.setValue(newPoints)
                    
                    // Add task completion history node
                    FirebaseConfig.getTaskHistoryReference()?.child(user.id)?.child(taskId)?.setValue(compTime)

                    // Write wallet transaction
                    FirebaseConfig.getWalletsReference()?.child(user.id)?.child("transactions")?.child(trans.id)?.setValue(trans)

                    // Write notification
                    FirebaseConfig.getNotificationsReference()?.child(user.id)?.child(notif.id)?.setValue(notif)

                    _statusMessage.value = "Task Completed! +$points Points."
                } catch (e: Exception) {
                    Log.e(TAG, "Task completion Firebase error, saving locally", e)
                    applyLocalTaskCompletion(user.id, taskId, newPoints, compTime, trans, notif, points)
                } finally {
                    _isLoading.value = false
                }
            }
        } else {
            applyLocalTaskCompletion(user.id, taskId, newPoints, compTime, trans, notif, points)
            _isLoading.value = false
        }
    }

    private fun applyLocalTaskCompletion(
        userId: String,
        taskId: String,
        newPoints: Int,
        compTime: Long,
        trans: Transaction,
        notif: NotificationItem,
        pointsAwarded: Int
    ) {
        // Update local tasks state
        _tasks.value = _tasks.value.map {
            if (it.id == taskId) it.copy(status = "Completed", completionTime = compTime) else it
        }

        // Save completed tasks in prefs
        val savedCompletedStr = prefs.getString("local_user_${userId}_completed_tasks", "") ?: ""
        val completedIds = savedCompletedStr.split(",").filter { it.isNotEmpty() }.toMutableList()
        if (taskId !in completedIds) {
            completedIds.add(taskId)
        }
        prefs.edit()
            .putString("local_user_${userId}_completed_tasks", completedIds.joinToString(","))
            .putInt("local_user_${userId}_points", newPoints)
            .apply()

        // Update profile state
        _currentUser.value = _currentUser.value?.copy(points = newPoints)

        // Update wallet
        val updatedTransactions = listOf(trans) + _wallet.value.transactions
        _wallet.value = _wallet.value.copy(
            totalPoints = newPoints,
            earnedToday = _wallet.value.earnedToday + pointsAwarded,
            transactions = updatedTransactions
        )
        prefs.edit().putString("local_user_${userId}_transactions", serializeTransactions(updatedTransactions)).apply()

        // Update notifications
        val updatedNotifs = listOf(notif) + _notifications.value
        _notifications.value = updatedNotifs
        saveNotifications(userId, updatedNotifs)

        _statusMessage.value = "Task Completed! +$pointsAwarded Points. (Offline)"
    }

    // 3. Lucky Spin
    fun spinWheelAndReward(pointsWon: Int) {
        val user = _currentUser.value ?: return
        val newPoints = user.points + pointsWon
        val timeNow = System.currentTimeMillis()

        val trans = Transaction(
            id = UUID.randomUUID().toString(),
            title = "Lucky Spin Reward",
            points = pointsWon,
            type = "Earned",
            timestamp = timeNow
        )

        val notif = NotificationItem(
            id = UUID.randomUUID().toString(),
            title = "Lucky Spin Winner!",
            message = "Lucky Spin awarded you $pointsWon points! Keep spinning!",
            type = "task_completed",
            timestamp = timeNow
        )

        _isLoading.value = true
        if (FirebaseConfig.isFirebaseActive) {
            viewModelScope.launch {
                try {
                    FirebaseConfig.getUsersReference()?.child(user.id)?.child("points")?.setValue(newPoints)
                    FirebaseConfig.getWalletsReference()?.child(user.id)?.child("transactions")?.child(trans.id)?.setValue(trans)
                    FirebaseConfig.getNotificationsReference()?.child(user.id)?.child(notif.id)?.setValue(notif)
                    _statusMessage.value = "Spin Won! +$pointsWon Points."
                } catch (e: Exception) {
                    Log.e(TAG, "Lucky Spin Firebase error, executing locally", e)
                    applyLocalLuckySpin(user.id, newPoints, trans, notif, pointsWon)
                } finally {
                    _isLoading.value = false
                }
            }
        } else {
            applyLocalLuckySpin(user.id, newPoints, trans, notif, pointsWon)
            _isLoading.value = false
        }
    }

    private fun applyLocalLuckySpin(userId: String, newPoints: Int, trans: Transaction, notif: NotificationItem, pointsWon: Int) {
        _currentUser.value = _currentUser.value?.copy(points = newPoints)
        prefs.edit().putInt("local_user_${userId}_points", newPoints).apply()

        val updatedTransactions = listOf(trans) + _wallet.value.transactions
        _wallet.value = _wallet.value.copy(
            totalPoints = newPoints,
            earnedToday = _wallet.value.earnedToday + pointsWon,
            transactions = updatedTransactions
        )
        prefs.edit().putString("local_user_${userId}_transactions", serializeTransactions(updatedTransactions)).apply()

        val updatedNotifs = listOf(notif) + _notifications.value
        _notifications.value = updatedNotifs
        saveNotifications(userId, updatedNotifs)

        _statusMessage.value = "Spin Won! +$pointsWon Points. (Offline)"
    }

    // 4. Submit Redeem Request
    fun redeemReward(rewardName: String, gmailId: String, costPoints: Int) {
        val user = _currentUser.value ?: return
        if (gmailId.isEmpty() || !gmailId.contains("@")) {
            _statusMessage.value = "Please enter a valid Gmail address."
            return
        }

        if (user.points < costPoints) {
            _statusMessage.value = "Insufficient points! You need $costPoints points."
            return
        }

        val newPoints = user.points - costPoints
        val timeNow = System.currentTimeMillis()
        val reqId = UUID.randomUUID().toString()

        val request = RedeemRequest(
            id = reqId,
            userEmail = user.email,
            gmail = gmailId,
            rewardName = rewardName,
            points = costPoints,
            status = "Pending",
            timestamp = timeNow
        )

        val trans = Transaction(
            id = UUID.randomUUID().toString(),
            title = "Redeemed $rewardName",
            points = costPoints,
            type = "Redeemed",
            timestamp = timeNow
        )

        val notif = NotificationItem(
            id = UUID.randomUUID().toString(),
            title = "Redeem Request Submitted!",
            message = "$rewardName request is submitted and under review for: $gmailId",
            type = "reward_approved",
            timestamp = timeNow
        )

        _isLoading.value = true
        if (FirebaseConfig.isFirebaseActive) {
            viewModelScope.launch {
                try {
                    // Update user points
                    FirebaseConfig.getUsersReference()?.child(user.id)?.child("points")?.setValue(newPoints)
                    // Push request
                    FirebaseConfig.getRedeemRequestsReference()?.child(reqId)?.setValue(request)
                    // Push wallet trans
                    FirebaseConfig.getWalletsReference()?.child(user.id)?.child("transactions")?.child(trans.id)?.setValue(trans)
                    // Push notification
                    FirebaseConfig.getNotificationsReference()?.child(user.id)?.child(notif.id)?.setValue(notif)

                    _statusMessage.value = "Request Submitted Successfully! (₹${rewardName.filter { it.isDigit() }} Card)"
                } catch (e: Exception) {
                    Log.e(TAG, "Redeem request Firebase error, writing locally", e)
                    applyLocalRedemption(user.id, newPoints, request, trans, notif, costPoints)
                } finally {
                    _isLoading.value = false
                }
            }
        } else {
            applyLocalRedemption(user.id, newPoints, request, trans, notif, costPoints)
            _isLoading.value = false
        }
    }

    private fun applyLocalRedemption(
        userId: String,
        newPoints: Int,
        request: RedeemRequest,
        trans: Transaction,
        notif: NotificationItem,
        costPoints: Int
    ) {
        _currentUser.value = _currentUser.value?.copy(points = newPoints)
        prefs.edit().putInt("local_user_${userId}_points", newPoints).apply()

        // Save Redeem request locally
        val updatedRedeems = listOf(request) + _redeemRequests.value
        _redeemRequests.value = updatedRedeems
        prefs.edit().putString("local_user_${userId}_redeems", serializeRedeems(updatedRedeems)).apply()

        // Save wallet transaction
        val updatedTransactions = listOf(trans) + _wallet.value.transactions
        _wallet.value = _wallet.value.copy(
            totalPoints = newPoints,
            redeemedToday = _wallet.value.redeemedToday + costPoints,
            transactions = updatedTransactions
        )
        prefs.edit().putString("local_user_${userId}_transactions", serializeTransactions(updatedTransactions)).apply()

        // Update notifications
        val updatedNotifs = listOf(notif) + _notifications.value
        _notifications.value = updatedNotifs
        saveNotifications(userId, updatedNotifs)

        _statusMessage.value = "Request Submitted Successfully! (Offline Mode)"
    }

    // 5. Simulate Status Update for Redemptions (Admin test tool)
    fun simulateStatusUpdate(reqId: String, newStatus: String) {
        val user = _currentUser.value ?: return
        val now = System.currentTimeMillis()
        
        val foundReq = _redeemRequests.value.find { it.id == reqId } ?: return
        val originalStatus = foundReq.status
        if (originalStatus == newStatus) return

        val notifType = when (newStatus) {
            "Approved" -> "reward_approved"
            "Rejected" -> "reward_rejected"
            "Sent" -> "reward_approved"
            else -> "new_task"
        }

        val notifMessage = when (newStatus) {
            "Approved" -> "Your request for ${foundReq.rewardName} is Approved!"
            "Rejected" -> "Your request for ${foundReq.rewardName} has been Rejected. Points are returned."
            "Sent" -> "Your ${foundReq.rewardName} Gift Card code has been sent to ${foundReq.gmail}!"
            else -> "Your request for ${foundReq.rewardName} status updated to $newStatus."
        }

        val notif = NotificationItem(
            id = UUID.randomUUID().toString(),
            title = "Reward Status: $newStatus",
            message = notifMessage,
            type = notifType,
            timestamp = now
        )

        _isLoading.value = true

        // If rejected, refund points
        val shouldRefund = newStatus == "Rejected" && originalStatus != "Rejected"
        val refundPoints = if (shouldRefund) foundReq.points else 0
        val finalPoints = user.points + refundPoints

        val refundTrans = if (shouldRefund) {
            Transaction(
                id = UUID.randomUUID().toString(),
                title = "Refund: Rejected ${foundReq.rewardName}",
                points = foundReq.points,
                type = "Earned",
                timestamp = now
            )
        } else null

        if (FirebaseConfig.isFirebaseActive) {
            viewModelScope.launch {
                try {
                    // Update redeem status in Firebase
                    FirebaseConfig.getRedeemRequestsReference()?.child(reqId)?.child("status")?.setValue(newStatus)
                    
                    if (shouldRefund) {
                        FirebaseConfig.getUsersReference()?.child(user.id)?.child("points")?.setValue(finalPoints)
                        refundTrans?.let {
                            FirebaseConfig.getWalletsReference()?.child(user.id)?.child("transactions")?.child(it.id)?.setValue(it)
                        }
                    }

                    // Push status update notification
                    FirebaseConfig.getNotificationsReference()?.child(user.id)?.child(notif.id)?.setValue(notif)
                    _statusMessage.value = "Status updated to $newStatus!"
                } catch (e: Exception) {
                    Log.e(TAG, "Status simulation error", e)
                    applyLocalStatusUpdate(user.id, reqId, newStatus, shouldRefund, refundPoints, finalPoints, refundTrans, notif)
                } finally {
                    _isLoading.value = false
                }
            }
        } else {
            applyLocalStatusUpdate(user.id, reqId, newStatus, shouldRefund, refundPoints, finalPoints, refundTrans, notif)
            _isLoading.value = false
        }
    }

    private fun applyLocalStatusUpdate(
        userId: String,
        reqId: String,
        newStatus: String,
        shouldRefund: Boolean,
        refundPoints: Int,
        finalPoints: Int,
        refundTrans: Transaction?,
        notif: NotificationItem
    ) {
        // Update redeem requests list state
        val updatedRedeems = _redeemRequests.value.map {
            if (it.id == reqId) it.copy(status = newStatus) else it
        }
        _redeemRequests.value = updatedRedeems
        prefs.edit().putString("local_user_${userId}_redeems", serializeRedeems(updatedRedeems)).apply()

        if (shouldRefund) {
            // Update points state
            _currentUser.value = _currentUser.value?.copy(points = finalPoints)
            prefs.edit().putInt("local_user_${userId}_points", finalPoints).apply()

            // Update transactions list
            val updatedTrans = if (refundTrans != null) listOf(refundTrans) + _wallet.value.transactions else _wallet.value.transactions
            _wallet.value = _wallet.value.copy(
                totalPoints = finalPoints,
                transactions = updatedTrans
            )
            prefs.edit().putString("local_user_${userId}_transactions", serializeTransactions(updatedTrans)).apply()
        }

        // Add notifications
        val updatedNotifs = listOf(notif) + _notifications.value
        _notifications.value = updatedNotifs
        saveNotifications(userId, updatedNotifs)

        _statusMessage.value = "Status updated to $newStatus! (Offline)"
    }

    // 6. Referral Code Entry
    fun enterReferralCode(code: String) {
        val user = _currentUser.value ?: return
        if (code.isEmpty()) {
            _statusMessage.value = "Please enter a valid referral code."
            return
        }

        if (code.uppercase() == user.referralCode.uppercase()) {
            _statusMessage.value = "You cannot use your own referral code!"
            return
        }

        if (user.referredBy.isNotEmpty()) {
            _statusMessage.value = "You have already used a referral code!"
            return
        }

        // Search for code - in online we would query firebase, in offline we award points.
        // Let's create an offline reward of 500 points for entering referral code.
        val rewardAmount = 500
        val newPoints = user.points + rewardAmount
        val now = System.currentTimeMillis()

        val trans = Transaction(
            id = UUID.randomUUID().toString(),
            title = "Entered Referral Code: $code",
            points = rewardAmount,
            type = "Earned",
            timestamp = now
        )

        val notif = NotificationItem(
            id = UUID.randomUUID().toString(),
            title = "Referral Code Applied!",
            message = "Referral code $code successfully applied. You earned $rewardAmount points!",
            type = "task_completed",
            timestamp = now
        )

        _isLoading.value = true
        if (FirebaseConfig.isFirebaseActive) {
            viewModelScope.launch {
                try {
                    FirebaseConfig.getUsersReference()?.child(user.id)?.child("referredBy")?.setValue(code)
                    FirebaseConfig.getUsersReference()?.child(user.id)?.child("points")?.setValue(newPoints)
                    FirebaseConfig.getWalletsReference()?.child(user.id)?.child("transactions")?.child(trans.id)?.setValue(trans)
                    FirebaseConfig.getNotificationsReference()?.child(user.id)?.child(notif.id)?.setValue(notif)
                    _statusMessage.value = "Referral Code Applied! +$rewardAmount Points."
                } catch (e: Exception) {
                    Log.e(TAG, "Referral Firebase error", e)
                    applyLocalReferral(user.id, code, newPoints, trans, notif, rewardAmount)
                } finally {
                    _isLoading.value = false
                }
            }
        } else {
            applyLocalReferral(user.id, code, newPoints, trans, notif, rewardAmount)
            _isLoading.value = false
        }
    }

    private fun applyLocalReferral(userId: String, code: String, newPoints: Int, trans: Transaction, notif: NotificationItem, amount: Int) {
        _currentUser.value = _currentUser.value?.copy(referredBy = code, points = newPoints)
        
        prefs.edit()
            .putString("local_user_${userId}_referredBy", code)
            .putInt("local_user_${userId}_points", newPoints)
            .apply()

        val updatedTrans = listOf(trans) + _wallet.value.transactions
        _wallet.value = _wallet.value.copy(
            totalPoints = newPoints,
            earnedToday = _wallet.value.earnedToday + amount,
            transactions = updatedTrans
        )
        prefs.edit().putString("local_user_${userId}_transactions", serializeTransactions(updatedTrans)).apply()

        val updatedNotifs = listOf(notif) + _notifications.value
        _notifications.value = updatedNotifs
        saveNotifications(userId, updatedNotifs)

        _statusMessage.value = "Referral Code Applied! +$amount Points. (Offline)"
    }

    // CLEAR STATUS MESSAGE
    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    // SERIALIZATION HELPERS
    private fun serializeTransactions(list: List<Transaction>): String {
        return list.joinToString(";") { "${it.id},${it.title},${it.points},${it.type},${it.timestamp}" }
    }

    private fun parseTransactions(str: String): List<Transaction> {
        if (str.isEmpty()) return emptyList()
        return try {
            str.split(";").map {
                val parts = it.split(",")
                Transaction(parts[0], parts[1], parts[2].toInt(), parts[3], parts[4].toLong())
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeRedeems(list: List<RedeemRequest>): String {
        return list.joinToString(";") { "${it.id},${it.userEmail},${it.gmail},${it.rewardName},${it.points},${it.status},${it.timestamp}" }
    }

    private fun parseRedeems(str: String): List<RedeemRequest> {
        if (str.isEmpty()) return emptyList()
        return try {
            str.split(";").map {
                val parts = it.split(",")
                RedeemRequest(parts[0], parts[1], parts[2], parts[3], parts[4].toInt(), parts[5], parts[6].toLong())
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeNotifications(list: List<NotificationItem>): String {
        return list.joinToString(";") { "${it.id},${it.title},${it.message},${it.type},${it.timestamp}" }
    }

    private fun parseNotifications(str: String): List<NotificationItem> {
        if (str.isEmpty()) return emptyList()
        return try {
            str.split(";").map {
                val parts = it.split(",")
                NotificationItem(parts[0], parts[1], parts[2], parts[3], parts[4].toLong())
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveNotifications(userId: String, list: List<NotificationItem>) {
        prefs.edit().putString("local_user_${userId}_notifications", serializeNotifications(list)).apply()
    }

    private fun getStartOfDay(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
