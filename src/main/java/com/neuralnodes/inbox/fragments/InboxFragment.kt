package com.neuralnodes.inbox.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.neuralnodes.inbox.NeuralNodesInbox
import com.neuralnodes.inbox.R
import com.neuralnodes.inbox.databinding.FragmentInboxBinding
import com.neuralnodes.inbox.models.Conversation
import com.neuralnodes.inbox.models.ConversationStatus
import com.neuralnodes.inbox.ui.ChatActivity
import com.neuralnodes.inbox.ui.ConversationAdapter
import com.neuralnodes.inbox.viewmodels.InboxViewModel
import kotlinx.coroutines.launch

/**
 * Inbox Fragment - Can be embedded in any Activity
 * 
 * Usage:
 * ```
 * val fragment = InboxFragment.newInstance(sdk)
 * supportFragmentManager.beginTransaction()
 *     .replace(R.id.container, fragment)
 *     .commit()
 * ```
 */
class InboxFragment : Fragment() {
    
    private var _binding: FragmentInboxBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var sdk: NeuralNodesInbox
    private lateinit var viewModel: InboxViewModel
    private lateinit var adapter: ConversationAdapter
    
    companion object {
        fun newInstance(sdk: NeuralNodesInbox): InboxFragment {
            return InboxFragment().apply {
                this.sdk = sdk
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInboxBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize SDK if not already done
        if (!::sdk.isInitialized) {
            sdk = NeuralNodesInbox.getInstance()
        }
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(
            this,
            InboxViewModelFactory(sdk)
        )[InboxViewModel::class.java]
        
        setupUI()
        observeViewModel()
    }
    
    private fun setupUI() {
        // Setup RecyclerView
        adapter = ConversationAdapter { conversation ->
            openConversation(conversation)
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@InboxFragment.adapter
        }
        
        // Setup SwipeRefresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadConversations()
        }
        
        // Setup filter chips
        setupFilterChips()
    }
    
    private fun setupFilterChips() {
        val filters = listOf(
            "All" to ConversationStatus.ALL,
            "Active" to ConversationStatus.ACTIVE,
            "Pending" to ConversationStatus.PENDING,
            "Resolved" to ConversationStatus.RESOLVED
        )
        
        filters.forEach { (label, status) ->
            val chip = Chip(requireContext()).apply {
                text = label
                isCheckable = true
                setOnClickListener {
                    viewModel.setStatusFilter(status)
                }
            }
            binding.chipGroup.addView(chip)
        }
        
        // Select "All" by default
        (binding.chipGroup.getChildAt(0) as Chip).isChecked = true
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.conversations.collect { conversations ->
                adapter.submitList(conversations)
                
                binding.emptyView.visibility = if (conversations.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerView.visibility = if (conversations.isEmpty()) View.GONE else View.VISIBLE
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.swipeRefresh.isRefreshing = isLoading
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                if (error != null) {
                    binding.errorView.visibility = View.VISIBLE
                    binding.errorText.text = error
                    binding.recyclerView.visibility = View.GONE
                } else {
                    binding.errorView.visibility = View.GONE
                }
            }
        }
    }
    
    private fun openConversation(conversation: Conversation) {
        val intent = Intent(requireContext(), ChatActivity::class.java).apply {
            putExtra("API_KEY", sdk.getAPIClient().toString()) // Get API key from SDK
            putExtra("CONVERSATION_ID", conversation.id)
            putExtra("CONVERSATION_NAME", conversation.displayName)
            putExtra("CONVERSATION_STATUS", conversation.status)
        }
        startActivity(intent)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ViewModel Factory
class InboxViewModelFactory(private val sdk: NeuralNodesInbox) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return InboxViewModel(sdk) as T
    }
}
