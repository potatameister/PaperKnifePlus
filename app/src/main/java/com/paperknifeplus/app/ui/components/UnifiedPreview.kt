package com.paperknifeplus.app.ui.components

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.ImageLoader
import coil.compose.AsyncImagePainter
import coil.compose.LocalImageLoader
import coil.compose.rememberAsyncImagePainter
import com.paperknifeplus.app.data.image.PdfPageFetcher
import com.paperknifeplus.app.data.image.PdfPageRequest

enum class PreviewMode {
    GRID,   // Scrollable grid (Split, etc.)
    COVER,  // Single high-res page (Compress, Protect, etc.)
    REORDER // Draggable grid (Rearrange)
}

@Composable
fun UnifiedPdfPreview(
    uri: Uri,
    pageCount: Int,
    mode: PreviewMode = PreviewMode.GRID,
    password: String? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    selectedPages: Set<Int>? = null,
    onToggleSelection: ((Int) -> Unit)? = null,
    pageOrder: List<Int>? = null,
    onOrderChange: ((List<Int>) -> Unit)? = null
) {
    val context = LocalContext.current
    var lightboxPage by remember { mutableStateOf<Int?>(null) }
    
    // NITRO ENGINE: Use Shared Global Loader (MainActivity)
    val imageLoader = coil.compose.LocalImageLoader.current

    if (mode == PreviewMode.COVER) {
        // ... TYPE B: ONE PAGE PREVIEW ---
        Box(contentAlignment = Alignment.BottomCenter) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.707f) // A4 Aspect Ratio
                    .clickable { lightboxPage = 0 },
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(0.1f)),
                colors = CardDefaults.cardColors(containerColor = if (MaterialTheme.colorScheme.background == Color.Black) Color(0xFF18181B) else Color(0xFFF5F5F5))
            ) {
                val request = remember(uri, password) { PdfPageRequest(uri, 0, password, 1.2f) }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val painter = rememberAsyncImagePainter(request, imageLoader)
                    val painterState = painter.state
                    
                    Image(
                        painter = painter,
                        contentDescription = "Document Cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    if (painterState is AsyncImagePainter.State.Loading) {
                        CircularProgressIndicator(color = accentColor, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                    } else if (painterState is AsyncImagePainter.State.Error && password != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Lock, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Protected File", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                        color = Color.Black.copy(0.3f),
                        shape = CircleShape
                    ) {
                        Icon(
                            Icons.Filled.ZoomIn, 
                            null, 
                            tint = Color.White, 
                            modifier = Modifier.padding(8.dp).size(20.dp)
                        )
                    }
                }
            }
        }
    } else if (mode == PreviewMode.REORDER && pageOrder != null && onOrderChange != null) {
        // --- NITRO REORDER 3.0: STABLE DRAGGING ---
        var draggedIndex by remember { mutableStateOf<Int?>(null) }
        var dragOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
        
        // Use a local copy for visual stability during drag
        val visualList = remember(pageOrder) { pageOrder.toMutableStateList() }
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
        ) {
            itemsIndexed(visualList, key = { _, pageIdx -> pageIdx }) { index, pageIdx ->
                val isDragging = draggedIndex == index
                val scale by animateFloatAsState(if (isDragging) 1.1f else 1f, spring(stiffness = Spring.StiffnessLow))
                val zIndex = if (isDragging) 10f else 1f
                
                Box(
                    modifier = Modifier
                        .zIndex(zIndex)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            if (isDragging) {
                                translationX = dragOffset.x
                                translationY = dragOffset.y
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { draggedIndex = index },
                                onDragEnd = {
                                    onOrderChange(visualList.toList())
                                    draggedIndex = null
                                    dragOffset = androidx.compose.ui.geometry.Offset.Zero
                                },
                                onDragCancel = {
                                    draggedIndex = null
                                    dragOffset = androidx.compose.ui.geometry.Offset.Zero
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount
                                    
                                    val cellWidth = size.width / 2f
                                    val cellHeight = cellWidth / 0.707f
                                    val currentIndex = draggedIndex ?: return@detectDragGesturesAfterLongPress
                                    
                                    // NITRO HYSTERESIS: Require 80% movement to trigger swap
                                    val swapThresholdY = cellHeight * 0.8f
                                    val swapThresholdX = cellWidth * 0.8f
                                    
                                    val colOffset = if (Math.abs(dragOffset.x) > swapThresholdX) (dragOffset.x / cellWidth).toInt() else 0
                                    val rowOffset = if (Math.abs(dragOffset.y) > swapThresholdY) (dragOffset.y / cellHeight).toInt() else 0
                                    
                                    val targetIndex = (currentIndex + colOffset + (rowOffset * 2)).coerceIn(0, visualList.size - 1)
                                    
                                    if (targetIndex != currentIndex) {
                                        val item = visualList.removeAt(currentIndex)
                                        visualList.add(targetIndex, item)
                                        
                                        // NITRO COMPENSATION: Maintains "Fixed Center" thumb feel
                                        val colDiff = (targetIndex % 2) - (currentIndex % 2)
                                        val rowDiff = (targetIndex / 2) - (currentIndex / 2)
                                        
                                        draggedIndex = targetIndex
                                        dragOffset = androidx.compose.ui.geometry.Offset(
                                            dragOffset.x - colDiff * cellWidth,
                                            dragOffset.y - rowDiff * cellHeight
                                        )
                                    }
                                }
                            )
                        }
                ) {
                    PdfPageItem(
                        uri = uri,
                        index = pageIdx,
                        password = password,
                        imageLoader = imageLoader,
                        accentColor = accentColor,
                        onClick = { lightboxPage = pageIdx },
                        // NITRO BLITZ: Ultra-low resolution for jitter-free drag
                        scale = 0.4f,
                        modifier = if (isDragging) Modifier.shadow(16.dp, RoundedCornerShape(12.dp), spotColor = accentColor) else Modifier
                    )
                }
            }
        }
    } else {
        // --- TYPE A: MINI PREVIEW GRID (Blitz Mode) ---
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
        ) {
            items(pageCount, key = { it }) { index ->
                val isSelected = selectedPages?.contains(index) == true
                
                PdfPageItem(
                    uri = uri,
                    index = index,
                    password = password,
                    imageLoader = imageLoader,
                    accentColor = accentColor,
                    onClick = { 
                        if (onToggleSelection != null) onToggleSelection(index)
                        else lightboxPage = index 
                    },
                    modifier = if (isSelected) Modifier.border(BorderStroke(3.dp, accentColor), RoundedCornerShape(12.dp)) else Modifier
                ) {
                    if (onToggleSelection != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(if (isSelected) accentColor.copy(alpha = 0.15f) else Color.Transparent)
                        )
                        
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(24.dp),
                            color = if (isSelected) accentColor else Color.Black.copy(0.3f),
                            shape = CircleShape
                        ) {
                            Icon(
                                Icons.Filled.Check, 
                                null, 
                                tint = Color.White, 
                                modifier = Modifier.padding(4.dp)
                            )
                        }

                        Surface(
                            onClick = { lightboxPage = index },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .size(24.dp),
                            color = Color.Black.copy(0.3f),
                            shape = CircleShape
                        ) {
                            Icon(
                                Icons.Filled.Fullscreen, 
                                null, 
                                tint = Color.White, 
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (lightboxPage != null) {
        PageLightbox(
            uri = uri,
            initialPage = lightboxPage!!,
            totalCount = pageCount,
            password = password,
            onDismiss = { lightboxPage = null },
            selectedPages = selectedPages,
            onToggleSelection = onToggleSelection
        )
    }
}

@Composable
fun PdfPageItem(
    uri: Uri,
    index: Int,
    password: String?,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    scale: Float = 0.7f,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val request = remember(uri, index, password, scale) { PdfPageRequest(uri, index, password, scale) }
    
    Box(
        modifier = modifier
            .aspectRatio(0.707f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (MaterialTheme.colorScheme.background == Color.Black) Color(0xFF18181B) else Color(0xFFF4F4F5))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        val painter = rememberAsyncImagePainter(
            model = request,
            imageLoader = imageLoader
        )
        val painterState = painter.state

        Image(
            painter = painter,
            contentDescription = "Page ${index + 1}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (painterState is AsyncImagePainter.State.Loading) {
            CircularProgressIndicator(color = accentColor, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else if (painterState is AsyncImagePainter.State.Error && password != null) {
             Icon(Icons.Filled.Lock, null, tint = Color.Gray.copy(0.3f), modifier = Modifier.size(32.dp))
        }
        
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            color = Color.Black.copy(0.6f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                "${index + 1}", 
                color = Color.White, 
                fontSize = 11.sp, 
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        
        content()
    }
}
