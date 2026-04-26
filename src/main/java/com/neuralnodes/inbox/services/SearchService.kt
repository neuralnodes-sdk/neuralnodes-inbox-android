package com.neuralnodes.inbox.services

import com.neuralnodes.inbox.network.APIClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Search service with debouncing for efficient API calls
 * Matches iOS SearchService implementation
 */
class SearchService(
    private val apiClient: APIClient,
    private val debounceInterval: Long = 300L // milliseconds
) {
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Debounce flows for different search types
    private val conversationSearchFlow = MutableSharedFlow<ConversationSearchFilters>()
    private val messageSearchFlow = MutableSharedFlow<MessageSearchFilters>()
    private val suggestionSearchFlow = MutableSharedFlow<String>()
    
    init {
        setupDebouncedSearches()
    }
    
    // MARK: - Setup Debouncing
    
    private fun setupDebouncedSearches() {
        // Debounced conversation search
        scope.launch {
            conversationSearchFlow
                .debounce(debounceInterval)
                .collect { filters ->
                    performConversationSearch(filters)
                }
        }
        
        // Debounced message search
        scope.launch {
            messageSearchFlow
                .debounce(debounceInterval)
                .collect { filters ->
                    performMessageSearch(filters)
                }
        }
        
        // Debounced suggestion search
        scope.launch {
            suggestionSearchFlow
                .debounce(debounceInterval)
                .collect { query ->
                    performSuggestionSearch(query)
                }
        }
    }
    
    // MARK: - Public Search Methods (Debounced)
    
    /**
     * Search conversations with debouncing
     * @param filters Search filters
     * @return Flow that emits search results
     */
    fun searchConversations(filters: ConversationSearchFilters): Flow<Result<SearchConversationsResponse>> {
        return flow {
            conversationSearchFlow.emit(filters)
            // Wait for result
            conversationResultFlows.getOrPut(filters.query) {
                MutableSharedFlow(replay = 1)
            }.first().let { emit(it) }
        }
    }
    
    /**
     * Search messages with debouncing
     * @param filters Search filters
     * @return Flow that emits search results
     */
    fun searchMessages(filters: MessageSearchFilters): Flow<Result<SearchMessagesResponse>> {
        return flow {
            messageSearchFlow.emit(filters)
            // Wait for result
            messageResultFlows.getOrPut(filters.query) {
                MutableSharedFlow(replay = 1)
            }.first().let { emit(it) }
        }
    }
    
    /**
     * Get search suggestions with debouncing
     * @param query Search query
     * @return Flow that emits suggestions
     */
    fun getSuggestions(query: String): Flow<Result<List<String>>> {
        return flow {
            suggestionSearchFlow.emit(query)
            // Wait for result
            suggestionResultFlows.getOrPut(query) {
                MutableSharedFlow(replay = 1)
            }.first().let { emit(it) }
        }
    }
    
    // MARK: - Direct API Methods (No Debouncing)
    
    /**
     * Search conversations immediately without debouncing
     */
    suspend fun searchConversationsImmediate(filters: ConversationSearchFilters): SearchConversationsResponse {
        return apiClient.searchConversations(filters)
    }
    
    /**
     * Search messages in a specific conversation immediately
     */
    suspend fun searchMessagesInConversation(
        conversationId: String,
        query: String,
        limit: Int = 50,
        offset: Int = 0
    ): SearchMessagesResponse {
        return apiClient.searchMessagesInConversation(conversationId, query, limit, offset)
    }
    
    /**
     * Search all messages immediately without debouncing
     */
    suspend fun searchAllMessagesImmediate(filters: MessageSearchFilters): SearchMessagesResponse {
        return apiClient.searchAllMessages(filters)
    }
    
    /**
     * Get suggestions immediately without debouncing
     */
    suspend fun getSuggestionsImmediate(query: String, limit: Int = 10): List<String> {
        val response = apiClient.getSearchSuggestions(query, limit)
        return response.suggestions
    }
    
    // MARK: - Private Implementation
    
    private val conversationResultFlows = mutableMapOf<String, MutableSharedFlow<Result<SearchConversationsResponse>>>()
    private val messageResultFlows = mutableMapOf<String, MutableSharedFlow<Result<SearchMessagesResponse>>>()
    private val suggestionResultFlows = mutableMapOf<String, MutableSharedFlow<Result<List<String>>>>()
    
    private suspend fun performConversationSearch(filters: ConversationSearchFilters) {
        val flow = conversationResultFlows.getOrPut(filters.query) {
            MutableSharedFlow(replay = 1)
        }
        
        try {
            val response = apiClient.searchConversations(filters)
            flow.emit(Result.success(response))
        } catch (e: Exception) {
            flow.emit(Result.failure(e))
        } finally {
            // Clean up after a delay
            scope.launch {
                delay(1000)
                conversationResultFlows.remove(filters.query)
            }
        }
    }
    
    private suspend fun performMessageSearch(filters: MessageSearchFilters) {
        val flow = messageResultFlows.getOrPut(filters.query) {
            MutableSharedFlow(replay = 1)
        }
        
        try {
            val response = apiClient.searchAllMessages(filters)
            flow.emit(Result.success(response))
        } catch (e: Exception) {
            flow.emit(Result.failure(e))
        } finally {
            // Clean up after a delay
            scope.launch {
                delay(1000)
                messageResultFlows.remove(filters.query)
            }
        }
    }
    
    private suspend fun performSuggestionSearch(query: String) {
        val flow = suggestionResultFlows.getOrPut(query) {
            MutableSharedFlow(replay = 1)
        }
        
        try {
            val response = apiClient.getSearchSuggestions(query, 10)
            flow.emit(Result.success(response.suggestions))
        } catch (e: Exception) {
            flow.emit(Result.failure(e))
        } finally {
            // Clean up after a delay
            scope.launch {
                delay(1000)
                suggestionResultFlows.remove(query)
            }
        }
    }
    
    // MARK: - Cancel All Searches
    
    /**
     * Cancel all pending searches
     */
    fun cancelAllSearches() {
        conversationResultFlows.clear()
        messageResultFlows.clear()
        suggestionResultFlows.clear()
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        cancelAllSearches()
        scope.cancel()
    }
}

// MARK: - Data Classes

data class ConversationSearchFilters(
    val query: String,
    val channel: String? = null,
    val status: String? = null,
    val limit: Int = 20,
    val offset: Int = 0
)

data class MessageSearchFilters(
    val query: String,
    val conversationId: String? = null,
    val limit: Int = 50,
    val offset: Int = 0
)

data class SearchConversationsResponse(
    val conversations: List<Any>, // Replace with actual Conversation type
    val total: Int,
    val hasMore: Boolean
)

data class SearchMessagesResponse(
    val messages: List<Any>, // Replace with actual Message type
    val total: Int,
    val hasMore: Boolean
)

data class SearchSuggestionsResponse(
    val suggestions: List<String>
)
