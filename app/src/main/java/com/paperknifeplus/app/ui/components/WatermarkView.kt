package com.paperknifeplus.app.ui.components

import android.graphics.*
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image as ComposeImage
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
    var unlockPassword by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var pageCount by remember { mutableIntStateOf(0) }
    var isFileLoading by remember { mutableStateOf(false) }
    var processingTime by remember { mutableStateOf("") }
    var showLoadingWarning by remember { mutableStateOf(false) }
    
    var watermarkBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showWatermarkOptions by remember { mutableStateOf(false) }
    var watermarkText by remember { mutableStateOf("") }
    var showTextInput by remember { mutableStateOf(false) }
    var selectedPages by remember { mutableStateOf<Set<Int>>(emptySet()) }

    // Transformation State
    var wmOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset(100f, 100f)) }
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
                val count = getPageCount(context, it, null)
                withContext(Dispatchers.Main) {
                    pageCount = count
                    selectedPages = (0 until count).toSet() // Default select all
                    currentState = ToolState.CONFIGURING
                    isFileLoading = false
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
                            showWatermarkOptions = false
                            currentState = ToolState.PROCESSING 
                        }
                    }
                } catch (e: Exception) {}
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
                        val document = PDDocument.load(inputStream)
                        watermarkBitmap?.let { wm ->
                            val pdImage = JPEGFactory.createFromImage(document, wm, 0.85f)
                            selectedPages.forEach { pageIdx ->
                                if (pageIdx < document.numberOfPages) {
                                    val page = document.getPage(pageIdx)
                                    PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                                        val pdfHeight = page.mediaBox.height
                                        val drawWidth = 250f * wmScale
                                        val drawHeight = (250f * (wm.height.toFloat() / wm.width.toFloat())) * wmScale
                                        cs.saveGraphicsState()
                                        cs.drawImage(pdImage, 50f + wmOffset.x/2, pdfHeight - drawHeight - 50f - wmOffset.y/2, drawWidth, drawHeight)
                                        cs.restoreGraphicsState()
                                    }
                                }
                            }
                        }
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
                        Text("Watermark", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                        Text("OVERLAY TEXT OR IMAGES", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isFileLoading) {
                LoadingStateView(accentColor, showLoadingWarning, "Reading document structure...")
            } else {
                when (currentState) {
                    ToolState.SELECTING -> {
                        SelectionGrid(
                            onSelect = { pickLauncher.launch("application/pdf") }, 
                            isDark = isDark,
                            icon = Icons.Filled.TextFields,
                            title = "Tap to enter file",
                            subtitle = "WATERMARK ALL PAGES",
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
                                
                                Row {
                                    TextButton(onClick = { selectedPages = (0 until pageCount).toSet() }) {
                                        Text("ALL", fontSize = 10.sp, fontWeight = FontWeight.Black, color = accentColor)
                                    }
                                    TextButton(onClick = { selectedPages = emptySet() }) {
                                        Text("NONE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                                    }
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
                                        "Select pages to watermark • Tap 'Continue' to place",
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
                                    selectedPages = selectedPages,
                                    onToggleSelection = { index ->
                                        selectedPages = if (selectedPages.contains(index)) selectedPages - index else selectedPages + index
                                    }
                                )
                            }
                            
                            Button(
                                onClick = { showWatermarkOptions = true },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).height(60.dp),
                                enabled = selectedPages.isNotEmpty(),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Text("CONTINUE TO WATERMARK", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    ToolState.PROCESSING -> {
                        if (watermarkBitmap != null) {
                            SignaturePlacementOverlay(
                                uri = selectedUri!!,
                                pageIndex = selectedPages.firstOrNull() ?: 0,
                                signature = watermarkBitmap!!,
                                offset = wmOffset,
                                scale = wmScale,
                                rotation = wmRotation,
                                onTransform = { o, s, r -> wmOffset = o; wmScale = s; wmRotation = r },
                                onCancel = { currentState = ToolState.CONFIGURING },
                                onConfirm = { saveLauncher.launch(fileName.replace(".pdf", "-watermarked.pdf")) },
                                accentColor = accentColor
                            )
                        } else {
                            ProcessingStateView(
                                accentColor = accentColor,
                                uri = selectedUri,
                                text = "Applying watermarks...",
                                current = 0,
                                total = 0,
                                showWarning = false
                            )
                        }
                    }
                    ToolState.SUCCESS -> {
                        SuccessView(
                            message = "Watermark Complete",
                            subMessage = "All pages watermarked successfully",
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
        }
    }

    if (showWatermarkOptions) {
        ModalBottomSheet(
            onDismissRequest = { showWatermarkOptions = false },
            containerColor = if (isDark) Color(0xFF121214) else Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(Modifier.padding(24.dp).navigationBarsPadding()) {
                Text("Select Watermark", fontWeight = FontWeight.Black, fontSize = 20.sp)
                Spacer(Modifier.height(20.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SignOptionCard("Text Watermark", Icons.Filled.Title, accentColor, Modifier.fillMaxWidth()) { 
                        showTextInput = true
                    }
                    SignOptionCard("Upload Image", Icons.Filled.CloudUpload, Color.Gray, Modifier.fillMaxWidth()) { 
                        imgLauncher.launch("image/*")
                    }
                    
                    Text("TEMPLATES", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 1.sp, modifier = Modifier.padding(top = 12.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TemplateCard("CONFIDENTIAL", Color.Red, Modifier.weight(1f)) {
                            watermarkBitmap = createTextBitmap("CONFIDENTIAL", Color.Red)
                            showWatermarkOptions = false
                            currentState = ToolState.PROCESSING
                        }
                        TemplateCard("DRAFT", Color.DarkGray, Modifier.weight(1f)) {
                            watermarkBitmap = createTextBitmap("DRAFT", Color.DarkGray)
                            showWatermarkOptions = false
                            currentState = ToolState.PROCESSING
                        }
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    if (showTextInput) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showTextInput = false },
            title = { Text("Text Watermark", fontWeight = FontWeight.Black) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Enter watermark text") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (text.isNotBlank()) {
                        watermarkBitmap = createTextBitmap(text, Color.Red)
                        showTextInput = false
                        showWatermarkOptions = false
                        currentState = ToolState.PROCESSING
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = accentColor)) { Text("CONTINUE") }
            }
        )
    }
}

private fun createTextBitmap(text: String, color: Color): Bitmap {
    val bitmap = Bitmap.createBitmap(1000, 300, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        this.color = color.toArgb()
        alpha = 120
        textSize = 100f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText(text, 500f, 180f, paint)
    return bitmap
}

@Composable
fun TemplateCard(text: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = color, fontWeight = FontWeight.Black, fontSize = 10.sp)
        }
    }
}

fun Color.toArgb(): Int {
    return (this.alpha * 255.0f + 0.5f).toInt() shl 24 or
           ((this.red * 255.0f + 0.5f).toInt() shl 16) or
           ((this.green * 255.0f + 0.5f).toInt() shl 8) or
           (this.blue * 255.0f + 0.5f).toInt()
}
