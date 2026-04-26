package com.neuralnodes.inbox.models

import androidx.compose.ui.graphics.Color

/**
 * Communication channel types
 * Exact match to iOS SDK Channel enum
 */
enum class Channel(val value: String) {
    ALL("all"),
    WHATSAPP("whatsapp"),
    TELEGRAM("telegram"),
    EMAIL("email");
    
    val displayName: String
        get() = when (this) {
            ALL -> "All Channels"
            WHATSAPP -> "WhatsApp"
            TELEGRAM -> "Telegram"
            EMAIL -> "Email"
        }
    
    val icon: String
        get() = when (this) {
            ALL -> "tray_2_fill"
            WHATSAPP -> "message_badge_filled"
            TELEGRAM -> "send"
            EMAIL -> "email"
        }
    
    val color: Color
        get() = when (this) {
            ALL -> Color(0xFF000000) // Primary/Black
            WHATSAPP -> Color(0xFF25D366) // WhatsApp Green
            TELEGRAM -> Color(0xFF0088CC) // Telegram Blue
            EMAIL -> Color(0xFF6B7280) // Email Gray
        }
    
    val emoji: String
        get() = when (this) {
            ALL -> "📱"
            WHATSAPP -> "💬"
            TELEGRAM -> "✈️"
            EMAIL -> "📧"
        }
    
    companion object {
        fun fromString(value: String): Channel {
            return values().find { it.value == value } ?: ALL
        }
        
        fun allCases(): List<Channel> = values().toList()
    }
}
