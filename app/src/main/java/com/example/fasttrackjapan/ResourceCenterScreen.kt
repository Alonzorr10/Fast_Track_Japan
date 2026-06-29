package com.example.fasttrackjapan

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ResourceLink(
    val title: String,
    val description: String,
    val url: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceCenterScreen(
    onBack: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    
    val resources = listOf(
        ResourceLink(
            "My Number Card Application",
            "Official PDF guide and application form for the My Number Card system.",
            "https://www.kojinbango-card.go.jp/en-kofu-yuso/"
        ),
        ResourceLink(
            "National Pension Exemption",
            "Information and forms for applying for National Pension premium exemptions.",
            "https://www.nenkin.go.jp/international/english/nationalpension/nationalpension.html#cms04"
        ),
        ResourceLink(
            "Driver's License Exchange",
            "Guide on how to switch your foreign driver's license to a Japanese one (Gaimen Kirikae).",
            "https://www.keishicho.metro.tokyo.lg.jp/multilingual/english/traffic_safety/drivers_licenses/index.html"
        ),
        ResourceLink(
            "Visa Extension/Change Form",
            "Official Immigration Services Agency forms for extending or changing your status of residence.",
            "https://www.moj.go.jp/isa/applications/procedures/16-3.html?hl=en"
        ),
        ResourceLink(
            "Move-out/Move-in Notification",
            "General information on the procedures for moving between different wards or cities.",
            "https://www.soumu.go.jp/main_sosiki/jichi_gyousei/c-gyousei/zairyu/english/move_index.html"
        ),
        ResourceLink(
            title = "Residence Card Renewal",
            description = "Form to apply for a renewal on one's Residence Card",
            url = "https://www.moj.go.jp/isa/content/001426709.pdf"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resource Center") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                text = "Quick access to official forms and guides for common procedures in Japan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(resources) { resource ->
                    ResourceCard(
                        resource = resource,
                        onClick = { uriHandler.openUri(resource.url) }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Note: Procedures may vary slightly by ward. Always check your local City Hall (Kuyakusho) website for specific local requirements.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResourceCard(
    resource: ResourceLink,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resource.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = resource.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Open Link",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
