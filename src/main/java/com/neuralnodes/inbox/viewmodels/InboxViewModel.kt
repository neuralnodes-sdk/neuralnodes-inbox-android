package com.neuralnodes.inbox.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuralnodes.inbox.NeuralNodesInbox
import com.neuralnodes.inbox.models.Conversation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Inbox - shared between Activity and Composable
 */
class InboxViewModel(private val sdk: NeuralNodesInbox) : ViewModel() {
    
    private val apiClient = sdk.getAPIClient()
    private val realtimeClient = sdk.getRealtimeClient()
    
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _selectedChannel = MutableStateFlow<String?>(null)
    val selectedChannel: StateFlow<String?> = _selectedChannel.asStateFlow()
    
    private val _selectedStatus = MutableStateFlow<String?>(null)
    val selectedStatus: StateFlow<String?> = _selectedStatus.asStateFlow()
    
    private var isSubscribed = false
    
    init {
        loadConversations()
        subscribeToUpdates()
    }
    
    fun loadConversations() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val allConversations = apiClient.getConversations(
                    status = _selectedStatus.value,
                    limit = 100
                )
                
                // Filter by channel if selected
                val filtered = if (_selectedChannel.value != null) {
                    allConversations.filter { it.channel == _selectedChannel.value }
                } else {
                    allConversations
                }
                
                // Filter out "web" channel - those belong to Live Chat
                _conversations.value = filtered.filter { it.channel.lowercase() != "web" }
                
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load conversations"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun setChannelFilter(channel: String?) {
        _selectedChannel.value = channel
        loadConversations()
    }
    
    fun setStatusFilter(status: String?) {
        _selectedStatus.value = status
        loadConversations()
    }
    
    fun clearError() {
        _error.value = null
    }
    
    private fun subscribeToUpdates() {
        if (isSubscribed) return
        
        realtimeClient.subscribeToInbox {
            loadConversations()
        }
        isSubscribed = true
    }
    
    override fun onCleared() {
        super.onCleared()
        // Don't disconnect - SDK manages connection lifecycle
    }
}
