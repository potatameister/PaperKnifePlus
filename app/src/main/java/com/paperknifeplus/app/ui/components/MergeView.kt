package com.paperknifeplus.app.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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

@Composable
fun MergeView(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val accentColor = Color(0xFFF43F5E)

    var currentState by remember { mutableStateOf<ToolState>(ToolState.SELECTING) }
    var selectedFiles by remember { mutableStateOf<List<MergeFile>>(emptyList()) }
    var processingTime by remember { mutableStateOf("") }
    var progressCount by remember { mutableIntStateOf(0) }
    
    // UI State
    var fileToUnlock by remember { mutableStateOf<MergeFile?>(null) }
    var lightboxFile by remember { mutableStateOf<MergeFile?>(null) }

    val imageLoader = coil.compose.LocalImageLoader.current

    // Launcher for picking multiple files
    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val currentUris = selectedFiles.map { it.uri }
                val duplicates = uris.filter { it in currentUris }
                
                if (duplicates.isNotEmpty()) {
                    Toast.makeText(context, "Skipped ${duplicates.size} duplicate files", Toast.LENGTH_SHORT).show()
                }

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
            }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { saveUri ->
            currentState = ToolState.PROCESSING
            val startTime = System.currentTimeMillis()
            scope.launch(Dispatchers.IO) {
                val openDocs = mutableListOf<PDDocument>()
                try {
                    val merger = PDFMergerUtility()
                    context.contentResolver.openOutputStream(saveUri)?.use { outputStream ->
                        merger.destinationStream = outputStream
                        
                        selectedFiles.forEachIndexed { index, file ->
                            withContext(Dispatchers.Main) { progressCount = index + 1 }
                            val uriToLoad = file.decryptedUri ?: file.uri
                            context.contentResolver.openInputStream(uriToLoad)?.use { inputStream ->
                                val doc = if (file.password != null && file.decryptedUri == null) {
                                    PDDocument.load(inputStream, file.password)
                                } else {
                                    PDDocument.load(inputStream)
                                }
                                doc.isAllSecurityToBeRemoved = true
                                openDocs.add(doc)
                                merger.addSource(doc)
                            }
                        }
                        
                        merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())
                        outputStream.flush()
                    }
                    
                    val endTime = System.currentTimeMillis()
                    val timeStr = String.format("%.1fs", (endTime - startTime) / 1000.0)
                    withContext(Dispatchers.Main) {
                        processingTime = timeStr
                        SessionManager.addEntry("Merged PDF", "Merge", "${selectedFiles.size} files joined", Icons.Filled.Layers)
                        currentState = ToolState.SUCCESS
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Merge failed: ${e.message}", Toast.LENGTH_LONG).show()
                        currentState = ToolState.CONFIGURING
                    }
                } finally {
                    openDocs.forEach { try { it.close() } catch (e: Exception) {} }
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

                    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            itemsIndexed(selectedFiles, key = { _, file -> file.uri.toString() }) { index, file ->
                                val isDragging = draggedIndex == index
                                val itemOffset = if (isDragging) dragOffset else 0f
                                
                                Box(
                                    modifier = Modifier
                                        .zIndex(if (isDragging) 1f else 0f)
                                        .graphicsLayer { translationY = itemOffset }
                                        .shadow(if (isDragging) 8.dp else 0.dp, RoundedCornerShape(20.dp))
                                        .pointerInput(selectedFiles) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { draggedIndex = index },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffset += dragAmount.y
                                                    
                                                    // Simple Swap Logic
                                                    val itemHeight = size.height.toFloat() // approx
                                                    val threshold = 100f // height of card + spacing
                                                    
                                                    if (dragOffset > threshold && index < selectedFiles.size - 1) {
                                                        val newList = selectedFiles.toMutableList()
                                                        Collections.swap(newList, index, index + 1)
                                                        selectedFiles = newList
                                                        draggedIndex = index + 1
                                                        dragOffset -= threshold
                                                    } else if (dragOffset < -threshold && index > 0) {
                                                        val newList = selectedFiles.toMutableList()
                                                        Collections.swap(newList, index, index - 1)
                                                        selectedFiles = newList
                                                        draggedIndex = index - 1
                                                        dragOffset += threshold
                                                    }
                                                },
                                                onDragEnd = {
                                                    draggedIndex = null
                                                    dragOffset = 0f
                                                },
                                                onDragCancel = {
                                                    draggedIndex = null
                                                    dragOffset = 0f
                                                }
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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                disabledContainerColor = accentColor.copy(alpha = 0.3f)
                            )
                        ) {
                            if (allReady) {
                                Text("MERGE ${selectedFiles.size} FILES", fontWeight = FontWeight.Black)
                            } else if (selectedFiles.size < 2) {
                                Text("SELECT AT LEAST 2 FILES", fontWeight = FontWeight.Black)
                            } else {
                                Text("UNLOCK ALL FILES FIRST", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
                
                ToolState.PROCESSING -> {
                    LoadingStateView(accentColor, false, "Merging document $progressCount of ${selectedFiles.size}...")
                }
                
                ToolState.SUCCESS -> {
                    SuccessView(
                        message = "Merge Complete",
                        subMessage = "Successfully joined ${selectedFiles.size} documents",
                        processingTime = processingTime,
                        onDone = onBack,
                        onProcessMore = { 
                            selectedFiles = emptyList()
                            currentState = ToolState.SELECTING 
                        },
                        accentColor = accentColor
                    )
                }
                else -> {}
            }
            
            // Locked File Prompt
            if (fileToUnlock != null) {
                LockedFilePrompt(
                    fileName = fileToUnlock!!.name,
                    onDismiss = { fileToUnlock = null },
                    onUnlocked = { password ->
                        val targetFile = fileToUnlock!!
                        scope.launch(Dispatchers.IO) {
                            val decryptedUri = decryptToCache(context, targetFile.uri, password)
                            if (decryptedUri != null) {
                                val count = getPageCount(context, decryptedUri, null)
                                withContext(Dispatchers.Main) {
                                    val newList = selectedFiles.map { 
                                        if (it.uri == targetFile.uri) {
                                            it.copy(isUnlocked = true, password = password, decryptedUri = decryptedUri, pageCount = count)
                                        } else it
                                    }
                                    selectedFiles = newList
                                    fileToUnlock = null
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Invalid Password", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    // Lightbox Overlay
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

@Composable
fun MergeFileItem(
    file: MergeFile,
    index: Int,
    totalCount: Int,
    isDark: Boolean,
    accentColor: Color,
    imageLoader: coil.ImageLoader,
    onDelete: () -> Unit,
    onUnlock: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF0F0F12) else Color.White),
        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(0.03f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail / Icon
            Surface(
                modifier = Modifier
                    .size(52.dp)
                    .clickable(enabled = !file.isLocked || file.isUnlocked) { onClick() },
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.05f)
            ) {
                if (file.isLocked && !file.isUnlocked) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Lock, null, tint = accentColor, modifier = Modifier.size(24.dp))
                    }
                } else {
                    val request = remember(file.uri, file.decryptedUri, file.password) { 
                        PdfPageRequest(file.decryptedUri ?: file.uri, 0, if (file.decryptedUri != null) null else file.password, 0.3f) 
                    }
                    Image(
                        painter = rememberAsyncImagePainter(request, imageLoader),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            // Details
            Column(Modifier.weight(1f)) {
                Text(file.name, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(file.size, fontSize = 10.sp, color = Color.Gray)
                    if (file.isLocked) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = if (file.isUnlocked) Color(0xFF10B981).copy(alpha = 0.1f) else accentColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                if (file.isUnlocked) "UNLOCKED" else "LOCKED",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (file.isUnlocked) Color(0xFF10B981) else accentColor
                            )
                        }
                    }
                }
            }
            
            // Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (file.isLocked && !file.isUnlocked) {
                    IconButton(onClick = onUnlock) {
                        Icon(Icons.Filled.LockOpen, null, tint = accentColor, modifier = Modifier.size(20.dp))
                    }
                }
                
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, null, tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                }

                Spacer(Modifier.width(4.dp))
                
                Icon(
                    Icons.Filled.DragHandle, 
                    null, 
                    tint = Color.Gray.copy(alpha = 0.3f), 
                    modifier = Modifier.size(24.dp).padding(4.dp)
                )
            }
        }
    }
}
