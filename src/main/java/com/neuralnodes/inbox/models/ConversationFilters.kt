package com.neuralnodes.inbox.models

/**
 * Filters for conversation queries
 * Exact match to iOS SDK ConversationFilters
 */
data class ConversationFilters(
    val channel: String? = null,
    val status: String? = null,
    val assignedTo: String? = null,
    val limit: Int = 50,
    val offset: Int = 0
) {
    /**
     * Convert to URL query parameters
     */
    fun toQueryMap(): Map<String, String> {
        val params = mutableMapOf<String, String>()
        
        channel?.let { params["channel"] = it }
        status?.let { params["status"] = it }
        assignedTo?.let { params["assigned_to"] = it }
        params["limit"] = limit.toString()
        params["offset"] = offset.toString()
        
        return params
    }
}
