package com.paperknifeplus.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperknifeplus.app.ui.theme.PaperPink

@Composable
fun SettingsView(
    isDarkMode: Boolean,
    onThemeChange: (Int) -> Unit,
    onNavigateToAbout: (String) -> Unit
) {
    val context = LocalContext.current
    val isDarkBg = MaterialTheme.colorScheme.background == Color.Black
    
    var autoAuthor by remember { mutableStateOf(PreferencesManager.getDefaultAuthor(context)) }
    var currentTheme by remember { mutableIntStateOf(PreferencesManager.getTheme(context)) }
    var historyRetention by remember { mutableIntStateOf(PreferencesManager.getHistoryRetention(context)) }
    
    var showRetentionDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

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
                Logo(modifier = Modifier.size(24.dp), partColor = if (isDarkBg) Color.White else Color.Black)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 0.dp, end = 20.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                SettingsGroup("APPEARANCE") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOption("System", Icons.Outlined.SettingsSuggest, currentTheme == 0, Modifier.weight(1f)) {
                            currentTheme = 0
                            onThemeChange(0)
                        }
                        ThemeOption("Light", Icons.Outlined.LightMode, currentTheme == 1, Modifier.weight(1f)) {
                            currentTheme = 1
                            onThemeChange(1)
                        }
                        ThemeOption("Dark", Icons.Outlined.DarkMode, currentTheme == 2, Modifier.weight(1f)) {
                            currentTheme = 2
                            onThemeChange(2)
                        }
                    }
                }
            }

            item {
                SettingsGroup("USER PROFILE") {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                        Text("DEFAULT AUTHOR", fontSize = 9.sp, fontWeight = FontWeight.Black, color = PaperPink, letterSpacing = 1.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("This name is automatically added to the 'Author' field in the metadata of every PDF you save.", fontSize = 10.sp, color = Color.Gray)
                        Spacer(Modifier.height(12.dp))
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
                    }
                }
            }

            item {
                SettingsGroup("PRIVACY & STORAGE") {
                    SettingsItem(Icons.Outlined.AutoDelete, "Auto-Delete History", when(historyRetention) {
                        0 -> "Unlimited (Never delete)"
                        1 -> "Delete after 24 hours"
                        7 -> "Delete after 7 days"
                        else -> "Delete after 30 days"
                    }) {
                        showRetentionDialog = true
                    }
                    SettingsItem(Icons.Outlined.DeleteSweep, "Clear History", "Wipe all session history now") {
                        showClearConfirm = true
                    }
                    SettingsItem(Icons.Outlined.DeleteForever, "Clear Cache", "Purge all temporary documents and previews") {
                        val cacheDir = context.cacheDir
                        val extCacheDir = context.externalCacheDir
                        val previewsDir = cacheDir.resolve("pdf_previews")
                        
                        var success = true
                        try {
                            if (previewsDir.exists()) previewsDir.deleteRecursively()
                            if (cacheDir.exists()) {
                                cacheDir.listFiles()?.forEach { 
                                    if (it.name != "pdf_previews") it.deleteRecursively() 
                                }
                            }
                            if (extCacheDir?.exists() == true) extCacheDir.deleteRecursively()
                        } catch (e: Exception) { success = false }
                        
                        if (success) Toast.makeText(context, "Cache wiped successfully", Toast.LENGTH_SHORT).show()
                        else Toast.makeText(context, "Partial cache clear", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            item {
                SettingsGroup("COMMUNITY") {
                    SettingsItem(Icons.Outlined.Info, "About PaperKnife+", "Our mission and how it works") { onNavigateToAbout("main") }
                    SettingsItem(Icons.Outlined.FavoriteBorder, "Support Us", "Help reach the Play Store", accentColor = Color(0xFFF43F5E)) { onNavigateToAbout("support") }
                    SettingsItem(Icons.Outlined.StarOutline, "Hall of Fame", "The legends who made this possible", accentColor = Color(0xFFFFD700)) { onNavigateToAbout("hall") }
                }
            }

            item {
                Column(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("OFFLINE VERSION 1.0", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                }
            }
        }
    }

    // DIALOGS
    if (showRetentionDialog) {
        AlertDialog(
            onDismissRequest = { showRetentionDialog = false },
            title = { Text("Auto-Delete History", fontWeight = FontWeight.Black) },
            text = {
                Column {
                    val options = listOf(0 to "Unlimited", 1 to "24 Hours", 7 to "7 Days", 30 to "30 Days")
                    options.forEach { (days, label) ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                historyRetention = days
                                PreferencesManager.setHistoryRetention(context, days)
                                showRetentionDialog = false
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = historyRetention == days, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(label, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear History?", fontWeight = FontWeight.Black) },
            text = { Text("This will permanently remove all entries from your history tab. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        SessionManager.clearHistory()
                        showClearConfirm = false
                        Toast.makeText(context, "History wiped", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.2f), contentColor = MaterialTheme.colorScheme.onSurface)
                ) { Text("WIPE EVERYTHING", fontWeight = FontWeight.Black) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("CANCEL", color = PaperPink) }
            }
        )
    }
}

@Composable
fun ThemeOption(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) PaperPink.copy(alpha = 0.1f) else Color.Transparent,
        border = BorderStroke(1.dp, if (selected) PaperPink else Color.Gray.copy(alpha = 0.1f))
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = if (selected) PaperPink else Color.Gray)
            Spacer(Modifier.height(8.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (selected) PaperPink else Color.Gray)
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
    accentColor: Color? = null,
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
                .background(accentColor?.copy(alpha = 0.1f) ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = accentColor ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
