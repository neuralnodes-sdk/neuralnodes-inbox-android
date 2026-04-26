package com.neuralnodes.inbox.extensions

import com.neuralnodes.inbox.models.Conversation
import java.util.*

/**
 * Extension to provide channel icon for UI
 * Exact match to iOS SDK ConversationExtensions
 */
val Conversation.channelIcon: String
    get() = when (channel) {
        "webchat" -> "chat"
        "whatsapp" -> "message"
        "telegram" -> "send"
        "email" -> "email"
        else -> "chat"
    }

/**
 * Time ago string for display
 * Exact match to iOS SDK timeAgo property
 */
val Conversation.timeAgo: String
    get() {
        // Use lastMessageAt if available, otherwise fall back to updatedAt
        val referenceDate = lastMessageAt ?: updatedAt
        val seconds = ((Date().time - referenceDate.time) / 1000).toInt()
        
        return when {
            seconds < 60 -> "Just now"
            seconds < 3600 -> {
                val minutes = seconds / 60
                "${minutes}m ago"
            }
            seconds < 86400 -> {
                val hours = seconds / 3600
                "${hours}h ago"
            }
            else -> {
                val days = seconds / 86400
                "${days}d ago"
            }
        }
    }
