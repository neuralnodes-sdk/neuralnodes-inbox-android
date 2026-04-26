package com.neuralnodes.inbox.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuralnodes.inbox.models.Escalation
import com.neuralnodes.inbox.models.EscalationStatus
import com.neuralnodes.inbox.extensions.timeAgo

/**
 * Live chat escalation row component
 * Exact match to iOS SDK LiveChatRow
 */
@Composable
fun LiveChatRow(
    escalation: Escalation,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color(0xFF4A6EE0).copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = getInitials(escalation.leadName ?: "Unknown"),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4A6EE0)
            )
        }
        
        // Content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Name and Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = escalation.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                
                Text(
                    text = escalation.timeAgo,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF6B7280)
                )
            }
            
            // Last message preview
            escalation.lastMessagePreview?.let { preview ->
                Text(
                    text = preview,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF6B7280),
                    maxLines = 1
                )
            }
            
            // Status and Unread
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                // Status Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = getStatusColor(escalation.status).copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(getStatusColor(escalation.status))
                        )
                        Text(
                            text = escalation.status.name.lowercase().replaceFirstChar { 
                                if (it.isLowerCase()) it.titlecase() else it.toString() 
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = getStatusColor(escalation.status)
                        )
                    }
                }
                
                // Unread Badge
                escalation.unreadCount?.let { count ->
                    if (count > 0) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEF4444)
                        ) {
                            Text(
                                text = count.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Get initials from name
 */
private fun getInitials(name: String): String {
    val components = name.split(" ")
    return when {
        components.size >= 2 -> {
            "${components[0].firstOrNull()?.uppercase() ?: ""}${components[1].firstOrNull()?.uppercase() ?: ""}"
        }
        components.isNotEmpty() -> {
            components[0].take(2).uppercase()
        }
        else -> "?"
    }
}

/**
 * Get status color for EscalationStatus enum
 */
private fun getStatusColor(status: EscalationStatus): Color {
    return when (status) {
        EscalationStatus.ACTIVE -> Color(0xFF10B981)
        EscalationStatus.PENDING -> Color(0xFFF59E0B)
        EscalationStatus.RESOLVED -> Color(0xFF6B7280)
        EscalationStatus.CLOSED -> Color(0xFFEF4444)
    }
}
