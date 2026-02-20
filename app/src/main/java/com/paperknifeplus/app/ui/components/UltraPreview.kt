package com.paperknifeplus.app.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.ImageLoader
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.paperknifeplus.app.data.image.PdfPageFetcher
import com.paperknifeplus.app.data.image.PdfPageRequest
import com.paperknifeplus.app.ui.theme.PaperPink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Image

@Composable
fun UltraPreview(
    uri: Uri,
    fileName: String,
    pageCount: Int,
    password: String? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchLoading by remember { mutableStateOf(false) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    
    var links by remember { mutableStateOf<List<PdfLink>>(emptyList()) }
    var pageSizes by remember { mutableStateOf<Map<Int, Pair<Float, Float>>>(emptyMap()) }

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

    LaunchedEffect(uri) {
        links = getLinksFromPdf(context, uri, password)
        // NITRO: Fetch actual page dimensions for accurate link mapping
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val document = if (password != null) com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputStream, password) else com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputStream)
                    val sizes = document.pages.mapIndexed { index, page ->
                        index to (page.mediaBox.width to page.mediaBox.height)
                    }.toMap()
                    withContext(Dispatchers.Main) { pageSizes = sizes }
                    document.close()
                }
            } catch (e: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (MaterialTheme.colorScheme.background == Color.Black) Color.Black else Color(0xFFF4F4F7))
    ) {
        // --- CONTINUOUS VERTICAL READER ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 5f)
                        if (newScale != scale || newScale > 1f) {
                            scale = newScale
                            offset = if (scale > 1f) offset + pan else androidx.compose.ui.geometry.Offset.Zero
                        }
                    }
                }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
                contentPadding = PaddingValues(top = 80.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp), // SEAMLESS
                horizontalAlignment = Alignment.CenterHorizontally,
                userScrollEnabled = scale == 1f // Lock scroll when zoomed
            ) {
                items(pageCount) { index ->
                    PdfPageReaderItem(
                        uri = uri,
                        index = index,
                        password = password,
                        imageLoader = imageLoader,
                        pageSize = pageSizes[index],
                        links = links.filter { it.pageIndex == index },
                        onLinkClick = { url ->
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }

        // --- TOP BAR (Integrated) ---
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.05f))
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.ArrowBack, "Back", modifier = Modifier.size(22.dp))
                    }
                    
                    if (isSearching) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            placeholder = { Text("Search text...") },
                            trailingIcon = {
                                if (isSearchLoading) {
                                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    IconButton(onClick = { isSearching = false; searchQuery = "" }) {
                                        Icon(Icons.Filled.Close, null)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = {
                                if (searchQuery.isNotBlank()) {
                                    isSearchLoading = true
                                    scope.launch {
                                        val result = findTextInPdf(context, uri, password, searchQuery, 1)
                                        if (result != -1) {
                                            listState.animateScrollToItem(result)
                                        } else {
                                            Toast.makeText(context, "No matches found", Toast.LENGTH_SHORT).show()
                                        }
                                        isSearchLoading = false
                                    }
                                }
                            })
                        )
                    } else {
                        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text(fileName, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1)
                            Text("PREMIUM READER", fontSize = 8.sp, fontWeight = FontWeight.Black, color = PaperPink, letterSpacing = 1.sp)
                        }
                        
                        Row {
                            IconButton(onClick = { isSearching = true }) {
                                Icon(Icons.Filled.Search, null)
                            }
                            IconButton(onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share PDF"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot share file", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Filled.Share, null)
                            }
                        }
                    }
                }
                Divider(color = Color.Gray.copy(alpha = 0.1f))
            }
        }

        // --- BOTTOM INDICATOR ---
        val currentPage by remember { derivedStateOf { listState.firstVisibleItemIndex + 1 } }
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 12.dp,
                border = BorderStroke(1.dp, Color.Gray.copy(0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$currentPage", fontWeight = FontWeight.Black, fontSize = 14.sp, color = PaperPink)
                    Text(" / $pageCount PAGES", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(start = 4.dp))
                    Spacer(Modifier.width(16.dp))
                    Box(Modifier.height(16.dp).width(1.dp).background(Color.Gray.copy(alpha = 0.2f)))
                    Spacer(Modifier.width(16.dp))
                    Icon(Icons.Filled.UnfoldMore, null, modifier = Modifier.size(16.dp).alpha(0.5f))
                }
            }
        }
    }
}

@Composable
fun PdfPageReaderItem(
    uri: Uri,
    index: Int,
    password: String?,
    imageLoader: ImageLoader,
    pageSize: Pair<Float, Float>?,
    links: List<PdfLink>,
    onLinkClick: (String) -> Unit
) {
    val request = remember(uri, index, password) { 
        PdfPageRequest(uri, index, password, 1.5f) // High Res
    }
    
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(pageSize?.let { it.first / it.second } ?: 0.707f)
            .background(Color.White)
    ) {
        val painter = rememberAsyncImagePainter(request, imageLoader)
        
        Image(
            painter = painter,
            contentDescription = "Page ${index + 1}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        if (painter.state is AsyncImagePainter.State.Loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PaperPink, modifier = Modifier.size(32.dp))
            }
        }

        // --- ACCURATE LINK MAPPING ---
        if (pageSize != null) {
            links.forEach { link ->
                val pageWidth = pageSize.first
                val pageHeight = pageSize.second
                
                // Ratio of screen pixels to PDF points
                val scaleX = maxWidth.value / pageWidth
                val scaleY = maxHeight.value / pageHeight
                
                // PDF: (0,0) bottom-left. Box: (0,0) top-left.
                val left = link.rect.lowerLeftX * scaleX
                val top = (pageHeight - link.rect.upperRightY) * scaleY
                val width = (link.rect.upperRightX - link.rect.lowerLeftX) * scaleX
                val height = (link.rect.upperRightY - link.rect.lowerLeftY) * scaleY
                
                Box(
                    modifier = Modifier
                        .offset(x = left.dp, y = top.dp)
                        .size(width = width.dp, height = height.dp)
                        .clickable { onLinkClick(link.url) }
                )
            }
        }
    }
}
