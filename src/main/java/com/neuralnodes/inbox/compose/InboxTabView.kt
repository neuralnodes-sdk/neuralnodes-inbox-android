package com.neuralnodes.inbox.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.neuralnodes.inbox.NeuralNodesInbox

/**
 * Tab View with Inbox, Live Chat, and Settings
 * This is a CONVENIENCE wrapper - developers can also use individual views
 * 
 * Usage:
 * ```
 * InboxTabView(sdk = NeuralNodesInbox.getInstance())
 * ```
 */
@Composable
fun InboxTabView(
    sdk: NeuralNodesInbox,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Email, contentDescription = "Inbox") },
                    label = { Text("Inbox") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF4A6EE0),
                        selectedTextColor = Color(0xFF4A6EE0),
                        indicatorColor = Color(0xFFEFF6FF)
                    )
                )
                
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Send, contentDescription = "Live Chat") },
                    label = { Text("Live Chat") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF4A6EE0),
                        selectedTextColor = Color(0xFF4A6EE0),
                        indicatorColor = Color(0xFFEFF6FF)
                    )
                )
                
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF4A6EE0),
                        selectedTextColor = Color(0xFF4A6EE0),
                        indicatorColor = Color(0xFFEFF6FF)
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> InboxView(sdk = sdk, showToolbar = true)
                1 -> LiveChatListView(sdk = sdk)
                2 -> SettingsView(sdk = sdk)
            }
        }
    }
}

@Composable
private fun LiveChatListView(sdk: NeuralNodesInbox) {
    // Placeholder for Live Chat list
    EmptyStateView(
        icon = "💬",
        title = "Live Chat",
        message = "Live chat escalations will appear here"
    )
}

@Composable
private fun SettingsView(sdk: NeuralNodesInbox) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // SDK Version
        SettingsItem(
            title = "SDK Version",
            value = NeuralNodesInbox.VERSION
        )
        
        // Dark Mode
        val config = sdk.getConfig()
        if (config?.features?.darkMode == true) {
            SettingsItem(
                title = "Dark Mode",
                value = "Enabled"
            )
        }
    }
}

@Composable
private fun SettingsItem(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
    Divider()
}
