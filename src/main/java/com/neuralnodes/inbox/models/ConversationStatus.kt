package com.neuralnodes.inbox.models

import androidx.compose.ui.graphics.Color

/**
 * Conversation status types
 * Exact match to iOS SDK ConversationStatus enum
 */
enum class ConversationStatus(val value: String) {
    ALL("all"),
    ACTIVE("active"),
    PENDING("pending"),
    RESOLVED("resolved"),
    CLOSED("closed");
    
    val displayName: String
        get() = when (this) {
            ALL -> "All Status"
            ACTIVE -> "Active"
            PENDING -> "Pending"
            RESOLVED -> "Resolved"
            CLOSED -> "Closed"
        }
    
    val icon: String
        get() = when (this) {
            ALL -> "grid_view"
            ACTIVE -> "circle"
            PENDING -> "schedule"
            RESOLVED -> "check_circle"
            CLOSED -> "cancel"
        }
    
    val color: Color
        get() = when (this) {
            ALL -> Color(0xFF000000) // Primary/Black
            ACTIVE -> Color(0xFF10B981) // Success Green
            PENDING -> Color(0xFFF59E0B) // Warning Yellow
            RESOLVED -> Color(0xFF6B7280) // Gray
            CLOSED -> Color(0xFFEF4444) // Error Red
        }
    
    companion object {
        fun fromString(value: String): ConversationStatus {
            return values().find { it.value == value } ?: ALL
        }
        
        fun allCases(): List<ConversationStatus> = values().toList()
    }
}
