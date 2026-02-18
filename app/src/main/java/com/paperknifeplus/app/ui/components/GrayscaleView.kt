package com.paperknifeplus.app.ui.components

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Compare
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun GrayscaleView(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val accentColor = Color(0xFFF59E0B)

    var currentState by remember { mutableStateOf<ToolState>(ToolState.SELECTING) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var unlockPassword by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf("") }
    var isFileLoading by remember { mutableStateOf(false) }
    var processingTime by remember { mutableStateOf("") }
    
    var pageCount by remember { mutableIntStateOf(0) }
    var isGrayscalePreview by remember { mutableStateOf(true) }
    var showLoadingWarning by remember { mutableStateOf(false) }
    var lightboxPage by remember { mutableStateOf<Int?>(null) }
    
    var progressPage by remember { mutableIntStateOf(0) }
    var firstPagePreview by remember { mutableStateOf<Bitmap?>(null) }

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
                        currentState = ToolState.UNLOCKING
                        isFileLoading = false
                    }
                } else {
                    val count = getPageCountLocal(context, it, null)
                    val preview = loadPreview(context, it, null)
                    withContext(Dispatchers.Main) {
                        pageCount = count
                        firstPagePreview = preview
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
                    performGrayscaleRewrite(context, selectedUri!!, saveUri, if (unlockPassword.isEmpty()) null else unlockPassword) { current, total ->
                        progressPage = current
                    }
                    val endTime = System.currentTimeMillis()
                    val timeStr = String.format("%.1fs", (endTime - startTime) / 1000.0)
                    withContext(Dispatchers.Main) {
                        processingTime = timeStr
                        SessionManager.addEntry(fileName, "Grayscale", "Converted to B&W", Icons.Outlined.Palette)
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
                    Column(Modifier.weight(1f)) {
                        Text("Grayscale", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                        Text("CONVERT DOCUMENT TO B&W", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
                    }
                    if (currentState == ToolState.CONFIGURING) {
                        IconButton(
                            onClick = { isGrayscalePreview = !isGrayscalePreview },
                            modifier = Modifier.background(if (isGrayscalePreview) accentColor else Color.Gray.copy(0.2f), CircleShape)
                        ) {
                            Icon(Icons.Outlined.Compare, null, tint = if (isGrayscalePreview) Color.White else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentState == ToolState.CONFIGURING) {
                FloatingActionButton(
                    onClick = { 
                        val defaultName = fileName.replace(".pdf", "", true) + "-grayscale.pdf"
                        saveLauncher.launch(defaultName) 
                    },
                    containerColor = accentColor,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) { Icon(Icons.Default.Save, "Save") }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            if (isFileLoading) {
                LoadingStateView(accentColor, showLoadingWarning, "Reading document layers...")
            } else {
                when (currentState) {
                    ToolState.SELECTING -> {
                        SelectionGrid(
                            onSelect = { pickLauncher.launch("application/pdf") }, 
                            isDark = isDark,
                            icon = Icons.Outlined.Palette,
                            title = "Tap to enter file",
                            subtitle = "GRAYSCALE ANY PDF DOCUMENT",
                            accentColor = accentColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    ToolState.UNLOCKING -> {
                        LockedFilePrompt(
                            fileName = fileName,
                            password = unlockPassword,
                            onPasswordChange = { unlockPassword = it },
                            onUnlock = {
                                isFileLoading = true
                                scope.launch(Dispatchers.IO) {
                                    val count = getPageCountLocal(context, selectedUri!!, unlockPassword)
                                    val preview = loadPreview(context, selectedUri!!, unlockPassword)
                                    if (count > 0) {
                                        withContext(Dispatchers.Main) {
                                            pageCount = count
                                            firstPagePreview = preview
                                            currentState = ToolState.CONFIGURING
                                            isFileLoading = false
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Invalid Password", Toast.LENGTH_SHORT).show()
                                            isFileLoading = false
                                        }
                                    }
                                }
                            },
                            onCancel = { selectedUri = null; currentState = ToolState.SELECTING },
                            accentColor = accentColor
                        )
                    }
                    ToolState.CONFIGURING -> {
                        Column(Modifier.fillMaxSize()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (isGrayscalePreview) "GRAYSCALE PREVIEW" else "ORIGINAL PREVIEW", fontSize = 9.sp, fontWeight = FontWeight.Black, color = if (isGrayscalePreview) accentColor else Color.Gray, letterSpacing = 1.sp)
                                Spacer(Modifier.width(8.dp))
                                Text("• $pageCount PAGES", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 1.sp)
                            }
                            Spacer(Modifier.height(12.dp))
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 100.dp)
                            ) {
                                items(pageCount) { index ->
                                    Box(modifier = Modifier.aspectRatio(0.707f).clickable { lightboxPage = index }) {
                                        PagePreviewThumbnail(
                                            context = context,
                                            uri = selectedUri!!,
                                            pageIndex = index,
                                            password = if (unlockPassword.isEmpty()) null else unlockPassword,
                                            isGrayscale = isGrayscalePreview,
                                            accentColor = accentColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                    ToolState.PROCESSING -> {
                        ProcessingStateView(
                            accentColor = accentColor,
                            preview = firstPagePreview,
                            text = "Applying grayscale filter...",
                            current = progressPage,
                            total = pageCount,
                            showWarning = showLoadingWarning
                        )
                    }
                    ToolState.SUCCESS -> {
                        SuccessView(
                            message = "Grayscale Complete",
                            subMessage = "Colors removed from all elements",
                            processingTime = processingTime,
                            onDone = onBack,
                            onProcessMore = { selectedUri = null; unlockPassword = ""; currentState = ToolState.SELECTING },
                            accentColor = accentColor
                        )
                    }
                }
            }
        }
    }

    if (lightboxPage != null && selectedUri != null) {
        PageLightbox(
            uri = selectedUri!!,
            initialPage = lightboxPage!!,
            totalCount = pageCount,
            password = if (unlockPassword.isEmpty()) null else unlockPassword,
            onDismiss = { lightboxPage = null }
        )
    }
}

@Composable
fun PagePreviewThumbnail(
    context: android.content.Context,
    uri: Uri,
    pageIndex: Int,
    password: String?,
    isGrayscale: Boolean,
    accentColor: Color
) {
    var bitmap by remember(uri, pageIndex, password) { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(uri, pageIndex, password) {
        withContext(Dispatchers.IO) {
            val b = renderPageToBitmap(context, uri, pageIndex, password, 0.3f)
            withContext(Dispatchers.Main) { bitmap = b }
        }
    }

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.Gray.copy(0.1f))
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (bitmap != null) {
                val displayBitmap = if (isGrayscale) remember(bitmap) { toGrayscaleBitmap(bitmap!!) } else bitmap!!
                Image(bitmap = displayBitmap.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Surface(modifier = Modifier.align(Alignment.BottomStart).padding(6.dp), color = Color.Black.copy(0.5f), shape = RoundedCornerShape(4.dp)) {
                    Text("${pageIndex + 1}", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp))
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = accentColor.copy(0.3f))
            }
        }
    }
}

private suspend fun getPageCountLocal(context: android.content.Context, uri: Uri, password: String?): Int = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val document = if (password != null) PDDocument.load(inputStream, password) else PDDocument.load(inputStream)
            val count = document.numberOfPages
            document.close()
            count
        } ?: 0
    } catch (e: Exception) { 0 }
}
