package com.neuralnodes.inbox.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuralnodes.inbox.models.Channel
import com.neuralnodes.inbox.models.ConversationStatus

/**
 * Filter bar with channel and status filters
 * Exact match to iOS SDK FilterBar
 */
@Composable
fun FilterBar(
    selectedChannel: Channel,
    selectedStatus: ConversationStatus,
    onChannelTap: () -> Unit,
    onStatusTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Channel Filter
        FilterButton(
            icon = selectedChannel.emoji,
            title = selectedChannel.displayName,
            color = selectedChannel.color,
            onClick = onChannelTap
        )
        
        // Status Filter
        FilterButton(
            icon = when (selectedStatus) {
                ConversationStatus.ALL -> "📊"
                ConversationStatus.ACTIVE -> "🟢"
                ConversationStatus.PENDING -> "🟡"
                ConversationStatus.RESOLVED -> "⚪"
                ConversationStatus.CLOSED -> "🔴"
            },
            title = selectedStatus.displayName,
            color = selectedStatus.color,
            onClick = onStatusTap
        )
        
        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * Filter button component
 * Exact match to iOS SDK FilterButton
 */
@Composable
fun FilterButton(
    icon: String,
    title: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 12.sp
            )
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Dropdown",
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
