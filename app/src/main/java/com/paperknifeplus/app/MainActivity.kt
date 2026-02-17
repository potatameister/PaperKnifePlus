package com.paperknifeplus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.paperknifeplus.app.ui.components.*
import com.paperknifeplus.app.ui.theme.PaperKnifePlusTheme
import com.paperknifeplus.app.ui.theme.PaperPink

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(applicationContext)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            var isDarkMode by remember { mutableStateOf(false) }
            PaperKnifePlusTheme(darkTheme = isDarkMode) {
                var currentScreen by remember { mutableStateOf("home") }
                val isMainView = currentScreen in listOf("home", "tools", "history", "settings", "about")

                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Content
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            color = Color.Transparent
                        ) {
                            when (currentScreen) {
                                "home" -> HomeView(
                                    isDarkMode = isDarkMode,
                                    onThemeToggle = { isDarkMode = !isDarkMode },
                                    onToolClick = { toolId -> currentScreen = toolId }
                                )
                                "tools" -> ToolsView(onToolClick = { toolId -> currentScreen = toolId })
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
                                
                                else -> HomeView(
                                    isDarkMode = isDarkMode,
                                    onThemeToggle = { isDarkMode = !isDarkMode },
                                    onToolClick = { toolId -> currentScreen = toolId }
                                )
                            }
                        }
                        
                        // Spacer for fixed bottom bar
                        if (isMainView) {
                            Spacer(modifier = Modifier.height(72.dp).navigationBarsPadding())
                        }
                    }

                    // Bottom Bar
                    if (isMainView) {
                        FixedTitanBottomBar(
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
fun FixedTitanBottomBar(
    modifier: Modifier = Modifier,
    currentScreen: String,
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(90.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Red Glow under FAB
        Box(
            modifier = Modifier
                .offset(y = (-32).dp)
                .size(40.dp)
                .background(Brush.radialGradient(listOf(PaperPink.copy(alpha = 0.4f), Color.Transparent)))
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            tonalElevation = 8.dp
        ) {
            Column {
                Divider(modifier = Modifier.fillMaxWidth(), thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Row(
                    modifier = Modifier.fillMaxSize().padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        NavItem(Icons.Outlined.Home, "Home", currentScreen == "home") { onNavigate("home") }
                        NavItem(Icons.Outlined.GridView, "Tools", currentScreen == "tools") { onNavigate("tools") }
                    }
                    
                    Spacer(modifier = Modifier.width(72.dp))
                    
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        NavItem(Icons.Outlined.History, "History", currentScreen == "history") { onNavigate("history") }
                        NavItem(Icons.Outlined.Settings, "Settings", currentScreen == "settings" || currentScreen == "about") { onNavigate("settings") }
                    }
                }
            }
        }

        // Standardized + Button with White Border
        Surface(
            modifier = Modifier
                .offset(y = (-34).dp)
                .size(58.dp)
                .border(2.5.dp, Color.White, RoundedCornerShape(20.dp))
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(20.dp), spotColor = PaperPink)
                .clickable { /* Action */ },
            shape = RoundedCornerShape(20.dp),
            color = PaperPink
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
fun NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) PaperPink else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
            color = if (selected) PaperPink else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            letterSpacing = 0.2.sp
        )
    }
}
