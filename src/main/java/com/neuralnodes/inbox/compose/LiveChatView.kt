package com.neuralnodes.inbox.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.neuralnodes.inbox.models.ChatMessage
import com.neuralnodes.inbox.viewmodels.LiveChatViewModel

/**
 * Composable Live Chat View - Can be embedded anywhere
 * 
 * Usage:
 * ```
 * LiveChatView(
 *     escalationId = "escalation_123",
 *     sdk = NeuralNodesInbox.getInstance()
 * )
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveChatView(
    escalationId: String,
    sdk: NeuralNodesInbox,
    modifier: Modifier = Modifier,
    showToolbar: Boolean = true,
    title: String = "Live Chat"
) {
    val viewModel: LiveChatViewModel = viewModel(
        factory = LiveChatViewModelFactory(escalationId, sdk)
    )
    
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val messageText by viewModel.messageText.collectAsState()
    val currentStatus by viewModel.currentStatus.collectAsState()
    
    val listState = rememberLazyListState()
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        // Optional Toolbar
        if (showToolbar) {
            TopAppBar(
                title = { Text(title) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        }
        
        // Messages List
        Box(modifier = Modifier.weight(1f)) {
            if (isLoading && messages.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF9FAFB)),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { message ->
                        MessageBubble(message = message)
                    }
                    
                    // Typing indicator
                    if (isTyping) {
                        item {
                            TypingIndicator()
                        }
                    }
                }
            }
        }
        
        // Input Bar
        if (currentStatus != "closed" && currentStatus != "resolved") {
            MessageInputBar(
                text = messageText,
                onTextChange = { viewModel.setMessageText(it) },
                onSend = {
                    viewModel.sendMessage(messageText)
                }
            )
        } else {
            // Closed chat indicator
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF3F4F6)
            ) {
                Text(
                    text = "This conversation has been closed",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isAgent = message.isFromAgent
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isAgent) Alignment.End else Alignment.Start
    ) {
        // Sender name for user messages
        if (!isAgent) {
            Text(
                text = message.displaySenderName,
                fontSize = 13.sp,
                color = Color(0xFF9CA3AF),
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        
        // Message bubble
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isAgent) Color(0xFF4A6EE0) else Color(0xFFF3F4F6),
            shadowElevation = if (isAgent) 2.dp else 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.messageText,
                    fontSize = 17.sp,
                    color = if (isAgent) Color.White else Color.Black,
                    lineHeight = 22.sp
                )
                
                Text(
                    text = formatTime(message.createdAt),
                    fontSize = 13.sp,
                    color = if (isAgent) Color(0xFFE0E7FF) else Color(0xFF6B7280),
                    modifier = Modifier
                        .align(if (isAgent) Alignment.End else Alignment.Start)
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(3) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Gray,
                modifier = Modifier.size(6.dp)
            ) {}
        }
        Text(
            text = "Customer is typing...",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
private fun MessageInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFD1D5DB),
                    unfocusedBorderColor = Color(0xFFD1D5DB)
                )
            )
            
            IconButton(
                onClick = onSend,
                enabled = text.isNotBlank()
            ) {
                Text(
                    text = "➤",
                    fontSize = 24.sp,
                    color = if (text.isNotBlank()) Color(0xFF4A6EE0) else Color.Gray
                )
            }
        }
    }
}

private fun formatTime(date: java.util.Date): String {
    return java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(date)
}

// ViewModel Factory
class LiveChatViewModelFactory(
    private val escalationId: String,
    private val sdk: NeuralNodesInbox
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return LiveChatViewModel(escalationId, sdk) as T
    }
}
