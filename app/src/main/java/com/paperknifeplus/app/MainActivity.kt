package com.paperknifeplus.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
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
    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
            var isInitialized by remember { mutableStateOf(false) }

            // NITRO: Background Initialization
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(1200) // Branding dwell time
                isInitialized = true
            }

            CompositionLocalProvider(coil.compose.LocalImageLoader provides nitroImageLoader) {
                PaperKnifePlusTheme(darkTheme = isDarkMode) {
                    Box(Modifier.fillMaxSize()) {
                        var currentTool by remember { mutableStateOf<String?>(null) }
                        var previewData by remember { mutableStateOf<Triple<Uri, String, Int>?>(null) }
                        
                        val mainScreens = listOf("home", "tools", "history", "settings")
                        val pagerState = androidx.compose.foundation.pager.rememberPagerState { mainScreens.size }
                        val scope = rememberCoroutineScope()

                        // --- INTELLIGENT BACK NAVIGATION ---
                        BackHandler(enabled = currentTool != null || pagerState.currentPage != 0) {
                            if (currentTool != null) {
                                currentTool = null
                            } else if (pagerState.currentPage != 0) {
                                scope.launch { pagerState.animateScrollToPage(0) }
                            }
                        }

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
                                    beyondBoundsPageCount = 0 // NITRO: Only render current page to save CPU/GPU
                                ) { page ->
                                    when (mainScreens[page]) {
                                        "home" -> HomeView(
                                            isDarkMode = isDarkMode,
                                            onThemeToggle = { isDarkMode = !isDarkMode },
                                            onToolClick = { currentTool = it },
                                            onOpenPreview = { uri, name, count ->
                                                previewData = Triple(uri, name, count)
                                                currentTool = "ultra_preview"
                                            }
                                        )
                                        "tools" -> ToolsView(onToolClick = { currentTool = it })
                                        "history" -> HistoryView(onItemClick = { uri, name, count ->
                                            previewData = Triple(uri, name, count)
                                            currentTool = "ultra_preview"
                                        })
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
                                            // Use scrollToPage (instant) for bar clicks to avoid rendering in-between pages
                                            scope.launch { pagerState.scrollToPage(index) }
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
                                        "merge" -> MergeView(
                                            onBack = { currentTool = null },
                                            onOpenPreview = { uri, name, count ->
                                                previewData = Triple(uri, name, count)
                                                currentTool = "ultra_preview"
                                            }
                                        )
                                        "split" -> SplitView(
                                            onBack = { currentTool = null },
                                            onOpenPreview = { uri, name, count ->
                                                previewData = Triple(uri, name, count)
                                                currentTool = "ultra_preview"
                                            }
                                        )
                                        "delete" -> DeleteView(
                                            onBack = { currentTool = null },
                                            onOpenPreview = { uri, name, count ->
                                                previewData = Triple(uri, name, count)
                                                currentTool = "ultra_preview"
                                            }
                                        )
                                        "compress" -> CompressView(
                                            onBack = { currentTool = null },
                                            onOpenPreview = { uri, name, count ->
                                                previewData = Triple(uri, name, count)
                                                currentTool = "ultra_preview"
                                            }
                                        )
                                        "repair" -> RepairView(
                                            onBack = { currentTool = null },
                                            onOpenPreview = { uri, name, count ->
                                                previewData = Triple(uri, name, count)
                                                currentTool = "ultra_preview"
                                            }
                                        )
                                        "rotate" -> RotateView(
                                            onBack = { currentTool = null },
                                            onOpenPreview = { uri, name, count ->
                                                previewData = Triple(uri, name, count)
                                                currentTool = "ultra_preview"
                                            }
                                        )
                                        "rearrange" -> RearrangeView(
                                            onBack = { currentTool = null },
                                            onOpenPreview = { uri, name, count ->
                                                previewData = Triple(uri, name, count)
                                                currentTool = "ultra_preview"
                                            }
                                        )
                                        "protect" -> ProtectView(
                                            onBack = { currentTool = null },
                                            onOpenPreview = { uri, name, count ->
                                                previewData = Triple(uri, name, count)
                                                currentTool = "ultra_preview"
                                            }
                                        )
                                        "unlock" -> UnlockView(
                                            onBack = { currentTool = null },
                                            onOpenPreview = { uri, name, count ->
                                                previewData = Triple(uri, name, count)
                                                currentTool = "ultra_preview"
                                            }
                                        )
                                        "grayscale" -> GrayscaleView(
                                            onBack = { currentTool = null },
                                            onOpenPreview = { uri, name, count ->
                                                previewData = Triple(uri, name, count)
                                                currentTool = "ultra_preview"
                                            }
                                        )
                                        "metadata" -> MetadataView(
                                            onBack = { currentTool = null },
                                            onOpenPreview = { uri, name, count ->
                                                previewData = Triple(uri, name, count)
                                                currentTool = "ultra_preview"
                                            }
                                        )
                                        "img2pdf" -> ImageToPdfView(
                                            onBack = { currentTool = null },
                                            onOpenPreview = { uri, name, count ->
                                                previewData = Triple(uri, name, count)
                                                currentTool = "ultra_preview"
                                            }
                                        )
                                        "pdf2img" -> PdfToImageView(
                                            onBack = { currentTool = null },
                                            onOpenPreview = { uri, name, count ->
                                                previewData = Triple(uri, name, count)
                                                currentTool = "ultra_preview"
                                            }
                                        )
                                        "pdf2text" -> PdfToTextView(
                                            onBack = { currentTool = null },
                                            onOpenPreview = { uri, name, count ->
                                                previewData = Triple(uri, name, count)
                                                currentTool = "ultra_preview"
                                            }
                                        )
                                        "pdf2zip" -> PdfToZipView(
                                            onBack = { currentTool = null },
                                            onOpenPreview = { uri, name, count ->
                                                previewData = Triple(uri, name, count)
                                                currentTool = "ultra_preview"
                                            }
                                        )
                                        "sign" -> SignView(
                                            onBack = { currentTool = null },
                                            onOpenPreview = { uri, name, count ->
                                                previewData = Triple(uri, name, count)
                                                currentTool = "ultra_preview"
                                            }
                                        )
                                        "watermark" -> WatermarkView(
                                            onBack = { currentTool = null },
                                            onOpenPreview = { uri, name, count ->
                                                previewData = Triple(uri, name, count)
                                                currentTool = "ultra_preview"
                                            }
                                        )
                                        "page-numbers" -> PageNumbersView(
                                            onBack = { currentTool = null },
                                            onOpenPreview = { uri, name, count ->
                                                previewData = Triple(uri, name, count)
                                                currentTool = "ultra_preview"
                                            }
                                        )
                                        "bookmarks" -> BookmarksView(
                                            onBack = { currentTool = null },
                                            onOpenPreview = { uri, name, count ->
                                                previewData = Triple(uri, name, count)
                                                currentTool = "ultra_preview"
                                            }
                                        )
                                        "extract-images" -> ExtractImagesView(
                                            onBack = { currentTool = null },
                                            onOpenPreview = { uri, name, count ->
                                                previewData = Triple(uri, name, count)
                                                currentTool = "ultra_preview"
                                            }
                                        )
                                        "ultra_preview" -> {
                                            previewData?.let { (uri, name, count) ->
                                                UltraPreview(
                                                    uri = uri,
                                                    fileName = name,
                                                    pageCount = count,
                                                    onDismiss = { currentTool = null }
                                                )
                                            }
                                        }
                                        "compare" -> CompareView(
                                            onBack = { currentTool = null },
                                            onOpenPreview = { uri, name, count ->
                                                previewData = Triple(uri, name, count)
                                                currentTool = "ultra_preview"
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // BLITZ SPLASH OVERLAY
                        androidx.compose.animation.AnimatedVisibility(
                            visible = !isInitialized,
                            exit = androidx.compose.animation.fadeOut(animationSpec = tween(500))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(if (isDarkMode) Color.Black else Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Logo(modifier = Modifier.size(64.dp), partColor = if (isDarkMode) Color.White else Color.Black)
                                    Spacer(Modifier.height(24.dp))
                                    CircularProgressIndicator(color = PaperPink, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
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
                Icon(Icons.Filled.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
fun NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    @Suppress("DEPRECATION")
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
