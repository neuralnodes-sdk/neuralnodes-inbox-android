package com.neuralnodes.inbox.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuralnodes.inbox.NeuralNodesInbox
import com.neuralnodes.inbox.models.Channel
import com.neuralnodes.inbox.models.Conversation
import com.neuralnodes.inbox.models.ConversationFilters
import com.neuralnodes.inbox.models.ConversationStatus
import com.neuralnodes.inbox.utils.NeuralNodesLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for Inbox - Exact match to iOS SDK InboxViewModel
 */
class InboxViewModel(private val sdk: NeuralNodesInbox) : ViewModel() {
    
    private val apiClient = sdk.getAPIClient()
    private val realtimeClient = sdk.getRealtimeClient()
    
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()
    
    private val _selectedChannel = MutableStateFlow(Channel.ALL)
    val selectedChannel: StateFlow<Channel> = _selectedChannel.asStateFlow()
    
    private val _selectedStatus = MutableStateFlow(ConversationStatus.ACTIVE)
    val selectedStatus: StateFlow<ConversationStatus> = _selectedStatus.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _showError = MutableStateFlow(false)
    val showError: StateFlow<Boolean> = _showError.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Alias for compatibility with InboxView
    val error: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private var hasSubscribed = false
    private var refreshTask: Job? = null
    private var isRefreshing = false // Prevent concurrent API calls
    
    val isSubscribed: Boolean
        get() = hasSubscribed
    
    init {
        // Watch for filter changes
        viewModelScope.launch {
            combine(_selectedChannel, _selectedStatus) { channel, status ->
                Pair(channel, status)
            }
                .drop(1) // Skip initial value
                .debounce(300) // Debounce filter changes
                .collect {
                    loadConversations()
                }
        }
    }
    
    private fun setupRealtimeSubscription() {
        // Only subscribe once
        if (hasSubscribed) {
            NeuralNodesLogger.info("[INBOX] Already subscribed to real-time updates")
            return
        }
        
        NeuralNodesLogger.info("[INBOX] Setting up real-time subscription")
        
        viewModelScope.launch {
            try {
                // Get clientId from API client
                val clientInfo = apiClient.getClientInfo()
                val clientId = clientInfo.id
                
                NeuralNodesLogger.info("[INBOX] Client ID: $clientId")
                
                realtimeClient.subscribeToInbox(clientId) {
                    NeuralNodesLogger.info("[INBOX] Ably callback triggered - scheduling refresh")
                    viewModelScope.launch {
                        loadConversationsWithDebounce()
                    }
                }
                
                hasSubscribed = true
                NeuralNodesLogger.info("[INBOX] Real-time subscription setup complete")
            } catch (e: Exception) {
                NeuralNodesLogger.error("[INBOX] Cannot subscribe - clientId not available", e)
            }
        }
    }
    
    /**
     * Load conversations with debounce to prevent rapid successive calls
     */
    suspend fun loadConversationsWithDebounce() {
        NeuralNodesLogger.info("[INBOX] loadConversationsWithDebounce called")
        
        // Cancel any pending refresh
        refreshTask?.cancel()
        
        // Schedule new refresh with 500ms delay
        refreshTask = viewModelScope.launch {
            NeuralNodesLogger.info("[INBOX] Waiting 500ms before refresh...")
            delay(500)
            NeuralNodesLogger.info("[INBOX] Executing refresh now")
            loadConversations()
        }
    }
    
    fun loadConversations() {
        NeuralNodesLogger.info("[INBOX] loadConversations started")
        
        // Prevent concurrent API calls
        if (isRefreshing) {
            NeuralNodesLogger.warning("[INBOX] Already refreshing, skipping this call")
            return
        }
        
        viewModelScope.launch {
            isRefreshing = true
            _isLoading.value = true
            _errorMessage.value = null
            _showError.value = false
            
            try {
                val channel = if (_selectedChannel.value == Channel.ALL) null else _selectedChannel.value.value
                val status = if (_selectedStatus.value == ConversationStatus.ALL) null else _selectedStatus.value.value
                
                val filters = ConversationFilters(
                    channel = channel,
                    status = status,
                    assignedTo = null,
                    limit = 50,
                    offset = 0
                )
                
                NeuralNodesLogger.info("[INBOX] Fetching conversations from API...")
                val fetchedConversations = apiClient.getConversations(
                    channel = filters.channel,
                    status = filters.status,
                    limit = filters.limit,
                    offset = filters.offset
                )
                
                NeuralNodesLogger.info("[INBOX] Received ${fetchedConversations.size} conversations")
                
                // Log unread counts for debugging
                val unreadConversations = fetchedConversations.filter { it.unreadCount > 0 }
                if (unreadConversations.isNotEmpty()) {
                    NeuralNodesLogger.info("[INBOX] Conversations with unread messages:")
                    unreadConversations.forEach { conv ->
                        NeuralNodesLogger.info("   - ${conv.contactName ?: "Unknown"}: ${conv.unreadCount} unread")
                    }
                }
                
                _conversations.value = fetchedConversations
                _isLoading.value = false
                isRefreshing = false
                
                // Setup real-time subscription after first successful load
                setupRealtimeSubscription()
            } catch (e: Exception) {
                NeuralNodesLogger.error("[INBOX] Error loading conversations", e)
                _errorMessage.value = e.localizedMessage
                _showError.value = true
                _isLoading.value = false
                isRefreshing = false
                _conversations.value = emptyList()
            }
        }
    }
    
    fun setChannelFilter(channel: Channel) {
        _selectedChannel.value = channel
    }
    
    fun setStatusFilter(status: ConversationStatus) {
        _selectedStatus.value = status
    }
    
    /**
     * Pause real-time updates (call when navigating to conversation detail)
     */
    fun pauseRealtimeUpdates() {
        NeuralNodesLogger.info("[INBOX] Pausing real-time updates")
        if (!hasSubscribed) return
        
        realtimeClient.unsubscribeFromInbox()
        hasSubscribed = false
    }
    
    /**
     * Resume real-time updates (call when returning to inbox list)
     */
    fun resumeRealtimeUpdates() {
        NeuralNodesLogger.info("[INBOX] Resuming real-time updates")
        setupRealtimeSubscription()
    }
    
    override fun onCleared() {
        super.onCleared()
        // Don't disconnect - SDK manages connection lifecycle
    }
}
