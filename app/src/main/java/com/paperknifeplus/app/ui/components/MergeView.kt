package com.paperknifeplus.app.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
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
import coil.compose.rememberAsyncImagePainter
import com.paperknifeplus.app.data.image.PdfPageRequest
import com.paperknifeplus.app.ui.theme.PaperPink
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Collections

data class MergeFile(
    val uri: Uri,
    val name: String,
    val size: String,
    val isLocked: Boolean,
    var password: String? = null,
    var isUnlocked: Boolean = false,
    val decryptedUri: Uri? = null,
    val pageCount: Int = 0
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MergeView(
    initialUri: Uri? = null,
    initialPassword: String? = null,
    onBack: () -> Unit,
    onOpenPreview: (Uri, String, Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val accentColor = Color(0xFFF43F5E)

    var currentState by remember { mutableStateOf<ToolState>(ToolState.SELECTING) }
    var selectedFiles by remember { mutableStateOf<List<MergeFile>>(emptyList()) }
    var processingTime by remember { mutableStateOf("") }
    var progressCount by remember { mutableIntStateOf(0) }
    var isFileLoading by remember { mutableStateOf(false) }
    var mergedUri by remember { mutableStateOf<Uri?>(null) }
    
    // UI State
    var fileToUnlock by remember { mutableStateOf<MergeFile?>(null) }
    var lightboxFile by remember { mutableStateOf<MergeFile?>(null) }

    val imageLoader = coil.compose.LocalImageLoader.current

    fun handleUris(uris: List<Uri>, password: String? = null) {
        if (uris.isNotEmpty()) {
            isFileLoading = true
            scope.launch {
                val currentUris = selectedFiles.map { it.uri }
                val newFiles = uris.filter { it !in currentUris }.map { uri ->
                    val details = getUriDetails(context, uri)
                    val isLocked = checkIsEncryptedLocal(context, uri)
                    val count = if (!isLocked) getPageCount(context, uri, null) else 0
                    MergeFile(uri, details.name, details.size, isLocked, password = password, pageCount = count)
                }
                if (newFiles.isNotEmpty()) {
                    selectedFiles = selectedFiles + newFiles
                    currentState = ToolState.CONFIGURING
                }
                isFileLoading = false
            }
        }
    }

    LaunchedEffect(initialUri, initialPassword) {
        if (initialUri != null) {
            handleUris(listOf(initialUri), initialPassword)
        }
    }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        handleUris(uris)
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { saveUri ->
            currentState = ToolState.PROCESSING
            val startTime = System.currentTimeMillis()
            scope.launch(Dispatchers.IO) {
                val sourcesToClose = mutableListOf<InputStream>()
                try {
                    val merger = PDFMergerUtility()
                    context.contentResolver.openOutputStream(saveUri, "w")?.use { outputStream ->
                        merger.destinationStream = outputStream
                        selectedFiles.forEachIndexed { index, file ->
                            withContext(Dispatchers.Main) { progressCount = index + 1 }
                            val uriToLoad = file.decryptedUri ?: file.uri
                            if (file.password != null && file.decryptedUri == null) {
                                context.contentResolver.openInputStream(uriToLoad)?.use { pSourceStream ->
                                    PDDocument.load(pSourceStream, file.password).use { doc ->
                                        doc.isAllSecurityToBeRemoved = true
                                        val baos = ByteArrayOutputStream()
                                        doc.save(baos)
                                        val bais = ByteArrayInputStream(baos.toByteArray())
                                        sourcesToClose.add(bais)
                                        merger.addSource(bais)
                                    }
                                }
                            } else {
                                context.contentResolver.openInputStream(uriToLoad)?.let { inputStream ->
                                    sourcesToClose.add(inputStream)
                                    merger.addSource(inputStream)
                                }
                            }
                        }
                        merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())
                        outputStream.flush()
                    }
                    val endTime = System.currentTimeMillis()
                    val timeStr = String.format("%.1fs", (endTime - startTime) / 1000.0)
                    val finalCount = getPageCount(context, saveUri, null)
                    withContext(Dispatchers.Main) {
                        processingTime = timeStr
                        mergedUri = saveUri
                        SessionManager.addEntry("Merged PDF", "Merge", "${selectedFiles.size} files joined", Icons.Filled.Layers, saveUri, finalCount)
                        currentState = ToolState.SUCCESS
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Merge failed: ${e.message}", Toast.LENGTH_LONG).show()
                        currentState = ToolState.CONFIGURING
                    }
                } finally {
                    sourcesToClose.forEach { try { it.close() } catch (e: Exception) {} }
                }
            }
        }
    }

    LaunchedEffect(Unit) { PDFBoxResourceLoader.init(context) }

    Scaffold(
        topBar = {
            if (currentState != ToolState.SUCCESS && currentState != ToolState.PROCESSING) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Merge", fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Text("JOIN MULTIPLE DOCUMENTS", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
                        }
                        if (selectedFiles.isNotEmpty() && currentState == ToolState.CONFIGURING) {
                            TextButton(onClick = { selectedFiles = emptyList(); currentState = ToolState.SELECTING }) {
                                Text("CHANGE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isFileLoading) {
                LoadingStateView(accentColor, false, "Reading files...")
            } else {
                when (currentState) {
                    ToolState.SELECTING -> {
                        SelectionGrid(
                            onSelect = { pickLauncher.launch("application/pdf") },
                            isDark = isDark,
                            icon = Icons.Filled.Layers,
                            title = "Tap to choose files",
                            subtitle = "SELECT MULTIPLE PDFS",
                            accentColor = accentColor,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)
                        )
                    }
                    
                    ToolState.CONFIGURING -> {
                        val listState = rememberLazyListState()
                        var draggedItem by remember { mutableStateOf<MergeFile?>(null) }
                        var dragOffsetY by remember { mutableFloatStateOf(0f) }
                        var lastReorderIndex by remember { mutableIntStateOf(-1) }

                        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.SwapVert, null, tint = accentColor.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Hold & Drag to reorder", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            }

                            LazyColumn(
                                state = listState,
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp)
                            ) {
                                itemsIndexed(
                                    items = selectedFiles,
                                    key = { _, file -> file.uri.toString() }
                                ) { index, file ->
                                    val isDragging = draggedItem?.uri == file.uri
                                    val elevation by animateDpAsState(
                                        targetValue = if (isDragging) 12.dp else 0.dp,
                                        label = "elevation"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .zIndex(if (isDragging) 10f else 1f)
                                            .graphicsLayer {
                                                if (isDragging) {
                                                    translationY = dragOffsetY
                                                    scaleX = 1.03f
                                                    scaleY = 1.03f
                                                }
                                            }
                                            .animateItemPlacement()
                                            .shadow(elevation, RoundedCornerShape(16.dp))
                                            .pointerInput(file.uri) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = {
                                                        draggedItem = file
                                                        lastReorderIndex = index
                                                        dragOffsetY = 0f
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragOffsetY += dragAmount.y

                                                        val layoutInfo = listState.layoutInfo
                                                        val itemInfo = layoutInfo.visibleItemsInfo.find { it.key == file.uri.toString() } ?: return@detectDragGesturesAfterLongPress
                                                        val itemCenterY = itemInfo.offset + itemInfo.size / 2 + dragOffsetY

                                                        val targetItem = layoutInfo.visibleItemsInfo.firstOrNull { info ->
                                                            info.key != file.uri.toString() &&
                                                            itemCenterY.toInt() in info.offset..(info.offset + info.size)
                                                        }

                                                        if (targetItem != null && targetItem.index != lastReorderIndex) {
                                                            if (selectedFiles.isNotEmpty() && lastReorderIndex in selectedFiles.indices) {
                                                                val currentList = selectedFiles.toMutableList()
                                                                val item = currentList.removeAt(lastReorderIndex)
                                                                val newPosition = if (targetItem.index > lastReorderIndex) {
                                                                    (targetItem.index).coerceIn(0, currentList.size)
                                                                } else {
                                                                    targetItem.index.coerceIn(0, currentList.size)
                                                                }
                                                                currentList.add(newPosition, item)
                                                                selectedFiles = currentList
                                                                lastReorderIndex = newPosition
                                                                dragOffsetY -= (targetItem.offset - itemInfo.offset).toFloat()
                                                            }
                                                        }
                                                    },
                                                    onDragEnd = {
                                                        draggedItem = null
                                                        dragOffsetY = 0f
                                                        lastReorderIndex = -1
                                                    },
                                                    onDragCancel = {
                                                        draggedItem = null
                                                        dragOffsetY = 0f
                                                        lastReorderIndex = -1
                                                    }
                                                )
                                            }
                                    ) {
                                        MergeFileListItem(
                                            file = file,
                                            index = index,
                                            accentColor = accentColor,
                                            imageLoader = imageLoader,
                                            onDelete = { selectedFiles = selectedFiles.filterIndexed { i, _ -> i != index } },
                                            onUnlock = { fileToUnlock = file },
                                            onClick = { lightboxFile = file }
                                        )
                                    }
                                }
                                
                                item {
                                    Surface(
                                        onClick = { pickLauncher.launch("application/pdf") },
                                        modifier = Modifier.fillMaxWidth().height(80.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        color = accentColor.copy(0.05f),
                                        border = BorderStroke(1.dp, accentColor.copy(0.2f))
                                    ) {
                                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Add, null, tint = accentColor)
                                            Spacer(Modifier.width(8.dp))
                                            Text("ADD MORE FILES", fontSize = 11.sp, fontWeight = FontWeight.Black, color = accentColor)
                                        }
                                    }
                                }
                            }
                            
                            val allReady = selectedFiles.size > 1 && selectedFiles.all { !it.isLocked || it.isUnlocked }
                            Button(
                                onClick = { saveLauncher.launch("merged_${System.currentTimeMillis() / 1000}.pdf") },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).height(60.dp),
                                enabled = allReady,
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accentColor, 
                                    contentColor = Color.White,
                                    disabledContainerColor = accentColor.copy(alpha = 0.3f)
                                )
                            ) {
                                Text(if (allReady) "MERGE ${selectedFiles.size} FILES" else "UNLOCK ALL FILES FIRST", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    
                    ToolState.PROCESSING -> {
                        ProcessingStateView(
                            accentColor = accentColor,
                            uri = selectedFiles.firstOrNull()?.decryptedUri ?: selectedFiles.firstOrNull()?.uri,
                            password = if (selectedFiles.firstOrNull()?.decryptedUri != null) null else selectedFiles.firstOrNull()?.password,
                            text = "Merging documents...",
                            current = progressCount,
                            total = selectedFiles.size,
                            showWarning = false
                        )
                    }
                    ToolState.SUCCESS -> {
                        SuccessView(
                            message = "Merge Complete",
                            subMessage = "Successfully joined ${selectedFiles.size} documents",
                            processingTime = processingTime,
                            onDone = onBack,
                            onProcessMore = { selectedFiles = emptyList(); mergedUri = null; currentState = ToolState.SELECTING },
                            onPreview = { mergedUri?.let { uri -> scope.launch { onOpenPreview(uri, "Merged PDF", getPageCount(context, uri, null)) } } },
                            accentColor = accentColor
                        )
                    }
                    else -> {}
                }
            }
            
            if (fileToUnlock != null) {
                val currentFileToUnlock = fileToUnlock!!
                LockedFilePrompt(
                    fileName = currentFileToUnlock.name,
                    onDismiss = { fileToUnlock = null },
                    onUnlocked = { password ->
                        isFileLoading = true
                        scope.launch(Dispatchers.IO) {
                            val decryptedUri = decryptToCache(context, currentFileToUnlock.uri, password)
                            if (decryptedUri != null) {
                                val count = getPageCount(context, decryptedUri, null)
                                withContext(Dispatchers.Main) {
                                    selectedFiles = selectedFiles.map { if (it.uri == currentFileToUnlock.uri) it.copy(isUnlocked = true, password = password, decryptedUri = decryptedUri, pageCount = count) else it }
                                    fileToUnlock = null
                                    isFileLoading = false
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Invalid Password", Toast.LENGTH_SHORT).show()
                                    isFileLoading = false
                                }
                            }
                        }
                    },
                    accentColor = accentColor,
                    isLoading = isFileLoading
                )
            }
        }
    }

    if (lightboxFile != null) {
        val currentLightboxFile = lightboxFile!!
        PageLightbox(
            uri = currentLightboxFile.decryptedUri ?: currentLightboxFile.uri,
            initialPage = 0,
            totalCount = currentLightboxFile.pageCount,
            password = if (currentLightboxFile.decryptedUri != null) null else currentLightboxFile.password,
            onDismiss = { lightboxFile = null }
        )
    }
}

