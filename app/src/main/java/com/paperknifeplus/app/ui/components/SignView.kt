package com.paperknifeplus.app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperknifeplus.app.ui.theme.PaperPink
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SignView(
    onBack: () -> Unit,
    onOpenPreview: (Uri, String, Int) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val accentColor = Color(0xFF6366F1) // Indigo for Sign

    var currentState by remember { mutableStateOf<ToolState>(ToolState.SELECTING) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var outputUri by remember { mutableStateOf<Uri?>(null) }
    var unlockPassword by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf("") }
    var pageCount by remember { mutableIntStateOf(0) }
    var isFileLoading by remember { mutableStateOf(false) }
    var processingTime by remember { mutableStateOf("") }
    var showLoadingWarning by remember { mutableStateOf(false) }
    var fileToUnlock by remember { mutableStateOf<String?>(null) }
    
    var selectedPageIndex by remember { mutableIntStateOf(-1) }
    var signatureBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showSignaturePad by remember { mutableStateOf(false) }

    LaunchedEffect(isFileLoading, currentState) {
        if (isFileLoading || currentState == ToolState.PROCESSING) {
            delay(5000)
            showLoadingWarning = true
        } else {
            showLoadingWarning = false
        }
    }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedUri = it
            val details = getUriDetails(context, it)
            fileName = details.name
            fileSize = details.size
            isFileLoading = true
            scope.launch(Dispatchers.IO) {
                val isEncrypted = checkIsEncryptedLocal(context, it)
                if (isEncrypted) {
                    withContext(Dispatchers.Main) {
                        fileToUnlock = fileName
                        isFileLoading = false
                    }
                } else {
                    val count = getPageCount(context, it, null)
                    withContext(Dispatchers.Main) {
                        pageCount = count
                        currentState = ToolState.CONFIGURING
                        isFileLoading = false
                    }
                }
            }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { saveUri ->
            currentState = ToolState.PROCESSING
            val startTime = System.currentTimeMillis()
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(selectedUri!!)?.use { inputStream ->
                        val document = if (unlockPassword.isNotEmpty()) PDDocument.load(inputStream, unlockPassword) else PDDocument.load(inputStream)
                        
                        signatureBitmap?.let { sig ->
                            val page = document.getPage(selectedPageIndex)
                            val pdImage = JPEGFactory.createFromImage(document, sig, 0.9f)
                            
                            PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true).use { contentStream ->
                                // Default placement: Bottom Right
                                val width = 150f
                                val height = width * (sig.height.toFloat() / sig.width.toFloat())
                                contentStream.drawImage(pdImage, page.mediaBox.width - width - 50f, 50f, width, height)
                            }
                        }
                        
                        saveAndFlush(context, document, saveUri)
                    }
                    val endTime = System.currentTimeMillis()
                    val timeStr = String.format("%.1fs", (endTime - startTime) / 1000.0)
                    withContext(Dispatchers.Main) {
                        processingTime = timeStr
                        outputUri = saveUri
                        SessionManager.addEntry(fileName, "Sign", "Document signed", Icons.Filled.Draw, saveUri, pageCount)
                        currentState = ToolState.SUCCESS
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        currentState = ToolState.CONFIGURING
                    }
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Sign PDF", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                        Text("PLACE SIGNATURE ON PAGES", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            if (isFileLoading) {
                LoadingStateView(accentColor, showLoadingWarning, "Preparing document...")
            } else {
                when (currentState) {
                    ToolState.SELECTING -> {
                        SelectionGrid(
                            onSelect = { pickLauncher.launch("application/pdf") }, 
                            isDark = isDark,
                            icon = Icons.Filled.Draw,
                            title = "Tap to enter file",
                            subtitle = "SIGN ANY PDF DOCUMENT",
                            accentColor = accentColor,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    ToolState.CONFIGURING -> {
                        Column(Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(fileName, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1)
                                    Text("SELECT PAGE TO SIGN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                }
                            }

                            // Info Row
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                color = accentColor.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Filled.Info, null, tint = accentColor, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Tap page to select • Add signature to proceed",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                UnifiedPdfPreview(
                                    uri = selectedUri!!,
                                    pageCount = pageCount,
                                    mode = PreviewMode.GRID,
                                    password = null, 
                                    accentColor = accentColor,
                                    selectedPages = if (selectedPageIndex != -1) setOf(selectedPageIndex) else emptySet(),
                                    onToggleSelection = { index ->
                                        selectedPageIndex = index
                                        showSignaturePad = true
                                    }
                                )
                            }

                            Button(
                                onClick = { saveLauncher.launch(fileName.replace(".pdf", "", true) + "-signed.pdf") }, 
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).height(60.dp), 
                                enabled = selectedPageIndex != -1 && signatureBitmap != null,
                                shape = RoundedCornerShape(20.dp), 
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Text("SAVE SIGNED PDF", fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }
                    ToolState.PROCESSING -> {
                        ProcessingStateView(
                            accentColor = accentColor,
                            uri = selectedUri,
                            password = unlockPassword.ifEmpty { null },
                            text = "Processing...",
                            current = 0,
                            total = 0,
                            showWarning = showLoadingWarning
                        )
                    }
                    ToolState.SUCCESS -> {
                        SuccessView(
                            message = "Sign Complete",
                            subMessage = "Signature placed successfully",
                            processingTime = processingTime,
                            onDone = onBack,
                            onProcessMore = { 
                                selectedUri = null
                                unlockPassword = ""
                                selectedPageIndex = -1
                                signatureBitmap = null
                                currentState = ToolState.SELECTING 
                            },
                            onPreview = {
                                outputUri?.let { uri ->
                                    onOpenPreview(uri, fileName, pageCount)
                                }
                            },
                            accentColor = accentColor
                        )
                    }
                    else -> {}
                }
            }

            if (fileToUnlock != null) {
                LockedFilePrompt(
                    fileName = fileToUnlock!!,
                    onDismiss = { fileToUnlock = null; selectedUri = null; currentState = ToolState.SELECTING },
                    onUnlocked = { pass ->
                        unlockPassword = pass
                        isFileLoading = true
                        scope.launch(Dispatchers.IO) {
                            val decryptedUri = decryptToCache(context, selectedUri!!, pass)
                            if (decryptedUri != null) {
                                val count = getPageCount(context, decryptedUri, null)
                                withContext(Dispatchers.Main) { 
                                    selectedUri = decryptedUri
                                    pageCount = count
                                    currentState = ToolState.CONFIGURING
                                    isFileLoading = false 
                                    fileToUnlock = null
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

    if (showSignaturePad) {
        SignaturePadDialog(
            onDismiss = { showSignaturePad = false },
            onSave = { bitmap ->
                signatureBitmap = bitmap
                showSignaturePad = false
            },
            accentColor = accentColor
        )
    }
}

@Composable
fun SignaturePadDialog(
    onDismiss: () -> Unit,
    onSave: (Bitmap) -> Unit,
    accentColor: Color
) {
    var paths by remember { mutableStateOf(mutableStateListOf<Path>()) }
    var currentPath by remember { mutableStateOf<Path?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Draw Signature", fontWeight = FontWeight.Black) },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, Color.Gray.copy(0.2f), RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentPath = Path().apply { moveTo(offset.x, offset.y) }
                                    currentPath?.let { paths.add(it) }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    currentPath?.lineTo(change.position.x, change.position.y)
                                    // Trigger recomposition
                                    val last = paths.last()
                                    paths.removeAt(paths.size - 1)
                                    paths.add(last)
                                }
                            )
                        }
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        paths.forEach { path ->
                            drawPath(
                                path = path.asComposePath(),
                                color = Color.Black,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = with(density) { 4.dp.toPx() })
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { paths.clear() }) {
                        Text("CLEAR", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    val paint = Paint().apply {
                        color = android.graphics.Color.BLACK
                        style = Paint.Style.STROKE
                        strokeWidth = 12f
                        isAntiAlias = true
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                    }
                    paths.forEach { path -> canvas.drawPath(path, paint) }
                    onSave(bitmap)
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("ADOPT SIGNATURE", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.Gray)
            }
        }
    )
}
