package com.paperknifeplus.app.ui.components

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import com.paperknifeplus.app.data.image.PdfPageFetcher
import com.paperknifeplus.app.data.image.PdfPageRequest

enum class PreviewMode {
    GRID,   // Scrollable grid (Split, Rearrange, etc.)
    COVER   // Single high-res page (Compress, Protect, etc.)
}

@Composable
fun UnifiedPdfPreview(
    uri: Uri,
    pageCount: Int,
    mode: PreviewMode = PreviewMode.GRID,
    password: String? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val context = LocalContext.current
    var lightboxPage by remember { mutableStateOf<Int?>(null) }
    
    // Optimized "Blitz Engine" Loader
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(PdfPageFetcher.Factory(context)) }
            .memoryCache {
                coil.memory.MemoryCache.Builder(context)
                    .maxSizePercent(0.30) // Use 30% of available RAM for grid cache
                    .build()
            }
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(context.cacheDir.resolve("pdf_previews"))
                    .maxSizeBytes(200 * 1024 * 1024) // 200MB disk cache
                    .build()
            }
            .crossfade(false) // Disable crossfade for maximum scroll speed
            .build()
    }

    if (mode == PreviewMode.COVER) {
        // --- TYPE B: ONE PAGE PREVIEW ---
        Box(contentAlignment = Alignment.BottomCenter) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.707f) // A4 Aspect Ratio
                    .clickable { lightboxPage = 0 },
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(0.1f)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                // INCREASE SCALE for cover mode to ensure images are crisp
                val request = remember(uri, password) { PdfPageRequest(uri, 0, password, 1.2f) }
                Box(Modifier.fillMaxSize()) {
                    Image(
                        painter = rememberAsyncImagePainter(request, imageLoader),
                        contentDescription = "Document Cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    
                    // Zoom Hint
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                        color = Color.Black.copy(0.3f),
                        shape = CircleShape
                    ) {
                        Icon(
                            Icons.Default.ZoomIn, 
                            null, 
                            tint = Color.White, 
                            modifier = Modifier.padding(8.dp).size(20.dp)
                        )
                    }
                }
            }
        }
    } else {
        // --- TYPE A: MINI PREVIEW GRID (Blitz Mode - Fixed 2-Column) ---
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(pageCount, key = { it }) { index ->
                PdfPageItem(
                    uri = uri,
                    index = index,
                    password = password,
                    imageLoader = imageLoader,
                    onClick = { lightboxPage = index }
                )
            }
        }
    }

    // --- SHARED LIGHTBOX ---
    if (lightboxPage != null) {
        PageLightbox(
            uri = uri,
            initialPage = lightboxPage!!,
            totalCount = pageCount,
            password = password,
            onDismiss = { lightboxPage = null }
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
    content: @Composable BoxScope.() -> Unit = {}
) {
    // LOW-RES THUMBNAILS (0.2f) for blitz-fast scrolling
    val request = remember(uri, index, password) { PdfPageRequest(uri, index, password, 0.2f) }
    
    Box(
        modifier = modifier
            .aspectRatio(0.707f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (MaterialTheme.colorScheme.background == Color.Black) Color(0xFF18181B) else Color(0xFFF4F4F5))
            .clickable { onClick() }
    ) {
        Image(
            painter = rememberAsyncImagePainter(request, imageLoader),
            contentDescription = "Page ${index + 1}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Page Number Overlay (Centralized)
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .size(32.dp),
            color = Color.Black.copy(0.5f),
            shape = RoundedCornerShape(6.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "${index + 1}", 
                    color = Color.White, 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        content()
    }
}