@Composable
fun MergeFileListItem(
    file: MergeFile,
    index: Int,
    accentColor: Color,
    imageLoader: coil.ImageLoader,
    onDelete: () -> Unit,
    onUnlock: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.Gray.copy(0.1f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            // Thumbnail
            Box(Modifier.width(80.dp).fillMaxHeight().background(Color.Gray.copy(0.05f))) {
                if (file.isLocked && !file.isUnlocked) {
                    Icon(Icons.Outlined.Lock, null, tint = accentColor.copy(0.3f), modifier = Modifier.align(Alignment.Center).size(24.dp))
                } else {
                    val request = remember(file.uri, file.decryptedUri, file.password) { 
                        PdfPageRequest(file.decryptedUri ?: file.uri, 0, if (file.decryptedUri != null) null else file.password, 0.4f) 
                    }
                    Image(
                        painter = rememberAsyncImagePainter(request, imageLoader),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clickable { onClick() },
                        contentScale = ContentScale.Crop
                    )
                }
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
                    color = Color.Black.copy(0.6f),
                    shape = CircleShape
                ) {
                    Text("${index + 1}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
                }
            }
            
            // Info
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(file.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                Text(file.size, fontSize = 11.sp, color = Color.Gray)
                if (file.isLocked && !file.isUnlocked) {
                    Text("LOCKED", fontSize = 9.sp, fontWeight = FontWeight.Black, color = accentColor)
                }
            }
            
            // Actions
            if (file.isLocked && !file.isUnlocked) {
                IconButton(onClick = onUnlock) {
                    Icon(Icons.Filled.LockOpen, null, tint = accentColor)
                }
            }
            
            IconButton(onClick = onDelete, modifier = Modifier.padding(end = 8.dp)) {
                Icon(Icons.Filled.DeleteOutline, null, tint = Color.Gray)
            }
            
            Icon(Icons.Filled.DragHandle, null, tint = Color.Gray.copy(0.3f), modifier = Modifier.padding(end = 12.dp))
        }
    }
}
