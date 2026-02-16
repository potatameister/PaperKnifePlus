package com.paperknifeplus.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsView(onNavigateToAbout: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 32.dp))
        
        Text("APPEARANCE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        SettingsItem(Icons.Default.Palette, "Theme", "System Default")
        
        Spacer(Modifier.height(24.dp))
        Text("SYSTEM", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        SettingsItem(Icons.Default.DeleteForever, "Clear Cache", "Remove temp files")
        
        Spacer(Modifier.height(24.dp))
        Text("PROJECT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        SettingsItem(Icons.Default.Info, "About PaperKnife+", "Credits and License", onClick = onNavigateToAbout)
        
        Spacer(Modifier.weight(1f))
        Text("Version 1.0.0", Modifier.align(Alignment.CenterHorizontally), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.6f))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.3f))
    }
}
