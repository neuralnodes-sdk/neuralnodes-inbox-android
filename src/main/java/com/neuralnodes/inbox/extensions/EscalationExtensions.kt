package com.neuralnodes.inbox.extensions

import com.neuralnodes.inbox.models.Escalation
import java.text.SimpleDateFormat
import java.util.*

/**
 * Extension to provide formatted time for UI
 * Exact match to iOS SDK EscalationExtensions
 */
val Escalation.timeAgo: String
    get() {
        val seconds = ((Date().time - updatedAt.time) / 1000).toInt()
        
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

/**
 * Formatted date string
 * Exact match to iOS SDK formattedDate property
 */
val Escalation.formattedDate: String
    get() {
        val formatter = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
        return formatter.format(createdAt)
    }
