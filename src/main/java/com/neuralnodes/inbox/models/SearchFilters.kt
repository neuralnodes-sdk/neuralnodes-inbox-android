package com.neuralnodes.inbox.models

import java.text.SimpleDateFormat
import java.util.*

/**
 * Filters for conversation search
 * Exact match to iOS SDK ConversationSearchFilters
 */
data class ConversationSearchFilters(
    val query: String,
    val channel: String? = null,
    val status: String? = null,
    val liveChat: Boolean? = null,
    val limit: Int = 50,
    val offset: Int = 0
) {
    fun toQueryMap(clientId: String): Map<String, String> {
        val params = mutableMapOf(
            "client_id" to clientId,
            "q" to query,
            "limit" to limit.toString(),
            "offset" to offset.toString()
        )
        
        channel?.let { params["channel"] = it }
        status?.let { params["status"] = it }
        liveChat?.let { params["live_chat"] = if (it) "true" else "false" }
        
        return params
    }
}

/**
 * Filters for message search
 * Exact match to iOS SDK MessageSearchFilters
 */
data class MessageSearchFilters(
    val query: String,
    val channel: String? = null,
    val senderType: String? = null,
    val dateFrom: Date? = null,
    val dateTo: Date? = null,
    val limit: Int = 50,
    val offset: Int = 0
) {
    fun toQueryMap(clientId: String): Map<String, String> {
        val params = mutableMapOf(
            "client_id" to clientId,
            "q" to query,
            "limit" to limit.toString(),
            "offset" to offset.toString()
        )
        
        channel?.let { params["channel"] = it }
        senderType?.let { params["sender_type"] = it }
        
        // ISO8601 date formatting
        val iso8601Format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        
        dateFrom?.let { params["date_from"] = iso8601Format.format(it) }
        dateTo?.let { params["date_to"] = iso8601Format.format(it) }
        
        return params
    }
}
