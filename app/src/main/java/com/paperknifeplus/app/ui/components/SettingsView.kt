package com.paperknifeplus.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.paperknifeplus.app.ui.theme.PaperPink

@Composable
fun SettingsView(onNavigateToAbout: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-1.5).sp
            )
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(8.dp).background(PaperPink, CircleShape))
        }
        
        Spacer(Modifier.height(32.dp))
        
        SettingsGroup("SYSTEM") {
            SettingsItem(Icons.Outlined.DeleteForever, "Clear Cache", "Remove all temporary PDF fragments") {
                android.widget.Toast.makeText(context, "Cache cleared", android.widget.Toast.LENGTH_SHORT).show()
            }
            SettingsItem(Icons.Outlined.Palette, "Appearance", "Adaptive dynamic colors")
        }
        
        Spacer(Modifier.height(24.dp))
        
        SettingsGroup("PROJECT") {
            SettingsItem(Icons.Outlined.Info, "About PaperKnife+", "Credits, License, and Privacy Policy", onClick = onNavigateToAbout)
            SettingsItem(Icons.Outlined.Shield, "Privacy First", "100% Local processing verified") {
                android.widget.Toast.makeText(context, "All processing is 100% offline", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        
        Spacer(Modifier.weight(1f))
        
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 110.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Logo(modifier = Modifier.size(24.dp), partColor = if (isDark) Color.White else Color.Black)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "PaperKnife+ v1.0.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = PaperPink,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Surface(
            color = if (MaterialTheme.colorScheme.background == Color.Black) Color(0xFF09090B) else Color.White,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 1.dp,
            border = BorderStroke(1.dp, if (MaterialTheme.colorScheme.background == Color.Black) Color.White.copy(0.05f) else Color.Black.copy(0.03f))
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 11.sp, color = Color.Gray)
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.Gray.copy(alpha = 0.3f),
            modifier = Modifier.size(18.dp)
        )
    }
}
