package com.neuralnodes.inbox.utils

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Helper class for showing confirmation dialogs
 */
object ConfirmationDialog {
    
    fun show(
        context: Context,
        title: String,
        message: String,
        positiveButtonText: String = "Confirm",
        negativeButtonText: String = "Cancel",
        onConfirm: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { dialog, _ ->
                onConfirm()
                dialog.dismiss()
            }
            .setNegativeButton(negativeButtonText) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }
    
    fun showResolveConfirmation(context: Context, onConfirm: () -> Unit) {
        show(
            context = context,
            title = "Resolve Conversation",
            message = "Are you sure you want to mark this conversation as resolved? The conversation will be moved to the resolved section.",
            positiveButtonText = "Resolve",
            onConfirm = onConfirm
        )
    }
    
    fun showCloseConfirmation(context: Context, onConfirm: () -> Unit) {
        show(
            context = context,
            title = "Close Conversation",
            message = "Are you sure you want to close this conversation? Closed conversations can be reopened later if needed.",
            positiveButtonText = "Close",
            onConfirm = onConfirm
        )
    }
    
    fun showUnresolveConfirmation(context: Context, onConfirm: () -> Unit) {
        show(
            context = context,
            title = "Unresolve Conversation",
            message = "Are you sure you want to move this conversation back to active? It will appear in your active conversations list.",
            positiveButtonText = "Unresolve",
            onConfirm = onConfirm
        )
    }
    
    fun showReopenConfirmation(context: Context, onConfirm: () -> Unit) {
        show(
            context = context,
            title = "Reopen Conversation",
            message = "Are you sure you want to reopen this conversation? It will be moved back to active conversations.",
            positiveButtonText = "Reopen",
            onConfirm = onConfirm
        )
    }
    
    fun showEndChatConfirmation(context: Context, onConfirm: () -> Unit) {
        show(
            context = context,
            title = "End Chat",
            message = "Are you sure you want to end this live chat session? This will disconnect the customer from the chat.",
            positiveButtonText = "End Chat",
            onConfirm = onConfirm
        )
    }
}
