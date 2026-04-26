package com.neuralnodes.inbox.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neuralnodes.inbox.NeuralNodesInbox
import com.neuralnodes.inbox.models.Conversation
import com.neuralnodes.inbox.models.ConversationStatus
import com.neuralnodes.inbox.viewmodels.InboxViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

/**
 * iOS-style Composable Inbox View
 * Matches iOS SDK design exactly with circular avatars, status badges, and channel icons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxView(
    sdk: NeuralNodesInbox,
    modifier: Modifier = Modifier,
    onConversationClick: (Conversation) -> Unit = {},
    showToolbar: Boolean = true
) {
    val viewModel: InboxViewModel = viewModel(
        factory = InboxViewModelFactory(sdk)
    )
    
    val conversations by viewModel.conversations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    Column(modifier = modifier.fillMaxSize()) {
        // Optional Toolbar
        if (showToolbar) {
            TopAppBar(
                title = { 
                    Text(
                        "Inbox",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        }
        
        // Search Bar
        var searchQuery by remember { mutableStateOf("") }
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = { /* Handle search */ },
            placeholder = "Search"
        )
        
        // Filter Chips - iOS style
        FilterChipsRow(
            selectedStatus = viewModel.selectedStatus.collectAsState().value.value,
            onStatusSelected = { status ->
                // Update filter by setting the status
                viewModel.setStatusFilter(
                    when (status) {
                        "active" -> ConversationStatus.ACTIVE
                        "pending" -> ConversationStatus.PENDING
                        "resolved" -> ConversationStatus.RESOLVED
                        "closed" -> ConversationStatus.CLOSED
                        else -> ConversationStatus.ALL
                    }
                )
            },
            conversations = conversations
        )
        
        // Content
        SwipeRefresh(
            state = rememberSwipeRefreshState(isLoading),
            onRefresh = { viewModel.loadConversations() }
        ) {
            when {
                error != null -> {
                    ErrorView(
                        message = error ?: "An error occurred",
                        onRetry = { viewModel.loadConversations() }
                    )
                }
                conversations.isEmpty() && !isLoading -> {
                    EmptyStateView(
                        icon = "📭",
                        title = "No Conversations",
                        message = "There are no conversations matching your filters"
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(conversations) { conversation ->
                            ConversationRow(
                                conversation = conversation,
                                onClick = { onConversationClick(conversation) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * iOS-style Filter Chips with counts
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipsRow(
    selectedStatus: String?,
    onStatusSelected: (String?) -> Unit,
    conversations: List<Conversation>
) {
    val allCount = conversations.size
    val activeCount = conversations.count { it.status == "active" }
    val pendingCount = conversations.count { it.status == "pending" }
    val resolvedCount = conversations.count { it.status == "resolved" }
    val closedCount = conversations.count { it.status == "closed" }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IOSFilterChip(
            label = "All ($allCount)",
            selected = selectedStatus == null,
            onClick = { onStatusSelected(null) }
        )
        IOSFilterChip(
            label = "Active ($activeCount)",
            selected = selectedStatus == "active",
            onClick = { onStatusSelected("active") },
            color = Color(0xFF10B981)
        )
        IOSFilterChip(
            label = "Pending ($pendingCount)",
            selected = selectedStatus == "pending",
            onClick = { onStatusSelected("pending") },
            color = Color(0xFFF59E0B)
        )
        IOSFilterChip(
            label = "Resolved ($resolvedCount)",
            selected = selectedStatus == "resolved",
            onClick = { onStatusSelected("resolved") },
            color = Color(0xFF6B7280)
        )
    }
}

/**
 * iOS-style Filter Chip
 */
@Composable
private fun IOSFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    color: Color = Color(0xFF4A6EE0)
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) color.copy(alpha = 0.12f) else Color(0xFFF3F4F6),
        modifier = Modifier.height(32.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) color else Color(0xFF6B7280)
            )
        }
    }
}

/**
 * iOS-style Conversation Row
 * Matches iOS SDK exactly: circular avatar with gradient, channel icon, status badge, unread count
 */
@Composable
private fun ConversationRow(
    conversation: Conversation,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Premium Channel Icon with Gradient Background
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            getChannelColor(conversation.channel).copy(alpha = 0.15f),
                            getChannelColor(conversation.channel).copy(alpha = 0.05f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getChannelIcon(conversation.channel),
                contentDescription = conversation.channel,
                tint = getChannelColor(conversation.channel),
                modifier = Modifier.size(20.dp)
            )
        }
        
        // Content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Name and Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = conversation.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = formatTimeAgo(conversation.updatedAt),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6B7280)
                )
            }
            
            // Last Message
            conversation.lastMessage?.let { message ->
                Text(
                    text = message,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF6B7280),
                    maxLines = 2,
                    lineHeight = 17.sp
                )
            }
            
            // Status and Unread Badge
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = getStatusColor(conversation.status).copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(getStatusColor(conversation.status))
                        )
                        Text(
                            text = conversation.status.replaceFirstChar { it.uppercase() },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = getStatusColor(conversation.status)
                        )
                    }
                }
                
                // Unread Badge
                if (conversation.unreadCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF4A6EE0) // Solid blue
                    ) {
                        Text(
                            text = "${conversation.unreadCount}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Get channel color matching iOS SDK
 */
private fun getChannelColor(channel: String): Color {
    return when (channel.lowercase()) {
        "webchat" -> Color(0xFF3B82F6)
        "whatsapp" -> Color(0xFF25D366)
        "telegram" -> Color(0xFF0088CC)
        "email" -> Color(0xFF6B7280)
        else -> Color(0xFF3B82F6)
    }
}

/**
 * Get channel icon
 */
private fun getChannelIcon(channel: String): ImageVector {
    return when (channel.lowercase()) {
        "webchat" -> Icons.Default.Chat
        "whatsapp" -> Icons.Default.Message
        "telegram" -> Icons.Default.Send
        "email" -> Icons.Default.Email
        else -> Icons.Default.Chat
    }
}

/**
 * Get status color matching iOS SDK
 */
private fun getStatusColor(status: String): Color {
    return when (status.lowercase()) {
        "active" -> Color(0xFF10B981)
        "pending" -> Color(0xFFF59E0B)
        "resolved" -> Color(0xFF6B7280)
        "closed" -> Color(0xFFEF4444)
        else -> Color(0xFF6B7280)
    }
}

/**
 * Format time ago
 */
private fun formatTimeAgo(date: java.util.Date): String {
    val now = java.util.Date()
    val diff = now.time - date.time
    
    return when {
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86400_000 -> "${diff / 3600_000}h"
        diff < 604800_000 -> "${diff / 86400_000}d"
        else -> java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(date)
    }
}

// ViewModel Factory
class InboxViewModelFactory(private val sdk: NeuralNodesInbox) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return InboxViewModel(sdk) as T
    }
}
