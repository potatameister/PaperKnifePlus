package com.paperknifeplus.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import com.paperknifeplus.app.data.image.PdfPageFetcher
import com.paperknifeplus.app.data.image.PdfPageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(PdfPageFetcher.Factory(context)) }
            .build()
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
                pageSpacing = 16.dp
            ) { pageIndex ->
                val request = remember(uri, pageIndex, password) { PdfPageRequest(uri, pageIndex, password, 1.5f) }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(
                        painter = rememberAsyncImagePainter(request, imageLoader),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
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
                
                Surface(color = Color.White.copy(0.1f), shape = RoundedCornerShape(12.dp)) {
                    Text(
                        "${pagerState.currentPage + 1} / $totalCount",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
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
