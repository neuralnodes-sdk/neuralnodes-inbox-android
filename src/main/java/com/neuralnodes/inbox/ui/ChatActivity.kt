package com.neuralnodes.inbox.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.neuralnodes.inbox.R
import com.neuralnodes.inbox.databinding.ActivityChatBinding
import com.neuralnodes.inbox.models.Message
import com.neuralnodes.inbox.network.APIClient
import com.neuralnodes.inbox.network.RealtimeClient
import kotlinx.coroutines.launch

/**
 * Chat activity for conversation detail
 */
class ChatActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityChatBinding
    private lateinit var apiClient: APIClient
    private lateinit var realtimeClient: RealtimeClient
    private lateinit var adapter: MessageAdapter
    private lateinit var layoutManager: LinearLayoutManager
    
    private lateinit var conversationId: String
    private var messages = mutableListOf<Message>()
    private var isLoadingMore = false
    private var hasMoreMessages = true
    private val PAGE_SIZE = 50
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get data from intent
        val apiKey = intent.getStringExtra("API_KEY") 
            ?: throw IllegalStateException("API_KEY required")
        conversationId = intent.getStringExtra("CONVERSATION_ID") 
            ?: throw IllegalStateException("CONVERSATION_ID required")
        val conversationName = intent.getStringExtra("CONVERSATION_NAME") ?: "Chat"
        
        apiClient = APIClient(apiKey)
        realtimeClient = RealtimeClient()
        
        setupUI(conversationName)
        loadMessages()
        subscribeToMessages()
        markAsRead()
    }
    
    private fun setupUI(conversationName: String) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = conversationName
        
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
        
        // Setup RecyclerView
        adapter = MessageAdapter()
        layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
            reverseLayout = false
        }
        
        binding.recyclerView.apply {
            layoutManager = this@ChatActivity.layoutManager
            adapter = this@ChatActivity.adapter
            
            // Apply RecyclerView styling
            config?.let { sdkConfig ->
                com.neuralnodes.inbox.utils.UICustomizer.applyBranding(this, sdkConfig)
            }
            
            // Add scroll listener for pagination
            addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    
                    // Check if scrolled to top
                    val firstVisibleItem = this@ChatActivity.layoutManager.findFirstVisibleItemPosition()
                    if (firstVisibleItem == 0 && !isLoadingMore && hasMoreMessages) {
                        loadMoreMessages()
                    }
                }
            })
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
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }
    
    private fun loadMessages() {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                // Load most recent messages first
                val recentMessages = apiClient.getMessages(
                    conversationId, 
                    limit = PAGE_SIZE,
                    offset = 0
                ).sortedBy { it.createdAt }
                
                messages = recentMessages.toMutableList()
                hasMoreMessages = recentMessages.size >= PAGE_SIZE
                
                adapter.submitList(messages)
                scrollToBottom()
            } catch (e: Exception) {
                showError(e.message ?: "Failed to load messages")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun loadMoreMessages() {
        if (isLoadingMore || !hasMoreMessages) return
        
        isLoadingMore = true
        val currentOffset = messages.size
        
        lifecycleScope.launch {
            try {
                val olderMessages = apiClient.getMessages(
                    conversationId,
                    limit = PAGE_SIZE,
                    offset = currentOffset
                ).sortedBy { it.createdAt }
                
                if (olderMessages.isEmpty()) {
                    hasMoreMessages = false
                } else {
                    // Remember scroll position
                    val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
                    val firstVisibleView = layoutManager.findViewByPosition(firstVisiblePosition)
                    val offsetTop = firstVisibleView?.top ?: 0
                    
                    // Add older messages at the beginning
                    messages.addAll(0, olderMessages)
                    adapter.submitList(messages.toList())
                    
                    // Restore scroll position
                    layoutManager.scrollToPositionWithOffset(
                        firstVisiblePosition + olderMessages.size,
                        offsetTop
                    )
                    
                    hasMoreMessages = olderMessages.size >= PAGE_SIZE
                }
            } catch (e: Exception) {
                // Silently fail for pagination
            } finally {
                isLoadingMore = false
            }
        }
    }
    
    private fun subscribeToMessages() {
        lifecycleScope.launch {
            realtimeClient.subscribeToConversation(conversationId).collect { message ->
                runOnUiThread {
                    messages.add(message)
                    adapter.submitList(messages.toList())
                    scrollToBottom()
                }
            }
        }
    }
    
    private fun markAsRead() {
        lifecycleScope.launch {
            try {
                apiClient.markAsRead(conversationId)
            } catch (e: Exception) {
                // Ignore errors for mark as read
            }
        }
    }
    
    private fun sendMessage() {
        val text = binding.inputField.text.toString().trim()
        if (text.isEmpty()) return
        
        // Clear input immediately for better UX
        binding.inputField.text?.clear()
        binding.sendButton.isEnabled = false
        
        // OPTIMISTIC UPDATE: Add message immediately with temporary ID
        val optimisticMessage = Message(
            id = "temp_${System.currentTimeMillis()}",
            conversationId = conversationId,
            messageType = "text",
            messageText = text,
            senderType = "agent",
            senderName = "You",
            senderId = null,
            attachmentUrl = null,
            attachmentType = null,
            attachmentName = null,
            isRead = true,
            createdAt = java.util.Date()
        )
        
        messages.add(optimisticMessage)
        
        // Force adapter update by creating new list and notifying
        val newList = messages.toList()
        adapter.submitList(newList)
        adapter.notifyItemInserted(newList.size - 1)
        scrollToBottom()
        
        println("📤 Optimistic message added: ${optimisticMessage.id}, total messages: ${newList.size}")
        
        lifecycleScope.launch {
            try {
                // Send message to server
                val serverMessage = apiClient.sendMessage(conversationId, text)
                
                // Replace optimistic message with server response
                val index = messages.indexOfFirst { it.id == optimisticMessage.id }
                if (index != -1) {
                    messages[index] = serverMessage
                    
                    // Force update with new list instance
                    val updatedList = messages.toList()
                    adapter.submitList(updatedList)
                    adapter.notifyItemChanged(index)
                    println("🔄 Replaced optimistic message with server message: ${serverMessage.id}")
                } else {
                    // If not found, just add it (shouldn't happen)
                    if (!messages.any { it.id == serverMessage.id }) {
                        messages.add(serverMessage)
                        
                        val updatedList = messages.toList()
                        adapter.submitList(updatedList)
                        adapter.notifyItemInserted(updatedList.size - 1)
                        scrollToBottom()
                        println("➕ Added server message: ${serverMessage.id}")
                    }
                }
                
                println("✅ Message sent successfully: ${serverMessage.id}")
                
            } catch (e: Exception) {
                // ROLLBACK: Remove optimistic message on error
                messages.removeAll { it.id == optimisticMessage.id }
                
                val rollbackList = messages.toList()
                adapter.submitList(rollbackList)
                adapter.notifyDataSetChanged()
                
                // Restore text to input field
                binding.inputField.setText(text)
                showError(e.message ?: "Failed to send message")
                
                println("❌ Failed to send message: ${e.message}")
            } finally {
                binding.inputField.isEnabled = true
                // Re-enable send button if there's text
                binding.sendButton.isEnabled = binding.inputField.text?.isNotEmpty() == true
            }
        }
    }
    
    private fun resolveConversation() {
        lifecycleScope.launch {
            try {
                apiClient.updateStatus(conversationId, "resolved")
                finish()
            } catch (e: Exception) {
                showError(e.message ?: "Failed to resolve conversation")
            }
        }
    }
    
    private fun scrollToBottom() {
        if (messages.isNotEmpty()) {
            binding.recyclerView.smoothScrollToPosition(messages.size - 1)
        }
    }
    
    private fun showError(message: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_chat, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_resolve -> {
                resolveConversation()
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        realtimeClient.unsubscribe(conversationId)
    }
}
