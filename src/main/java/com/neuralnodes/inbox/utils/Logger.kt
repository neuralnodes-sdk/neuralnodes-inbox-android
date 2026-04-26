package com.neuralnodes.inbox.utils

import android.util.Log

/**
 * Structured logging utility
 * Exact match to iOS SDK Logger
 */
object NeuralNodesLogger {
    
    private const val TAG = "NeuralNodesInbox"
    
    var isEnabled = true
    
    enum class Level {
        DEBUG,
        INFO,
        WARNING,
        ERROR
    }
    
    fun debug(message: String) {
        if (isEnabled) {
            Log.d(TAG, "🔍 $message")
        }
    }
    
    fun info(message: String) {
        if (isEnabled) {
            Log.i(TAG, "ℹ️ $message")
        }
    }
    
    fun warning(message: String) {
        if (isEnabled) {
            Log.w(TAG, "⚠️ $message")
        }
    }
    
    fun error(message: String, error: Throwable? = null) {
        if (isEnabled) {
            if (error != null) {
                Log.e(TAG, "❌ $message", error)
            } else {
                Log.e(TAG, "❌ $message")
            }
        }
    }
    
    // Specific logging methods matching iOS SDK
    
    fun logRealtimeConnected(service: String) {
        info("✅ $service connected")
    }
    
    fun logRealtimeDisconnected(service: String) {
        warning("🔌 $service disconnected")
    }
    
    fun logRealtimeMessage(channel: String) {
        debug("📨 Received message on channel: $channel")
    }
    
    fun logAPIRequest(method: String, path: String) {
        debug("🌐 API Request: $method $path")
    }
    
    fun logAPIResponse(statusCode: Int, path: String) {
        debug("✅ API Response: $statusCode $path")
    }
    
    fun logAPIError(statusCode: Int, path: String, message: String?) {
        error("❌ API Error: $statusCode $path - ${message ?: "Unknown error"}")
    }
}
