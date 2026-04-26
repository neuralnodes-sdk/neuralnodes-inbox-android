package com.neuralnodes.inbox.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neuralnodes.inbox.NeuralNodesInbox
import com.neuralnodes.inbox.models.Conversation
import com.neuralnodes.inbox.viewmodels.InboxViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

/**
 * Composable Inbox View - Can be embedded anywhere
 * 
 * Usage:
 * ```
 * InboxView(
 *     sdk = NeuralNodesInbox.getInstance(),
 *     onConversationClick = { conversation ->
 *         // Navigate to conversation detail
 *     }
 * )
 * ```
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
                title = { Text("Inbox") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        }
        
        // Filter Chips
        FilterChipsRow(
            selectedStatus = viewModel.selectedStatus.collectAsState().value,
            onStatusSelected = { viewModel.setStatusFilter(it) }
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
                            ConversationItem(
                                conversation = conversation,
                                onClick = { onConversationClick(conversation) }
                            )
                            Divider(
                                modifier = Modifier.padding(start = 68.dp),
                                color = Color(0xFFE5E7EB),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipsRow(
    selectedStatus: String?,
    onStatusSelected: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedStatus == null,
            onClick = { onStatusSelected(null) },
            label = { Text("All") }
        )
        FilterChip(
            selected = selectedStatus == "active",
            onClick = { onStatusSelected("active") },
            label = { Text("Active") }
        )
        FilterChip(
            selected = selectedStatus == "pending",
            onClick = { onStatusSelected("pending") },
            label = { Text("Pending") }
        )
        FilterChip(
            selected = selectedStatus == "resolved",
            onClick = { onStatusSelected("resolved") },
            label = { Text("Resolved") }
        )
    }
}

@Composable
private fun ConversationItem(
    conversation: Conversation,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .padding(end = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = Color(0xFFEFF6FF)
            ) {
                Text(
                    text = conversation.displayName.firstOrNull()?.uppercase() ?: "A",
                    modifier = Modifier.padding(8.dp),
                    color = Color(0xFF4A6EE0),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = conversation.displayName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            }
            
            // Status
            Row(
                modifier = Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .padding(end = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = getStatusColor(conversation.status)
                    ) {
                        Spacer(modifier = Modifier.size(6.dp))
                    }
                }
                Text(
                    text = conversation.status.capitalize(),
                    fontSize = 13.sp,
                    color = getStatusColor(conversation.status)
                )
            }
            
            // Last message
            conversation.lastMessage?.let { message ->
                Text(
                    text = message,
                    fontSize = 15.sp,
                    color = Color(0xFF6B7280),
                    maxLines = 2,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // Timestamp
            Text(
                text = formatTimeAgo(conversation.updatedAt),
                fontSize = 13.sp,
                color = Color(0xFF9CA3AF),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun getStatusColor(status: String): Color {
    return when (status.lowercase()) {
        "active" -> Color(0xFF10B981)
        "pending" -> Color(0xFFF59E0B)
        "resolved" -> Color(0xFF6B7280)
        "closed" -> Color(0xFF6B7280)
        else -> Color(0xFF6B7280)
    }
}

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
