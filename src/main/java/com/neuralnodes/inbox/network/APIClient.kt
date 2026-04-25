package com.neuralnodes.inbox.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.neuralnodes.inbox.models.*
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

/**
 * HTTP API client for NeuralNodes backend
 */
class APIClient(private val apiKey: String, baseURL: String = "https://api.neuralnodes.space") {
    
    private val api: InboxAPI
    private val gson = Gson()
    
    init {
        val gsonBuilder = GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .create()
        
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("X-API-Key", apiKey)
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }
        
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(baseURL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gsonBuilder))
            .build()
        
        api = retrofit.create(InboxAPI::class.java)
    }
    
    // SDK Configuration
    suspend fun getConfig(): SDKConfig = api.getConfig().config
    
    // Conversations
    suspend fun getConversations(
        channel: String? = null,
        status: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<Conversation> = api.getConversations(channel, status, limit, offset).conversations
    
    // Messages
    suspend fun getMessages(conversationId: String, limit: Int = 100, offset: Int = 0): List<Message> =
        api.getMessages(conversationId, limit, offset).messages
    
    suspend fun sendMessage(conversationId: String, text: String): Message =
        api.sendMessage(conversationId, SendMessageRequest(text)).message
    
    suspend fun markAsRead(conversationId: String) {
        api.markAsRead(conversationId)
    }
    
    // Conversation Management
    suspend fun updateStatus(conversationId: String, status: String) {
        api.updateStatus(conversationId, mapOf("status" to status))
    }
    
    suspend fun closeConversation(conversationId: String) {
        api.updateStatus(conversationId, mapOf("status" to "closed"))
    }
    
    suspend fun unresolveConversation(conversationId: String) {
        api.updateStatus(conversationId, mapOf("status" to "active"))
    }
    
    suspend fun reopenConversation(conversationId: String) {
        api.updateStatus(conversationId, mapOf("status" to "active"))
    }
    
    // Device Registration
    suspend fun registerDevice(token: String, platform: String, deviceInfo: Map<String, String> = emptyMap()) {
        val deviceInfoJson = gson.toJson(deviceInfo)
        api.registerDevice(token, platform, deviceInfoJson)
    }
    
    // ========== LIVE CHAT ENDPOINTS ==========
    
    // Escalations
    suspend fun getEscalations(
        status: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<Escalation> {
        println("🔍 Loading escalations with status: $status, limit: $limit")
        val response = api.getEscalations(status, limit, offset)
        println("📊 Loaded ${response.escalations.size} escalations")
        return response.escalations
    }
    
    // Escalation Messages
    suspend fun getEscalationMessages(
        escalationId: String,
        limit: Int = 100,
        offset: Int = 0
    ): List<ChatMessage> {
        println("📨 Loading messages for escalation: $escalationId")
        val response = api.getEscalationMessages(escalationId, limit, offset)
        println("📊 Loaded ${response.messages.size} messages")
        return response.messages
    }
    
    suspend fun sendClientMessage(
        escalationId: String,
        messageText: String
    ): ChatMessage {
        println("📤 Sending message to escalation: $escalationId")
        val response = api.sendClientMessage(
            escalationId,
            SendChatMessageRequest(messageText)
        )
        println("✅ Message sent: ${response.message.id}")
        return response.message
    }
    
    suspend fun markEscalationMessagesRead(escalationId: String) {
        api.markEscalationMessagesRead(escalationId)
    }
    
    // Escalation Status
    suspend fun updateEscalationStatus(
        escalationId: String,
        status: String,
        resolutionNotes: String? = null
    ) {
        api.updateEscalationStatus(
            escalationId,
            UpdateEscalationStatusRequest(status, resolutionNotes)
        )
    }
    
    suspend fun resolveEscalation(escalationId: String) {
        api.updateEscalationStatus(
            escalationId,
            UpdateEscalationStatusRequest("resolved", null)
        )
    }
    
    suspend fun endEscalation(escalationId: String) {
        api.updateEscalationStatus(
            escalationId,
            UpdateEscalationStatusRequest("closed", "Chat ended by agent")
        )
    }
    
    // Typing Indicator
    suspend fun sendClientTypingIndicator(escalationId: String, isTyping: Boolean) {
        api.sendClientTypingIndicator(
            escalationId,
            mapOf("is_typing" to isTyping)
        )
    }
    
    // Client Info (for getting client ID)
    suspend fun getClientInfo(): ClientInfoResponse = api.getClientInfo()
}

interface InboxAPI {
    // SDK Config
    @GET("/sdk/config")
    suspend fun getConfig(): SDKConfigResponse
    
    // Inbox - Conversations
    @GET("/client-portal/inbox/conversations")
    suspend fun getConversations(
        @Query("channel") channel: String?,
        @Query("status") status: String?,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): ConversationsResponse
    
    @GET("/client-portal/inbox/conversations/{id}/messages")
    suspend fun getMessages(
        @Path("id") conversationId: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): MessagesResponse
    
    @POST("/client-portal/inbox/conversations/{id}/messages")
    suspend fun sendMessage(
        @Path("id") conversationId: String,
        @Body request: SendMessageRequest
    ): SendMessageResponse
    
    @POST("/client-portal/inbox/conversations/{id}/mark-read")
    suspend fun markAsRead(@Path("id") conversationId: String)
    
    @PUT("/client-portal/inbox/conversations/{id}/status")
    suspend fun updateStatus(
        @Path("id") conversationId: String,
        @Body body: Map<String, String>
    )
    
    // Device Registration
    @POST("/sdk/register-device")
    suspend fun registerDevice(
        @Query("device_token") deviceToken: String,
        @Query("platform") platform: String,
        @Query("device_info") deviceInfo: String
    )
    
    // ========== LIVE CHAT ENDPOINTS ==========
    
    // Escalations
    @GET("/client-portal/live-chat/escalations")
    suspend fun getEscalations(
        @Query("status") status: String?,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): EscalationsResponse
    
    @GET("/client-portal/live-chat/escalations/{id}/messages")
    suspend fun getEscalationMessages(
        @Path("id") escalationId: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): ChatMessagesResponse
    
    @POST("/client-portal/live-chat/escalations/{id}/messages")
    suspend fun sendClientMessage(
        @Path("id") escalationId: String,
        @Body request: SendChatMessageRequest
    ): SendChatMessageResponse
    
    @POST("/client-portal/live-chat/escalations/{id}/mark-read")
    suspend fun markEscalationMessagesRead(@Path("id") escalationId: String)
    
    @PUT("/client-portal/live-chat/escalations/{id}/status")
    suspend fun updateEscalationStatus(
        @Path("id") escalationId: String,
        @Body request: UpdateEscalationStatusRequest
    )
    
    @POST("/client-portal/live-chat/escalations/{id}/typing")
    suspend fun sendClientTypingIndicator(
        @Path("id") escalationId: String,
        @Body body: Map<String, Boolean>
    )
    
    @GET("/client-portal/info")
    suspend fun getClientInfo(): ClientInfoResponse
}

data class ClientInfoResponse(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("slug") val slug: String?,
    @SerializedName("api_key") val apiKey: String?,
    @SerializedName("chat_api_key") val chatApiKey: String?
)

data class RegisterDeviceRequest(
    @SerializedName("device_token") val deviceToken: String,
    @SerializedName("platform") val platform: String,
    @SerializedName("device_info") val deviceInfo: Map<String, String>
)
