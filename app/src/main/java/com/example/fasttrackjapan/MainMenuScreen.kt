package com.example.fasttrackjapan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fasttrackjapan.ui.theme.FastTrackJapanTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    onBillTrackerClick: () -> Unit,
    onExpirationTrackerClick: () -> Unit,
    onResourceCenterClick: () -> Unit,
    onGarbageScheduleClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fast Track Japan") },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile"
                        )
                    }
                    IconButton(onClick = onSignOutClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
        ) {
            item {
                MainMenuCard(
                    title = "Bill Tracker",
                    subtitle = "Manage paper bills with camera",
                    icon = Icons.Default.Description,
                    onClick = onBillTrackerClick
                )
            }

            item {
                MainMenuCard(
                    title = "Expiration Tracker",
                    subtitle = "Track document expiration dates",
                    icon = Icons.Default.Badge,
                    onClick = onExpirationTrackerClick
                )
            }

            item {
                MainMenuCard(
                    title = "Resource Center",
                    subtitle = "Official forms and procedure guides",
                    icon = Icons.Default.Info,
                    onClick = onResourceCenterClick
                )
            }
            
            item {
                MainMenuCard(
                    title = "Garbage Schedule",
                    subtitle = "Collection days & reminders for your ward",
                    icon = Icons.Default.Delete,
                    onClick = onGarbageScheduleClick
                )
            }

            // Placeholder for the remaining future component
            item {
                MainMenuCard(
                    title = "Component 5",
                    subtitle = "Coming soon",
                    icon = Icons.Default.Lock,
                    enabled = false,
                    onClick = {}
                )
            }
        }
    }
}

@Composable
fun MainMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surfaceVariant 
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary 
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant 
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}
@Composable
@Preview(showBackground = true)
fun MainMenuScreenPreview(){
    FastTrackJapanTheme() {
        MainMenuScreen(
            onBillTrackerClick = {},
            onExpirationTrackerClick = {},
            onResourceCenterClick = {},
            onGarbageScheduleClick = {},
            onProfileClick = {},
            onSignOutClick = {}
        )
    }
}
