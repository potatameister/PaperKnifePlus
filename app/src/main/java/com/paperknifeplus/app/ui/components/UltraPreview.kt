package com.paperknifeplus.app.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.onGloballyPositioned
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
import java.io.File
import kotlin.math.roundToInt

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
    var searchProgress by remember { mutableStateOf(0f) }
    var searchResults by remember { mutableStateOf<List<TextMatch>>(emptyList()) }
    var currentMatchIndex by remember { mutableIntStateOf(0) }

    var showJumpDialog by remember { mutableStateOf(false) }
    var jumpPageInput by remember { mutableStateOf("") }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    
    var links by remember { mutableStateOf<List<PdfLink>>(emptyList()) }
    var pageSizes by remember { mutableStateOf<Map<Int, Pair<Float, Float>>>(emptyMap()) }
    var isInitializing by remember { mutableStateOf(true) }
    var activeUri by remember { mutableStateOf(uri) }
    var activePassword by remember { mutableStateOf(password) }

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

    DisposableEffect(uri) {
        onDispose {
            // Cleanup decrypted cache if it exists
            if (activeUri != uri) {
                try {
                    activeUri.path?.let { File(it).delete() }
                } catch (e: Exception) {}
            }
        }
    }

    LaunchedEffect(uri) {
        isInitializing = true
        withContext(Dispatchers.IO) {
            try {
                // 1. Decrypt to cache if password exists for "Nitro" speed rendering
                if (password != null) {
                    val cachedUri = decryptToCache(context, uri, password)
                    if (cachedUri != null) {
                        activeUri = cachedUri
                        activePassword = null // Cached file is now unencrypted
                    }
                }

                // 2. Fetch page dimensions & links using active (potentially cached) URI
                context.contentResolver.openInputStream(activeUri)?.use { inputStream ->
                    val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputStream)
                    val sizes = document.pages.mapIndexed { index, page ->
                        index to (page.mediaBox.width to page.mediaBox.height)
                    }.toMap()
                    
                    val extractedLinks = mutableListOf<PdfLink>()
                    document.pages.forEachIndexed { index, page ->
                        page.annotations.filterIsInstance<com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink>().forEach { link ->
                            val action = link.action
                            if (action is com.tom_roush.pdfbox.pdmodel.interactive.action.PDActionURI) {
                                extractedLinks.add(PdfLink(index, link.rectangle, action.uri))
                            }
                        }
                    }

                    withContext(Dispatchers.Main) { 
                        pageSizes = sizes 
                        links = extractedLinks
                        isInitializing = false
                    }
                    document.close()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { 
                    isInitializing = false 
                    Toast.makeText(context, "Failed to load document", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (MaterialTheme.colorScheme.background == Color.Black) Color.Black else Color(0xFFF4F4F7))
    ) {
        if (isInitializing) {
            LoadingStateView(PaperPink, false, "Decryption & Nitro Optimization...")
        } else {
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
                            uri = activeUri,
                            index = index,
                            password = activePassword,
                            imageLoader = imageLoader,
                            pageSize = pageSizes[index],
                            links = links.filter { it.pageIndex == index },
                            matches = searchResults.filter { it.pageIndex == index },
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
                        Icon(Icons.Filled.ArrowBack, "Back", modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurface)
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
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(end = 8.dp)) {
                                        CircularProgressIndicator(progress = { searchProgress }, modifier = Modifier.size(24.dp), strokeWidth = 3.dp, color = PaperPink)
                                        Text("${(searchProgress * 100).toInt()}%", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (searchResults.isNotEmpty()) {
                                            Text("${currentMatchIndex + 1}/${searchResults.size}", fontSize = 10.sp, color = Color.Gray)
                                            IconButton(onClick = {
                                                currentMatchIndex = if (currentMatchIndex > 0) currentMatchIndex - 1 else searchResults.size - 1
                                                scope.launch { listState.scrollToItem(searchResults[currentMatchIndex].pageIndex) }
                                            }) { Icon(Icons.Filled.KeyboardArrowUp, null) }
                                            IconButton(onClick = {
                                                currentMatchIndex = (currentMatchIndex + 1) % searchResults.size
                                                scope.launch { listState.scrollToItem(searchResults[currentMatchIndex].pageIndex) }
                                            }) { Icon(Icons.Filled.KeyboardArrowDown, null) }
                                        }
                                        IconButton(onClick = { isSearching = false; searchQuery = ""; searchResults = emptyList() }) {
                                            Icon(Icons.Filled.Close, null)
                                        }
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = {
                                if (searchQuery.isNotBlank()) {
                                    isSearchLoading = true
                                    searchProgress = 0f
                                    scope.launch {
                                        val matches = findAllTextMatches(context, activeUri, activePassword, searchQuery) { current, total ->
                                            searchProgress = current.toFloat() / total
                                        }
                                        searchResults = matches
                                        currentMatchIndex = 0
                                        if (matches.isNotEmpty()) {
                                            listState.scrollToItem(matches[0].pageIndex)
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
                            Text(fileName, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
                            Text("PREMIUM READER", fontSize = 8.sp, fontWeight = FontWeight.Black, color = PaperPink, letterSpacing = 1.sp)
                        }
                        
                        Row {
                            IconButton(onClick = { isSearching = true }) {
                                Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.onSurface)
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
                                Icon(Icons.Filled.Share, null, tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
                Divider(color = Color.Gray.copy(alpha = 0.1f))
            }
        }

        // --- JUMP TO PAGE DIALOG ---
        if (showJumpDialog) {
            AlertDialog(
                onDismissRequest = { showJumpDialog = false },
                title = { Text("Go to Page", fontWeight = FontWeight.Black) },
                text = {
                    OutlinedTextField(
                        value = jumpPageInput,
                        onValueChange = { if (it.all { char -> char.isDigit() }) jumpPageInput = it },
                        label = { Text("Page Number (1-$pageCount)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onGo = {
                            val pageNum = jumpPageInput.toIntOrNull()
                            if (pageNum != null && pageNum in 1..pageCount) {
                                scope.launch { listState.scrollToItem(pageNum - 1) }
                                showJumpDialog = false
                            }
                        })
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val pageNum = jumpPageInput.toIntOrNull()
                        if (pageNum != null && pageNum in 1..pageCount) {
                            scope.launch { listState.scrollToItem(pageNum - 1) }
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

        // --- BOTTOM INDICATOR & NITRO FAST SCROLL BAR ---
        val currentPage by remember { derivedStateOf { listState.firstVisibleItemIndex + 1 } }
        var trackHeight by remember { mutableFloatStateOf(0f) }

        // Custom Scrollbar with Robust Draggable Thumb
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
                .fillMaxHeight(0.6f)
                .width(40.dp)
                .onGloballyPositioned { trackHeight = it.size.height.toFloat() }
        ) {
            val scrollPercentage = if (pageCount > 1) listState.firstVisibleItemIndex.toFloat() / (pageCount - 1) else 0f
            
            // Track
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .align(Alignment.Center)
                    .background(Color.Gray.copy(alpha = 0.1f), CircleShape)
            )
            
            // Thumb
            Box(
                modifier = Modifier
                    .fillMaxHeight(1f / pageCount.coerceAtLeast(5))
                    .width(10.dp)
                    .graphicsLayer {
                        translationY = scrollPercentage * (trackHeight - (trackHeight / pageCount.coerceAtLeast(5)))
                    }
                    .background(PaperPink, CircleShape)
                    .align(Alignment.TopCenter)
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            if (trackHeight > 0) {
                                val newPercent = (scrollPercentage + delta / trackHeight).coerceIn(0f, 1f)
                                val targetPage = (newPercent * (pageCount - 1)).roundToInt()
                                scope.launch { listState.scrollToItem(targetPage) }
                            }
                        }
                    )
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Surface(
                onClick = {
                    jumpPageInput = currentPage.toString()
                    showJumpDialog = true
                },
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
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
                    Icon(Icons.Filled.UnfoldMore, null, modifier = Modifier.size(16.dp).alpha(0.5f), tint = MaterialTheme.colorScheme.onSurface)
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
    matches: List<TextMatch>,
    onLinkClick: (String) -> Unit
) {
    // NITRO 4.0: Two-Stage Progressive Loading
    val lowResRequest = remember(uri, index, password) { 
        PdfPageRequest(uri, index, password, 0.5f, priority = 1) 
    }
    val highResRequest = remember(uri, index, password) { 
        PdfPageRequest(uri, index, password, 1.5f, priority = 0) 
    }
    
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(pageSize?.let { it.first / it.second } ?: 0.707f)
            .background(Color.White)
    ) {
        val lowResPainter = rememberAsyncImagePainter(lowResRequest, imageLoader)
        val highResPainter = rememberAsyncImagePainter(highResRequest, imageLoader)
        
        // Layer 1: Ghost/Thumb (Instantly visible from cache)
        Image(
            painter = lowResPainter,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // Layer 2: High-Res (Fades in over ghost)
        AnimatedVisibility(
            visible = highResPainter.state is AsyncImagePainter.State.Success,
            enter = fadeIn(tween(300)),
            exit = fadeOut()
        ) {
            Image(
                painter = highResPainter,
                contentDescription = "Page ${index + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        if (lowResPainter.state is AsyncImagePainter.State.Loading && highResPainter.state is AsyncImagePainter.State.Loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PaperPink, modifier = Modifier.size(32.dp))
            }
        }

        // --- ACCURATE LINK & MATCH MAPPING (NITRO ENGINE 3.0) ---
        if (pageSize != null) {
            val pageWidth = pageSize.first
            val pageHeight = pageSize.second
            val scaleX = maxWidth.value / pageWidth
            val scaleY = maxHeight.value / pageHeight

            // Draw Links (PDF coords are bottom-up)
            links.forEach { link ->
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

            // Draw Search Highlights
            matches.forEach { match ->
                val left = match.rect.lowerLeftX * scaleX
                // NITRO: Match rect from ToolUtils is already bottom-up PDF coords
                val top = (pageHeight - match.rect.upperRightY) * scaleY
                val width = match.rect.width * scaleX
                val height = match.rect.height * scaleY

                Box(
                    modifier = Modifier
                        .offset(x = left.dp, y = top.dp)
                        .size(width = width.dp, height = height.dp)
                        .background(Color.Yellow.copy(alpha = 0.35f))
                        .border(0.5.dp, Color.Yellow.copy(alpha = 0.5f))
                )
            }
        }
    }
}
