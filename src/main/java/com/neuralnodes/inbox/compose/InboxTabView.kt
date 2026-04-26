package com.neuralnodes.inbox.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuralnodes.inbox.NeuralNodesInbox

/**
 * iOS-style Tab View with Inbox, Live Chat, and Settings
 * Matches iOS SDK bottom tab bar exactly with proper icons and colors
 * 
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
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                // Inbox Tab
                NavigationBarItem(
                    icon = { 
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Inbox",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { 
                        Text(
                            "Inbox",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Medium
                        ) 
                    },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF4A6EE0), // iOS primary blue
                        selectedTextColor = Color(0xFF4A6EE0),
                        unselectedIconColor = Color(0xFF6B7280),
                        unselectedTextColor = Color(0xFF6B7280),
                        indicatorColor = Color(0xFF4A6EE0).copy(alpha = 0.12f)
                    )
                )
                
                // Live Chat Tab
                NavigationBarItem(
                    icon = { 
                        Icon(
                            imageVector = Icons.Default.Message,
                            contentDescription = "Live Chat",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { 
                        Text(
                            "Live Chat",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Medium
                        ) 
                    },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF4A6EE0),
                        selectedTextColor = Color(0xFF4A6EE0),
                        unselectedIconColor = Color(0xFF6B7280),
                        unselectedTextColor = Color(0xFF6B7280),
                        indicatorColor = Color(0xFF4A6EE0).copy(alpha = 0.12f)
                    )
                )
                
                // Settings Tab
                NavigationBarItem(
                    icon = { 
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { 
                        Text(
                            "Settings",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 2) FontWeight.SemiBold else FontWeight.Medium
                        ) 
                    },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF4A6EE0),
                        selectedTextColor = Color(0xFF4A6EE0),
                        unselectedIconColor = Color(0xFF6B7280),
                        unselectedTextColor = Color(0xFF6B7280),
                        indicatorColor = Color(0xFF4A6EE0).copy(alpha = 0.12f)
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

/**
 * Live Chat List View
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveChatListView(sdk: NeuralNodesInbox) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { 
                Text(
                    "Live Chat",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                ) 
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White,
                titleContentColor = Color.Black
            )
        )
        
        EmptyStateView(
            icon = "💬",
            title = "Live Chat",
            message = "Live chat escalations will appear here"
        )
    }
}

/**
 * Settings View
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsView(sdk: NeuralNodesInbox) {
    Column(modifier = Modifier.fillMaxSize()) {
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
                value = com.neuralnodes.inbox.SDKVersion.version
            )
            
            // Full Version
            SettingsItem(
                title = "Full Version",
                value = com.neuralnodes.inbox.SDKVersion.fullVersion
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
}

/**
 * Settings Item Row
 */
@Composable
private fun SettingsItem(title: String, value: String) {
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
        Divider(
            color = Color(0xFFE5E7EB),
            thickness = 0.5.dp
        )
    }
}
