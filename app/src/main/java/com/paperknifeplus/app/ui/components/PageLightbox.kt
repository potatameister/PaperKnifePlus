package com.paperknifeplus.app.ui.components

import android.net.Uri
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.paperknifeplus.app.data.image.PdfPageFetcher
import com.paperknifeplus.app.data.image.PdfPageRequest
import kotlinx.coroutines.launch

import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PageLightbox(
    uri: Uri,
    initialPage: Int,
    totalCount: Int,
    password: String?,
    onDismiss: () -> Unit,
    selectedPages: Set<Int>? = null,
    onToggleSelection: ((Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(initialPage = initialPage) { totalCount }
    val scope = rememberCoroutineScope()
    var isNightMode by remember { mutableStateOf(false) }
    
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

    // NITRO: Track zoom per page to intelligently enable/disable pager scroll
    val zoomLevels = remember { mutableStateMapOf<Int, Float>() }
    val isCurrentPageZoomed by remember {
        derivedStateOf { (zoomLevels[pagerState.currentPage] ?: 1f) > 1.05f }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
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
                userScrollEnabled = !isCurrentPageZoomed
            ) { pageIndex ->
                val request = remember(uri, pageIndex, password) { 
                    PdfPageRequest(uri, pageIndex, password, 2.0f, prefetch = false) 
                }
                
                var scale by remember { mutableFloatStateOf(1f) }
                var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                
                // Sync local scale with global zoom tracker
                LaunchedEffect(scale) {
                    zoomLevels[pageIndex] = scale
                }

                // NITRO: Smooth Animated Zoom
                val animatedScale by animateFloatAsState(
                    targetValue = scale,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                    label = "scale"
                )
                val animatedOffset by animateOffsetAsState(
                    targetValue = offset,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "offset"
                )

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
                    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
                        scale = (scale * zoomChange).coerceIn(1f, 4f)
                        
                        if (scale > 1.05f) {
                            val maxX = (constraints.maxWidth * (scale - 1) / 2)
                            val maxY = (constraints.maxHeight * (scale - 1) / 2)
                            val newOffset = offset + offsetChange
                            offset = androidx.compose.ui.geometry.Offset(
                                newOffset.x.coerceIn(-maxX, maxX),
                                newOffset.y.coerceIn(-maxY, maxY)
                            )
                        } else {
                            offset = androidx.compose.ui.geometry.Offset.Zero
                        }
                    }

                    Box(
                        Modifier
                            .fillMaxSize()
                            .then(
                                if (scale > 1.05f) {
                                    Modifier
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onDoubleTap = { scale = 1f; offset = androidx.compose.ui.geometry.Offset.Zero }
                                            )
                                        }
                                        .transformable(state = state)
                                } else {
                                    Modifier.pointerInput(Unit) {
                                        detectTapGestures(
                                            onDoubleTap = { tapOffset ->
                                                scale = 2.5f
                                                val x = (size.width / 2 - tapOffset.x) * (2.5f - 1f)
                                                val y = (size.height / 2 - tapOffset.y) * (2.5f - 1f)
                                                offset = androidx.compose.ui.geometry.Offset(x, y)
                                            }
                                        )
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val painter = rememberAsyncImagePainter(request, imageLoader)
                        
                        Image(
                            painter = painter,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = animatedScale,
                                    scaleY = animatedScale,
                                    translationX = animatedOffset.x,
                                    translationY = animatedOffset.y
                                ),
                            contentScale = ContentScale.Fit,
                            colorFilter = if (isNightMode) ColorFilter.colorMatrix(ColorMatrix().apply {
                                // Invert colors but keep luminance similar
                                set(floatArrayOf(
                                    -1f, 0f, 0f, 0f, 255f,
                                    0f, -1f, 0f, 0f, 255f,
                                    0f, 0f, -1f, 0f, 255f,
                                    0f, 0f, 0f, 1f, 0f
                                ))
                            }) else null
                        )

                        if (painter.state is AsyncImagePainter.State.Loading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp))
                        }
                    }
                }
            }


            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(Color.White.copy(0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { isNightMode = !isNightMode },
                        modifier = Modifier.background(if (isNightMode) PaperPink else Color.White.copy(0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Brightness4, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(color = Color.White.copy(0.1f), shape = RoundedCornerShape(12.dp)) {
                        Text(
                            "${pagerState.currentPage + 1} / $totalCount",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                
                if (onToggleSelection != null && selectedPages != null) {
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
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (isSelected) "SELECTED" else "SELECT", fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                } else {
                    Spacer(Modifier.width(48.dp))
                }
            }

            // Navigation
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
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
