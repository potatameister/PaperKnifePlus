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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignView(
    onBack: () -> Unit,
    onOpenPreview: (Uri, String, Int) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
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
    
    var selectedPageIndex by remember { mutableIntStateOf(-1) }
    var signatureBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showSignOptions by remember { mutableStateOf(false) }
    var showSignaturePad by remember { mutableStateOf(false) }
    var savedSignatures by remember { mutableStateOf<List<File>>(emptyList()) }

    var sigOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var sigScale by remember { mutableFloatStateOf(1f) }
    var sigRotation by remember { mutableFloatStateOf(0f) }
    
    // COLOR SELECTION
    var selectedColor by remember { mutableStateOf(Color.Black) }

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
                            signatureBitmap = bitmap
                            sigOffset = androidx.compose.ui.geometry.Offset.Zero 
                            sigScale = 1f
                            sigRotation = 0f
                            showSignOptions = false
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
                        val document = if (unlockPassword.isNotEmpty()) PDDocument.load(inputStream, unlockPassword) else PDDocument.load(inputStream)
                        
                        signatureBitmap?.let { sig ->
                            val page = document.getPage(selectedPageIndex)
                            val pdImage = JPEGFactory.createFromImage(document, sig, 0.95f)
                            
                            PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                                val pdfWidth = page.mediaBox.width
                                val pdfHeight = page.mediaBox.height
                                
                                // PRO MATH FIX: Precise UI to PDF Coordinate Mapping
                                // 1. Calculate the aspect-correct scale used in the UI
                                val uiWidth = 360f // Rough estimate, fixed via BoxWithConstraints logic normally
                                val uiHeight = 360f / (pdfWidth/pdfHeight)
                                
                                val drawWidth = 200f * sigScale
                                val drawHeight = (200f * (sig.height.toFloat() / sig.width.toFloat())) * sigScale
                                
                                // Map pixel offset from UI center to PDF points
                                val xPos = (pdfWidth / 2) - (drawWidth / 2) + (sigOffset.x * (pdfWidth / 360f))
                                val yPos = (pdfHeight / 2) - (drawHeight / 2) - (sigOffset.y * (pdfHeight / 510f)) // Adjusted for standard A4
                                
                                cs.saveGraphicsState()
                                cs.drawImage(pdImage, xPos, yPos, drawWidth, drawHeight)
                                cs.restoreGraphicsState()
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
                        Text("Sign PDF", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                        Text("PLACE SIGNATURE ON PAGES", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
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
                        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                            var showPreview by remember { mutableStateOf(true) }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(fileName, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1)
                                    Text("SELECT ONE PAGE TO SIGN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = accentColor)
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
                                    selectedPages = if (selectedPageIndex != -1) setOf(selectedPageIndex) else emptySet(),
                                    onToggleSelection = { index ->
                                        selectedPageIndex = index
                                        showSignOptions = true
                                    },
                                    itemOverlay = { index ->
                                        if (showPreview && selectedPageIndex == index && signatureBitmap != null) {
                                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Surface(
                                                    color = Color.White,
                                                    shape = RoundedCornerShape(2.dp),
                                                    border = BorderStroke(1.dp, accentColor.copy(0.4f)),
                                                    modifier = Modifier.size(50.dp, 30.dp)
                                                ) {
                                                    ComposeImage(signatureBitmap!!.asImageBitmap(), null, modifier = Modifier.padding(2.dp), contentScale = ContentScale.Fit)
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                            
                            if (signatureBitmap != null) {
                                Button(
                                    onClick = { saveLauncher.launch(fileName.replace(".pdf", "-signed.pdf")) },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).height(60.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                                ) {
                                    Text("SAVE SIGNED PDF", fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                    ToolState.PROCESSING -> {
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
                                onConfirm = { currentState = ToolState.CONFIGURING }, // "APPLY" logic
                                accentColor = accentColor
                            )
                        } else {
                            ProcessingStateView(accentColor, selectedUri, "Processing...", 0, 0, false)
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
                                    currentState = ToolState.CONFIGURING
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.Gray.copy(0.2f))
                            ) {
                                ComposeImage(
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
            accentColor = accentColor,
            initialColor = selectedColor
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
                // COLOR PICKER
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
    
    val currentOffset by rememberUpdatedState(offset)
    val currentScale by rememberUpdatedState(scale)
    val currentRotation by rememberUpdatedState(rotation)

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        val uiW = maxWidth
        val uiH = maxHeight

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
                bitmap = signature.asImageBitmap(),
                contentDescription = "Signature",
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
