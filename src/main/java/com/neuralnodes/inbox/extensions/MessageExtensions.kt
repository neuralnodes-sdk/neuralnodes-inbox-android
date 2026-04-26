package com.neuralnodes.inbox.extensions

import com.neuralnodes.inbox.models.Message
import java.text.SimpleDateFormat
import java.util.*

/**
 * Extension to provide formatted time for UI
 * Exact match to iOS SDK MessageExtensions
 */
val Message.formattedTime: String
    get() {
        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        return formatter.format(createdAt)
    }

/**
 * Formatted date string
 * Exact match to iOS SDK formattedDate property
 */
val Message.formattedDate: String
    get() {
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return formatter.format(createdAt)
    }

/**
 * Relative time string (e.g., "2 hours ago")
 * Exact match to iOS SDK relativeTime property
 */
val Message.relativeTime: String
    get() {
        val seconds = ((Date().time - createdAt.time) / 1000).toInt()
        
        return when {
            seconds < 60 -> "just now"
            seconds < 3600 -> {
                val minutes = seconds / 60
                "$minutes min ago"
            }
            seconds < 86400 -> {
                val hours = seconds / 3600
                "$hours hr ago"
            }
            seconds < 604800 -> {
                val days = seconds / 86400
                "$days day${if (days > 1) "s" else ""} ago"
            }
            else -> {
                val weeks = seconds / 604800
                "$weeks week${if (weeks > 1) "s" else ""} ago"
            }
        }
    }
