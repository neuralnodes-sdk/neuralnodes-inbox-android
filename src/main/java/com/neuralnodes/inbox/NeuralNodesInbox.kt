package com.neuralnodes.inbox

import android.content.Context
import android.content.Intent
import com.neuralnodes.inbox.models.SDKConfig
import com.neuralnodes.inbox.network.APIClient
import com.neuralnodes.inbox.network.RealtimeClient
import com.neuralnodes.inbox.network.PusherClient
import com.neuralnodes.inbox.services.PushNotificationService
import com.neuralnodes.inbox.ui.InboxActivity
import com.neuralnodes.inbox.ui.LiveChatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Main SDK class for NeuralNodes
 * Supports both Unified Inbox and Live Chat
 */
class NeuralNodesInbox private constructor(private val apiKey: String) {
    
    private val apiClient = APIClient(apiKey)
    private val realtimeClient = RealtimeClient()
    private var pusherClient: PusherClient? = null
    private var config: SDKConfig? = null
    private var clientId: String? = null
    
    companion object {
        @Volatile
        private var instance: NeuralNodesInbox? = null
        
        /**
         * Get or create SDK instance
         */
        fun getInstance(apiKey: String): NeuralNodesInbox {
            return instance ?: synchronized(this) {
                instance ?: NeuralNodesInbox(apiKey).also { instance = it }
            }
        }
        
        /**
         * Get existing SDK instance (throws if not initialized)
         */
        fun getInstance(): NeuralNodesInbox {
            return instance ?: throw IllegalStateException("SDK not initialized. Call getInstance(apiKey) first.")
        }
    }
    
    init {
        // Initialize push notification service
        PushNotificationService.initialize(apiKey)
    }
    
    /**
     * Initialize the SDK and load configuration
     */
    suspend fun initialize(): Result<SDKConfig> {
        return try {
            val sdkConfig = apiClient.getConfig()
            config = sdkConfig
            
            // Get client info for Pusher (with error handling)
            try {
                val clientInfo = apiClient.getClientInfo()
                clientId = clientInfo.id
            } catch (e: Exception) {
                println("⚠️ Failed to get client info: ${e.message}")
                // Continue without client ID - Live Chat won't work but Inbox will
            }
            
            // Connect to Ably for Inbox (if enabled)
            sdkConfig.ablyKey?.let { key ->
                realtimeClient.connect(key)
            }
            
            // Initialize Pusher for Live Chat (if enabled and we have client ID)
            if (sdkConfig.pusherKey != null && sdkConfig.pusherCluster != null && clientId != null) {
                try {
                    pusherClient = PusherClient(apiKey, sdkConfig.apiBaseURL).apply {
                        initialize(sdkConfig.pusherKey, sdkConfig.pusherCluster, clientId!!)
                    }
                    println("✅ Pusher initialized successfully")
                } catch (e: Exception) {
                    println("⚠️ Failed to initialize Pusher: ${e.message}")
                    // Continue without Pusher - Live Chat will work but without real-time updates
                }
            } else {
                println("⚠️ Pusher not configured: pusherKey=${sdkConfig.pusherKey != null}, pusherCluster=${sdkConfig.pusherCluster != null}, clientId=$clientId")
            }
            
            println("✅ SDK initialized successfully")
            println("📋 Features: ${sdkConfig.features}")
            println("🎨 UI Customization: ${sdkConfig.uiCustomization}")
            println("⚙️ Limits: ${sdkConfig.limits}")
            
            Result.success(sdkConfig)
        } catch (e: Exception) {
            println("❌ SDK initialization failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Show the Unified Inbox UI
     */
    fun showInbox(context: Context) {
        if (config == null) {
            throw IllegalStateException("SDK not initialized. Call initialize() first.")
        }
        
        val intent = Intent(context, InboxActivity::class.java).apply {
            putExtra("API_KEY", apiKey)
        }
        context.startActivity(intent)
    }
    
    /**
     * Show the Live Chat UI
     */
    fun showLiveChat(context: Context) {
        if (config == null) {
            throw IllegalStateException("SDK not initialized. Call initialize() first.")
        }
        
        if (pusherClient == null) {
            throw IllegalStateException("Live Chat not enabled. Check Pusher configuration.")
        }
        
        val intent = Intent(context, LiveChatActivity::class.java).apply {
            putExtra("API_KEY", apiKey)
            putExtra("CLIENT_ID", clientId)
        }
        context.startActivity(intent)
    }
    
    /**
     * Register device for push notifications
     */
    suspend fun registerForPushNotifications(token: String) {
        try {
            val deviceInfo = mapOf(
                "model" to android.os.Build.MODEL,
                "manufacturer" to android.os.Build.MANUFACTURER,
                "sdk_version" to android.os.Build.VERSION.SDK_INT.toString()
            )
            apiClient.registerDevice(token, "android", deviceInfo)
            println("✅ Device registered for push notifications")
        } catch (e: Exception) {
            println("❌ Failed to register device: ${e.message}")
        }
    }
    
    /**
     * Handle incoming push notification
     * @return Conversation ID or Escalation ID if available
     */
    fun handlePushNotification(data: Map<String, String>): Pair<String?, String?> {
        val conversationId = data["conversation_id"]
        val escalationId = data["escalation_id"]
        return Pair(conversationId, escalationId)
    }
    
    /**
     * Get API client for custom implementations
     */
    fun getAPIClient(): APIClient = apiClient
    
    /**
     * Get Ably real-time client for custom implementations
     */
    fun getRealtimeClient(): RealtimeClient = realtimeClient
    
    /**
     * Get current SDK configuration
     */
    fun getConfig(): SDKConfig? = config
    
    /**
     * Get Pusher client for custom implementations
     */
    fun getPusherClient(): PusherClient? = pusherClient
    
    /**
     * Disconnect and cleanup all resources
     * CRITICAL: Call this to prevent memory leaks
     */
    fun disconnect() {
        realtimeClient.disconnect()
        pusherClient?.disconnect()
        pusherClient = null
        config = null
        clientId = null
        println("✅ SDK disconnected and cleaned up")
    }
}
