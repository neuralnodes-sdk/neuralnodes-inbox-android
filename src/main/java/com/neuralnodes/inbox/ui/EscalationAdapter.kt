package com.neuralnodes.inbox.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.neuralnodes.inbox.R
import com.neuralnodes.inbox.models.Escalation
import com.neuralnodes.inbox.models.EscalationStatus
import java.text.SimpleDateFormat
import java.util.*

class EscalationAdapter(
    private val onEscalationClick: (Escalation) -> Unit
) : RecyclerView.Adapter<EscalationAdapter.ViewHolder>() {
    
    private var escalations = listOf<Escalation>()
    private var selectedEscalationId: String? = null
    
    fun submitList(list: List<Escalation>) {
        escalations = list
        notifyDataSetChanged()
    }
    
    fun setSelectedEscalation(escalationId: String?) {
        selectedEscalationId = escalationId
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_escalation, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(escalations[position])
    }
    
    override fun getItemCount() = escalations.size
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.nameText)
        private val statusText: TextView = itemView.findViewById(R.id.statusText)
        private val statusDot: View = itemView.findViewById(R.id.statusDot)
        private val messagePreview: TextView = itemView.findViewById(R.id.messagePreview)
        private val timeText: TextView = itemView.findViewById(R.id.timeText)
        private val unreadBadge: View = itemView.findViewById(R.id.unreadBadge)
        private val unreadCount: TextView = itemView.findViewById(R.id.unreadCount)
        private val avatarText: TextView = itemView.findViewById(R.id.avatarText)
        
        fun bind(escalation: Escalation) {
            nameText.text = escalation.displayName
            
            // Set avatar initial
            val initial = escalation.displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            avatarText.text = initial
            
            // Set status with appropriate color
            when (escalation.status) {
                EscalationStatus.ACTIVE -> {
                    statusText.text = "Active"
                    statusDot.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF10B981.toInt())
                }
                EscalationStatus.PENDING -> {
                    statusText.text = "Pending"
                    statusDot.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFF59E0B.toInt())
                }
                EscalationStatus.RESOLVED -> {
                    statusText.text = "Resolved"
                    statusDot.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF6B7280.toInt())
                }
                EscalationStatus.CLOSED -> {
                    statusText.text = "Closed"
                    statusDot.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF6B7280.toInt())
                }
            }
            
            messagePreview.text = escalation.lastMessagePreview ?: "No messages yet"
            timeText.text = formatTime(escalation.lastMessageAt ?: escalation.createdAt)
            
            // Show unread badge
            if (escalation.unreadCount > 0) {
                unreadBadge.visibility = View.VISIBLE
                unreadCount.text = if (escalation.unreadCount > 9) "9+" else escalation.unreadCount.toString()
            } else {
                unreadBadge.visibility = View.GONE
            }
            
            // Highlight selected escalation with elevation
            if (escalation.id == selectedEscalationId) {
                (itemView as com.google.android.material.card.MaterialCardView).apply {
                    strokeWidth = 4
                    strokeColor = itemView.context.getColor(android.R.color.holo_blue_light)
                }
            } else {
                (itemView as com.google.android.material.card.MaterialCardView).apply {
                    strokeWidth = 0
                }
            }
            
            itemView.setOnClickListener {
                onEscalationClick(escalation)
            }
        }
        
        private fun formatTime(date: Date): String {
            val now = Date()
            val diff = now.time - date.time
            
            return when {
                diff < 60000 -> "now"
                diff < 3600000 -> "${diff / 60000}m"
                diff < 86400000 -> "${diff / 3600000}h"
                diff < 604800000 -> "${diff / 86400000}d"
                else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
            }
        }
    }
}
