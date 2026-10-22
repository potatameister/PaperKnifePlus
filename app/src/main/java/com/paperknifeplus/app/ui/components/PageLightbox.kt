package com.paperknifeplus.app.ui.components

import android.net.Uri
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil.ImageLoader
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.paperknifeplus.app.data.image.PdfPageFetcher
import com.paperknifeplus.app.data.image.PdfPageRequest
import com.paperknifeplus.app.ui.theme.PaperPink
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.input.ImeAction

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PageLightbox(
    uri: Uri,
    initialPage: Int,
    totalCount: Int,
    password: String?,
    onDismiss: () -> Unit,
    selectedPages: Set<Int>? = null,
    onToggleSelection: ((Int) -> Unit)? = null,
    isGrayscale: Boolean = false,
    itemOverlay: @Composable (BoxScope.(Int) -> Unit)? = null,
    bottomBar: @Composable (BoxScope.(Int) -> Unit)? = null,
    zoomEnabled: Boolean = true
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(initialPage = initialPage) { totalCount }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val view = LocalView.current
    val navBarPx = remember(view) {
        ViewCompat.getRootWindowInsets(view)
            ?.getInsets(WindowInsetsCompat.Type.navigationBars())
            ?.bottom ?: 0
    }
    val navBarPadding = with(density) { navBarPx.toDp() }
    
    var showJumpDialog by remember { mutableStateOf(false) }
    var jumpPageInput by remember { mutableStateOf("") }

    // Dedicated High-Res Loader for Lightbox
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(PdfPageFetcher.Factory(context)) }
            .memoryCache {
                coil.memory.MemoryCache.Builder(context)
                    .maxSizePercent(0.40)
                    .build()
            }
            .build()
    }

    // NITRO: Track zoom per page
    val zoomLevels = remember { mutableStateMapOf<Int, Float>() }
    val isCurrentPageZoomed by remember {
        derivedStateOf { (zoomLevels[pagerState.currentPage] ?: 1f) > 1.01f }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 16.dp,
                beyondBoundsPageCount = 1,
                userScrollEnabled = !isCurrentPageZoomed && (bottomBar == null)
            ) { pageIndex ->
                val request = remember(uri, pageIndex, password) { 
                    PdfPageRequest(uri, pageIndex, password, 2.0f, priority = 1) 
                }
                
                var scale by remember { mutableFloatStateOf(1f) }
                var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                
                LaunchedEffect(scale) {
                    zoomLevels[pageIndex] = scale
                }

                // Smooth Animation
                val animatedScale by animateFloatAsState(targetValue = scale, label = "scale")
                val animatedOffset by animateOffsetAsState(targetValue = offset, label = "offset")

                LaunchedEffect(pagerState.currentPage) {
                    if (pagerState.currentPage != pageIndex) {
                        scale = 1f
                        offset = androidx.compose.ui.geometry.Offset.Zero
                    }
                }

                BoxWithConstraints(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val containerWidth = if (constraints.hasBoundedWidth) {
                        constraints.maxWidth.toFloat()
                    } else {
                        1f
                    }
                    val containerHeight = if (constraints.hasBoundedHeight) {
                        constraints.maxHeight.toFloat()
                    } else {
                        1f
                    }
                    val painter = rememberAsyncImagePainter(request, imageLoader)
                    val intrinsic = painter.intrinsicSize
                    val rawPageAspect = if (intrinsic.isSpecified) {
                        val intrinsicWidth = intrinsic.width
                        val intrinsicHeight = intrinsic.height
                        if (
                            intrinsicWidth.isFinite() &&
                            intrinsicHeight.isFinite() &&
                            intrinsicWidth > 0f &&
                            intrinsicHeight > 0f
                        ) {
                            intrinsicWidth / intrinsicHeight
                        } else {
                            0f
                        }
                    } else {
                        0f
                    }
                    val pageAspect = rawPageAspect.takeIf { it.isFinite() && it > 0f } ?: 0.707f
                    val containerAspect = if (
                        containerWidth.isFinite() &&
                        containerHeight.isFinite() &&
                        containerHeight > 0f
                    ) {
                        containerWidth / containerHeight
                    } else {
                        0.707f
                    }
                    val rawFitWidth = if (containerAspect > pageAspect) {
                        containerHeight * pageAspect
                    } else {
                        containerWidth
                    }
                    val rawFitHeight = if (containerAspect > pageAspect) {
                        containerHeight
                    } else if (pageAspect > 0f) {
                        containerWidth / pageAspect
                    } else {
                        containerWidth
                    }
                    val fitWidth = rawFitWidth.takeIf { it.isFinite() && it > 0f } ?: 1f
                    val fitHeight = rawFitHeight.takeIf { it.isFinite() && it > 0f } ?: 1f

                    val gestureModifier = if (zoomEnabled) {
                        Modifier
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = { tapOffset ->
                                        if (scale > 1.01f) {
                                            scale = 1f
                                            offset = Offset.Zero
                                        } else {
                                            scale = 2.5f
                                            val x = (size.width / 2 - tapOffset.x) * (2.5f - 1f)
                                            val y = (size.height / 2 - tapOffset.y) * (2.5f - 1f)
                                            offset = Offset(x, y)
                                        }
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val pressedCount = event.changes.count { it.pressed }

                                        if (pressedCount >= 2) {
                                            val zoom = event.calculateZoom()
                                            val pan = event.calculatePan()
                                            val newScale = (scale * zoom).coerceIn(1f, 4f)
                                            scale = newScale
                                            if (newScale > 1f) {
                                                val maxX = fitWidth * (newScale - 1) / 2f
                                                val maxY = fitHeight * (newScale - 1) / 2f
                                                val newOffset = offset + pan
                                                offset = Offset(
                                                    newOffset.x.coerceIn(-maxX, maxX),
                                                    newOffset.y.coerceIn(-maxY, maxY)
                                                )
                                            } else {
                                                offset = Offset.Zero
                                            }
                                            event.changes.forEach { it.consume() }
                                        } else if (pressedCount == 1 && scale > 1.01f) {
                                            val pan = event.calculatePan()
                                            if (pan != Offset.Zero) {
                                                val maxX = fitWidth * (scale - 1) / 2f
                                                val maxY = fitHeight * (scale - 1) / 2f
                                                val newOffset = offset + pan
                                                offset = Offset(
                                                    newOffset.x.coerceIn(-maxX, maxX),
                                                    newOffset.y.coerceIn(-maxY, maxY)
                                                )
                                                event.changes.forEach { it.consume() }
                                            }
                                        }
                                    }
                                }
                            }
                    } else {
                        Modifier
                    }

                    Box(
                        Modifier.fillMaxSize().then(gestureModifier),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(
                                    width = with(density) { fitWidth.toDp() },
                                    height = with(density) { fitHeight.toDp() }
                                )
                                .graphicsLayer(
                                    scaleX = animatedScale,
                                    scaleY = animatedScale,
                                    translationX = animatedOffset.x,
                                    translationY = animatedOffset.y
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painter,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds,
                                colorFilter = if (isGrayscale) androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) }) else null
                            )

                            // The overlay now receives the exact page bounds, not the screen bounds.
                            itemOverlay?.invoke(this, pageIndex)
                        }

                        if (painter.state is AsyncImagePainter.State.Loading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp))
                        }
                    }
                }
            }

            // Top Bar
            Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.background(Color.White.copy(0.1f), CircleShape)
                        ) {
                            Icon(Icons.Filled.Close, null, tint = Color.White)
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share PDF"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot share this file", Toast.LENGTH_SHORT).show()
                            }
                        }, modifier = Modifier.background(Color.White.copy(0.1f), CircleShape)) {
                            Icon(Icons.Filled.Share, null, tint = Color.White)
                        }
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            onClick = { 
                                jumpPageInput = (pagerState.currentPage + 1).toString()
                                showJumpDialog = true 
                            },
                            color = Color.White.copy(0.1f), 
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "${pagerState.currentPage + 1} / $totalCount",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
            
            // Jump to Page Dialog
            if (showJumpDialog) {
                AlertDialog(
                    onDismissRequest = { showJumpDialog = false },
                    title = { Text("Go to Page", fontWeight = FontWeight.Black) },
                    text = {
                        OutlinedTextField(
                            value = jumpPageInput,
                            onValueChange = { if (it.all { char -> char.isDigit() }) jumpPageInput = it },
                            label = { Text("Page Number (1-$totalCount)") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onGo = {
                                val pageNum = jumpPageInput.toIntOrNull()
                                if (pageNum != null && pageNum in 1..totalCount) {
                                    scope.launch { pagerState.scrollToPage(pageNum - 1) }
                                    showJumpDialog = false
                                }
                            })
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val pageNum = jumpPageInput.toIntOrNull()
                            if (pageNum != null && pageNum in 1..totalCount) {
                                scope.launch { pagerState.scrollToPage(pageNum - 1) }
                                showJumpDialog = false
                            }
                        }) { Text("GO", fontWeight = FontWeight.Black, color = PaperPink) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showJumpDialog = false }) { Text("CANCEL", color = Color.Gray) }
                    },
                    shape = RoundedCornerShape(28.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                )
            }
            
            if (onToggleSelection != null && selectedPages != null && bottomBar == null) {
                Box(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 72.dp), contentAlignment = Alignment.TopCenter) {
                    val isSelected = selectedPages.contains(pagerState.currentPage)
                    Button(
                        onClick = { onToggleSelection(pagerState.currentPage) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color(0xFF10B981) else Color.White.copy(0.1f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        if (isSelected) {
                            Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (isSelected) "SELECTED" else "SELECT", fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            // Custom Tool Bottom Bar (e.g. Sign controls)
            Box(Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = navBarPadding + 12.dp)
                ) {
                    bottomBar?.invoke(this, pagerState.currentPage)
                }
            }

            // Navigation
            if (bottomBar == null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = navBarPadding + 36.dp)
                        .background(Color.Black.copy(0.5f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                        enabled = pagerState.currentPage > 0
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = if (pagerState.currentPage > 0) Color.White else Color.White.copy(0.3f))
                    }

                    IconButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                        enabled = pagerState.currentPage < totalCount - 1
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = if (pagerState.currentPage < totalCount - 1) Color.White else Color.White.copy(0.3f))
                    }
                }
            }
        }
    }
}
