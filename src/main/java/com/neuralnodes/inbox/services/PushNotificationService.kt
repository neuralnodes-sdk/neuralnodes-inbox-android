package com.neuralnodes.inbox.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.neuralnodes.inbox.R
import com.neuralnodes.inbox.network.APIClient
import com.neuralnodes.inbox.ui.InboxActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Firebase Cloud Messaging service for push notifications
 */
class PushNotificationService : FirebaseMessagingService() {
    
    companion object {
        private const val CHANNEL_ID = "neuralnodes_inbox"
        private const val CHANNEL_NAME = "NeuralNodes Inbox"
        private var apiClient: APIClient? = null
        
        fun initialize(apiKey: String) {
            apiClient = APIClient(apiKey)
        }
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        println("🔑 New FCM token: $token")
        
        // Register device with backend
        apiClient?.let { client ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val deviceInfo = mapOf(
                        "model" to Build.MODEL,
                        "manufacturer" to Build.MANUFACTURER,
                        "sdk_version" to Build.VERSION.SDK_INT.toString()
                    )
                    client.registerDevice(token, "android", deviceInfo)
                    println("✅ Device registered with backend")
                } catch (e: Exception) {
                    println("❌ Failed to register device: ${e.message}")
                }
            }
        }
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        println("📩 Push notification received")
        
        // Extract data
        val conversationId = remoteMessage.data["conversation_id"]
        val title = remoteMessage.data["title"] ?: "New Message"
        val body = remoteMessage.data["body"] ?: "You have a new message"
        
        // Show notification
        showNotification(title, body, conversationId)
    }
    
    private fun showNotification(title: String, body: String, conversationId: String?) {
        createNotificationChannel()
        
        val intent = Intent(this, InboxActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            conversationId?.let { putExtra("conversation_id", it) }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new messages in NeuralNodes Inbox"
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
