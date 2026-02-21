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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    
    var watermarkBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showWatermarkOptions by remember { mutableStateOf(false) }
    var showTextInput by remember { mutableStateOf(false) }
    var selectedPages by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var wmColor by remember { mutableStateOf(Color.Black) }

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

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { saveUri ->
            currentState = ToolState.PROCESSING
            val startTime = System.currentTimeMillis()
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(selectedUri!!)?.use { inputStream ->
                        val document = PDDocument.load(inputStream)
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
                LoadingStateView(accentColor, false, "Reading document structure...")
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
                            var showPreview by remember { mutableStateOf(true) }
                            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(fileName, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1)
                                    Text("${selectedPages.size} / $pageCount PAGES SELECTED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                }
                                // NITRO CUSTOM TOGGLE
                                Surface(
                                    onClick = { showPreview = !showPreview },
                                    color = if (showPreview) accentColor.copy(0.15f) else Color.Gray.copy(0.1f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (showPreview) accentColor.copy(0.3f) else Color.Transparent)
                                ) {
                                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (showPreview) Icons.Filled.Preview else Icons.Filled.VisibilityOff, 
                                            null, 
                                            tint = if (showPreview) accentColor else Color.Gray, 
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            if (showPreview) "PREVIEW" else "ORIGINAL", 
                                            fontSize = 9.sp, 
                                            fontWeight = FontWeight.Black,
                                            color = if (showPreview) accentColor else Color.Gray
                                        )
                                    }
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
                                    },
                                    itemOverlay = { index ->
                                        if (showPreview && selectedPages.contains(index) && watermarkBitmap != null) {
                                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Surface(
                                                    color = Color.White.copy(0.7f),
                                                    shape = RoundedCornerShape(2.dp),
                                                    border = BorderStroke(1.dp, accentColor.copy(0.4f)),
                                                    modifier = Modifier.size(60.dp, 40.dp)
                                                ) {
                                                    ComposeImage(watermarkBitmap!!.asImageBitmap(), null, modifier = Modifier.padding(2.dp), contentScale = ContentScale.Fit)
                                                }
                                            }
                                        }
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
                                        onClick = { saveLauncher.launch(fileName.replace(".pdf", "-watermarked.pdf")) },
                                        modifier = Modifier.weight(1f).height(60.dp),
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                                    ) { Text("SAVE WATERMARK", fontWeight = FontWeight.Black) }
                                }
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
                                onConfirm = { currentState = ToolState.CONFIGURING },
                                accentColor = accentColor
                            )
                        } else {
                            ProcessingStateView(accentColor, selectedUri, "Applying Watermarks...", 0, 0, false)
                        }
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
