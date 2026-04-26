package com.neuralnodes.inbox.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
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
import com.neuralnodes.inbox.viewmodels.ConversationDetailViewModel
import kotlinx.coroutines.launch

/**
 * Conversation detail view with messages
 * Exact match to iOS SDK ConversationDetailView
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationDetailView(
    conversationId: String,
    conversationStatus: String,
    conversationName: String,
    sdk: NeuralNodesInbox,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ConversationDetailViewModel = viewModel(
        factory = ConversationDetailViewModelFactory(conversationId, conversationStatus, sdk)
    )
    
    val messages by viewModel.messages.collectAsState()
    val messageText by viewModel.messageText.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val scrollToMessageId by viewModel.scrollToMessageId.collectAsState()
    val hasMoreMessages by viewModel.hasMoreMessages.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    var showStatusMenu by remember { mutableStateOf(false) }
    
    // Load messages on first composition
    LaunchedEffect(Unit) {
        viewModel.loadMessages()
        viewModel.startListening()
        viewModel.markAsRead()
    }
    
    // Auto-scroll to new messages
    LaunchedEffect(scrollToMessageId) {
        scrollToMessageId?.let { messageId ->
            val index = messages.indexOfFirst { it.id == messageId }
            if (index != -1) {
                coroutineScope.launch {
                    listState.animateScrollToItem(index)
                }
            }
        }
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopListening()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        conversationName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showStatusMenu = true }) {
                        Icon(Icons.Default.MoreVert, "More")
                    }
                    
                    DropdownMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { showStatusMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Mark as Active") },
                            onClick = {
                                viewModel.updateStatus("active")
                                showStatusMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Mark as Resolved") },
                            onClick = {
                                viewModel.updateStatus("resolved")
                                showStatusMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Close Conversation") },
                            onClick = {
                                viewModel.updateStatus("closed")
                                showStatusMenu = false
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        },
        bottomBar = {
            if (conversationStatus != "closed" && conversationStatus != "resolved") {
                IOSMessageInputBar(
                    text = messageText,
                    onTextChange = { viewModel.setMessageText(it) },
                    onSend = { viewModel.sendMessage() },
                    enabled = !isSending
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF3F4F6)
                ) {
                    Text(
                        text = "This conversation has been ${conversationStatus}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
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
                    // Load more indicator
                    if (hasMoreMessages && !isLoadingMore) {
                        item {
                            LaunchedEffect(Unit) {
                                viewModel.loadMoreMessages()
                            }
                        }
                    }
                    
                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFF4A6EE0)
                                )
                            }
                        }
                    }
                    
                    // Messages
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(message = message)
                    }
                }
            }
        }
    }
}

/**
 * ViewModel Factory
 */
class ConversationDetailViewModelFactory(
    private val conversationId: String,
    private val conversationStatus: String,
    private val sdk: NeuralNodesInbox
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ConversationDetailViewModel(conversationId, conversationStatus, sdk) as T
    }
}
