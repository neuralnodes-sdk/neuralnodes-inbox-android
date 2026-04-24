package com.neuralnodes.inbox.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.neuralnodes.inbox.R
import com.neuralnodes.inbox.databinding.ActivityInboxBinding
import com.neuralnodes.inbox.models.Conversation
import com.neuralnodes.inbox.network.APIClient
import com.neuralnodes.inbox.network.RealtimeClient
import kotlinx.coroutines.launch

/**
 * Main inbox activity showing list of conversations
 */
class InboxActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityInboxBinding
    private lateinit var apiClient: APIClient
    private lateinit var realtimeClient: RealtimeClient
    private lateinit var adapter: ConversationAdapter
    
    private var conversations = listOf<Conversation>()
    private var currentFilter: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInboxBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get API key from intent
        val apiKey = intent.getStringExtra("API_KEY") 
            ?: throw IllegalStateException("API_KEY required")
        
        apiClient = APIClient(apiKey)
        realtimeClient = RealtimeClient()
        
        setupUI()
        loadConversations()
        subscribeToUpdates()
    }
    
    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Inbox"
        
        // Setup RecyclerView
        adapter = ConversationAdapter { conversation ->
            openChat(conversation)
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@InboxActivity)
            adapter = this@InboxActivity.adapter
        }
        
        // Setup SwipeRefreshLayout
        binding.swipeRefresh.setOnRefreshListener {
            loadConversations()
        }
        
        // Setup filter chips
        setupFilterChips()
    }
    
    private fun setupFilterChips() {
        val filters = listOf(
            "All" to null,
            "Active" to "active",
            "Pending" to "pending",
            "Resolved" to "resolved"
        )
        
        filters.forEach { (label, status) ->
            val chip = Chip(this).apply {
                text = label
                isCheckable = true
                setOnClickListener {
                    currentFilter = status
                    loadConversations()
                }
            }
            binding.chipGroup.addView(chip)
        }
        
        // Select "All" by default
        (binding.chipGroup.getChildAt(0) as Chip).isChecked = true
    }
    
    private fun loadConversations() {
        binding.swipeRefresh.isRefreshing = true
        binding.errorView.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                // Fetch all conversations
                val allConversations = apiClient.getConversations(
                    status = currentFilter,
                    limit = 100
                )
                
                // Filter out "web" channel - those belong to Live Chat only
                conversations = allConversations.filter { conversation ->
                    conversation.channel.lowercase() != "web"
                }
                
                adapter.submitList(conversations)
                
                binding.emptyView.visibility = if (conversations.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerView.visibility = if (conversations.isEmpty()) View.GONE else View.VISIBLE
                
            } catch (e: Exception) {
                showError(e.message ?: "Failed to load conversations")
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
    
    private fun subscribeToUpdates() {
        realtimeClient.subscribeToInbox {
            runOnUiThread {
                loadConversations()
            }
        }
    }
    
    private fun openChat(conversation: Conversation) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("API_KEY", intent.getStringExtra("API_KEY"))
            putExtra("CONVERSATION_ID", conversation.id)
            putExtra("CONVERSATION_NAME", conversation.displayName)
        }
        startActivity(intent)
    }
    
    private fun showError(message: String) {
        binding.errorView.visibility = View.VISIBLE
        binding.errorText.text = message
        binding.recyclerView.visibility = View.GONE
        binding.emptyView.visibility = View.GONE
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    override fun onDestroy() {
        super.onDestroy()
        realtimeClient.disconnect()
    }
}
