package com.paperknifeplus.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AboutView() {
    val scrollState = rememberScrollState()
    Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        PaperKnifeLogo(64, MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(16.dp))
        Text("PaperKnife+", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text("Privacy-First PDF Toolkit", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        
        Spacer(Modifier.height(32.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("PROTOCOL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                Text("All PDF processing happens locally on your device. Zero servers. No data is ever uploaded.", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("HALL OF FAME", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth())
        Text("Thank you to our future supporters and the open-source community.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)

        Spacer(Modifier.height(24.dp))
        Text("STACK", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth())
        listOf("PdfBox-Android" to "Engine", "Compose" to "UI", "Coil" to "Images").forEach { (n, d) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), Arrangement.SpaceBetween) {
                Text(n, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(d, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
            }
        }

        Spacer(Modifier.height(48.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Made with ", fontSize = 12.sp)
            Icon(Icons.Default.Favorite, null, tint = Color(0xFFF43F5E), modifier = Modifier.size(14.dp))
            Text(" for Privacy", fontSize = 12.sp)
        }
    }
}
