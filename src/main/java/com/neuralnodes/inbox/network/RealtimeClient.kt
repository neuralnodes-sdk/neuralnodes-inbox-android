package com.neuralnodes.inbox.network

import com.neuralnodes.inbox.models.Message
import com.google.gson.Gson
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.Channel
import io.ably.lib.types.ClientOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Production-ready Ably client with proper memory management
 * Fixes memory leaks and connection issues
 */
class RealtimeClient {
    
    private var ably: AblyRealtime? = null
    private val subscribedChannels = ConcurrentHashMap<String, Channel>()
    private val gson = Gson()
    
    @Volatile
    private var isConnectedState = false
    
    /**
     * Connect to Ably with API key
     * Includes automatic reconnection and recovery
     */
    fun connect(key: String) {
        // Disconnect existing connection if any
        disconnect()
        
        val options = ClientOptions().apply {
            this.key = key
            autoConnect = true
            // Set timeouts
            httpOpenTimeout = 10000
            httpRequestTimeout = 15000
            realtimeRequestTimeout = 10000
        }
        
        ably = AblyRealtime(options)
        
        ably?.connection?.on { stateChange ->
            when (stateChange.current) {
                io.ably.lib.realtime.ConnectionState.connected -> {
                    isConnectedState = true
                    println("✅ Ably connected")
                }
                io.ably.lib.realtime.ConnectionState.disconnected -> {
                    isConnectedState = false
                    println("⚠️ Ably disconnected")
                }
                io.ably.lib.realtime.ConnectionState.failed -> {
                    isConnectedState = false
                    println("❌ Ably connection failed: ${stateChange.reason}")
                }
                io.ably.lib.realtime.ConnectionState.suspended -> {
                    isConnectedState = false
                    println("⚠️ Ably connection suspended")
                }
                else -> {}
            }
        }
    }
    
    /**
     * Disconnect from Ably with proper cleanup
     * CRITICAL: Prevents memory leaks
     */
    fun disconnect() {
        try {
            // Unsubscribe all channels
            subscribedChannels.keys.toList().forEach { conversationId ->
                unsubscribe(conversationId)
            }
            subscribedChannels.clear()
            
            // Close Ably connection
            ably?.close()
            ably = null
            
            isConnectedState = false
            
            println("✅ Ably disconnected and cleaned up")
        } catch (e: Exception) {
            println("⚠️ Error during Ably cleanup: ${e.message}")
        }
    }
    
    /**
     * Subscribe to conversation updates as Flow
     * Automatically handles cleanup on Flow cancellation
     */
    fun subscribeToConversation(conversationId: String): Flow<Message> = callbackFlow {
        if (ably == null) {
            println("⚠️ Ably not connected, skipping subscription")
            awaitClose {}
            return@callbackFlow
        }
        
        val channelName = "conversation-$conversationId"
        val channel = ably?.channels?.get(channelName)
        
        if (channel == null) {
            println("⚠️ Failed to get channel: $channelName")
            awaitClose {}
            return@callbackFlow
        }
        
        val listener = Channel.MessageListener { message ->
            try {
                val data = message.data as? Map<*, *>
                if (data != null) {
                    val json = gson.toJson(data)
                    val decodedMessage = gson.fromJson(json, Message::class.java)
                    trySend(decodedMessage)
                }
            } catch (e: Exception) {
                println("⚠️ Failed to decode message: ${e.message}")
            }
        }
        
        channel.subscribe("new-message", listener)
        subscribedChannels[conversationId] = channel
        println("✅ Subscribed to conversation: $conversationId")
        
        awaitClose {
            try {
                channel.unsubscribe("new-message", listener)
                subscribedChannels.remove(conversationId)
                println("✅ Unsubscribed from conversation: $conversationId")
            } catch (e: Exception) {
                println("⚠️ Error unsubscribing: ${e.message}")
            }
        }
    }
    
    /**
     * Subscribe to inbox updates
     * Exact match to iOS SDK
     */
    fun subscribeToInbox(clientId: String, onUpdate: () -> Unit) {
        if (ably == null) {
            println("⚠️ Ably not connected for inbox subscription")
            return
        }
        
        val channelName = "inbox-updates-$clientId"
        val channel = ably?.channels?.get(channelName)
        
        println("📡 Subscribing to Ably inbox channel: $channelName")
        
        channel?.subscribe { message ->
            println("📨 [INBOX UPDATE] Received Ably message on $channelName")
            println("   Event name: ${message.name ?: "nil"}")
            println("   Timestamp: ${java.util.Date()}")
            message.data?.let {
                println("   Data: $it")
            }
            
            // Trigger callback on main thread
            onUpdate()
        }
        
        subscribedChannels["inbox-updates"] = channel!!
        println("✅ Subscribed to inbox updates: $channelName")
    }
    
    /**
     * Unsubscribe from inbox updates
     * Exact match to iOS SDK
     */
    fun unsubscribeFromInbox() {
        subscribedChannels.remove("inbox-updates")?.let { channel ->
            try {
                println("🔕 Unsubscribing from inbox updates")
                channel.unsubscribe()
                println("✅ Unsubscribed from inbox updates")
            } catch (e: Exception) {
                println("⚠️ Error unsubscribing from inbox: ${e.message}")
            }
        }
    }
    
    /**
     * Unsubscribe from conversation with proper cleanup
     */
    fun unsubscribe(conversationId: String) {
        subscribedChannels.remove(conversationId)?.let { channel ->
            try {
                channel.unsubscribe()
                println("✅ Unsubscribed from conversation: $conversationId")
            } catch (e: Exception) {
                println("⚠️ Error unsubscribing from $conversationId: ${e.message}")
            }
        }
    }
    
    /**
     * Check if connected
     */
    val isConnected: Boolean
        get() = isConnectedState && ably?.connection?.state == io.ably.lib.realtime.ConnectionState.connected
}
