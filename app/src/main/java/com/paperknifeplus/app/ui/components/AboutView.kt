package com.paperknifeplus.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.paperknifeplus.app.ui.theme.PaperPink

@Composable
fun AboutView() {
    val scrollState = rememberScrollState()
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(scrollState).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        PaperKnifeLogo(size = 64, partColor = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(16.dp))
        Text("PaperKnife+", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
        Text("Privacy-First PDF Toolkit", style = MaterialTheme.typography.bodyMedium, color = PaperPink, fontWeight = FontWeight.Black)
        
        Spacer(Modifier.height(32.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("PROTOCOL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = PaperPink, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                Text("All PDF processing happens locally on your device. Zero servers. No data is ever uploaded.", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("HALL OF FAME", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth(), color = Color.Gray, letterSpacing = 1.sp)
        Text("Thank you to our future supporters and the open-source community.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Left, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(24.dp))
        Text("STACK", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth(), color = Color.Gray, letterSpacing = 1.sp)
        listOf("PdfBox-Android" to "Engine", "Compose" to "UI", "Coil" to "Images").forEach { (n, d) ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(n, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(d, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontWeight = FontWeight.Black)
            }
        }

        Spacer(Modifier.height(48.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Made with ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.Favorite, null, tint = PaperPink, modifier = Modifier.size(14.dp))
            Text(" for Privacy", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(100.dp))
    }
}
