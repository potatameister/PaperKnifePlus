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
import androidx.compose.runtime.CompositionLocalProvider
import coil.ImageLoader
import coil.compose.LocalImageLoader
import com.paperknifeplus.app.data.image.PdfPageFetcher
import com.paperknifeplus.app.ui.components.*
import com.paperknifeplus.app.ui.theme.PaperKnifePlusTheme
import com.paperknifeplus.app.ui.theme.PaperPink
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(applicationContext)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // --- NITRO ENGINE: GLOBAL IMAGE LOADER ---
        val nitroImageLoader = ImageLoader.Builder(applicationContext)
            .components { add(PdfPageFetcher.Factory(applicationContext)) }
            .memoryCache {
                coil.memory.MemoryCache.Builder(applicationContext)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(applicationContext.cacheDir.resolve("pdf_previews"))
                    .maxSizeBytes(150 * 1024 * 1024)
                    .build()
            }
            .crossfade(false)
            .build()

        setContent {
            var isDarkMode by remember { mutableStateOf(false) }
            CompositionLocalProvider(coil.compose.LocalImageLoader provides nitroImageLoader) {
                PaperKnifePlusTheme(darkTheme = isDarkMode) {
                    var currentTool by remember { mutableStateOf<String?>(null) }
                    
                    val mainScreens = listOf("home", "tools", "history", "settings")
                    val pagerState = androidx.compose.foundation.pager.rememberPagerState { mainScreens.size }
                    val scope = rememberCoroutineScope()

                    Box(modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Main Navigation Pager (Slidable & Lazy)
                            androidx.compose.foundation.pager.HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.weight(1f),
                                userScrollEnabled = currentTool == null,
                                beyondBoundsPageCount = 0
                            ) { page ->
                                when (mainScreens[page]) {
                                    "home" -> HomeView(
                                        isDarkMode = isDarkMode,
                                        onThemeToggle = { isDarkMode = !isDarkMode },
                                        onToolClick = { currentTool = it }
                                    )
                                    "tools" -> ToolsView(onToolClick = { currentTool = it })
                                    "history" -> HistoryView()
                                    "settings" -> SettingsView(onNavigateToAbout = { currentTool = "about" })
                                }
                            }
                            
                            // Bottom Nav Spacer
                            if (currentTool == null) {
                                Spacer(modifier = Modifier.height(72.dp).navigationBarsPadding())
                            }
                        }

                        // Bottom Bar
                        if (currentTool == null) {
                            FixedTitanBottomBar(
                                modifier = Modifier.align(Alignment.BottomCenter),
                                currentScreen = mainScreens[pagerState.currentPage],
                                onNavigate = { screen ->
                                    val index = mainScreens.indexOf(screen)
                                    if (index != -1) {
                                        scope.launch { pagerState.animateScrollToPage(index) }
                                    }
                                }
                            )
                        }

                        // NITRO TOOL OVERLAY
                        androidx.compose.animation.AnimatedVisibility(
                            visible = currentTool != null,
                            enter = androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.fadeOut()
                        ) {
                            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                                when (currentTool) {
                                    "about" -> AboutView()
                                    "merge" -> MergeView(onBack = { currentTool = null })
                                    "split" -> SplitView(onBack = { currentTool = null })
                                    "compress" -> CompressView(onBack = { currentTool = null })
                                    "repair" -> RepairView(onBack = { currentTool = null })
                                    "rotate" -> RotateView(onBack = { currentTool = null })
                                    "rearrange" -> RearrangeView(onBack = { currentTool = null })
                                    "protect" -> ProtectView(onBack = { currentTool = null })
                                    "unlock" -> UnlockView(onBack = { currentTool = null })
                                    "grayscale" -> GrayscaleView(onBack = { currentTool = null })
                                    "metadata" -> MetadataView(onBack = { currentTool = null })
                                    "img2pdf" -> ImageToPdfView(onBack = { currentTool = null })
                                    "pdf2img" -> PdfToImageView(onBack = { currentTool = null })
                                    "pdf2text" -> PdfToTextView(onBack = { currentTool = null })
                                    "extract-images" -> ExtractImagesView(onBack = { currentTool = null })
                                    "page-numbers" -> ComingSoonView("Page Numbers", onBack = { currentTool = null })
                                    "watermark" -> ComingSoonView("Watermark", onBack = { currentTool = null })
                                    "signature" -> ComingSoonView("Signature", onBack = { currentTool = null })
                                }
                            }
                        }
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
                .border(2.5.dp, if (MaterialTheme.colorScheme.background == Color.Black) Color.Black else Color.White, RoundedCornerShape(20.dp))
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
