package com.neuralnodes.inbox.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuralnodes.inbox.NeuralNodesInbox
import com.neuralnodes.inbox.SDKVersion

/**
 * Settings view for the inbox
 * Exact match to iOS SDK InboxSettingsView
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxSettingsView(
    sdk: NeuralNodesInbox,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    "Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White,
                titleContentColor = Color.Black
            )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // SDK Version
            SettingsItem(
                title = "SDK Version",
                value = SDKVersion.VERSION
            )
            
            // Full Version
            SettingsItem(
                title = "Full Version",
                value = SDKVersion.FULL_VERSION
            )
            
            // Dark Mode
            val config = sdk.getConfig()
            if (config?.features?.darkMode == true) {
                SettingsItem(
                    title = "Dark Mode",
                    value = "Enabled"
                )
            }
            
            // Features Section
            config?.features?.let { features ->
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Features",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                SettingsItem(
                    title = "Push Notifications",
                    value = if (features.pushNotifications) "Enabled" else "Disabled"
                )
                
                SettingsItem(
                    title = "File Upload",
                    value = if (features.fileUpload) "Enabled" else "Disabled"
                )
                
                SettingsItem(
                    title = "Typing Indicators",
                    value = if (features.typingIndicators) "Enabled" else "Disabled"
                )
                
                SettingsItem(
                    title = "Read Receipts",
                    value = if (features.readReceipts) "Enabled" else "Disabled"
                )
                
                SettingsItem(
                    title = "Conversation Search",
                    value = if (features.conversationSearch) "Enabled" else "Disabled"
                )
            }
            
            // Limits Section
            config?.limits?.let { limits ->
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Limits",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                SettingsItem(
                    title = "Max File Size",
                    value = "${limits.maxFileSizeMB} MB"
                )
                
                SettingsItem(
                    title = "Max Message Length",
                    value = "${limits.maxMessageLength} chars"
                )
                
                SettingsItem(
                    title = "Messages Per Page",
                    value = limits.messagesPerPage.toString()
                )
            }
        }
    }
}

/**
 * Settings item row
 * Exact match to iOS SDK SettingsItem
 */
@Composable
private fun SettingsItem(
    title: String,
    value: String
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF6B7280)
            )
        }
        HorizontalDivider(
            color = Color(0xFFE5E7EB),
            thickness = 0.5.dp
        )
    }
}
