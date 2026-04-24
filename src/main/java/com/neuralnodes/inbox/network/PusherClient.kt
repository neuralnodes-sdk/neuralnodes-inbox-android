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
        
        val authorizer = HttpAuthorizer("$apiBaseUrl/pusher/auth").apply {
            setHeaders(mapOf("X-API-Key" to apiKey))
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
                    println("Pusher connection state: ${change.currentState}")
                }
                
                override fun onError(message: String?, code: String?, e: Exception?) {
                    println("Pusher error: $message (code: $code)")
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
                unsubscribe(channelName)
            }
        } catch (e: Exception) {
            println("❌ Failed to subscribe to client dashboard: ${e.message}")
            awaitClose {}
        }
    }
    
    /**
     * Subscribe to escalation updates with proper cleanup
     */
    fun subscribeToEscalation(
        escalationId: String,
        onNewMessage: ((ChatMessage) -> Unit)? = null,
        onTypingIndicator: ((TypingIndicator) -> Unit)? = null,
        onStatusChanged: ((Map<String, Any>) -> Unit)? = null
    ): Flow<String> = callbackFlow {
        val channelName = "private-escalation-$escalationId"
        
        try {
            val channel = pusher?.subscribePrivate(channelName) ?: run {
                println("⚠️ Pusher not initialized")
                awaitClose {}
                return@callbackFlow
            }
            
            subscribedChannels[channelName] = channel
            
            // New message
            channel.bind("new-message") { event ->
                try {
                    val message = gson.fromJson(event.data, ChatMessage::class.java)
                    onNewMessage?.invoke(message)
                    trySend("new-message")
                } catch (e: Exception) {
                    println("⚠️ Failed to parse message: ${e.message}")
                }
            }
            
            // Typing indicator
            channel.bind("typing-indicator") { event ->
                try {
                    val indicator = gson.fromJson(event.data, TypingIndicator::class.java)
                    // Only show typing from users, not agents
                    if (indicator.senderType != "agent") {
                        onTypingIndicator?.invoke(indicator)
                    }
                } catch (e: Exception) {
                    println("⚠️ Failed to parse typing indicator: ${e.message}")
                }
            }
            
            // Status changed
            channel.bind("status-changed") { event ->
                try {
                    val json = gson.fromJson(event.data, Map::class.java) as Map<String, Any>
                    onStatusChanged?.invoke(json)
                    trySend("status-changed")
                } catch (e: Exception) {
                    println("⚠️ Failed to parse status change: ${e.message}")
                }
            }
            
            println("✅ Subscribed to escalation: $channelName")
            
            awaitClose {
                unsubscribe(channelName)
            }
        } catch (e: Exception) {
            println("❌ Failed to subscribe to escalation: ${e.message}")
            awaitClose {}
        }
    }
    
    /**
     * Unsubscribe from a channel with proper cleanup
     */
    fun unsubscribe(channelName: String) {
        subscribedChannels.remove(channelName)?.let { channel ->
            try {
                // Unbind all events from the channel
                channel.unbind("analytics-updated", null)
                channel.unbind("new-escalation", null)
                channel.unbind("new-message", null)
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
                unsubscribe(channelName)
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
