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
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.foundation.Image as ComposeImage
import kotlin.math.roundToInt

data class PlacedWatermark(
    val id: String = java.util.UUID.randomUUID().toString(),
    val pageIndex: Int, // -1 means all pages
    val bitmap: Bitmap,
    var offset: androidx.compose.ui.geometry.Offset,
    var scale: Float,
    var rotation: Float
)

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
    var fileToUnlock by remember { mutableStateOf<String?>(null) }
    
    // Logic State
    var placedWatermarks by remember { mutableStateOf<List<PlacedWatermark>>(emptyList()) }
    var activeWatermark by remember { mutableStateOf<PlacedWatermark?>(null) }
    var isFocusMode by remember { mutableStateOf(false) }
    var lightboxPage by remember { mutableStateOf<Int?>(null) }
    
    var showWatermarkOptions by remember { mutableStateOf(false) }
    var showTextInput by remember { mutableStateOf(false) }

    val imageLoader = coil.compose.LocalImageLoader.current

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
                            activeWatermark = PlacedWatermark(
                                pageIndex = lightboxPage ?: 0,
                                bitmap = bitmap,
                                offset = androidx.compose.ui.geometry.Offset.Zero,
                                scale = 1f,
                                rotation = 0f
                            )
                            isFocusMode = true
                            showWatermarkOptions = false
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
                        val document = if (unlockPassword.isNotEmpty()) PDDocument.load(inputStream, unlockPassword) else PDDocument.load(inputStream)
                        if (document.isEncrypted) document.isAllSecurityToBeRemoved = true
                        
                        placedWatermarks.forEach { wm ->
                            val targetIndices = if (wm.pageIndex == -1) (0 until document.numberOfPages) else listOf(wm.pageIndex)
                            targetIndices.forEach { pageIdx ->
                                if (pageIdx < document.numberOfPages) {
                                    val page = document.getPage(pageIdx)
                                    PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                                        val pdImage = LosslessFactory.createFromImage(document, wm.bitmap)
                                        val pdfWidth = page.mediaBox.getWidth()
                                        val pdfHeight = page.mediaBox.getHeight()
                                        
                                        val drawWidth = 250f * wm.scale
                                        val drawHeight = (250f * (wm.bitmap.height.toFloat() / wm.bitmap.width.toFloat())) * wm.scale
                                        
                                        val xPos = (pdfWidth / 2) - (drawWidth / 2) + (wm.offset.x * (pdfWidth / 360f))
                                        val yPos = (pdfHeight / 2) - (drawHeight / 2) - (wm.offset.y * (pdfHeight / 510f))
                                        
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

    val watermarkOverlay: @Composable (BoxScope.(Int) -> Unit) = { pageIndex ->
        // Render confirmed watermarks
        placedWatermarks.filter { it.pageIndex == pageIndex || it.pageIndex == -1 }.forEach { wm ->
            ComposeImage(
                bitmap = wm.bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.Center)
                    .offset { IntOffset(wm.offset.x.roundToInt(), wm.offset.y.roundToInt()) }
                    .graphicsLayer {
                        scaleX = wm.scale
                        scaleY = wm.scale
                        rotationZ = wm.rotation
                    }
            )
        }
        
        if (isFocusMode && activeWatermark != null && activeWatermark!!.pageIndex == pageIndex) {
            val wm = activeWatermark!!
            var wmOffset by remember { mutableStateOf(wm.offset) }
            var wmScale by remember { mutableFloatStateOf(wm.scale) }
            var wmRotation by remember { mutableFloatStateOf(wm.rotation) }
            
            LaunchedEffect(wmOffset, wmScale, wmRotation) {
                activeWatermark = activeWatermark?.copy(offset = wmOffset, scale = wmScale, rotation = wmRotation)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, rot ->
                            wmOffset += pan
                            wmScale = (wmScale * zoom).coerceIn(0.1f, 15f)
                            wmRotation += rot
                        }
                    }
            ) {
                ComposeImage(
                    bitmap = activeWatermark!!.bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(250.dp)
                        .align(Alignment.Center)
                        .offset { IntOffset(wmOffset.x.roundToInt(), wmOffset.y.roundToInt()) }
                        .graphicsLayer {
                            scaleX = wmScale
                            scaleY = wmScale
                            rotationZ = wmRotation
                        }
                        .border(1.dp, accentColor.copy(0.3f), RoundedCornerShape(2.dp))
                )
            }
        }
    }

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
                            Text("Watermark", fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Text("PRECISION OVERLAY", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
                        }
                        if (selectedUri != null && currentState == ToolState.CONFIGURING) {
                            TextButton(onClick = { selectedUri = null; currentState = ToolState.SELECTING; placedWatermarks = emptyList() }) {
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
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(Modifier.height(12.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                UnifiedPdfPreview(
                                    uri = selectedUri!!,
                                    pageCount = pageCount,
                                    mode = PreviewMode.GRID,
                                    password = unlockPassword.ifEmpty { null }, 
                                    accentColor = accentColor,
                                    showSelectionIcon = false,
                                    showZoomIcon = false,
                                    itemOverlay = watermarkOverlay,
                                    onToggleSelection = { index -> lightboxPage = index }
                                )
                            }
                            
                            val hasWatermark = placedWatermarks.isNotEmpty()
                            Button(
                                onClick = { if (hasWatermark) saveLauncher.launch(fileName.replace(".pdf", "-watermarked.pdf")) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).height(60.dp),
                                enabled = hasWatermark,
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Text(if (hasWatermark) "SAVE WATERMARKED PDF" else "SELECT A PAGE TO WATERMARK", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    ToolState.PROCESSING -> {
                        ProcessingStateView(
                            accentColor = accentColor,
                            uri = selectedUri,
                            password = unlockPassword.ifEmpty { null },
                            text = "Applying watermarks...",
                            current = 0,
                            total = 0,
                            showWarning = false
                        )
                    }
                    ToolState.SUCCESS -> {
                        SuccessView(
                            message = "Watermark Complete",
                            subMessage = "Watermarks applied successfully",
                            processingTime = processingTime,
                            onDone = onBack,
                            onProcessMore = { 
                                selectedUri = null
                                placedWatermarks = emptyList()
                                currentState = ToolState.SELECTING 
                            },
                            onPreview = { outputUri?.let { uri -> onOpenPreview(uri, fileName, pageCount) } },
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

    if (lightboxPage != null) {
        PageLightbox(
            uri = selectedUri!!,
            initialPage = lightboxPage!!,
            totalCount = pageCount,
            password = unlockPassword.ifEmpty { null },
            onDismiss = { lightboxPage = null },
            itemOverlay = watermarkOverlay,
            bottomBar = { pageIndex ->
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(0.8f),
                    border = BorderStroke(1.dp, Color.White.copy(0.1f))
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (isFocusMode) {
                            TextButton(onClick = { isFocusMode = false; activeWatermark = null }, modifier = Modifier.weight(0.8f)) {
                                Text("CANCEL", color = Color.LightGray, fontWeight = FontWeight.Bold)
                            }
                            Row(Modifier.weight(2f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { 
                                        activeWatermark?.let { placedWatermarks = placedWatermarks + it.copy(pageIndex = -1) }
                                        isFocusMode = false
                                        activeWatermark = null
                                    },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                                ) { Text("APPLY TO ALL", fontSize = 10.sp, fontWeight = FontWeight.Black) }
                                Button(
                                    onClick = { 
                                        activeWatermark?.let { placedWatermarks = placedWatermarks + it.copy(pageIndex = pageIndex) }
                                        isFocusMode = false
                                        activeWatermark = null
                                    },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                                ) { Text("APPLY", fontSize = 10.sp, fontWeight = FontWeight.Black) }
                            }
                        } else {
                            Button(
                                onClick = { showWatermarkOptions = true },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("ADD WATERMARK", fontWeight = FontWeight.Black)
                            }
                            Spacer(Modifier.width(12.dp))
                            Row(Modifier.weight(1.2f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { lightboxPage = null },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(0.3f))
                                ) { Text("MORE", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Black) }
                                Button(
                                    onClick = { 
                                        lightboxPage = null
                                        saveLauncher.launch(fileName.replace(".pdf", "-watermarked.pdf")) 
                                    },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                                ) { Text("SAVE", fontSize = 10.sp, fontWeight = FontWeight.Black) }
                            }
                        }
                    }
                }
            }
        )
    }

    if (showWatermarkOptions) {
        AlertDialog(
            onDismissRequest = { showWatermarkOptions = false },
            title = { Text("Select Watermark", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SignOptionCard("Text Watermark", Icons.Filled.Title, accentColor, Modifier.fillMaxWidth()) { 
                        showTextInput = true; showWatermarkOptions = false 
                    }
                    SignOptionCard("Upload Image", Icons.Filled.CloudUpload, Color.Gray, Modifier.fillMaxWidth()) { 
                        imgLauncher.launch("image/*"); showWatermarkOptions = false 
                    }
                }
            },
            confirmButton = {}
        )
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
                        val bitmap = createTextBitmap(text, selectedColor)
                        activeWatermark = PlacedWatermark(
                            pageIndex = lightboxPage ?: 0,
                            bitmap = bitmap,
                            offset = androidx.compose.ui.geometry.Offset.Zero,
                            scale = 1f,
                            rotation = 0f
                        )
                        isFocusMode = true
                        showTextInput = false
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
