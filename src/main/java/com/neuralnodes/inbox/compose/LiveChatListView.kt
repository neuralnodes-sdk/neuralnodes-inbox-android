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
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.neuralnodes.inbox.NeuralNodesInbox
import com.neuralnodes.inbox.models.Escalation
import com.neuralnodes.inbox.viewmodels.LiveChatListViewModel

/**
 * Live chat escalation list view
 * Exact match to iOS SDK LiveChatListView
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveChatListView(
    sdk: NeuralNodesInbox,
    onEscalationClick: (Escalation) -> Unit = {},
    showToolbar: Boolean = true,
    modifier: Modifier = Modifier
) {
    val viewModel: LiveChatListViewModel = viewModel(
        factory = LiveChatListViewModelFactory(sdk)
    )
    
    val escalations by viewModel.escalations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Load escalations on first composition
    LaunchedEffect(Unit) {
        viewModel.loadEscalations()
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        // Optional Toolbar
        if (showToolbar) {
            TopAppBar(
                title = {
                    Text(
                        "Live Chat",
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
        
        // Content
        SwipeRefresh(
            state = rememberSwipeRefreshState(isLoading),
            onRefresh = { viewModel.loadEscalations() }
        ) {
            when {
                escalations.isEmpty() && !isLoading -> {
                    EmptyStateView(
                        icon = "💬",
                        title = "No Live Chats",
                        message = "Live chat escalations will appear here"
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        items(escalations, key = { it.id }) { escalation ->
                            Box(
                                modifier = Modifier.clickable {
                                    onEscalationClick(escalation)
                                }
                            ) {
                                LiveChatRow(escalation = escalation)
                            }
                            
                            Divider(
                                modifier = Modifier.padding(start = 62.dp),
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

/**
 * ViewModel Factory
 */
class LiveChatListViewModelFactory(
    private val sdk: NeuralNodesInbox
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return LiveChatListViewModel(sdk) as T
    }
}
