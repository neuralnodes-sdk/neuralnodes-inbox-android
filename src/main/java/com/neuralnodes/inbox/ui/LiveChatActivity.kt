package com.neuralnodes.inbox.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.neuralnodes.inbox.R
import com.neuralnodes.inbox.databinding.ActivityLiveChatBinding
import com.neuralnodes.inbox.models.Escalation
import com.neuralnodes.inbox.models.ChatMessage
import com.neuralnodes.inbox.network.APIClient
import com.neuralnodes.inbox.network.PusherClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Live Chat activity for managing escalations
 * Production-ready with proper memory management
 */
class LiveChatActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLiveChatBinding
    private lateinit var apiClient: APIClient
    private lateinit var pusherClient: PusherClient
    private lateinit var escalationAdapter: EscalationAdapter
    private lateinit var messageAdapter: ChatMessageAdapter
    
    private var clientId: String? = null
    private var activeEscalation: Escalation? = null
    private var escalations = mutableListOf<Escalation>()
    private var allEscalations = mutableListOf<Escalation>() // Store all escalations
    private var messages = mutableListOf<ChatMessage>()
    private var currentStatusFilter: String? = null // Track current filter
    
    // Coroutine jobs for cleanup
    private var dashboardSubscriptionJob: Job? = null
    private var escalationSubscriptionJob: Job? = null
    private var typingTimeoutHandler: Handler? = null
    private var typingTimeoutRunnable: Runnable? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLiveChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get data from intent
        val apiKey = intent.getStringExtra("API_KEY") 
            ?: throw IllegalStateException("API_KEY required")
        clientId = intent.getStringExtra("CLIENT_ID")
            ?: throw IllegalStateException("CLIENT_ID required")
        
        apiClient = APIClient(apiKey)
        
        // Get PusherClient from singleton SDK instance
        try {
            val sdk = com.neuralnodes.inbox.NeuralNodesInbox.getInstance()
            pusherClient = sdk.getPusherClient() ?: throw IllegalStateException("Pusher not initialized")
            println("✅ Using SDK PusherClient")
        } catch (e: Exception) {
            println("❌ Failed to get PusherClient from SDK: ${e.message}")
            // Fallback: create new instance (shouldn't happen in production)
            pusherClient = PusherClient(apiKey, "https://api.neuralnodes.space")
        }
        
        setupUI()
        loadEscalations()
        subscribeToClientDashboard()
    }
    
    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Live Chat"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Setup status filter chips
        setupStatusFilters()
        
        // Setup escalations RecyclerView
        escalationAdapter = EscalationAdapter { escalation ->
            selectEscalation(escalation)
        }
        binding.escalationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@LiveChatActivity)
            adapter = escalationAdapter
        }
        
        // Setup messages RecyclerView
        messageAdapter = ChatMessageAdapter()
        binding.messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@LiveChatActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
        
        // Setup send button
        binding.sendButton.setOnClickListener {
            sendMessage()
        }
        
        // Enable send button only when text is entered
        binding.inputField.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.sendButton.isEnabled = !s.isNullOrBlank()
                
                // Send typing indicator
                if (!s.isNullOrBlank() && activeEscalation != null) {
                    sendTypingIndicator(true)
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        
        // Monitor connection status
        monitorConnectionStatus()
    }
    
    private fun setupStatusFilters() {
        val filters = listOf(
            "All" to null,
            "Active" to "active",
            "Pending" to "pending",
            "Resolved" to "resolved",
            "Closed" to "closed"
        )
        
        filters.forEach { (label, status) ->
            val chip = com.google.android.material.chip.Chip(this).apply {
                text = label
                isCheckable = true
                setOnClickListener {
                    currentStatusFilter = status
                    filterEscalations()
                }
            }
            binding.statusChipGroup.addView(chip)
        }
        
        // Select "All" by default
        (binding.statusChipGroup.getChildAt(0) as com.google.android.material.chip.Chip).isChecked = true
    }
    
    private fun filterEscalations() {
        val filtered = if (currentStatusFilter == null) {
            allEscalations
        } else {
            allEscalations.filter { it.status.name.lowercase() == currentStatusFilter }
        }
        
        escalations = filtered.toMutableList()
        escalationAdapter.submitList(escalations)
        
        if (escalations.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
        }
    }
    
    private fun loadEscalations() {
        binding.escalationsProgressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val loadedEscalations = apiClient.getEscalations(limit = 50)
                allEscalations = loadedEscalations.toMutableList()
                filterEscalations() // Apply current filter
            } catch (e: Exception) {
                showError("Failed to load escalations: ${e.message}")
            } finally {
                binding.escalationsProgressBar.visibility = View.GONE
            }
        }
    }
    
    private fun selectEscalation(escalation: Escalation) {
        // Unsubscribe from previous escalation
        escalationSubscriptionJob?.cancel()
        
        activeEscalation = escalation
        escalationAdapter.setSelectedEscalation(escalation.id)
        
        // Update toolbar title
        supportActionBar?.title = escalation.displayName
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Show chat UI, hide escalations list
        binding.escalationsContainer.visibility = View.GONE
        binding.chatContainer.visibility = View.VISIBLE
        binding.inputContainer.visibility = View.VISIBLE
        
        // Load messages
        loadMessages(escalation.id)
        
        // Subscribe to escalation updates
        subscribeToEscalation(escalation.id)
        
        // Mark messages as read
        markMessagesAsRead(escalation.id)
    }
    
    private fun loadMessages(escalationId: String) {
        binding.messagesProgressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val loadedMessages = apiClient.getEscalationMessages(escalationId, limit = 100)
                messages = loadedMessages.sortedBy { it.createdAt }.toMutableList()
                messageAdapter.submitList(messages)
                scrollToBottom()
            } catch (e: Exception) {
                showError("Failed to load messages: ${e.message}")
            } finally {
                binding.messagesProgressBar.visibility = View.GONE
            }
        }
    }
    
    private fun subscribeToClientDashboard() {
        dashboardSubscriptionJob = lifecycleScope.launch {
            try {
                pusherClient.subscribeToClientDashboard(
                    clientId!!,
                    onNewEscalation = { data ->
                        runOnUiThread {
                            // Refresh escalations list
                            loadEscalations()
                        }
                    }
                ).collect { event ->
                    println("Dashboard event: $event")
                }
            } catch (e: Exception) {
                println("Failed to subscribe to dashboard: ${e.message}")
            }
        }
    }
    
    private fun subscribeToEscalation(escalationId: String) {
        println("🔔 Subscribing to escalation: $escalationId")
        
        escalationSubscriptionJob = lifecycleScope.launch {
            try {
                pusherClient.subscribeToEscalation(
                    escalationId,
                    onNewMessage = { message ->
                        println("📨 New message received: ${message.id} from ${message.senderType}")
                        
                        runOnUiThread {
                            // Check if message already exists (avoid duplicates)
                            val existingIndex = messages.indexOfFirst { it.id == message.id }
                            
                            if (existingIndex == -1) {
                                // New message - add it
                                messages.add(message)
                                messages.sortBy { it.createdAt }
                                messageAdapter.submitList(messages.toList())
                                scrollToBottom()
                                
                                println("✅ Message added to list: ${message.id}")
                                
                                // Mark as read if from user
                                if (message.isFromUser) {
                                    markMessagesAsRead(escalationId)
                                }
                            } else {
                                // Message exists - update it (in case of status change)
                                messages[existingIndex] = message
                                messageAdapter.submitList(messages.toList())
                                println("🔄 Message updated: ${message.id}")
                            }
                        }
                    },
                    onTypingIndicator = { indicator ->
                        runOnUiThread {
                            if (indicator.isTyping) {
                                binding.typingIndicator.text = "${indicator.senderName} is typing..."
                                binding.typingIndicator.visibility = View.VISIBLE
                                
                                // Hide after 3 seconds
                                Handler(Looper.getMainLooper()).postDelayed({
                                    binding.typingIndicator.visibility = View.GONE
                                }, 3000)
                            } else {
                                binding.typingIndicator.visibility = View.GONE
                            }
                        }
                    },
                    onStatusChanged = { data ->
                        println("📊 Status changed: $data")
                        runOnUiThread {
                            // Refresh escalations list
                            loadEscalations()
                        }
                    }
                ).collect { event ->
                    println("🔔 Escalation event: $event")
                }
            } catch (e: Exception) {
                println("❌ Failed to subscribe to escalation: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    private fun sendMessage() {
        val text = binding.inputField.text.toString().trim()
        if (text.isEmpty() || activeEscalation == null) return
        
        // Clear input immediately for better UX
        binding.inputField.text?.clear()
        binding.sendButton.isEnabled = false
        
        // Stop typing indicator
        sendTypingIndicator(false)
        
        // OPTIMISTIC UPDATE: Add message immediately with temporary ID
        val optimisticMessage = ChatMessage(
            id = "temp_${System.currentTimeMillis()}",
            escalationId = activeEscalation!!.id,
            messageType = "text",
            messageText = text,
            senderType = "agent",
            senderName = "You",
            senderId = null,
            attachmentUrl = null,
            attachmentType = null,
            isRead = true,
            readAt = null,
            createdAt = java.util.Date()
        )
        
        messages.add(optimisticMessage)
        messageAdapter.submitList(messages.toList())
        scrollToBottom()
        
        lifecycleScope.launch {
            try {
                // Send message to server
                val serverMessage = apiClient.sendClientMessage(activeEscalation!!.id, text)
                
                // Replace optimistic message with server response
                val index = messages.indexOfFirst { it.id == optimisticMessage.id }
                if (index != -1) {
                    messages[index] = serverMessage
                    messageAdapter.submitList(messages.toList())
                } else {
                    // If not found, just add it (shouldn't happen)
                    if (!messages.any { it.id == serverMessage.id }) {
                        messages.add(serverMessage)
                        messageAdapter.submitList(messages.toList())
                        scrollToBottom()
                    }
                }
                
                println("✅ Message sent successfully: ${serverMessage.id}")
                
            } catch (e: Exception) {
                // ROLLBACK: Remove optimistic message on error
                messages.removeAll { it.id == optimisticMessage.id }
                messageAdapter.submitList(messages.toList())
                
                // Restore text to input field
                binding.inputField.setText(text)
                
                // Show error with retry option
                showErrorWithRetry("Failed to send message: ${e.message}") {
                    // Retry: set text back and user can send again
                    binding.inputField.setText(text)
                    binding.sendButton.isEnabled = true
                }
                
                println("❌ Failed to send message: ${e.message}")
            } finally {
                binding.inputField.isEnabled = true
                // Re-enable send button if there's text
                binding.sendButton.isEnabled = binding.inputField.text?.isNotEmpty() == true
            }
        }
    }
    
    private fun sendTypingIndicator(isTyping: Boolean) {
        if (activeEscalation == null) return
        
        // Cancel existing timeout
        typingTimeoutRunnable?.let { typingTimeoutHandler?.removeCallbacks(it) }
        
        if (isTyping) {
            // Send typing indicator
            lifecycleScope.launch {
                try {
                    apiClient.sendClientTypingIndicator(activeEscalation!!.id, true)
                } catch (e: Exception) {
                    // Silently fail
                }
            }
            
            // Set timeout to stop typing after 2 seconds
            typingTimeoutHandler = Handler(Looper.getMainLooper())
            typingTimeoutRunnable = Runnable {
                sendTypingIndicator(false)
            }
            typingTimeoutHandler?.postDelayed(typingTimeoutRunnable!!, 2000)
        } else {
            // Send stop typing
            lifecycleScope.launch {
                try {
                    apiClient.sendClientTypingIndicator(activeEscalation!!.id, false)
                } catch (e: Exception) {
                    // Silently fail
                }
            }
        }
    }
    
    private fun markMessagesAsRead(escalationId: String) {
        lifecycleScope.launch {
            try {
                apiClient.markEscalationMessagesRead(escalationId)
                
                // Update local messages
                messages.forEach { msg ->
                    if (msg.isFromUser && !msg.isRead) {
                        // Message is now read
                    }
                }
                
                // Refresh escalations to update unread count
                loadEscalations()
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }
    
    private fun monitorConnectionStatus() {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val state = pusherClient.getConnectionState()
                if (state != "CONNECTED") {
                    binding.connectionStatusCard.visibility = View.VISIBLE
                    binding.connectionStatusText.text = "Connection: $state"
                } else {
                    binding.connectionStatusCard.visibility = View.GONE
                }
                handler.postDelayed(this, 5000)
            }
        }
        handler.post(runnable)
    }
    
    private fun scrollToBottom() {
        if (messages.isNotEmpty()) {
            binding.messagesRecyclerView.smoothScrollToPosition(messages.size - 1)
        }
    }
    
    private fun showError(message: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun showErrorWithRetry(message: String, onRetry: () -> Unit) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("Retry") { _, _ -> onRetry() }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // CRITICAL: Cleanup to prevent memory leaks
        dashboardSubscriptionJob?.cancel()
        escalationSubscriptionJob?.cancel()
        typingTimeoutRunnable?.let { typingTimeoutHandler?.removeCallbacks(it) }
        
        // Unsubscribe from all channels
        activeEscalation?.let { pusherClient.unsubscribe("private-escalation-${it.id}") }
        clientId?.let { pusherClient.unsubscribe("private-client-$it") }
        
        println("✅ LiveChatActivity destroyed and cleaned up")
    }
    
    override fun onSupportNavigateUp(): Boolean {
        // If viewing a chat, go back to escalations list
        if (binding.chatContainer.visibility == View.VISIBLE) {
            showEscalationsList()
            return true
        }
        // Otherwise, finish activity
        finish()
        return true
    }
    
    private fun showEscalationsList() {
        // Cancel escalation subscription
        escalationSubscriptionJob?.cancel()
        
        // Reset active escalation
        activeEscalation = null
        escalationAdapter.setSelectedEscalation(null)
        
        // Update toolbar
        supportActionBar?.title = "Live Chat"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Show escalations list, hide chat
        binding.escalationsContainer.visibility = View.VISIBLE
        binding.chatContainer.visibility = View.GONE
        
        // Clear messages
        messages.clear()
        messageAdapter.submitList(messages)
    }
    
    override fun onBackPressed() {
        if (binding.chatContainer.visibility == View.VISIBLE) {
            showEscalationsList()
        } else {
            super.onBackPressed()
        }
    }
}
