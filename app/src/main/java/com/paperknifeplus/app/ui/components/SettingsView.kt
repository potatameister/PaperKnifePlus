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

import com.paperknifeplus.app.ui.theme.PaperPink

@Composable
fun SettingsView(onNavigateToAbout: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Text(
            text = "SYSTEM",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = PaperPink,
            letterSpacing = 1.sp
        )
        SettingsItem(Icons.Default.DeleteForever, "Clear Cache", "Remove all temporary PDF fragments")
        
        Spacer(Modifier.height(32.dp))
        Text(
            text = "PROJECT",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = PaperPink,
            letterSpacing = 1.sp
        )
        SettingsItem(Icons.Default.Info, "About PaperKnife+", "Credits, License, and Privacy Policy", onClick = onNavigateToAbout)
        
        Spacer(Modifier.weight(1f))
        Text(
            text = "Version 1.0.0 (Kotlin Edition)",
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 24.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}
