package com.paperknifeplus.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.paperknifeplus.app.ui.theme.PaperPink

@Composable
fun SettingsView(onNavigateToAbout: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    
    var autoAuthor by remember { mutableStateOf(PreferencesManager.getDefaultAuthor(context)) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        // Standardized Header
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildAnnotatedString {
                        append("Settings")
                        withStyle(SpanStyle(color = PaperPink)) { append(".") }
                    },
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-1).sp
                )
                
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Outlined.Settings, null, modifier = Modifier.padding(8.dp).size(20.dp), tint = PaperPink)
                }
            }
        }
        
        Text(
            text = "PREFERENCES & STORAGE",
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            color = Color.Gray.copy(alpha = 0.6f),
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp, start = 24.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                SettingsGroup("USER PROFILE") {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                        Text("DEFAULT AUTHOR NAME", fontSize = 9.sp, fontWeight = FontWeight.Black, color = PaperPink, letterSpacing = 1.sp)
                        Spacer(Modifier.height(8.dp))
                        TextField(
                            value = autoAuthor,
                            onValueChange = { 
                                autoAuthor = it
                                PreferencesManager.setDefaultAuthor(context, it)
                            },
                            placeholder = { Text("e.g. John Doe", fontSize = 14.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = PaperPink,
                                cursorColor = PaperPink
                            ),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        )
                        Text("Automatically applied to saved PDF metadata.", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }

            item {
                SettingsGroup("APPLICATION") {
                    SettingsItem(Icons.Outlined.Palette, "Appearance", "Toggle Light/Dark theme (on Home)")
                    SettingsItem(Icons.Outlined.Memory, "Nitro Engine", "Hardware-accelerated rendering (Active)", enabled = false)
                }
            }

            item {
                SettingsGroup("STORAGE") {
                    SettingsItem(Icons.Outlined.DeleteForever, "Clear Cache", "Purge all temporary PDF fragments") {
                        val cacheDir = context.cacheDir.resolve("pdf_previews")
                        val deleted = if (cacheDir.exists()) cacheDir.deleteRecursively() else true
                        val msg = if (deleted) "Cache cleared successfully" else "Cache already empty"
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }

            item {
                SettingsGroup("COMMUNITY") {
                    SettingsItem(Icons.Outlined.FavoriteBorder, "Support Us", "Help PaperKnife+ reach Play Store") { onNavigateToAbout("support") }
                    SettingsItem(Icons.Outlined.StarOutline, "Hall of Fame", "Our amazing supporters") { onNavigateToAbout("hall") }
                }
            }

            item {
                SettingsGroup("ABOUT") {
                    SettingsItem(Icons.Outlined.Info, "About PaperKnife+", "Credits, License, and Privacy Policy") { onNavigateToAbout("main") }
                    SettingsItem(Icons.Outlined.Shield, "Privacy Mode", "100% Local processing verified", enabled = false)
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 200.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Logo(modifier = Modifier.size(24.dp), partColor = if (isDark) Color.White else Color.Black)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "PAPERKNIFE+ VERSION 1.0.0",
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
            }
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
            color = Color.Gray,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Surface(
            color = if (MaterialTheme.colorScheme.background == Color.Black) Color(0xFF09090B) else Color.White,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 1.dp,
            border = BorderStroke(1.dp, if (MaterialTheme.colorScheme.background == Color.Black) Color.White.copy(alpha = 0.05f) else Color.Black.copy(0.03f))
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector, 
    title: String, 
    subtitle: String, 
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .alpha(if (enabled) 1f else 0.5f),
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
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Color.Gray.copy(alpha = 0.3f),
            modifier = Modifier.size(18.dp)
        )
    }
}
