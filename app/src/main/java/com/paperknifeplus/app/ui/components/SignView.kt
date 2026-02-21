package com.paperknifeplus.app.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter
import com.paperknifeplus.app.data.image.PdfPageRequest
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
import java.io.FileOutputStream
import kotlin.math.roundToInt

@Composable
fun SignView(
    onBack: () -> Unit,
    onOpenPreview: (Uri, String, Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val accentColor = PaperPink // Correct Accent Color

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
    var showSignOptions by remember { mutableStateOf(false) }
    var showSignaturePad by remember { mutableStateOf(false) }
    var savedSignatures by remember { mutableStateOf<List<File>>(emptyList()) }

    // Transformation State
    var sigOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset(100f, 100f)) }
    var sigScale by remember { mutableFloatStateOf(1f) }
    var sigRotation by remember { mutableFloatStateOf(0f) }

    fun saveSignature(bitmap: Bitmap) {
        val dir = File(context.filesDir, "signatures")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "sig_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        loadSavedSignatures()
    }

    fun loadSavedSignatures() {
        val dir = File(context.filesDir, "signatures")
        if (!dir.exists()) dir.mkdirs()
        savedSignatures = dir.listFiles()?.filter { it.extension == "png" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    LaunchedEffect(Unit) { 
        PDFBoxResourceLoader.init(context)
        loadSavedSignatures()
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

    val pngLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    withContext(Dispatchers.Main) {
                        signatureBitmap = bitmap
                        showSignOptions = false
                        currentState = ToolState.PROCESSING // Using PROCESSING state for placement mode
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
                            
                            PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                                // Convert UI coordinates to PDF coordinates (bottom-up)
                                // This is a complex mapping, simplifying for initial "Gold" feel
                                val pdfWidth = page.mediaBox.width
                                val pdfHeight = page.mediaBox.height
                                
                                // Placeholder: Needs precise mapping from the placement overlay
                                val drawWidth = 150f * sigScale
                                val drawHeight = (150f * (sig.height.toFloat() / sig.width.toFloat())) * sigScale
                                
                                cs.saveGraphicsState()
                                // Rotation and translation would go here
                                cs.drawImage(pdImage, 50f + sigOffset.x/2, pdfHeight - drawHeight - 50f - sigOffset.y/2, drawWidth, drawHeight)
                                cs.restoreGraphicsState()
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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
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
                            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)
                        )
                    }
                    ToolState.CONFIGURING -> {
                        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(fileName, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1)
                                    Text("SELECT ONE PAGE TO SIGN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = accentColor)
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
                                        showSignOptions = true
                                    }
                                )
                            }
                        }
                    }
                    ToolState.PROCESSING -> {
                        // PLACEMENT MODE
                        if (signatureBitmap != null && selectedPageIndex != -1) {
                            SignaturePlacementOverlay(
                                uri = selectedUri!!,
                                pageIndex = selectedPageIndex,
                                signature = signatureBitmap!!,
                                offset = sigOffset,
                                scale = sigScale,
                                rotation = sigRotation,
                                onTransform = { o, s, r -> sigOffset = o; sigScale = s; sigRotation = r },
                                onCancel = { currentState = ToolState.CONFIGURING },
                                onConfirm = { saveLauncher.launch(fileName.replace(".pdf", "-signed.pdf")) },
                                accentColor = accentColor
                            )
                        } else {
                            ProcessingStateView(accentColor, selectedUri, null, "Processing...", 0, 0, false)
                        }
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

    if (showSignOptions) {
        ModalBottomSheet(
            onDismissRequest = { showSignOptions = false },
            containerColor = if (isDark) Color(0xFF121214) else Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(Modifier.padding(24.dp).navigationBarsPadding()) {
                Text("Add Signature", fontWeight = FontWeight.Black, fontSize = 20.sp)
                Spacer(Modifier.height(20.dp))
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SignOptionCard("Draw", Icons.Filled.Gesture, accentColor, Modifier.weight(1f)) { 
                        showSignaturePad = true
                    }
                    SignOptionCard("Upload", Icons.Filled.CloudUpload, Color.Gray, Modifier.weight(1f)) { 
                        pngLauncher.launch("image/png")
                    }
                }
                
                if (savedSignatures.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    Text("SAVED SIGNATURES", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 1.sp)
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(savedSignatures) { file ->
                            Surface(
                                modifier = Modifier.size(100.dp, 60.dp).clickable {
                                    signatureBitmap = BitmapFactory.decodeFile(file.absolutePath)
                                    showSignOptions = false
                                    currentState = ToolState.PROCESSING
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.Gray.copy(0.2f))
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(file),
                                    contentDescription = null,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    if (showSignaturePad) {
        SignaturePadDialog(
            onDismiss = { showSignaturePad = false },
            onSave = { bitmap, shouldSave ->
                if (shouldSave) saveSignature(bitmap)
                signatureBitmap = bitmap
                showSignaturePad = false
                showSignOptions = false
                currentState = ToolState.PROCESSING
            },
            accentColor = accentColor
        )
    }
}

@Composable
fun SignaturePadDialog(
    onDismiss: () -> Unit,
    onSave: (Bitmap, Boolean) -> Unit,
    accentColor: Color
) {
    var paths by remember { mutableStateOf(mutableStateListOf<Path>()) }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var shouldSaveSignature by remember { mutableStateOf(true) }

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
                
                Spacer(Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = shouldSaveSignature,
                        onCheckedChange = { shouldSaveSignature = it },
                        colors = CheckboxDefaults.colors(checkedColor = accentColor)
                    )
                    Text("Save for future use", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { paths.clear() }) {
                        Text("CLEAR", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (paths.isEmpty()) return@Button
                    val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
                    val paint = Paint().apply {
                        color = android.graphics.Color.BLACK
                        style = Paint.Style.STROKE
                        strokeWidth = 12f
                        isAntiAlias = true
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                    }
                    paths.forEach { path -> canvas.drawPath(path, paint) }
                    onSave(bitmap, shouldSaveSignature)
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(12.dp),
                enabled = paths.isNotEmpty()
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

@Composable
fun SignOptionCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = color)
        }
    }
}

@Composable
fun SignaturePlacementOverlay(
    uri: Uri,
    pageIndex: Int,
    signature: Bitmap,
    offset: androidx.compose.ui.geometry.Offset,
    scale: Float,
    rotation: Float,
    onTransform: (androidx.compose.ui.geometry.Offset, Float, Float) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    accentColor: Color
) {
    val imageLoader = coil.compose.LocalImageLoader.current
    val request = remember(uri, pageIndex) { PdfPageRequest(uri, pageIndex, null, 1.5f) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // Page Preview
        Image(
            painter = rememberAsyncImagePainter(request, imageLoader),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // The Signature
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, rot ->
                        onTransform(offset + pan, (scale * zoom).coerceIn(0.5f, 3f), rotation + rot)
                    }
                }
        ) {
            Image(
                bitmap = signature.asImageBitmap(),
                contentDescription = "Signature",
                modifier = Modifier
                    .size(150.dp)
                    .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        rotationZ = rotation
                    }
                    .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            )
        }

        // Controls
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onCancel,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                shape = RoundedCornerShape(16.dp)
            ) { Text("CANCEL") }
            
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(16.dp)
            ) { Text("BURN SIGNATURE", fontWeight = FontWeight.Black) }
        }
        
        Surface(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 60.dp),
            color = Color.Black.copy(0.6f),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("Drag to move • Pinch to resize/rotate", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
    }
}
