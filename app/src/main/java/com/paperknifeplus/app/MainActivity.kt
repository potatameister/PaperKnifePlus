package com.paperknifeplus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
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
        WindowCompat.setDecorFitsSystemWindows(window, false) // Edge-to-edge
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
                                
                                else -> HomeView(onToolClick = { toolId -> currentScreen = toolId })
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
fun Header(isDarkMode: Boolean, onThemeToggle: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Logo(modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "PaperKnife",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.8).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .graphicsLayer { alpha = dotAlpha }
                            .background(Color(0xFF06D6A0), CircleShape)
                    )
                }
                Text(
                    text = "SECURE ENGINE",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black,
                    color = PaperPink,
                    letterSpacing = 1.2.sp
                )
            }
        }
        
        IconButton(
            onClick = onThemeToggle,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "Toggle Theme",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
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
            .height(100.dp), // Increased height to prevent cropping
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            tonalElevation = 8.dp
        ) {
            Column {
                Divider(modifier = Modifier.fillMaxWidth(), thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Row(
                    modifier = Modifier.fillMaxSize().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavItem(Icons.Default.Home, "Home", currentScreen == "home") { onNavigate("home") }
                    NavItem(Icons.Default.GridView, "Tools", currentScreen == "tools") { onNavigate("tools") }
                    
                    Spacer(modifier = Modifier.width(72.dp))
                    
                    NavItem(Icons.Default.History, "History", currentScreen == "history") { onNavigate("history") }
                    NavItem(Icons.Default.Settings, "Settings", currentScreen == "settings" || currentScreen == "about") { onNavigate("settings") }
                }
            }
        }

        // Standardized + Button (Elevated and centered)
        Surface(
            modifier = Modifier
                .offset(y = (-32).dp)
                .size(60.dp)
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(20.dp), spotColor = PaperPink)
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
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) PaperPink else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
            color = if (selected) PaperPink else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            letterSpacing = 0.5.sp
        )
    }
}
