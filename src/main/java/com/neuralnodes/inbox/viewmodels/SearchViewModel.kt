package com.neuralnodes.inbox.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuralnodes.inbox.NeuralNodesInbox
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/**
 * ViewModel for Search functionality
 */
@OptIn(FlowPreview::class)
class SearchViewModel(private val sdk: NeuralNodesInbox) : ViewModel() {
    
    private val apiClient = sdk.getAPIClient()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _searchResults = MutableStateFlow<List<Any>>(emptyList())
    val searchResults: StateFlow<List<Any>> = _searchResults.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        // Debounce search queries
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .collect { query ->
                    if (query.isNotBlank()) {
                        performSearch(query)
                    } else {
                        _searchResults.value = emptyList()
                    }
                }
        }
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _isSearching.value = query.isNotBlank()
    }
    
    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _isSearching.value = false
    }
    
    private fun performSearch(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Search conversations
                val conversations = apiClient.getConversations(limit = 50)
                val filtered = conversations.filter {
                    it.displayName.contains(query, ignoreCase = true) ||
                    it.lastMessage?.contains(query, ignoreCase = true) == true ||
                    it.contactEmail?.contains(query, ignoreCase = true) == true
                }
                
                _searchResults.value = filtered
                
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
