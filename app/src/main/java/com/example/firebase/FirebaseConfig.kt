package com.example.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.tasks.await

object FirebaseConfig {
    private const val TAG = "FirebaseConfig"
    
    private var isInitialized = false
    var isFirebaseActive = false
        private set

    var auth: FirebaseAuth? = null
        private set

    var database: FirebaseDatabase? = null
        private set

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            Log.d(TAG, "Initializing Firebase manually with custom config options...")
            val options = FirebaseOptions.Builder()
                .setApiKey("AIzaSyAYzZJZcyhbqOOEyo3W_IzTJdGu20BLpkE")
                .setApplicationId("1:884012607306:web:705dfa358760c6511003b3")
                .setProjectId("coffee-spark-ai-barista-40db4")
                .setStorageBucket("coffee-spark-ai-barista-40db4.firebasestorage.app")
                .setDatabaseUrl("https://coffee-spark-ai-barista-40db4-default-rtdb.firebaseio.com")
                .setGcmSenderId("884012607306")
                .build()

            // Initialize app and capture references
            val app = try {
                FirebaseApp.initializeApp(context, options, "FunEarningApp")
            } catch (e: Exception) {
                // If named initialization fails or is already initialized, fallback
                try {
                    FirebaseApp.initializeApp(context, options)
                } catch (ex: Exception) {
                    FirebaseApp.getInstance()
                }
            }

            auth = FirebaseAuth.getInstance(app)
            database = FirebaseDatabase.getInstance(app)
            isFirebaseActive = true
            isInitialized = true
            Log.d(TAG, "Firebase initialized successfully. Connected to: ${options.projectId}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase SDK manually: ${e.localizedMessage}. Operating in robust offline/local simulation fallback.", e)
            isFirebaseActive = false
            isInitialized = true
        }
    }

    // Helper functions for RTDB references
    fun getUsersReference(): DatabaseReference? = database?.getReference("users")
    fun getWalletsReference(): DatabaseReference? = database?.getReference("wallets")
    fun getTasksReference(): DatabaseReference? = database?.getReference("tasks")
    fun getTaskHistoryReference(): DatabaseReference? = database?.getReference("taskHistory")
    fun getRedeemRequestsReference(): DatabaseReference? = database?.getReference("redeemRequests")
    fun getRewardHistoryReference(): DatabaseReference? = database?.getReference("rewardHistory")
    fun getNotificationsReference(): DatabaseReference? = database?.getReference("notifications")
    fun getSettingsReference(): DatabaseReference? = database?.getReference("settings")
}
