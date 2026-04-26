package com.neuralnodes.inbox.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
 * Filter sheet for selecting channel or status
 * Exact match to iOS SDK FilterSheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FilterSheet(
    title: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    onDismiss: () -> Unit,
    getDisplayName: (T) -> String,
    getIcon: (T) -> String,
    getColor: (T) -> Color
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // Title
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
            
            // Options List
            LazyColumn {
                items(options) { option ->
                    FilterSheetItem(
                        icon = getIcon(option),
                        title = getDisplayName(option),
                        color = getColor(option),
                        isSelected = option == selectedOption,
                        onClick = {
                            onOptionSelected(option)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterSheetItem(
    icon: String,
    title: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Text(
            text = icon,
            fontSize = 18.sp,
            color = color
        )
        
        // Title
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )
        
        // Checkmark
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color(0xFF4A6EE0),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Channel filter sheet
 */
@Composable
fun ChannelFilterSheet(
    selectedChannel: Channel,
    onChannelSelected: (Channel) -> Unit,
    onDismiss: () -> Unit
) {
    FilterSheet(
        title = "Select Channel",
        options = Channel.allCases(),
        selectedOption = selectedChannel,
        onOptionSelected = onChannelSelected,
        onDismiss = onDismiss,
        getDisplayName = { it.displayName },
        getIcon = { it.emoji },
        getColor = { it.color }
    )
}

/**
 * Status filter sheet
 */
@Composable
fun StatusFilterSheet(
    selectedStatus: ConversationStatus,
    onStatusSelected: (ConversationStatus) -> Unit,
    onDismiss: () -> Unit
) {
    FilterSheet(
        title = "Select Status",
        options = ConversationStatus.allCases(),
        selectedOption = selectedStatus,
        onOptionSelected = onStatusSelected,
        onDismiss = onDismiss,
        getDisplayName = { it.displayName },
        getIcon = { 
            when (it) {
                ConversationStatus.ALL -> "📊"
                ConversationStatus.ACTIVE -> "🟢"
                ConversationStatus.PENDING -> "🟡"
                ConversationStatus.RESOLVED -> "⚪"
                ConversationStatus.CLOSED -> "🔴"
            }
        },
        getColor = { it.color }
    )
}
