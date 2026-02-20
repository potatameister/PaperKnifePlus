package com.paperknifeplus.app.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.ImageLoader
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

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            isFileLoading = true
            scope.launch {
                val currentUris = selectedFiles.map { it.uri }
                val newFiles = uris.filter { it !in currentUris }.map { uri ->
                    val details = getUriDetails(context, uri)
                    val isLocked = checkIsEncryptedLocal(context, uri)
                    val count = if (!isLocked) getPageCount(context, uri, null) else 0
                    MergeFile(uri, details.name, details.size, isLocked, pageCount = count)
                }
                if (newFiles.isNotEmpty()) {
                    selectedFiles = selectedFiles + newFiles
                    currentState = ToolState.CONFIGURING
                }
                isFileLoading = false
            }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { saveUri ->
            currentState = ToolState.PROCESSING
            val startTime = System.currentTimeMillis()
            scope.launch(Dispatchers.IO) {
                val sourcesToClose = mutableListOf<InputStream>()
                try {
                    val merger = PDFMergerUtility()
                    context.contentResolver.openOutputStream(saveUri)?.use { outputStream ->
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
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), CircleShape)) {
                        Icon(Icons.Filled.ArrowBack, "Back", modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Merge", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                        Text("COMBINE MULTIPLE PDFS", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isFileLoading) {
                LoadingStateView(accentColor, false, "Processing files...")
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
                        var draggedIndex by remember { mutableStateOf<Int?>(null) }
                        var dragOffset by remember { mutableStateOf(0f) }
                        var itemHeightPx by remember { mutableFloatStateOf(0f) }

                        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Info, null, tint = accentColor.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Long-press handle to reorder • Tap preview to inspect",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                            }

                            LazyColumn(
                                state = listState,
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                itemsIndexed(selectedFiles, key = { _, file -> file.uri.toString() }) { index, file ->
                                    val isDragging = draggedIndex == index
                                    val itemOffset = if (isDragging) dragOffset else 0f
                                    
                                    Box(
                                        modifier = Modifier
                                            .zIndex(if (isDragging) 10f else 1f)
                                            .onGloballyPositioned { if (!isDragging && index == 0) itemHeightPx = it.size.height.toFloat() + 10.dp.value }
                                            .graphicsLayer { translationY = itemOffset }
                                            .animateItemPlacement(
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            )
                                            .shadow(
                                                elevation = if (isDragging) 16.dp else 0.dp,
                                                shape = RoundedCornerShape(20.dp),
                                                spotColor = accentColor
                                            )
                                            .pointerInput(Unit) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = { draggedIndex = index },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragOffset += dragAmount.y
                                                        
                                                        val height = if (itemHeightPx > 0) itemHeightPx else 200f
                                                        val currentIndex = draggedIndex ?: return@detectDragGesturesAfterLongPress
                                                        val offsetSlots = (dragOffset / height).toInt()
                                                        val targetIndex = (currentIndex + offsetSlots).coerceIn(0, selectedFiles.size - 1)
                                                        
                                                        if (targetIndex != currentIndex) {
                                                            val newList = selectedFiles.toMutableList()
                                                            val item = newList.removeAt(currentIndex)
                                                            newList.add(targetIndex, item)
                                                            selectedFiles = newList
                                                            
                                                            // NITRO COMPENSATION: Adjust dragOffset to maintain thumb position
                                                            dragOffset -= (targetIndex - currentIndex) * height
                                                            draggedIndex = targetIndex
                                                        }
                                                    },
                                                    onDragEnd = { draggedIndex = null; dragOffset = 0f },
                                                    onDragCancel = { draggedIndex = null; dragOffset = 0f }
                                                )
                                            }
                                    ) {
                                        MergeFileItem(
                                            file = file,
                                            index = index,
                                            totalCount = selectedFiles.size,
                                            isDark = isDark,
                                            accentColor = accentColor,
                                            imageLoader = imageLoader,
                                            onDelete = { selectedFiles = selectedFiles.filterIndexed { i, _ -> i != index } },
                                            onUnlock = { fileToUnlock = file },
                                            onClick = { lightboxFile = file }
                                        )
                                    }
                                }
                                
                                item {
                                    OutlinedButton(
                                        onClick = { pickLauncher.launch("application/pdf") },
                                        modifier = Modifier.fillMaxWidth().height(56.dp).padding(vertical = 4.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor)
                                    ) {
                                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("ADD MORE FILES", fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
                                    }
                                }
                                item { Spacer(Modifier.height(100.dp)) }
                            }
                            
                            val allReady = selectedFiles.size > 1 && selectedFiles.all { !it.isLocked || it.isUnlocked }
                            Button(
                                onClick = { saveLauncher.launch("merged_${System.currentTimeMillis() / 1000}.pdf") },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).height(60.dp),
                                enabled = allReady,
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor, disabledContainerColor = accentColor.copy(alpha = 0.3f))
                            ) {
                                Text(if (allReady) "MERGE ${selectedFiles.size} FILES" else if (selectedFiles.size < 2) "SELECT AT LEAST 2 FILES" else "UNLOCK ALL FILES FIRST", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    
                    ToolState.PROCESSING -> {
                        ProcessingStateView(
                            accentColor = accentColor,
                            uri = selectedFiles.firstOrNull()?.decryptedUri ?: selectedFiles.firstOrNull()?.uri,
                            password = if (selectedFiles.firstOrNull()?.decryptedUri != null) null else selectedFiles.firstOrNull()?.password,
                            text = "Merging ${selectedFiles.size} documents...",
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
                LockedFilePrompt(
                    fileName = fileToUnlock!!.name,
                    onDismiss = { fileToUnlock = null },
                    onUnlocked = { password ->
                        val targetFile = fileToUnlock!!
                        isFileLoading = true
                        scope.launch(Dispatchers.IO) {
                            val decryptedUri = decryptToCache(context, targetFile.uri, password)
                            if (decryptedUri != null) {
                                val count = getPageCount(context, decryptedUri, null)
                                withContext(Dispatchers.Main) {
                                    selectedFiles = selectedFiles.map { if (it.uri == targetFile.uri) it.copy(isUnlocked = true, password = password, decryptedUri = decryptedUri, pageCount = count) else it }
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
        PageLightbox(
            uri = lightboxFile!!.decryptedUri ?: lightboxFile!!.uri,
            initialPage = 0,
            totalCount = lightboxFile!!.pageCount,
            password = if (lightboxFile!!.decryptedUri != null) null else lightboxFile!!.password,
            onDismiss = { lightboxFile = null }
        )
    }
}
