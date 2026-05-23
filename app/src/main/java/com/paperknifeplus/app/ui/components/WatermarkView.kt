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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.util.Matrix
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
    val normalizedX: Float,
    val normalizedY: Float,
    val normalizedWidth: Float,
    val rotation: Float,
    val opacity: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatermarkView(
    initialUri: Uri? = null,
    onBack: () -> Unit,
    onOpenPreview: (Uri, String, Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val accentColor = PaperPink

    var currentState by remember { mutableStateOf<ToolState>(ToolState.SELECTING) }
    var selectedUri by remember { mutableStateOf(initialUri) }
    var outputUri by remember { mutableStateOf<Uri?>(null) }
    var unlockPassword by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var pageCount by remember { mutableIntStateOf(0) }
    var isFileLoading by remember { mutableStateOf(false) }
    var processingTime by remember { mutableStateOf("") }
    var fileToUnlock by remember { mutableStateOf<String?>(null) }
    
    // Logic State
    var placedWatermarks by remember { mutableStateOf<List<PlacedWatermark>>(emptyList()) }
    var activeWatermarkBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var activeOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var activeScale by remember { mutableFloatStateOf(1f) }
    var activeRotation by remember { mutableFloatStateOf(0f) }
    var activeOpacity by remember { mutableFloatStateOf(1f) }
    var isFocusMode by remember { mutableStateOf(false) }
    var lightboxPage by remember { mutableStateOf<Int?>(null) }
    
    var showWatermarkOptions by remember { mutableStateOf(false) }
    var showTextInput by remember { mutableStateOf(false) }

    val imageLoader = coil.compose.LocalImageLoader.current

    LaunchedEffect(Unit) { PDFBoxResourceLoader.init(context) }

    LaunchedEffect(initialUri) {
        if (initialUri != null) {
            selectedUri = initialUri
            val details = getUriDetails(context, initialUri)
            fileName = details.name
            isFileLoading = true
            scope.launch(Dispatchers.IO) {
                val isEncrypted = checkIsEncryptedLocal(context, initialUri)
                if (isEncrypted) {
                    withContext(Dispatchers.Main) {
                        fileToUnlock = fileName
                        isFileLoading = false
                    }
                } else {
                    val count = getPageCount(context, initialUri, null)
                    withContext(Dispatchers.Main) {
                        pageCount = count
                        currentState = ToolState.CONFIGURING
                        isFileLoading = false
                    }
                }
            }
        }
    }

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
                            activeWatermarkBitmap = bitmap
                            activeOffset = androidx.compose.ui.geometry.Offset.Zero
                            activeScale = 1f
                            activeRotation = 0f
                            activeOpacity = 1f
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
                                        val pdfWidth = page.mediaBox.width
                                        val pdfHeight = page.mediaBox.height
                                        
                                        val drawWidth = pdfWidth * wm.normalizedWidth
                                        val drawHeight = drawWidth * (wm.bitmap.height.toFloat() / wm.bitmap.width.toFloat())
                                        
                                        val xPos = (wm.normalizedX * pdfWidth) - (drawWidth / 2)
                                        val yPos = (pdfHeight - (wm.normalizedY * pdfHeight)) - (drawHeight / 2)
                                        
                                        cs.saveGraphicsState()
                                        val matrix = Matrix()
                                        matrix.translate(xPos + drawWidth/2, yPos + drawHeight/2)
                                        matrix.rotate(Math.toRadians((-wm.rotation).toDouble()))
                                        matrix.translate(-(xPos + drawWidth/2), -(yPos + drawHeight/2))
                                        cs.transform(matrix)

                                        if (wm.opacity < 1f) {
                                            val gs = PDExtendedGraphicsState()
                                            gs.nonStrokingAlphaConstant = wm.opacity
                                            cs.setGraphicsStateParameters(gs)
                                        }
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
                        SessionManager.addEntry(getUriDetails(context, saveUri).name, "Watermark", "Branding applied locally", Icons.Filled.TextFields, saveUri, pageCount)
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
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val containerWidth = constraints.maxWidth.toFloat()
            val containerHeight = constraints.maxHeight.toFloat()
            val fitRect = LayoutMath.getFitRect(containerWidth, containerHeight, 0.707f)
            
            val pageWidth = fitRect.width()
            val pageHeight = fitRect.height()

            // Render confirmed watermarks
            placedWatermarks.filter { it.pageIndex == pageIndex || it.pageIndex == -1 }.forEach { wm ->
                ComposeImage(
                    bitmap = wm.bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .requiredSize(
                            width = (pageWidth * wm.normalizedWidth).dp / LocalDensity.current.density,
                            height = (pageWidth * wm.normalizedWidth * (wm.bitmap.height.toFloat() / wm.bitmap.width.toFloat())).dp / LocalDensity.current.density
                        )
                        .graphicsLayer {
                            translationX = fitRect.left + (wm.normalizedX * pageWidth) - (containerWidth / 2)
                            translationY = fitRect.top + (wm.normalizedY * pageHeight) - (containerHeight / 2)
                            rotationZ = wm.rotation
                            alpha = wm.opacity
                        }
                        .align(Alignment.Center)
                )
            }
            
            if (isFocusMode && activeWatermarkBitmap != null && (lightboxPage == pageIndex || lightboxPage == null)) {
                val bitmap = activeWatermarkBitmap!!
                val baseWidth = pageWidth * 0.5f
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, rot ->
                                activeOffset += pan
                                activeScale = (activeScale * zoom).coerceIn(0.1f, 15f)
                                activeRotation += rot
                            }
                        }
                ) {
                    ComposeImage(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .requiredSize(
                                width = (baseWidth * activeScale).dp / LocalDensity.current.density,
                                height = (baseWidth * activeScale * (bitmap.height.toFloat() / bitmap.width.toFloat())).dp / LocalDensity.current.density
                            )
                            .graphicsLayer {
                                translationX = activeOffset.x
                                translationY = activeOffset.y
                                rotationZ = activeRotation
                                alpha = activeOpacity
                            }
                            .align(Alignment.Center)
                            .border(1.dp, accentColor.copy(0.3f), RoundedCornerShape(2.dp))
                    )
                }
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
                            Text("CUSTOM OVERLAY", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
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
        val pageIdx = lightboxPage!!
        BoxWithConstraints {
            val containerWidth = constraints.maxWidth.toFloat()
            val containerHeight = constraints.maxHeight.toFloat()
            val fitRect = LayoutMath.getFitRect(containerWidth, containerHeight, 0.707f)
            
            PageLightbox(
                uri = selectedUri!!,
                initialPage = pageIdx,
                totalCount = pageCount,
                password = unlockPassword.ifEmpty { null },
                onDismiss = { lightboxPage = null; isFocusMode = false; activeWatermarkBitmap = null },
                itemOverlay = watermarkOverlay,
                bottomBar = { currentPage ->
                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Black.copy(0.85f),
                        border = BorderStroke(1.dp, Color.White.copy(0.1f))
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            if (isFocusMode) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text("Opacity", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(50.dp))
                                    Slider(value = activeOpacity, onValueChange = { activeOpacity = it }, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor))
                                }
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { isFocusMode = false; activeWatermarkBitmap = null }, modifier = Modifier.weight(0.8f)) {
                                        Text("CANCEL", color = Color.LightGray, fontWeight = FontWeight.Bold)
                                    }
                                    Row(Modifier.weight(2.5f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { 
                                                activeWatermarkBitmap?.let {
                                                    val nx = (activeOffset.x + (containerWidth / 2) - fitRect.left) / fitRect.width()
                                                    val ny = (activeOffset.y + (containerHeight / 2) - fitRect.top) / fitRect.height()
                                                    
                                                    placedWatermarks = placedWatermarks + PlacedWatermark(
                                                        pageIndex = -1,
                                                        bitmap = it,
                                                        normalizedX = nx,
                                                        normalizedY = ny,
                                                        normalizedWidth = (fitRect.width() * 0.5f * activeScale) / fitRect.width(),
                                                        rotation = activeRotation,
                                                        opacity = activeOpacity
                                                    )
                                                }
                                                isFocusMode = false
                                                activeWatermarkBitmap = null
                                            },
                                            modifier = Modifier.weight(1f).height(50.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                                        ) { Text("APPLY TO ALL", fontSize = 10.sp, fontWeight = FontWeight.Black) }
                                        Button(
                                            onClick = { 
                                                activeWatermarkBitmap?.let {
                                                    val nx = (activeOffset.x + (containerWidth / 2) - fitRect.left) / fitRect.width()
                                                    val ny = (activeOffset.y + (containerHeight / 2) - fitRect.top) / fitRect.height()
                                                    
                                                    placedWatermarks = placedWatermarks + PlacedWatermark(
                                                        pageIndex = currentPage,
                                                        bitmap = it,
                                                        normalizedX = nx,
                                                        normalizedY = ny,
                                                        normalizedWidth = (fitRect.width() * 0.5f * activeScale) / fitRect.width(),
                                                        rotation = activeRotation,
                                                        opacity = activeOpacity
                                                    )
                                                }
                                                isFocusMode = false
                                                activeWatermarkBitmap = null
                                            },
                                            modifier = Modifier.weight(1f).height(50.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White)
                                        ) { Text("APPLY", fontSize = 10.sp, fontWeight = FontWeight.Black) }
                                    }
                                }
                            } else {
                                val hasAppliedOnThisPage = placedWatermarks.any { it.pageIndex == currentPage || it.pageIndex == -1 }
                                
                                if (!hasAppliedOnThisPage) {
                                    Button(
                                        onClick = { showWatermarkOptions = true },
                                        modifier = Modifier.fillMaxWidth().height(50.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White)
                                    ) {
                                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("ADD WATERMARK", fontWeight = FontWeight.Black)
                                    }
                                } else {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        OutlinedButton(
                                            onClick = { lightboxPage = null },
                                            modifier = Modifier.weight(1f).height(50.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(1.dp, Color.White.copy(0.3f))
                                        ) { Text("SIGN MORE", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Black) }
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
                }
            )
        }
    }

    if (showWatermarkOptions) {
        AlertDialog(
            onDismissRequest = { showWatermarkOptions = false },
            title = { Text("Select Watermark", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("TEMPLATES", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 1.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("DRAFT", "CONFIDENTIAL", "APPROVED").forEach { template ->
                            Surface(
                                onClick = {
                                    activeWatermarkBitmap = createTextBitmap(template, Color.Gray)
                                    activeOffset = androidx.compose.ui.geometry.Offset.Zero
                                    activeScale = 1f
                                    activeRotation = 0f
                                    activeOpacity = 0.5f
                                    isFocusMode = true
                                    showWatermarkOptions = false
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = accentColor.copy(alpha = 0.05f),
                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.1f))
                            ) {
                                Text(template, fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, modifier = Modifier.padding(8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
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
                        val colors = listOf(Color.Black, Color.DarkGray, Color.Gray, Color.White, Color.Red, Color.Blue, Color(0xFF10B981), Color(0xFF8B5CF6), Color(0xFFF59E0B), Color(0xFFEC4899))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(colors) { color ->
                                Surface(
                                    onClick = { selectedColor = color },
                                    modifier = Modifier.size(32.dp).border(2.dp, if (selectedColor == color) accentColor else Color.Transparent, CircleShape),
                                    shape = CircleShape,
                                    color = color
                                ) {
                                    if (color == Color.White) {
                                        Box(Modifier.fillMaxSize().border(1.dp, Color.Gray.copy(0.3f), CircleShape))
                                    }
                                }
                            }
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
                        activeWatermarkBitmap = bitmap
                        activeOffset = androidx.compose.ui.geometry.Offset.Zero
                        activeScale = 1f
                        activeRotation = 0f
                        activeOpacity = 1f
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
        textSize = 120f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        // Add subtle shadow for visibility if white
        if (color == Color.White) {
            setShadowLayer(4f, 0f, 0f, android.graphics.Color.GRAY)
        }
    }
    canvas.drawText(text, 500f, 220f, paint)
    return bitmap
}
