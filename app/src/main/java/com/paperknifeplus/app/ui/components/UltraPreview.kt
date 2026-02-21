package com.paperknifeplus.app.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UltraPreview(
    uri: Uri,
    fileName: String,
    pageCount: Int,
    password: String? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    var showJumpDialog by remember { mutableStateOf(false) }
    var jumpPageInput by remember { mutableStateOf("") }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    
    var links by remember { mutableStateOf<List<PdfLink>>(emptyList()) }
    var pageSizes by remember { mutableStateOf<Map<Int, Pair<Float, Float>>>(emptyMap()) }
    var isInitializing by remember { mutableStateOf(true) }
    var activeUri by remember { mutableStateOf(uri) }
    var activePassword by remember { mutableStateOf(password) }
    var fileToUnlock by remember { mutableStateOf<String?>(null) }
    var isDecrypting by remember { mutableStateOf(false) }
    var activePageCount by remember { mutableIntStateOf(pageCount) }

    // NITRO 12.0: Platinum High-Res Reader ImageLoader (45% RAM usage)
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(PdfPageFetcher.Factory(context)) }
            .memoryCache {
                coil.memory.MemoryCache.Builder(context)
                    .maxSizePercent(0.45)
                    .build()
            }
            .build()
    }

    DisposableEffect(uri) {
        onDispose {
            if (activeUri != uri) {
                try {
                    activeUri.path?.let { File(it).delete() }
                } catch (e: Exception) {}
            }
        }
    }

    fun initializeReader(targetUri: Uri, targetPass: String?) {
        scope.launch(Dispatchers.IO) {
            isInitializing = true
            try {
                var workingUri = targetUri
                var workingPass = targetPass

                if (workingPass == null) {
                    val isEncrypted = checkIsEncryptedLocal(context, workingUri)
                    if (isEncrypted) {
                        withContext(Dispatchers.Main) {
                            fileToUnlock = fileName
                            isInitializing = false
                        }
                        return@launch
                    }
                }

                if (workingPass != null) {
                    isDecrypting = true
                    val cachedUri = decryptToCache(context, workingUri, workingPass)
                    if (cachedUri != null) {
                        workingUri = cachedUri
                        workingPass = null
                    }
                    isDecrypting = false
                }

                activeUri = workingUri
                activePassword = workingPass

                context.contentResolver.openInputStream(activeUri)?.use { inputStream ->
                    val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputStream)
                    val count = document.numberOfPages
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
                        activePageCount = count
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

    LaunchedEffect(uri) {
        initializeReader(uri, password)
    }

    Scaffold(
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isInitializing) {
                LoadingStateView(PaperPink, false, "Loading Platinum View...")
            } else if (fileToUnlock == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { tapOffset ->
                                    if (scale > 1.01f) {
                                        scale = 1f
                                        offset = androidx.compose.ui.geometry.Offset.Zero
                                    } else {
                                        scale = 3f
                                        val x = (size.width / 2 - tapOffset.x) * (3f - 1f)
                                        val y = (size.height / 2 - tapOffset.y) * (3f - 1f)
                                        offset = androidx.compose.ui.geometry.Offset(x, y)
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (scale * zoom).coerceIn(1f, 8f)
                                scale = newScale
                                if (scale > 1f) {
                                    offset += pan
                                } else {
                                    offset = androidx.compose.ui.geometry.Offset.Zero
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
                        contentPadding = PaddingValues(bottom = 120.dp), // REMOVED TOP PADDING TO FIX VOID
                        verticalArrangement = Arrangement.spacedBy(0.dp), 
                        horizontalAlignment = Alignment.CenterHorizontally,
                        userScrollEnabled = scale == 1f 
                    ) {
                        items(activePageCount) { index ->
                            PdfPageReaderItem(
                                uri = activeUri,
                                index = index,
                                password = activePassword,
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
            }

            if (!isInitializing && fileToUnlock == null) {
                val currentPage by remember { derivedStateOf { listState.firstVisibleItemIndex + 1 } }
                var trackHeight by remember { mutableFloatStateOf(0f) }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .fillMaxHeight(0.75f)
                        .width(48.dp)
                        .onGloballyPositioned { trackHeight = it.size.height.toFloat() }
                        .pointerInput(activePageCount, trackHeight) {
                            detectTapGestures { offset ->
                                val newPercent = (offset.y / trackHeight).coerceIn(0f, 1f)
                                val targetPage = (newPercent * (activePageCount - 1)).roundToInt()
                                scope.launch { listState.scrollToItem(targetPage) }
                            }
                        }
                ) {
                    val scrollPercentage = if (activePageCount > 1) listState.firstVisibleItemIndex.toFloat() / (activePageCount - 1) else 0f
                    Box(
                        modifier = Modifier.fillMaxHeight().width(6.dp).align(Alignment.Center).background(Color.Gray.copy(alpha = 0.15f), CircleShape)
                    )
                    val minThumbHeight = 48.dp
                    val thumbHeightFactor = 1f / activePageCount.coerceAtLeast(1)
                    Box(
                        modifier = Modifier
                            .heightIn(min = minThumbHeight)
                            .fillMaxHeight(thumbHeightFactor.coerceIn(0.08f, 0.25f))
                            .width(12.dp)
                            .graphicsLayer {
                                val minThumbHeightPx = with(density) { minThumbHeight.toPx() }
                                val thumbHeightPx = (trackHeight * thumbHeightFactor.coerceIn(0.08f, 0.25f)).coerceAtLeast(minThumbHeightPx)
                                translationY = scrollPercentage * (trackHeight - thumbHeightPx)
                            }
                            .background(PaperPink, CircleShape)
                            .align(Alignment.TopCenter)
                            .draggable(
                                orientation = Orientation.Vertical,
                                state = rememberDraggableState { delta ->
                                    if (trackHeight > 0) {
                                        val minThumbHeightPx = with(density) { minThumbHeight.toPx() }
                                        val thumbHeightPx = (trackHeight * thumbHeightFactor.coerceIn(0.08f, 0.25f)).coerceAtLeast(minThumbHeightPx)
                                        val newPercent = (scrollPercentage + delta / (trackHeight - thumbHeightPx)).coerceIn(0f, 1f)
                                        val targetPage = (newPercent * (activePageCount - 1)).roundToInt()
                                        scope.launch { listState.scrollToItem(targetPage) }
                                    }
                                }
                            )
                    )
                }

                Box(
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp)
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
                            Text(" / $activePageCount PAGES", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(start = 4.dp))
                            Spacer(Modifier.width(16.dp))
                            Box(Modifier.height(16.dp).width(1.dp).background(Color.Gray.copy(alpha = 0.2f)))
                            Spacer(Modifier.width(16.dp))
                            Icon(Icons.Filled.UnfoldMore, null, modifier = Modifier.size(16.dp).alpha(0.5f), tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.ArrowBack, "Back", modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurface)
                    }
                    
                    Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        Text(fileName, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
                        Text("PLATINUM ULTRA READER", fontSize = 8.sp, fontWeight = FontWeight.Black, color = PaperPink, letterSpacing = 1.sp)
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
                Divider(color = Color.Gray.copy(alpha = 0.1f))
            }
        }

        if (fileToUnlock != null) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(top = 100.dp)) {
                LockedFilePrompt(
                    fileName = fileToUnlock!!,
                    onDismiss = onDismiss,
                    onUnlocked = { pass ->
                        fileToUnlock = null
                        initializeReader(uri, pass)
                    },
                    accentColor = PaperPink,
                    isLoading = isDecrypting
                )
            }
        }

        if (showJumpDialog) {
            AlertDialog(
                onDismissRequest = { showJumpDialog = false },
                title = { Text("Go to Page", fontWeight = FontWeight.Black) },
                text = {
                    OutlinedTextField(
                        value = jumpPageInput,
                        onValueChange = { if (it.all { char -> char.isDigit() }) jumpPageInput = it },
                        label = { Text("Page Number (1-$activePageCount)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onGo = {
                            val pageNum = jumpPageInput.toIntOrNull()
                            if (pageNum != null && pageNum in 1..activePageCount) {
                                scope.launch { listState.scrollToItem(pageNum - 1) }
                                showJumpDialog = false
                            }
                        })
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val pageNum = jumpPageInput.toIntOrNull()
                        if (pageNum != null && pageNum in 1..activePageCount) {
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
    // NITRO 12.0: Pro 6.0f Resolution
    val lowResRequest = remember(uri, index, password) { PdfPageRequest(uri, index, password, 0.7f, priority = 1) }
    val highResRequest = remember(uri, index, password) { PdfPageRequest(uri, index, password, 6.0f, priority = 0) }
    
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(pageSize?.let { it.first / it.second } ?: 0.707f)
            .background(Color.White)
    ) {
        val lowResPainter = rememberAsyncImagePainter(lowResRequest, imageLoader)
        val highResPainter = rememberAsyncImagePainter(highResRequest, imageLoader)
        
        Image(
            painter = lowResPainter,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        AnimatedVisibility(
            visible = highResPainter.state is AsyncImagePainter.State.Success,
            enter = fadeIn(tween(400)),
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

        if (pageSize != null) {
            val pageWidthDp = maxWidth
            val pageHeightDp = maxHeight
            links.forEach { link ->
                val scaleX = pageWidthDp.value / pageSize.first
                val scaleY = pageHeightDp.value / pageSize.second
                val left = link.rect.lowerLeftX * scaleX
                val top = (pageSize.second - link.rect.upperRightY) * scaleY
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
