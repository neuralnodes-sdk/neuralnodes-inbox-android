package com.neuralnodes.inbox.example

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.messaging.FirebaseMessaging
import com.neuralnodes.inbox.NeuralNodesInbox
import com.neuralnodes.inbox.services.PushNotificationService
import kotlinx.coroutines.launch

/**
 * Example app demonstrating NeuralNodes Inbox SDK usage
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var inbox: NeuralNodesInbox
    private lateinit var statusText: TextView
    private lateinit var openInboxButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        statusText = findViewById(R.id.statusText)
        openInboxButton = findViewById(R.id.openInboxButton)
        
        // Replace with your actual API key
        val apiKey = "your-client-api-key-here"
        inbox = NeuralNodesInbox(apiKey)
        
        // Initialize push notification service
        PushNotificationService.initialize(apiKey)
        
        // Initialize SDK
        initializeSDK()
        
        // Setup UI
        openInboxButton.setOnClickListener {
            inbox.showInbox(this)
        }
        
        // Request push notification permission and get token
        requestPushNotifications()
    }
    
    private fun initializeSDK() {
        statusText.text = "Initializing SDK..."
        openInboxButton.isEnabled = false
        
        lifecycleScope.launch {
            inbox.initialize().onSuccess { config ->
                runOnUiThread {
                    statusText.text = """
                        ✅ SDK Ready
                        
                        Features:
                        • Push Notifications: ${if (config.features.pushNotifications) "✅" else "❌"}
                        • File Upload: ${if (config.features.fileUpload) "✅" else "❌"}
                        • Real-time: ${if (config.ablyKey != null) "✅" else "❌"}
                    """.trimIndent()
                    openInboxButton.isEnabled = true
                }
            }.onFailure { error ->
                runOnUiThread {
                    statusText.text = "❌ SDK initialization failed: ${error.message}"
                }
            }
        }
    }
    
    private fun requestPushNotifications() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                println("🔑 FCM Token: $token")
                
                // Register device with backend
                lifecycleScope.launch {
                    try {
                        inbox.registerForPushNotifications(token)
                        println("✅ Device registered for push notifications")
                    } catch (e: Exception) {
                        println("❌ Failed to register device: ${e.message}")
                    }
                }
            } else {
                println("❌ Failed to get FCM token: ${task.exception}")
            }
        }
    }
}
