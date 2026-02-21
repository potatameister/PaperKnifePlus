package com.paperknifeplus.app.ui.components

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
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
import kotlinx.coroutines.delay

enum class PreviewMode {
    GRID,   // Scrollable grid (Split, etc.)
    COVER,  // Single high-res page (Compress, Protect, etc.)
    REORDER, // Draggable grid (Rearrange)
    ROTATE  // Tap to rotate (Rotate)
}

@OptIn(ExperimentalFoundationApi::class)
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
    onOrderChange: ((List<Int>) -> Unit)? = null,
    pageRotations: Map<Int, Int>? = null,
    onRotatePage: ((Int) -> Unit)? = null
) {
    val context = LocalContext.current
    var lightboxPage by remember { mutableStateOf<Int?>(null) }
    val imageLoader = coil.compose.LocalImageLoader.current

    if (mode == PreviewMode.COVER) {
        Box(contentAlignment = Alignment.BottomCenter) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.707f)
                    .clickable { lightboxPage = 0 },
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(0.1f)),
                colors = CardDefaults.cardColors(containerColor = if (MaterialTheme.colorScheme.background == Color.Black) Color(0xFF18181B) else Color(0xFFF5F5F5))
            ) {
                val request = remember(uri, password) { PdfPageRequest(uri, 0, password, 1.2f) }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val painter = rememberAsyncImagePainter(request, imageLoader)
                    Image(
                        painter = painter,
                        contentDescription = "Document Cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    if (painter.state is AsyncImagePainter.State.Loading) {
                        CircularProgressIndicator(color = accentColor, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                    }
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                        color = Color.Black.copy(0.3f),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Filled.ZoomIn, null, tint = Color.White, modifier = Modifier.padding(8.dp).size(20.dp))
                    }
                }
            }
        }
    } else if (mode == PreviewMode.REORDER && pageOrder != null && onOrderChange != null) {
        val gridState = rememberLazyGridState()
        var draggedIndex by remember { mutableStateOf<Int?>(null) }
        var dragOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
        var initialTouchPoint by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
        val scope = rememberCoroutineScope()
        val visualList = remember(pageOrder) { pageOrder.toMutableStateList() }

        LaunchedEffect(draggedIndex, dragOffset) {
            if (draggedIndex != null) {
                while (true) {
                    val containerHeight = gridState.layoutInfo.viewportSize.height
                    val dragY = dragOffset.y
                    if (dragY < -containerHeight * 0.12f) gridState.animateScrollBy(-800f)
                    else if (dragY > containerHeight * 0.12f) gridState.animateScrollBy(800f)
                    else break
                    delay(10)
                }
            }
        }
        
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp)
        ) {
            itemsIndexed(visualList, key = { _, pageIdx -> pageIdx }) { index, pageIdx ->
                val isDragging = draggedIndex == index
                val scale by animateFloatAsState(if (isDragging) 1.35f else 1f, spring(stiffness = Spring.StiffnessLow))
                
                Box(
                    modifier = Modifier
                        .zIndex(if (isDragging) 1000f else 1f)
                        .animateItemPlacement(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            if (isDragging) {
                                translationX = dragOffset.x
                                translationY = dragOffset.y
                            }
                        }
                        .pointerInput(visualList.size) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    draggedIndex = index
                                    initialTouchPoint = offset
                                    dragOffset = androidx.compose.ui.geometry.Offset.Zero
                                },
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
                                    val currentDragged = draggedIndex ?: return@detectDragGesturesAfterLongPress
                                    val layoutInfo = gridState.layoutInfo
                                    val draggedItemInfo = layoutInfo.visibleItemsInfo.find { it.index == currentDragged } ?: return@detectDragGesturesAfterLongPress
                                    val fingerX = draggedItemInfo.offset.x + initialTouchPoint.x + dragOffset.x
                                    val fingerY = draggedItemInfo.offset.y + initialTouchPoint.y + dragOffset.y
                                    val targetItem = layoutInfo.visibleItemsInfo.find { item ->
                                        if (item.index == currentDragged) return@find false
                                        val centerX = item.offset.x + item.size.width / 2
                                        val centerY = item.offset.y + item.size.height / 2
                                        Math.abs(fingerX - centerX) < item.size.width * 0.7f &&
                                        Math.abs(fingerY - centerY) < item.size.height * 0.7f
                                    }
                                    if (targetItem != null && targetItem.index < visualList.size) {
                                        val item = visualList.removeAt(currentDragged)
                                        visualList.add(targetItem.index, item)
                                        val prevOffset = draggedItemInfo.offset
                                        val newOffset = targetItem.offset
                                        dragOffset = androidx.compose.ui.geometry.Offset(
                                            dragOffset.x + (prevOffset.x - newOffset.x),
                                            dragOffset.y + (prevOffset.y - newOffset.y)
                                        )
                                        draggedIndex = targetItem.index
                                    }
                                }
                            )
                        }
                ) {
                    PdfPageItem(
                        uri = uri, index = pageIdx, password = password, imageLoader = imageLoader, accentColor = accentColor,
                        onClick = { if (draggedIndex == null) lightboxPage = pageIdx }, scale = 0.6f,
                        modifier = if (isDragging) Modifier.shadow(64.dp, RoundedCornerShape(12.dp), spotColor = accentColor) else Modifier
                    )
                }
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
        ) {
            items(pageCount, key = { it }) { index ->
                val isSelected = selectedPages?.contains(index) == true
                val rotation = pageRotations?.get(index) ?: 0
                PdfPageItem(
                    uri = uri, index = index, password = password, imageLoader = imageLoader, accentColor = accentColor, rotation = rotation,
                    onClick = { 
                        if (mode == PreviewMode.ROTATE && onRotatePage != null) onRotatePage(index)
                        else if (onToggleSelection != null) onToggleSelection(index)
                        else lightboxPage = index 
                    },
                    scale = 0.6f,
                    modifier = if (isSelected) Modifier.border(BorderStroke(3.dp, accentColor), RoundedCornerShape(12.dp)) else Modifier
                ) {
                    if (mode == PreviewMode.ROTATE) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp),
                            color = Color.Black.copy(0.4f), shape = CircleShape
                        ) { Icon(Icons.Filled.RotateRight, null, tint = Color.White, modifier = Modifier.padding(6.dp)) }
                    } else if (onToggleSelection != null) {
                        Box(modifier = Modifier.fillMaxSize().background(if (isSelected) accentColor.copy(alpha = 0.15f) else Color.Transparent))
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(24.dp),
                            color = if (isSelected) accentColor else Color.Black.copy(0.3f), shape = CircleShape
                        ) { Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.padding(4.dp)) }
                        Surface(
                            onClick = { lightboxPage = index },
                            modifier = Modifier.align(Alignment.TopStart).padding(8.dp).size(24.dp),
                            color = Color.Black.copy(0.3f), shape = CircleShape
                        ) { Icon(Icons.Filled.Fullscreen, null, tint = Color.White, modifier = Modifier.padding(4.dp)) }
                    }
                }
            }
        }
    }

    if (lightboxPage != null) {
        PageLightbox(
            uri = uri, initialPage = lightboxPage!!, totalCount = pageCount, password = password,
            onDismiss = { lightboxPage = null }, selectedPages = selectedPages, onToggleSelection = onToggleSelection
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
    scale: Float = 0.6f,
    rotation: Int = 0,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val request = remember(uri, index, password, scale, rotation) { PdfPageRequest(uri, index, password, scale, rotation) }
    Box(
        modifier = modifier.aspectRatio(if (rotation % 180 != 0) 1.414f else 0.707f).clip(RoundedCornerShape(12.dp))
            .background(if (MaterialTheme.colorScheme.background == Color.Black) Color(0xFF18181B) else Color(0xFFF4F4F5))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        val painter = rememberAsyncImagePainter(model = request, imageLoader = imageLoader)
        Image(
            painter = painter, contentDescription = "Page ${index + 1}",
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit
        )
        if (painter.state is AsyncImagePainter.State.Loading) {
            CircularProgressIndicator(color = accentColor, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else if (painter.state is AsyncImagePainter.State.Error && password != null) {
             Icon(Icons.Filled.Lock, null, tint = Color.Gray.copy(0.3f), modifier = Modifier.size(32.dp))
        }
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp).graphicsLayer { rotationZ = 0f },
            color = Color.Black.copy(0.6f), shape = RoundedCornerShape(8.dp)
        ) {
            Text("${index + 1}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
        }
        content()
    }
}
