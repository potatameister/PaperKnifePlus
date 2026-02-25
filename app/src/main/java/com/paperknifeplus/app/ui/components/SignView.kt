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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image as ComposeImage
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
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
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

data class PlacedSignature(
    val id: String = java.util.UUID.randomUUID().toString(),
    val pageIndex: Int,
    val bitmap: Bitmap,
    var offset: androidx.compose.ui.geometry.Offset,
    var scale: Float,
    var rotation: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignView(
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
    var placedSignatures by remember { mutableStateOf<List<PlacedSignature>>(emptyList()) }
    var activeSignature by remember { mutableStateOf<PlacedSignature?>(null) }
    var isFocusMode by remember { mutableStateOf(false) }
    var lightboxPage by remember { mutableStateOf<Int?>(null) }
    
    var showSignOptions by remember { mutableStateOf(false) }
    var showSignaturePad by remember { mutableStateOf(false) }
    var savedSignatures by remember { mutableStateOf<List<File>>(emptyList()) }

    val imageLoader = coil.compose.LocalImageLoader.current

    fun loadSavedSignatures() {
        val dir = File(context.filesDir, "signatures")
        if (!dir.exists()) dir.mkdirs()
        savedSignatures = dir.listFiles()?.filter { it.extension == "png" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun saveSignature(bitmap: Bitmap) {
        val dir = File(context.filesDir, "signatures")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "sig_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        loadSavedSignatures()
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
                try {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        val bitmap = BitmapFactory.decodeStream(stream)
                        withContext(Dispatchers.Main) {
                            activeSignature = PlacedSignature(
                                pageIndex = lightboxPage ?: 0,
                                bitmap = bitmap,
                                offset = androidx.compose.ui.geometry.Offset.Zero,
                                scale = 1f,
                                rotation = 0f
                            )
                            isFocusMode = true
                            showSignOptions = false
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
                        
                        placedSignatures.groupBy { it.pageIndex }.forEach { (pageIdx, sigs) ->
                            if (pageIdx < document.numberOfPages) {
                                val page = document.getPage(pageIdx)
                                PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                                    sigs.forEach { sig ->
                                        val pdImage = LosslessFactory.createFromImage(document, sig.bitmap)
                                        val pdfWidth = page.mediaBox.getWidth()
                                        val pdfHeight = page.mediaBox.getHeight()
                                        
                                        // Coordinate mapping fix: Scale and Center
                                        val drawWidth = 200f * sig.scale
                                        val drawHeight = (200f * (sig.bitmap.height.toFloat() / sig.bitmap.width.toFloat())) * sig.scale
                                        
                                        // UI offset is in PX, PDF is in PTS. Standard preview is around 360dp width.
                                        val xPos = (pdfWidth / 2) - (drawWidth / 2) + (sig.offset.x * (pdfWidth / 360f))
                                        val yPos = (pdfHeight / 2) - (drawHeight / 2) - (sig.offset.y * (pdfHeight / 510f))
                                        
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

    val signatureOverlay: @Composable (BoxScope.(Int) -> Unit) = { pageIndex ->
        // Render confirmed signatures (Scaled for Grid if needed)
        placedSignatures.filter { it.pageIndex == pageIndex }.forEach { sig ->
            ComposeImage(
                bitmap = sig.bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.Center)
                    .offset { IntOffset(sig.offset.x.roundToInt(), sig.offset.y.roundToInt()) }
                    .graphicsLayer {
                        scaleX = sig.scale
                        scaleY = sig.scale
                        rotationZ = sig.rotation
                    }
            )
        }
        
        if (isFocusMode && activeSignature != null && activeSignature!!.pageIndex == pageIndex) {
            val sig = activeSignature!!
            var sigOffset by remember { mutableStateOf(sig.offset) }
            var sigScale by remember { mutableFloatStateOf(sig.scale) }
            var sigRotation by remember { mutableFloatStateOf(sig.rotation) }
            
            LaunchedEffect(sigOffset, sigScale, sigRotation) {
                activeSignature = activeSignature?.copy(offset = sigOffset, scale = sigScale, rotation = sigRotation)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, rot ->
                            sigOffset += pan
                            sigScale = (sigScale * zoom).coerceIn(0.2f, 10f)
                            sigRotation += rot
                        }
                    }
            ) {
                ComposeImage(
                    bitmap = activeSignature!!.bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(200.dp)
                        .align(Alignment.Center)
                        .offset { IntOffset(sigOffset.x.roundToInt(), sigOffset.y.roundToInt()) }
                        .graphicsLayer {
                            scaleX = sigScale
                            scaleY = sigScale
                            rotationZ = sigRotation
                        }
                        .border(1.dp, accentColor, RoundedCornerShape(2.dp))
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
                            Text("Sign PDF", fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Text("GOLD STANDARD SIGNING", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
                        }
                        if (selectedUri != null && currentState == ToolState.CONFIGURING) {
                            TextButton(onClick = { selectedUri = null; currentState = ToolState.SELECTING; placedSignatures = emptyList() }) {
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
                LoadingStateView(accentColor, false, "Preparing document...")
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
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(Modifier.height(12.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                // CLEAN PREVIEW: Disable selection and zoom icons
                                UnifiedPdfPreview(
                                    uri = selectedUri!!,
                                    pageCount = pageCount,
                                    mode = PreviewMode.GRID,
                                    password = unlockPassword.ifEmpty { null }, 
                                    accentColor = accentColor,
                                    showSelectionIcon = false, 
                                    showZoomIcon = false, 
                                    itemOverlay = signatureOverlay,
                                    onToggleSelection = { index -> lightboxPage = index }
                                )
                            }
                            
                            val hasSigned = placedSignatures.isNotEmpty()
                            Button(
                                onClick = { if (hasSigned) saveLauncher.launch(fileName.replace(".pdf", "-signed.pdf")) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).height(60.dp),
                                enabled = hasSigned,
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (hasSigned) accentColor else Color.Gray.copy(0.3f),
                                    contentColor = if (hasSigned) Color.White else Color.Gray
                                )
                            ) {
                                Text(if (hasSigned) "SAVE SIGNED PDF" else "SELECT A PAGE TO SIGN", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    ToolState.PROCESSING -> {
                        ProcessingStateView(
                            accentColor = accentColor,
                            uri = selectedUri,
                            password = unlockPassword.ifEmpty { null },
                            text = "Synthesizing PDF...",
                            current = 0,
                            total = 0,
                            showWarning = false
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
                                placedSignatures = emptyList()
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
            itemOverlay = signatureOverlay,
            bottomBar = { pageIndex ->
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(0.8f),
                    border = BorderStroke(1.dp, Color.White.copy(0.1f))
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (isFocusMode) {
                            TextButton(onClick = { isFocusMode = false; activeSignature = null }, modifier = Modifier.weight(1f)) {
                                Text("CANCEL", color = Color.LightGray, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { 
                                    activeSignature?.let { placedSignatures = placedSignatures + it }
                                    isFocusMode = false
                                    activeSignature = null
                                },
                                modifier = Modifier.weight(1.5f).height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Text("APPLY", fontWeight = FontWeight.Black)
                            }
                        } else {
                            Button(
                                onClick = { showSignOptions = true },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("ADD SIGN", fontWeight = FontWeight.Black)
                            }
                            Spacer(Modifier.width(12.dp))
                            Row(Modifier.weight(1.2f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { lightboxPage = null },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(0.3f))
                                ) {
                                    Text("SIGN MORE", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Black)
                                }
                                Button(
                                    onClick = { 
                                        lightboxPage = null // CLEAR BEFORE PROCESSING
                                        saveLauncher.launch(fileName.replace(".pdf", "-signed.pdf")) 
                                    },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                                ) {
                                    Text("SAVE", fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    if (showSignOptions) {
        AlertDialog(
            onDismissRequest = { showSignOptions = false },
            title = { Text("Add Signature", fontWeight = FontWeight.Black) },
            text = {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SignOptionCard("Draw", Icons.Filled.Gesture, accentColor, Modifier.weight(1f)) { showSignaturePad = true; showSignOptions = false }
                        SignOptionCard("Upload", Icons.Filled.CloudUpload, Color.Gray, Modifier.weight(1f)) { pngLauncher.launch("image/png"); showSignOptions = false }
                    }
                    
                    if (savedSignatures.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        Text("SAVED SIGNATURES", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 1.sp)
                        Spacer(Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(savedSignatures) { file ->
                                Surface(
                                    modifier = Modifier.size(100.dp, 60.dp).clickable {
                                        activeSignature = PlacedSignature(pageIndex = lightboxPage ?: 0, bitmap = BitmapFactory.decodeFile(file.absolutePath), offset = androidx.compose.ui.geometry.Offset.Zero, scale = 1f, rotation = 0f)
                                        isFocusMode = true
                                        showSignOptions = false
                                    },
                                    shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.Gray.copy(0.2f))
                                ) { ComposeImage(painter = rememberAsyncImagePainter(file), contentDescription = null, modifier = Modifier.padding(8.dp)) }
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showSignaturePad) {
        SignaturePadDialog(
            onDismiss = { showSignaturePad = false },
            onSave = { bitmap, shouldSave ->
                if (shouldSave) saveSignature(bitmap)
                activeSignature = PlacedSignature(pageIndex = lightboxPage ?: 0, bitmap = bitmap, offset = androidx.compose.ui.geometry.Offset.Zero, scale = 1f, rotation = 0f)
                isFocusMode = true
                showSignaturePad = false
                showSignOptions = false
            },
            accentColor = accentColor,
            initialColor = Color.Black
        )
    }
}

@Composable
fun SignaturePadDialog(
    onDismiss: () -> Unit,
    onSave: (Bitmap, Boolean) -> Unit,
    accentColor: Color,
    initialColor: Color
) {
    val density = LocalDensity.current
    var paths by remember { mutableStateOf(mutableStateListOf<Pair<Path, Color>>()) }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var shouldSaveSignature by remember { mutableStateOf(true) }
    var strokeColor by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Draw Signature", fontWeight = FontWeight.Black) },
        text = {
            Column {
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Color.Black, Color.Blue, Color(0xFFC00000), Color(0xFF0070C0), PaperPink).forEach { color ->
                        Surface(
                            onClick = { strokeColor = color },
                            modifier = Modifier.size(32.dp).border(2.dp, if (strokeColor == color) accentColor else Color.Transparent, CircleShape),
                            shape = CircleShape,
                            color = color
                        ) {}
                    }
                }

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
                                    currentPath?.let { paths.add(it to strokeColor) }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentPath?.lineTo(change.position.x, change.position.y)
                                    val last = paths.removeAt(paths.size - 1)
                                    paths.add(last)
                                }
                            )
                        }
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        paths.forEach { (path, color) ->
                            drawPath(
                                path = path.asComposePath(),
                                color = color,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = with(density) { 4.dp.toPx() },
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                                )
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = shouldSaveSignature, onCheckedChange = { shouldSaveSignature = it }, colors = CheckboxDefaults.colors(checkedColor = accentColor))
                    Text("Save signature", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { paths.clear() }) { Text("CLEAR", color = Color.Gray, fontWeight = FontWeight.Bold) }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (paths.isEmpty()) return@Button
                    val bitmap = Bitmap.createBitmap(1200, 1000, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    val paint = Paint().apply {
                        style = Paint.Style.STROKE
                        strokeWidth = 16f
                        isAntiAlias = true
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                    }
                    
                    canvas.save()
                    canvas.translate(200f, 200f) 
                    paths.forEach { (path, color) -> 
                        paint.color = color.toArgb()
                        canvas.drawPath(path, paint) 
                    }
                    canvas.restore()
                    onSave(bitmap, shouldSaveSignature)
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(12.dp),
                enabled = paths.isNotEmpty()
            ) { Text("ADOPT", fontWeight = FontWeight.Black) }
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
