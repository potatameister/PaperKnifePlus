package com.paperknifeplus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperknifeplus.app.ui.components.*
import com.paperknifeplus.app.ui.theme.PaperKnifePlusTheme
import com.paperknifeplus.app.ui.theme.PaperPink

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDarkMode by remember { mutableStateOf(false) }
            PaperKnifePlusTheme(darkTheme = isDarkMode) {
                var currentScreen by remember { mutableStateOf("home") }
                val isMainView = currentScreen in listOf("home", "tools", "history", "settings", "about")

                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header
                        if (isMainView) {
                            Header(
                                isDarkMode = isDarkMode,
                                onThemeToggle = { isDarkMode = !isDarkMode }
                            )
                        }

                        // Content
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            color = Color.Transparent
                        ) {
                            when (currentScreen) {
                                "home" -> HomeView(onToolClick = { toolId -> currentScreen = toolId })
                                "tools" -> HomeView(onToolClick = { toolId -> currentScreen = toolId })
                                "history" -> Box(Modifier.fillMaxSize()) { Text("History coming soon", Modifier.padding(16.dp)) }
                                "settings" -> SettingsView(onNavigateToAbout = { currentScreen = "about" })
                                "about" -> AboutView()
                                
                                "merge" -> MergeView(onBack = { currentScreen = "home" })
                                "split" -> SplitView(onBack = { currentScreen = "home" })
                                "rotate" -> RotateView(onBack = { currentScreen = "home" })
                                "rearrange" -> RearrangeView(onBack = { currentScreen = "home" })
                                "protect" -> ProtectView(onBack = { currentScreen = "home" })
                                "unlock" -> UnlockView(onBack = { currentScreen = "home" })
                                "img2pdf" -> ImageToPdfView(onBack = { currentScreen = "home" })
                                "pdf2img" -> PdfToImageView(onBack = { currentScreen = "home" })
                                
                                else -> HomeView(onToolClick = { toolId -> currentScreen = toolId })
                            }
                        }
                        
                        // Spacer for bottom bar
                        if (isMainView) {
                            Spacer(modifier = Modifier.height(90.dp))
                        }
                    }

                    // Bottom Bar
                    if (isMainView) {
                        TitanBottomBar(
                            modifier = Modifier.align(Alignment.BottomCenter),
                            currentScreen = currentScreen,
                            onNavigate = { currentScreen = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Header(isDarkMode: Boolean, onThemeToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Logo(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PaperKnife",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF06D6A0))
                )
            }
            Text(
                text = "SECURE ENGINE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = PaperPink,
                letterSpacing = 1.sp
            )
        }
        
        IconButton(
            onClick = onThemeToggle,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "Toggle Theme",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun TitanBottomBar(
    modifier: Modifier = Modifier,
    currentScreen: String,
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = modifier
            .padding(bottom = 24.dp)
            .fillMaxWidth()
            .height(70.dp),
        contentAlignment = Alignment.Center
    ) {
        // Rounded bar background
        Surface(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxSize(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavItem(Icons.Default.Home, "Home", currentScreen == "home") { onNavigate("home") }
                NavItem(Icons.Default.GridView, "Tools", currentScreen == "tools") { onNavigate("tools") }
                
                Spacer(modifier = Modifier.width(56.dp)) // Space for FAB
                
                NavItem(Icons.Default.History, "History", currentScreen == "history") { onNavigate("history") }
                NavItem(Icons.Default.Settings, "Settings", currentScreen == "settings" || currentScreen == "about") { onNavigate("settings") }
            }
        }
        
        // Floating Center FAB
        FloatingActionButton(
            onClick = { /* Action for FAB */ },
            modifier = Modifier
                .offset(y = (-30).dp)
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp)),
            containerColor = PaperPink,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) PaperPink else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (selected) PaperPink else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}
