package com.paperknifeplus.app.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
    GRID,   // For Merge, Split, Rearrange (Lots of pages)
    COVER   // For Compress, Protect, Unlock (One page)
}

@Composable
fun UnifiedPdfPreview(
    uri: Uri,
    pageCount: Int,
    mode: PreviewMode = PreviewMode.GRID,
    password: String? = null,
    onPageClick: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    
    // Custom ImageLoader that knows how to read PDF pages
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(PdfPageFetcher.Factory(context)) }
            .crossfade(true)
            .build()
    }

    if (mode == PreviewMode.COVER) {
        // High Quality Cover View
        Card(
            modifier = Modifier.fillMaxWidth().height(280.dp),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.Gray.copy(0.1f)),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            val request = PdfPageRequest(uri, 0, password)
            Image(
                painter = rememberAsyncImagePainter(request, imageLoader),
                contentDescription = "Cover Preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    } else {
        // Blitz-Fast Grid View
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(pageCount) { index ->
                val request = PdfPageRequest(uri, index, password)
                
                Box(
                    modifier = Modifier
                        .aspectRatio(0.707f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Gray.copy(0.1f))
                        .clickable { onPageClick(index) }
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(request, imageLoader),
                        contentDescription = "Page ${index + 1}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Page Number Overlay
                    Surface(
                        modifier = Modifier.align(Alignment.BottomStart).padding(4.dp),
                        color = Color.Black.copy(0.5f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "${index + 1}", 
                            color = Color.White, 
                            fontSize = 10.sp, 
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
