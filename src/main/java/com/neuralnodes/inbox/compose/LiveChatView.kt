package com.neuralnodes.inbox.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neuralnodes.inbox.NeuralNodesInbox
import com.neuralnodes.inbox.models.ChatMessage
import com.neuralnodes.inbox.models.Message
import com.neuralnodes.inbox.viewmodels.LiveChatViewModel
import java.util.Date

/**
 * iOS-style Composable Live Chat View
 * Matches iOS SDK design with rounded input bar and proper message bubbles
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
                title = { 
                    Text(
                        title,
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
        
        // Messages List
        Box(modifier = Modifier.weight(1f)) {
            if (isLoading && messages.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF4A6EE0)
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
                    items(messages) { chatMessage ->
                        // Convert ChatMessage to Message for MessageBubble
                        val message = Message(
                            id = chatMessage.id,
                            conversationId = "",
                            messageText = chatMessage.messageText,
                            senderType = chatMessage.senderType,
                            senderName = chatMessage.senderName,
                            createdAt = chatMessage.createdAt,
                            updatedAt = chatMessage.createdAt
                        )
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
            IOSMessageInputBar(
                text = messageText,
                onTextChange = { viewModel.setMessageText(it) },
                onSend = {
                    viewModel.sendMessage()
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
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}

/**
 * iOS-style Typing Indicator
 */
@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6B7280))
            )
        }
        Text(
            text = "Customer is typing...",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(start = 4.dp)
        )
    }
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
