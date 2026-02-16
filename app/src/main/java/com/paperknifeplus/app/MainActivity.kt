package com.paperknifeplus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.paperknifeplus.app.ui.components.*
import com.paperknifeplus.app.ui.theme.PaperKnifePlusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PaperKnifePlusTheme {
                var currentScreen by remember { mutableStateOf("home") }
                val isMainView = currentScreen in listOf("home", "tools", "history", "settings", "about")

                Scaffold(
                    bottomBar = {
                        if (isMainView) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.background,
                                tonalElevation = 8.dp
                            ) {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                    label = { Text("Home") },
                                    selected = currentScreen == "home",
                                    onClick = { currentScreen = "home" }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.GridView, contentDescription = null) },
                                    label = { Text("Tools") },
                                    selected = currentScreen == "tools",
                                    onClick = { currentScreen = "tools" }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                                    label = { Text("History") },
                                    selected = currentScreen == "history",
                                    onClick = { currentScreen = "history" }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                    label = { Text("Settings") },
                                    selected = currentScreen == "settings" || currentScreen == "about",
                                    onClick = { currentScreen = "settings" }
                                )
                            }
                        }
                    }
                ) { padding ->
                    Surface(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        color = MaterialTheme.colorScheme.background
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
                }
            }
        }
    }
}
