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
        
        println("🚀 LiveChatActivity started")
        println("   API Key: ${apiKey.take(10)}...")
        println("   Client ID: $clientId")
        
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
        
        // Apply COMPLETE UI customizations from SDK config
        val sdk = com.neuralnodes.inbox.NeuralNodesInbox.getInstance()
        val config = sdk.getConfig()
        
        config?.let { sdkConfig ->
            // Apply complete activity configuration
            com.neuralnodes.inbox.utils.UICustomizer.applyActivityConfiguration(binding.root, sdkConfig)
            
            // Apply toolbar styling
            com.neuralnodes.inbox.utils.UICustomizer.applyBranding(binding.toolbar, sdkConfig)
            
            // Update placeholder text
            binding.inputField.hint = com.neuralnodes.inbox.utils.UICustomizer.getCustomText(sdkConfig, "input_placeholder")
            
            // Apply send button customization
            binding.sendButton.text = com.neuralnodes.inbox.utils.UICustomizer.getCustomText(sdkConfig, "send_button")
            com.neuralnodes.inbox.utils.UICustomizer.applyBranding(binding.sendButton, sdkConfig)
        }
        
        // Setup status filter chips
        setupStatusFilters()
        
        // Setup escalations RecyclerView
        escalationAdapter = EscalationAdapter { escalation ->
            selectEscalation(escalation)
        }
        binding.escalationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@LiveChatActivity)
            adapter = escalationAdapter
            
            // Apply RecyclerView styling
            config?.let { sdkConfig ->
                com.neuralnodes.inbox.utils.UICustomizer.applyBranding(this, sdkConfig)
            }
        }
        
        // Setup messages RecyclerView
        messageAdapter = ChatMessageAdapter()
        binding.messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@LiveChatActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
            
            // Apply RecyclerView styling
            config?.let { sdkConfig ->
                com.neuralnodes.inbox.utils.UICustomizer.applyBranding(this, sdkConfig)
            }
        }
        
        // Setup send button
        binding.sendButton.setOnClickListener {
            sendMessage()
        }
        
        // Setup action buttons
        binding.resolveButton.setOnClickListener {
            resolveEscalation()
        }
        
        binding.endChatButton.setOnClickListener {
            endEscalation()
        }
        
        // Add debug refresh button (temporary)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                android.R.id.home -> {
                    onSupportNavigateUp()
                    true
                }
                else -> {
                    // Manual refresh for debugging
                    println("🔄 Manual refresh triggered")
                    loadEscalations()
                    true
                }
            }
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
        
        println("📊 Filter results:")
        println("   Current filter: $currentStatusFilter")
        println("   All escalations: ${allEscalations.size}")
        println("   Filtered escalations: ${escalations.size}")
        
        if (escalations.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.escalationsRecyclerView.visibility = View.GONE
            println("📭 Showing empty state")
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.escalationsRecyclerView.visibility = View.VISIBLE
            println("📋 Showing ${escalations.size} escalations")
        }
    }
    
    private fun loadEscalations() {
        binding.escalationsProgressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                println("🔄 Loading escalations for client: $clientId")
                val loadedEscalations = apiClient.getEscalations(limit = 50)
                println("📊 Received ${loadedEscalations.size} escalations")
                
                allEscalations = loadedEscalations.toMutableList()
                filterEscalations() // Apply current filter
                
                println("✅ Escalations loaded successfully")
            } catch (e: Exception) {
                println("❌ Failed to load escalations: ${e.message}")
                e.printStackTrace()
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
        
        // Hide filter chips when viewing chat
        binding.filterChipsContainer.visibility = View.GONE
        
        // Show chat UI, hide escalations list
        binding.escalationsContainer.visibility = View.GONE
        binding.chatContainer.visibility = View.VISIBLE
        binding.inputContainer.visibility = View.VISIBLE
        
        // Show/hide action buttons based on status
        updateActionButtons(escalation.status.name.lowercase())
        
        // Load messages
        loadMessages(escalation.id)
        
        // Subscribe to escalation updates
        subscribeToEscalation(escalation.id)
        
        // Mark messages as read
        markMessagesAsRead(escalation.id)
    }
    
    private fun updateActionButtons(status: String) {
        when (status) {
            "active", "pending" -> {
                // Show Resolve and End Chat buttons
                binding.resolveButton.visibility = View.VISIBLE
                binding.endChatButton.visibility = View.VISIBLE
                binding.actionButtonsCard.visibility = View.VISIBLE
                
                // Adjust RecyclerView margin to account for action buttons
                val params = binding.messagesRecyclerView.layoutParams as android.view.ViewGroup.MarginLayoutParams
                params.bottomMargin = dpToPx(140) // 72dp input + 68dp action buttons
                binding.messagesRecyclerView.layoutParams = params
                
                // Adjust typing indicator margin
                val typingParams = binding.typingIndicatorCard.layoutParams as android.widget.FrameLayout.LayoutParams
                typingParams.bottomMargin = dpToPx(148)
                binding.typingIndicatorCard.layoutParams = typingParams
            }
            else -> {
                // Hide action buttons for resolved/closed chats
                binding.resolveButton.visibility = View.GONE
                binding.endChatButton.visibility = View.GONE
                binding.actionButtonsCard.visibility = View.GONE
                
                // Reset RecyclerView margin to just input height
                val params = binding.messagesRecyclerView.layoutParams as android.view.ViewGroup.MarginLayoutParams
                params.bottomMargin = dpToPx(72) // Just input container
                binding.messagesRecyclerView.layoutParams = params
                
                // Reset typing indicator margin
                val typingParams = binding.typingIndicatorCard.layoutParams as android.widget.FrameLayout.LayoutParams
                typingParams.bottomMargin = dpToPx(80)
                binding.typingIndicatorCard.layoutParams = typingParams
            }
        }
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
    
    private fun resolveEscalation() {
        if (activeEscalation == null) return
        
        com.neuralnodes.inbox.utils.ConfirmationDialog.showResolveConfirmation(this) {
            lifecycleScope.launch {
                try {
                    apiClient.resolveEscalation(activeEscalation!!.id)
                    showSuccess("Chat resolved")
                    
                    // Update local status
                    activeEscalation = activeEscalation!!.copy(
                        status = com.neuralnodes.inbox.models.EscalationStatus.RESOLVED
                    )
                    updateActionButtons("resolved")
                    
                    // Refresh escalations list
                    loadEscalations()
                } catch (e: Exception) {
                    showError("Failed to resolve chat: ${e.message}")
                }
            }
        }
    }
    
    private fun endEscalation() {
        if (activeEscalation == null) return
        
        com.neuralnodes.inbox.utils.ConfirmationDialog.showEndChatConfirmation(this) {
            lifecycleScope.launch {
                try {
                    apiClient.endEscalation(activeEscalation!!.id)
                    showSuccess("Chat ended")
                    
                    // Go back to escalations list
                    showEscalationsList()
                    
                    // Refresh escalations list
                    loadEscalations()
                } catch (e: Exception) {
                    showError("Failed to end chat: ${e.message}")
                }
            }
        }
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
                println("🔔 Subscribing to client dashboard: $clientId")
                pusherClient.subscribeToClientDashboard(
                    clientId!!,
                    onNewEscalation = { data ->
                        println("📨 New escalation received: $data")
                        runOnUiThread {
                            // Refresh escalations list immediately
                            loadEscalations()
                        }
                    }
                ).collect { event ->
                    println("📡 Dashboard event: $event")
                }
            } catch (e: Exception) {
                println("❌ Failed to subscribe to dashboard: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    private fun subscribeToEscalation(escalationId: String) {
        println("🔔 Subscribing to escalation: $escalationId")
        
        escalationSubscriptionJob = lifecycleScope.launch {
            try {
                pusherClient.subscribeToEscalation(
                    escalationId,
                    onMessage = { message ->
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
                    onTyping = { isTyping ->
                        runOnUiThread {
                            if (isTyping) {
                                binding.typingIndicator.text = "Customer is typing..."
                                binding.typingIndicator.visibility = View.VISIBLE
                                
                                // Hide after 3 seconds
                                Handler(Looper.getMainLooper()).postDelayed({
                                    binding.typingIndicator.visibility = View.GONE
                                }, 3000)
                            } else {
                                binding.typingIndicator.visibility = View.GONE
                            }
                        }
                    }
                )
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
        messages.sortBy { it.createdAt }
        
        // Force adapter update by creating new list and notifying
        val newList = messages.toList()
        messageAdapter.submitList(newList)
        messageAdapter.notifyItemInserted(newList.size - 1)
        scrollToBottom()
        
        println("📤 Optimistic message added: ${optimisticMessage.id}, total messages: ${newList.size}")
        
        lifecycleScope.launch {
            try {
                // Send message to server
                val serverMessage = apiClient.sendClientMessage(activeEscalation!!.id, text)
                
                // Replace optimistic message with server response
                val index = messages.indexOfFirst { it.id == optimisticMessage.id }
                if (index != -1) {
                    messages[index] = serverMessage
                    messages.sortBy { it.createdAt }
                    
                    // Force update with new list instance
                    val newList = messages.toList()
                    messageAdapter.submitList(newList)
                    messageAdapter.notifyItemChanged(index)
                    println("🔄 Replaced optimistic message with server message: ${serverMessage.id}")
                } else {
                    // If not found, just add it (shouldn't happen)
                    if (!messages.any { it.id == serverMessage.id }) {
                        messages.add(serverMessage)
                        messages.sortBy { it.createdAt }
                        
                        val newList = messages.toList()
                        messageAdapter.submitList(newList)
                        messageAdapter.notifyItemInserted(newList.size - 1)
                        scrollToBottom()
                        println("➕ Added server message: ${serverMessage.id}")
                    }
                }
                
                println("✅ Message sent successfully: ${serverMessage.id}")
                
            } catch (e: Exception) {
                // ROLLBACK: Remove optimistic message on error
                messages.removeAll { it.id == optimisticMessage.id }
                
                val newList = messages.toList()
                messageAdapter.submitList(newList)
                messageAdapter.notifyDataSetChanged()
                
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
        println("❌ Error: $message")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNeutralButton("Retry") { _, _ -> 
                loadEscalations()
            }
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
    
    private fun showSuccess(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // CRITICAL: Cleanup to prevent memory leaks
        dashboardSubscriptionJob?.cancel()
        escalationSubscriptionJob?.cancel()
        typingTimeoutRunnable?.let { typingTimeoutHandler?.removeCallbacks(it) }
        
        // Unsubscribe from all channels
        activeEscalation?.let { pusherClient.unsubscribeFromEscalation(it.id) }
        
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
        
        // Show filter chips when viewing list
        binding.filterChipsContainer.visibility = View.VISIBLE
        
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
