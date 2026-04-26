package com.neuralnodes.inbox.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuralnodes.inbox.NeuralNodesInbox
import com.neuralnodes.inbox.models.Escalation
import com.neuralnodes.inbox.utils.NeuralNodesLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for live chat escalation list
 * Exact match to iOS SDK LiveChatListViewModel
 */
class LiveChatListViewModel(private val sdk: NeuralNodesInbox) : ViewModel() {
    
    private val _escalations = MutableStateFlow<List<Escalation>>(emptyList())
    val escalations: StateFlow<List<Escalation>> = _escalations
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private var hasSubscribed = false
    private var refreshTask: Job? = null
    private var isRefreshing = false // Prevent concurrent API calls
    
    private fun setupRealtimeSubscription() {
        // Only subscribe once
        if (hasSubscribed) {
            NeuralNodesLogger.info("[LIVE CHAT LIST] Already subscribed to real-time updates")
            return
        }
        
        NeuralNodesLogger.info("[LIVE CHAT LIST] Setting up real-time subscription")
        
        val pusherClient = sdk.getPusherClient()
        val apiClient = sdk.getAPIClient()
        
        viewModelScope.launch {
            try {
                // Get clientId from API client
                val clientInfo = apiClient.getClientInfo()
                val clientId = clientInfo.id
                
                NeuralNodesLogger.info("[LIVE CHAT LIST] Client ID: $clientId")
                
                pusherClient.subscribeToEscalationList(clientId) {
                    NeuralNodesLogger.info("[LIVE CHAT LIST] Pusher callback triggered - scheduling refresh")
                    viewModelScope.launch {
                        loadEscalationsWithDebounce()
                    }
                }
                
                hasSubscribed = true
                NeuralNodesLogger.info("[LIVE CHAT LIST] Real-time subscription setup complete")
            } catch (e: Exception) {
                NeuralNodesLogger.error("[LIVE CHAT LIST] Cannot subscribe - clientId not available", e)
            }
        }
    }
    
    /**
     * Load escalations with debounce to prevent rapid successive calls
     */
    private suspend fun loadEscalationsWithDebounce() {
        NeuralNodesLogger.info("[LIVE CHAT LIST] loadEscalationsWithDebounce called")
        
        // Cancel any pending refresh
        refreshTask?.cancel()
        
        // Schedule new refresh with 500ms delay
        refreshTask = viewModelScope.launch {
            NeuralNodesLogger.info("[LIVE CHAT LIST] Waiting 500ms before refresh...")
            delay(500)
            NeuralNodesLogger.info("[LIVE CHAT LIST] Executing refresh now")
            loadEscalations()
        }
    }
    
    fun loadEscalations(status: String? = null) {
        NeuralNodesLogger.info("[LIVE CHAT LIST] loadEscalations started")
        
        // Prevent concurrent API calls
        if (isRefreshing) {
            NeuralNodesLogger.warning("[LIVE CHAT LIST] Already refreshing, skipping this call")
            return
        }
        
        viewModelScope.launch {
            isRefreshing = true
            _isLoading.value = true
            
            try {
                val liveChatClient = sdk.getLiveChatClient()
                NeuralNodesLogger.info("[LIVE CHAT LIST] Fetching escalations from API...")
                val fetchedEscalations = liveChatClient.getEscalations(limit = 50)
                
                NeuralNodesLogger.info("[LIVE CHAT LIST] Received ${fetchedEscalations.size} escalations")
                
                _escalations.value = fetchedEscalations
                _isLoading.value = false
                isRefreshing = false
                
                // Setup real-time subscription after first successful load
                setupRealtimeSubscription()
            } catch (e: Exception) {
                NeuralNodesLogger.error("[LIVE CHAT LIST] Error loading escalations", e)
                _isLoading.value = false
                isRefreshing = false
            }
        }
    }
}
