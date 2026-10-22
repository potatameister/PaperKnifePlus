package com.paperknifeplus.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
    @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(applicationContext)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
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
            // NITRO: Dynamic Theme Engine
            var themePreference by remember { mutableIntStateOf(PreferencesManager.getTheme(applicationContext)) }
            val systemDark = isSystemInDarkTheme()
            val isDarkMode = remember(themePreference, systemDark) {
                when(themePreference) {
                    1 -> false
                    2 -> true
                    else -> systemDark
                }
            }

            var isInitialized by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                val retention = PreferencesManager.getHistoryRetention(applicationContext)
                SessionManager.purgeHistory(retention)
                kotlinx.coroutines.delay(1200) 
                isInitialized = true
            }

            CompositionLocalProvider(coil.compose.LocalImageLoader provides nitroImageLoader) {
                PaperKnifePlusTheme(darkTheme = isDarkMode) {
                    val scope = rememberCoroutineScope()

                    Box(Modifier.fillMaxSize()) {
                        var currentTool by remember { mutableStateOf<String?>(null) }
                        var previewData by remember { mutableStateOf<Triple<Uri, String, Int>?>(null) }
                        var toolInitialUri by remember { mutableStateOf<Uri?>(null) }
                        var toolInitialPassword by remember { mutableStateOf<String?>(null) }
                        var aboutInitialPage by remember { mutableStateOf("main") }
                        var isAboutFromSettings by remember { mutableStateOf(false) }
                        
                        val mainScreens = listOf("home", "tools", "history", "settings")
                        val pagerState = androidx.compose.foundation.pager.rememberPagerState { mainScreens.size }

                        // Handle incoming PDF from external apps (e.g., WhatsApp file share)
                        val incomingPdfData = remember(intent) {
                            val uri = intent?.data
                            if (uri != null && (intent?.type == "application/pdf" || uri.toString().lowercase().endsWith(".pdf"))) {
                                // Grant persistent URI permission
                                try {
                                    contentResolver.takePersistableUriPermission(
                                        uri,
                                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    )
                                } catch (e: SecurityException) {
                                    // Permission already taken or not available
                                }
                                // Get file name
                                var fileName = ""
                                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                                    if (cursor.moveToFirst()) {
                                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                        if (nameIndex >= 0) {
                                            fileName = cursor.getString(nameIndex) ?: "Document"
                                        }
                                    }
                                }
                                if (fileName.isEmpty()) {
                                    fileName = uri.lastPathSegment ?: "Document"
                                }
                                Pair(uri, fileName)
                            } else {
                                null
                            }
                        }

                        // Navigate to UltraPreview when incoming PDF is detected
                        LaunchedEffect(incomingPdfData) {
                            if (incomingPdfData != null && currentTool == null) {
                                previewData = Triple(incomingPdfData.first, incomingPdfData.second, 0)
                                currentTool = "ultra_preview"
                            }
                        }

                        BackHandler(enabled = currentTool != null || pagerState.currentPage != 0) {
                            if (currentTool != null) {
                                currentTool = null
                            } else if (pagerState.currentPage != 0) {
                                scope.launch { pagerState.animateScrollToPage(0) }
                            }
                        }

                        // ROOT CONTAINER
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .navigationBarsPadding()
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                androidx.compose.foundation.pager.HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.weight(1f),
                                    userScrollEnabled = currentTool == null,
                                    beyondBoundsPageCount = 0
                                ) { page ->
                                    when (mainScreens[page]) {
                                        "home" -> HomeView(
                                            isDarkMode = isDarkMode,
                                            onThemeToggle = { 
                                                themePreference = if (isDarkMode) 1 else 2
                                                PreferencesManager.setTheme(applicationContext, themePreference)
                                            },
                                            onToolClick = { 
                                                toolInitialUri = null
                                                if (it == "about") {
                                                    aboutInitialPage = "main"
                                                    isAboutFromSettings = false
                                                    currentTool = "about"
                                                } else if (it == "tools") {
                                                    scope.launch { pagerState.animateScrollToPage(1) }
                                                } else if (it == "history") {
                                                    scope.launch { pagerState.animateScrollToPage(2) }
                                                } else {
                                                    currentTool = it 
                                                }
                                            },
                                            onOpenPreview = { uri, name, count ->
                                                previewData = Triple(uri, name, count)
                                                currentTool = "ultra_preview"
                                            }
                                        )
                                        "tools" -> ToolsView(onToolClick = { 
                                            toolInitialUri = null
                                            toolInitialPassword = null
                                            currentTool = it 
                                        })
                                        "history" -> HistoryView(onItemClick = { uri, name, count ->
                                            previewData = Triple(uri, name, count)
                                            toolInitialPassword = null
                                            currentTool = "ultra_preview"
                                        })
                                        "settings" -> SettingsView(
                                            isDarkMode = isDarkMode,
                                            onThemeChange = { theme ->
                                                themePreference = theme
                                                PreferencesManager.setTheme(applicationContext, theme)
                                            },
                                            onNavigateToAbout = { 
                                                aboutInitialPage = it
                                                isAboutFromSettings = true
                                                currentTool = "about" 
                                            }
                                        )
                                    }
                                }
                            }

                            if (currentTool == null) {
                                PaperKnifeBottomBar(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter),
                                    currentScreen = mainScreens[pagerState.currentPage],
                                    onNavigate = { screen ->
                                        val index = mainScreens.indexOf(screen)
                                        if (index != -1) {
                                            scope.launch { pagerState.scrollToPage(index) }
                                        }
                                    }
                                )
                            }

                            androidx.compose.animation.AnimatedVisibility(
                                visible = currentTool != null,
                                enter = androidx.compose.animation.fadeIn(),
                                exit = androidx.compose.animation.fadeOut()
                            ) {
                                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                                    when (currentTool) {
                                        "about" -> AboutView(initialPage = aboutInitialPage, isFromSettings = isAboutFromSettings, onBack = { currentTool = null })
                                        "merge" -> MergeView(initialUri = toolInitialUri, initialPassword = toolInitialPassword, onBack = { currentTool = null }, onOpenPreview = { uri, name, count -> previewData = Triple(uri, name, count); currentTool = "ultra_preview" })
                                        "split" -> SplitView(initialUri = toolInitialUri, initialPassword = toolInitialPassword, onBack = { currentTool = null }, onOpenPreview = { uri, name, count -> previewData = Triple(uri, name, count); currentTool = "ultra_preview" })
                                        "delete" -> DeleteView(initialUri = toolInitialUri, onBack = { currentTool = null }, onOpenPreview = { uri, name, count -> previewData = Triple(uri, name, count); currentTool = "ultra_preview" })
                                        "compress" -> CompressView(initialUri = toolInitialUri, initialPassword = toolInitialPassword, onBack = { currentTool = null }, onOpenPreview = { uri, name, count -> previewData = Triple(uri, name, count); currentTool = "ultra_preview" })
                                        "repair" -> RepairView(initialUri = toolInitialUri, onBack = { currentTool = null }, onOpenPreview = { uri, name, count -> previewData = Triple(uri, name, count); currentTool = "ultra_preview" })
                                        "rotate" -> RotateView(initialUri = toolInitialUri, onBack = { currentTool = null }, onOpenPreview = { uri, name, count -> previewData = Triple(uri, name, count); currentTool = "ultra_preview" })
                                        "rearrange" -> RearrangeView(initialUri = toolInitialUri, initialPassword = toolInitialPassword, onBack = { currentTool = null }, onOpenPreview = { uri, name, count -> previewData = Triple(uri, name, count); currentTool = "ultra_preview" })
                                        "protect" -> ProtectView(initialUri = toolInitialUri, onBack = { currentTool = null }, onOpenPreview = { uri, name, count -> previewData = Triple(uri, name, count); currentTool = "ultra_preview" })
                                        "unlock" -> UnlockView(initialUri = toolInitialUri, onBack = { currentTool = null }, onOpenPreview = { uri, name, count -> previewData = Triple(uri, name, count); currentTool = "ultra_preview" })
                                        "grayscale" -> GrayscaleView(initialUri = toolInitialUri, onBack = { currentTool = null }, onOpenPreview = { uri, name, count -> previewData = Triple(uri, name, count); currentTool = "ultra_preview" })
                                        "metadata" -> MetadataView(initialUri = toolInitialUri, onBack = { currentTool = null }, onOpenPreview = { uri, name, count -> previewData = Triple(uri, name, count); currentTool = "ultra_preview" })
                                        "img2pdf" -> ImageToPdfView(onBack = { currentTool = null }, onOpenPreview = { uri, name, count -> previewData = Triple(uri, name, count); currentTool = "ultra_preview" })
                                        "pdf2img" -> PdfToImageView(initialUri = toolInitialUri, onBack = { currentTool = null }, onOpenPreview = { uri, name, count -> previewData = Triple(uri, name, count); currentTool = "ultra_preview" })
                                        "pdf2text" -> PdfToTextView(initialUri = toolInitialUri, onBack = { currentTool = null }, onOpenPreview = { uri, name, count -> previewData = Triple(uri, name, count); currentTool = "ultra_preview" })
                                        "pdf2zip" -> PdfToZipView(initialUri = toolInitialUri, onBack = { currentTool = null }, onOpenPreview = { uri, name, count -> previewData = Triple(uri, name, count); currentTool = "ultra_preview" })
                                        "sign" -> SignView(initialUri = toolInitialUri, onBack = { currentTool = null }, onOpenPreview = { uri, name, count -> previewData = Triple(uri, name, count); currentTool = "ultra_preview" })
                                        "watermark" -> WatermarkView(initialUri = toolInitialUri, onBack = { currentTool = null }, onOpenPreview = { uri, name, count -> previewData = Triple(uri, name, count); currentTool = "ultra_preview" })
                                        "page-numbers" -> PageNumbersView(initialUri = toolInitialUri, onBack = { currentTool = null }, onOpenPreview = { uri, name, count -> previewData = Triple(uri, name, count); currentTool = "ultra_preview" })
                                        "bookmarks" -> BookmarksView(initialUri = toolInitialUri, onBack = { currentTool = null }, onOpenPreview = { uri, name, count -> previewData = Triple(uri, name, count); currentTool = "ultra_preview" })
                                        "extract-images" -> ExtractImagesView(initialUri = toolInitialUri, onBack = { currentTool = null }, onOpenPreview = { uri, name, count -> previewData = Triple(uri, name, count); currentTool = "ultra_preview" })
                                        "compare" -> CompareView(initialUri = toolInitialUri, onBack = { currentTool = null }, onOpenPreview = { uri, name, count -> previewData = Triple(uri, name, count); currentTool = "ultra_preview" })
                                        "ultra_preview" -> {
                                            previewData?.let { (uri, name, count) ->
                                                UltraPreview(
                                                    uri = uri, fileName = name, pageCount = count, 
                                                    onDismiss = { currentTool = null }, 
                                                    onOpenInTool = { tool, targetUri, targetPass -> 
                                                        toolInitialUri = targetUri
                                                        toolInitialPassword = targetPass
                                                        currentTool = tool 
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

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
fun PaperKnifeBottomBar(
    modifier: Modifier = Modifier,
    currentScreen: String,
    onNavigate: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                icon = if (currentScreen == "home") Icons.Filled.Home else Icons.Outlined.Home,
                label = "Home",
                selected = currentScreen == "home",
                modifier = Modifier.weight(1f)
            ) { onNavigate("home") }

            NavItem(
                icon = if (currentScreen == "tools") Icons.Filled.GridView else Icons.Outlined.GridView,
                label = "Tools",
                selected = currentScreen == "tools",
                modifier = Modifier.weight(1f)
            ) { onNavigate("tools") }

            NavItem(
                icon = if (currentScreen == "history") Icons.Filled.History else Icons.Outlined.History,
                label = "History",
                selected = currentScreen == "history",
                modifier = Modifier.weight(1f)
            ) { onNavigate("history") }

            NavItem(
                icon = if (currentScreen == "settings" || currentScreen == "about") Icons.Filled.Settings else Icons.Outlined.Settings,
                label = "Settings",
                selected = currentScreen == "settings" || currentScreen == "about",
                modifier = Modifier.weight(1f)
            ) { onNavigate("settings") }
        }
    }
}

@Composable
fun NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tint by animateColorAsState(
        targetValue = if (selected) PaperPink else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        label = "navTint"
    )
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 2.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(23.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
            color = tint,
            letterSpacing = 0.2.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(3.dp)
                .clip(CircleShape)
                .background(if (selected) PaperPink else Color.Transparent)
        )
    }
}
