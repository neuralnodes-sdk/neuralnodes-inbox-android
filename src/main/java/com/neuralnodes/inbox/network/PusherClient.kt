package com.neuralnodes.inbox.network

import com.google.gson.Gson
import com.neuralnodes.inbox.models.*
import com.pusher.client.Pusher
import com.pusher.client.PusherOptions
import com.pusher.client.channel.PrivateChannel
import com.pusher.client.connection.ConnectionEventListener
import com.pusher.client.connection.ConnectionState
import com.pusher.client.connection.ConnectionStateChange
import com.pusher.client.util.HttpAuthorizer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Production-ready Pusher client with proper memory management
 * Fixes memory leaks and connection issues
 */
class PusherClient(
    private val apiKey: String,
    private val apiBaseUrl: String
) {
    private var pusher: Pusher? = null
    private val subscribedChannels = ConcurrentHashMap<String, PrivateChannel>()
    private val gson = Gson()
    private var clientId: String? = null
    
    @Volatile
    private var isConnected = false
    
    /**
     * Initialize Pusher connection with proper configuration
     */
    fun initialize(pusherKey: String, pusherCluster: String, clientId: String) {
        // Disconnect existing connection if any
        disconnect()
        
        this.clientId = clientId
        
        println("🔧 Initializing Pusher with:")
        println("   Key: ${pusherKey.take(10)}...")
        println("   Cluster: $pusherCluster")
        println("   Client ID: $clientId")
        println("   Auth URL: $apiBaseUrl/pusher/auth")
        
        val authorizer = HttpAuthorizer("$apiBaseUrl/pusher/auth").apply {
            setHeaders(mapOf(
                "X-API-Key" to apiKey,
                "Content-Type" to "application/x-www-form-urlencoded"
            ))
        }
        
        val options = PusherOptions().apply {
            setCluster(pusherCluster)
            setAuthorizer(authorizer)
            isUseTLS = true
        }
        
        pusher = Pusher(pusherKey, options).apply {
            connection.bind(ConnectionState.ALL, object : ConnectionEventListener {
                override fun onConnectionStateChange(change: ConnectionStateChange) {
                    isConnected = change.currentState == ConnectionState.CONNECTED
                    println("🔗 Pusher connection state: ${change.currentState}")
                    
                    if (change.currentState == ConnectionState.CONNECTED) {
                        println("✅ Pusher connected successfully")
                    } else if (change.currentState == ConnectionState.DISCONNECTED) {
                        println("❌ Pusher disconnected")
                    }
                }
                
                override fun onError(message: String?, code: String?, e: Exception?) {
                    println("❌ Pusher error: $message (code: $code)")
                    e?.printStackTrace()
                }
            })
            
            connect()
        }
    }
    
    /**
     * Subscribe to client dashboard updates
     */
    fun subscribeToClientDashboard(
        clientId: String,
        onAnalyticsUpdated: ((RealTimeAnalytics) -> Unit)? = null,
        onNewEscalation: ((Map<String, Any>) -> Unit)? = null
    ): Flow<String> = callbackFlow {
        val channelName = "private-client-$clientId"
        
        try {
            val channel = pusher?.subscribePrivate(channelName) ?: run {
                println("⚠️ Pusher not initialized")
                awaitClose {}
                return@callbackFlow
            }
            
            subscribedChannels[channelName] = channel
            
            // Analytics updates
            channel.bind("analytics-updated") { event ->
                try {
                    val json = gson.fromJson(event.data, Map::class.java)
                    val analytics = gson.fromJson(gson.toJson(json["analytics"]), RealTimeAnalytics::class.java)
                    onAnalyticsUpdated?.invoke(analytics)
                } catch (e: Exception) {
                    println("⚠️ Failed to parse analytics: ${e.message}")
                }
            }
            
            // New escalation
            channel.bind("new-escalation") { event ->
                try {
                    val json = gson.fromJson(event.data, Map::class.java) as Map<String, Any>
                    onNewEscalation?.invoke(json)
                    trySend("new-escalation")
                } catch (e: Exception) {
                    println("⚠️ Failed to parse escalation: ${e.message}")
                }
            }
            
            println("✅ Subscribed to client dashboard: $channelName")
            
            awaitClose {
                unsubscribeFromChannel(channelName)
            }
        } catch (e: Exception) {
            println("❌ Failed to subscribe to client dashboard: ${e.message}")
            awaitClose {}
        }
    }
    
    /**
     * Subscribe to escalation updates - Simple signature matching iOS SDK
     */
    fun subscribeToEscalation(
        escalationId: String,
        onMessage: (ChatMessage) -> Unit,
        onTyping: (Boolean) -> Unit
    ) {
        val channelName = "private-escalation-$escalationId"
        
        try {
            println("📡 [PUSHER] Subscribing to channel: $channelName")
            val channel = pusher?.subscribePrivate(channelName) ?: run {
                println("⚠️ Pusher not initialized")
                return
            }
            
            subscribedChannels[channelName] = channel
            
            // Subscribe to new messages
            channel.bind("new-message") { event ->
                println("📨 [PUSHER] Received new-message event on $channelName")
                println("   Event data: ${event.data ?: "nil"}")
                
                try {
                    println("📦 [PUSHER] Parsing message JSON...")
                    val message = gson.fromJson(event.data, ChatMessage::class.java)
                    println("✅ [PUSHER] Message decoded successfully: ${message.messageText}")
                    println("🔔 [PUSHER] Calling onMessage callback...")
                    onMessage(message)
                } catch (e: Exception) {
                    println("❌ [PUSHER] Failed to decode message: ${e.message}")
                }
            }
            
            // Subscribe to typing indicators
            channel.bind("typing") { event ->
                try {
                    val json = gson.fromJson(event.data, Map::class.java) as Map<String, Any>
                    val isTyping = json["is_typing"] as? Boolean ?: false
                    onTyping(isTyping)
                } catch (e: Exception) {
                    println("⚠️ Failed to parse typing indicator: ${e.message}")
                }
            }
            
            println("✅ Subscribed to escalation: $channelName")
        } catch (e: Exception) {
            println("❌ Failed to subscribe to escalation: ${e.message}")
        }
    }
    
    /**
     * Subscribe to escalation list updates
     * Exact match to iOS SDK
     */
    fun subscribeToEscalationList(clientId: String, onUpdate: () -> Unit) {
        val channelName = "private-client-$clientId"
        
        try {
            println("📡 [PUSHER] Subscribing to escalation list channel: $channelName")
            val channel = pusher?.subscribePrivate(channelName) ?: run {
                println("⚠️ Pusher not initialized")
                return
            }
            
            subscribedChannels["escalation-list"] = channel
            
            channel.bind("new-escalation") { event ->
                println("🔔 [PUSHER] Received new-escalation event")
                onUpdate()
            }
            
            channel.bind("escalation-update") { event ->
                println("🔔 [PUSHER] Received escalation-update event")
                onUpdate()
            }
            
            println("✅ [PUSHER] Subscribed to escalation list updates: $channelName")
        } catch (e: Exception) {
            println("❌ Failed to subscribe to escalation list: ${e.message}")
        }
    }
    
    /**
     * Unsubscribe from escalation - Takes escalationId
     * Exact match to iOS SDK
     */
    fun unsubscribeFromEscalation(escalationId: String) {
        val channelName = "private-escalation-$escalationId"
        unsubscribeFromChannel(channelName)
    }
    
    /**
     * Unsubscribe from a channel with proper cleanup
     */
    private fun unsubscribeFromChannel(channelName: String) {
        subscribedChannels.remove(channelName)?.let { channel ->
            try {
                println("🔕 [PUSHER] Unsubscribing from channel: $channelName")
                // Unbind all events from the channel
                channel.unbind("analytics-updated", null)
                channel.unbind("new-escalation", null)
                channel.unbind("escalation-update", null)
                channel.unbind("new-message", null)
                channel.unbind("typing", null)
                channel.unbind("typing-indicator", null)
                channel.unbind("status-changed", null)
                pusher?.unsubscribe(channelName)
                println("✅ Unsubscribed from: $channelName")
            } catch (e: Exception) {
                println("⚠️ Error unsubscribing from $channelName: ${e.message}")
            }
        }
    }
    
    /**
     * Get connection state
     */
    fun getConnectionState(): String {
        return pusher?.connection?.state?.name ?: "DISCONNECTED"
    }
    
    /**
     * Check if connected
     */
    fun isConnected(): Boolean = isConnected
    
    /**
     * Disconnect and cleanup all resources
     * CRITICAL: Prevents memory leaks
     */
    fun disconnect() {
        try {
            // Unbind all channels
            subscribedChannels.keys.toList().forEach { channelName ->
                unsubscribeFromChannel(channelName)
            }
            subscribedChannels.clear()
            
            // Disconnect Pusher
            pusher?.disconnect()
            pusher = null
            
            isConnected = false
            clientId = null
            
            println("✅ Pusher disconnected and cleaned up")
        } catch (e: Exception) {
            println("⚠️ Error during Pusher cleanup: ${e.message}")
        }
    }
}
