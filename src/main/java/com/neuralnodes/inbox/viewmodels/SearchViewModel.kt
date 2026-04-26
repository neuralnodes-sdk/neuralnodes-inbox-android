package com.neuralnodes.inbox.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuralnodes.inbox.models.*
import com.neuralnodes.inbox.services.SearchService
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for search functionality with debouncing
 * Exact match to iOS SDK SearchViewModel.swift
 */
@OptIn(FlowPreview::class)
class SearchViewModel(private val searchService: SearchService) : ViewModel() {
    
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    
    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()
    
    private val _searchResults = MutableStateFlow<List<SearchConversationResult>>(emptyList())
    val searchResults: StateFlow<List<SearchConversationResult>> = _searchResults.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private var searchTask: Job? = null
    
    init {
        setupSearchDebouncing()
    }
    
    private fun setupSearchDebouncing() {
        // Debounce search text changes
        viewModelScope.launch {
            _searchText
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isEmpty()) {
                        _suggestions.value = emptyList()
                        _searchResults.value = emptyList()
                        _isLoading.value = false
                    } else if (query.length >= 2) {
                        // Fetch suggestions for autocomplete
                        fetchSuggestions(query)
                        
                        // Perform search
                        performSearch(query)
                    }
                }
        }
    }
    
    private fun fetchSuggestions(query: String) {
        viewModelScope.launch {
            try {
                val suggestions = searchService.getSuggestionsImmediate(query, limit = 5)
                _suggestions.value = suggestions
            } catch (e: Exception) {
                // Silently fail for suggestions
                _suggestions.value = emptyList()
            }
        }
    }
    
    private fun performSearch(query: String) {
        // Cancel previous search
        searchTask?.cancel()
        
        searchTask = viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val filters = ConversationSearchFilters(
                    query = query,
                    channel = null,
                    status = null,
                    liveChat = null,
                    limit = 50,
                    offset = 0
                )
                
                val response = searchService.searchConversationsImmediate(filters)
                
                _searchResults.value = response.results
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Search failed"
                _isLoading.value = false
            }
        }
    }
    
    fun setSearchText(text: String) {
        _searchText.value = text
        _isSearching.value = text.isNotBlank()
    }
    
    fun selectSuggestion(suggestion: String) {
        _searchText.value = suggestion
        _suggestions.value = emptyList()
    }
    
    fun clearSearch() {
        _searchText.value = ""
        _suggestions.value = emptyList()
        _searchResults.value = emptyList()
        _isLoading.value = false
        _errorMessage.value = null
        _isSearching.value = false
        searchTask?.cancel()
    }
    
    override fun onCleared() {
        super.onCleared()
        searchTask?.cancel()
    }
}
