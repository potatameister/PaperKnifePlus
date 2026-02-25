package com.paperknifeplus.app.ui.components

import android.graphics.*
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.paperknifeplus.app.data.image.PdfPageRequest
import com.paperknifeplus.app.ui.theme.PaperPink
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.foundation.Image as ComposeImage
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatermarkView(
    onBack: () -> Unit,
    onOpenPreview: (Uri, String, Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val accentColor = PaperPink

    var currentState by remember { mutableStateOf<ToolState>(ToolState.SELECTING) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var outputUri by remember { mutableStateOf<Uri?>(null) }
    var previewUri by remember { mutableStateOf<Uri?>(null) }
    var unlockPassword by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var pageCount by remember { mutableIntStateOf(0) }
    var isFileLoading by remember { mutableStateOf(false) }
    var processingTime by remember { mutableStateOf("") }
    var fileToUnlock by remember { mutableStateOf<String?>(null) }
    
    var watermarkBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showWatermarkOptions by remember { mutableStateOf(false) }
    var showTextInput by remember { mutableStateOf(false) }
    var selectedPages by remember { mutableStateOf<Set<Int>>(emptySet()) }

    var wmOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var wmScale by remember { mutableFloatStateOf(1f) }
    var wmRotation by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) { PDFBoxResourceLoader.init(context) }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedUri = it
            val details = getUriDetails(context, it)
            fileName = details.name
            isFileLoading = true
            scope.launch(Dispatchers.IO) {
                val isEnc = checkIsEncryptedLocal(context, it)
                if (isEnc) {
                    withContext(Dispatchers.Main) {
                        fileToUnlock = fileName
                        isFileLoading = false
                    }
                } else {
                    val count = getPageCount(context, it, null)
                    withContext(Dispatchers.Main) {
                        pageCount = count
                        selectedPages = (0 until count).toSet() 
                        currentState = ToolState.CONFIGURING
                        isFileLoading = false
                    }
                }
            }
        }
    }

    val imgLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        val bitmap = BitmapFactory.decodeStream(stream)
                        withContext(Dispatchers.Main) {
                            watermarkBitmap = bitmap
                            wmOffset = androidx.compose.ui.geometry.Offset.Zero
                            wmScale = 1f
                            wmRotation = 0f
                            showWatermarkOptions = false
                            currentState = ToolState.PROCESSING 
                        }
                    }
                } catch (e: Exception) {}
            }
        }
    }

    suspend fun generatePreview() {
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(selectedUri!!)?.use { inputStream ->
                    val document = if (unlockPassword.isNotEmpty()) PDDocument.load(inputStream, unlockPassword) else PDDocument.load(inputStream)
                    watermarkBitmap?.let { wm ->
                        val pdImage = JPEGFactory.createFromImage(document, wm, 0.9f)
                        selectedPages.forEach { pageIdx ->
                            if (pageIdx < document.numberOfPages) {
                                val page = document.getPage(pageIdx)
                                PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                                    val pdfWidth = page.mediaBox.width
                                    val pdfHeight = page.mediaBox.height
                                    val drawWidth = 250f * wmScale
                                    val drawHeight = (250f * (wm.height.toFloat() / wm.width.toFloat())) * wmScale
                                    val xPos = (pdfWidth / 2) - (drawWidth / 2) + (wmOffset.x * (pdfWidth / 360f))
                                    val yPos = (pdfHeight / 2) - (drawHeight / 2) - (wmOffset.y * (pdfHeight / 510f))
                                    cs.saveGraphicsState()
                                    cs.drawImage(pdImage, xPos, yPos, drawWidth, drawHeight)
                                    cs.restoreGraphicsState()
                                }
                            }
                        }
                    }
                    val tempUri = saveToTemp(context, document)
                    withContext(Dispatchers.Main) {
                        previewUri = tempUri
                        currentState = ToolState.PREVIEW_RESULT
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Preview failed", Toast.LENGTH_SHORT).show()
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
                try {
                    context.contentResolver.openInputStream(previewUri!!)?.use { inputStream ->
                        val document = PDDocument.load(inputStream)
                        saveAndFlush(context, document, saveUri)
                    }
                    val endTime = System.currentTimeMillis()
                    withContext(Dispatchers.Main) {
                        processingTime = String.format("%.1fs", (endTime - startTime) / 1000.0)
                        outputUri = saveUri
                        currentState = ToolState.SUCCESS
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        currentState = ToolState.PREVIEW_RESULT
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            if (currentState != ToolState.SUCCESS && currentState != ToolState.PROCESSING && currentState != ToolState.PREVIEW_RESULT) {
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
                            Text("Watermark", fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Text("OVERLAY TEXT OR IMAGES", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
                        }
                        if (selectedUri != null && currentState == ToolState.CONFIGURING) {
                            TextButton(onClick = { selectedUri = null; currentState = ToolState.SELECTING }) {
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
                LoadingStateView(accentColor, false, "Reading document...")
            } else {
                when (currentState) {
                    ToolState.SELECTING -> {
                        SelectionGrid(
                            onSelect = { pickLauncher.launch("application/pdf") }, 
                            isDark = isDark,
                            icon = Icons.Filled.TextFields,
                            title = "Tap to enter file",
                            subtitle = "WATERMARK PAGES",
                            accentColor = accentColor,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)
                        )
                    }
                    ToolState.CONFIGURING -> {
                        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(fileName, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1)
                                    Text("${selectedPages.size} / $pageCount PAGES SELECTED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                UnifiedPdfPreview(
                                    uri = selectedUri!!,
                                    pageCount = pageCount,
                                    mode = PreviewMode.GRID,
                                    password = unlockPassword.ifEmpty { null }, 
                                    accentColor = accentColor,
                                    selectedPages = selectedPages,
                                    onToggleSelection = { index ->
                                        selectedPages = if (selectedPages.contains(index)) selectedPages - index else selectedPages + index
                                    }
                                )
                            }
                            
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = { showWatermarkOptions = true },
                                    modifier = Modifier.weight(1f).height(60.dp),
                                    enabled = selectedPages.isNotEmpty(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(0.1f), contentColor = accentColor)
                                ) { Text("PICK WATERMARK", fontWeight = FontWeight.Black) }
                                
                                if (watermarkBitmap != null) {
                                    Button(
                                        onClick = { currentState = ToolState.PROCESSING },
                                        modifier = Modifier.weight(1f).height(60.dp),
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                                    ) { Text("ADJUST PLACEMENT", fontWeight = FontWeight.Black) }
                                }
                            }
                        }
                    }
                    ToolState.PROCESSING -> {
                        if (watermarkBitmap != null) {
                            WatermarkPlacementOverlay(
                                uri = selectedUri!!,
                                pageIndex = selectedPages.firstOrNull() ?: 0,
                                watermark = watermarkBitmap!!,
                                offset = wmOffset,
                                scale = wmScale,
                                rotation = wmRotation,
                                onTransform = { o: androidx.compose.ui.geometry.Offset, s: Float, r: Float -> wmOffset = o; wmScale = s; wmRotation = r },
                                onCancel = { currentState = ToolState.CONFIGURING },
                                onConfirm = { 
                                    scope.launch { generatePreview() }
                                },
                                accentColor = accentColor
                            )
                        } else {
                            LoadingStateView(accentColor, false, "Generating comparison...")
                        }
                    }
                    ToolState.PREVIEW_RESULT -> {
                        SynchronizedComparison(
                            originalUri = selectedUri!!,
                            modifiedUri = previewUri!!,
                            onBack = { currentState = ToolState.PROCESSING },
                            onConfirm = { saveLauncher.launch(fileName.replace(".pdf", "-watermarked.pdf")) },
                            accentColor = accentColor
                        )
                    }
                    ToolState.SUCCESS -> {
                        SuccessView(
                            message = "Watermark Complete",
                            subMessage = "Document watermarked successfully",
                            processingTime = processingTime,
                            onDone = onBack,
                            onProcessMore = { 
                                selectedUri = null
                                watermarkBitmap = null
                                currentState = ToolState.SELECTING 
                            },
                            onPreview = {
                                outputUri?.let { uri -> onOpenPreview(uri, fileName, pageCount) }
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
                        isFileLoading = true
                        scope.launch(Dispatchers.IO) {
                            val count = getPageCount(context, selectedUri!!, pass)
                            withContext(Dispatchers.Main) { 
                                if (count > 0) {
                                    unlockPassword = pass
                                    pageCount = count
                                    selectedPages = (0 until count).toSet()
                                    currentState = ToolState.CONFIGURING
                                    fileToUnlock = null
                                } else {
                                    Toast.makeText(context, "Invalid Password", Toast.LENGTH_SHORT).show()
                                }
                                isFileLoading = false
                            }
                        }
                    },
                    accentColor = accentColor,
                    isLoading = isFileLoading
                )
            }
        }
    }

    if (showWatermarkOptions) {
        ModalBottomSheet(
            onDismissRequest = { showWatermarkOptions = false },
            containerColor = if (isDark) Color(0xFF121214) else Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(Modifier.padding(24.dp).padding(bottom = 32.dp).navigationBarsPadding()) {
                Text("Select Watermark", fontWeight = FontWeight.Black, fontSize = 20.sp)
                Spacer(Modifier.height(20.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SignOptionCard("Text Watermark", Icons.Filled.Title, accentColor, Modifier.fillMaxWidth()) { 
                        showTextInput = true
                    }
                    SignOptionCard("Upload Image", Icons.Filled.CloudUpload, Color.Gray, Modifier.fillMaxWidth()) { 
                        imgLauncher.launch("image/*")
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    if (showTextInput) {
        var text by remember { mutableStateOf("") }
        var selectedColor by remember { mutableStateOf(Color.Black) }
        AlertDialog(
            onDismissRequest = { showTextInput = false },
            title = { Text("Text Watermark", fontWeight = FontWeight.Black) },
            text = {
                Column {
                    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(Color.Black, Color.Blue, Color(0xFFC00000), Color(0xFF0070C0), PaperPink).forEach { color ->
                            Surface(
                                onClick = { selectedColor = color },
                                modifier = Modifier.size(32.dp).border(2.dp, if (selectedColor == color) accentColor else Color.Transparent, CircleShape),
                                shape = CircleShape,
                                color = color
                            ) {}
                        }
                    }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Enter watermark text") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (text.isNotBlank()) {
                        watermarkBitmap = createTextBitmap(text, selectedColor)
                        wmOffset = androidx.compose.ui.geometry.Offset.Zero
                        wmScale = 1f
                        wmRotation = 0f
                        showTextInput = false
                        showWatermarkOptions = false
                        currentState = ToolState.PROCESSING
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = accentColor)) { Text("CONTINUE") }
            }
        )
    }
}

fun createTextBitmap(text: String, color: Color): Bitmap {
    val bitmap = Bitmap.createBitmap(1000, 400, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        this.color = color.toArgb()
        alpha = 140
        textSize = 120f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText(text, 500f, 220f, paint)
    return bitmap
}

@Composable
fun WatermarkPlacementOverlay(
    uri: Uri,
    pageIndex: Int,
    watermark: Bitmap,
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
    
    val currentOffset by rememberUpdatedState(offset)
    val currentScale by rememberUpdatedState(scale)
    val currentRotation by rememberUpdatedState(rotation)

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        ComposeImage(
            painter = rememberAsyncImagePainter(request, imageLoader),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, rot ->
                        onTransform(currentOffset + pan, (currentScale * zoom).coerceIn(0.1f, 15f), currentRotation + rot)
                    }
                }
        ) {
            ComposeImage(
                bitmap = watermark.asImageBitmap(),
                contentDescription = "Watermark",
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.Center)
                    .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        rotationZ = rotation
                    }
                    .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
            )
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp).padding(horizontal = 24.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onCancel, modifier = Modifier.weight(1f).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray), shape = RoundedCornerShape(16.dp)) { Text("CANCEL") }
            Button(onClick = onConfirm, modifier = Modifier.weight(1f).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = accentColor), shape = RoundedCornerShape(16.dp)) { Text("APPLY PREVIEW", fontWeight = FontWeight.Black) }
        }
    }
}
