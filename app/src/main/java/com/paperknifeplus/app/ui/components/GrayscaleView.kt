package com.paperknifeplus.app.ui.components

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Compare
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperknifeplus.app.ui.theme.PaperPink
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
    
    var pageCount by remember { mutableStateOf(0) }
    var isGrayscalePreview by remember { mutableStateOf(true) }
    var showLoadingWarning by remember { mutableStateOf(false) }
    
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
                    val count = getPageCount(context, it, null)
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
                        SessionManager.addEntry(fileName, "Grayscale", "Converted", Icons.Outlined.Palette)
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
                        Text("REMOVE DOCUMENT COLORS", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
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
                LoadingStateView(accentColor, showLoadingWarning, "Preparing document...")
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
                                    val count = getPageCount(context, selectedUri!!, unlockPassword)
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
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(bottom = 100.dp)
                            ) {
                                items(pageCount) { index ->
                                    PagePreviewItem(
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
                    ToolState.PROCESSING -> {
                        ProcessingStateView(
                            accentColor = accentColor,
                            preview = firstPagePreview,
                            text = "Rewriting document layers...",
                            current = progressPage,
                            total = pageCount,
                            showWarning = showLoadingWarning
                        )
                    }
                    ToolState.SUCCESS -> {
                        SuccessView(
                            processingTime = processingTime,
                            onDone = onBack,
                            onProcessMore = { 
                                selectedUri = null
                                unlockPassword = ""
                                currentState = ToolState.SELECTING 
                            },
                            accentColor = accentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProcessingStateView(
    accentColor: Color, 
    preview: Bitmap?,
    text: String,
    current: Int,
    total: Int,
    showWarning: Boolean
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Card(
                modifier = Modifier.size(180.dp, 240.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(0.1f))
            ) {
                if (preview != null) {
                    Image(bitmap = preview.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Box(Modifier.fillMaxSize().background(Color.Gray.copy(0.1f)))
                }
            }
            
            Spacer(Modifier.height(32.dp))
            CircularProgressIndicator(color = accentColor)
            Spacer(Modifier.height(24.dp))
            
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.Black)
            
            if (total > 0) {
                Spacer(Modifier.height(8.dp))
                Text("Page $current of $total", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            }

            AnimatedVisibility(visible = showWarning, enter = fadeIn(), exit = fadeOut()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(24.dp))
                    Surface(color = accentColor.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Info, null, tint = accentColor, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("This is a complex operation. Please keep the app open.", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingStateView(accentColor: Color, showWarning: Boolean, text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            CircularProgressIndicator(color = accentColor)
            Spacer(Modifier.height(24.dp))
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            
            AnimatedVisibility(visible = showWarning, enter = fadeIn(), exit = fadeOut()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(16.dp))
                    Surface(color = accentColor.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Info, null, tint = accentColor, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Processing may take a moment depending on the device.", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PagePreviewItem(
    context: android.content.Context,
    uri: Uri,
    pageIndex: Int,
    password: String?,
    isGrayscale: Boolean,
    accentColor: Color
) {
    // Standard preview quality
    var bitmap by remember(uri, pageIndex, password) { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(uri, pageIndex, password) {
        withContext(Dispatchers.IO) {
            val b = renderPageToBitmap(context, uri, pageIndex, password, 0.4f)
            withContext(Dispatchers.Main) { bitmap = b }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(0.707f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.Gray.copy(0.1f))
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (bitmap != null) {
                val displayBitmap = if (isGrayscale) {
                    remember(bitmap) { toGrayscaleBitmap(bitmap!!) }
                } else {
                    bitmap!!
                }
                Image(
                    bitmap = displayBitmap.asImageBitmap(), 
                    null, 
                    contentScale = ContentScale.Fit, 
                    modifier = Modifier.fillMaxSize()
                )
                
                Surface(
                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "PAGE ${pageIndex + 1}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = accentColor.copy(0.3f))
            }
        }
    }
}

private suspend fun getPageCount(context: android.content.Context, uri: Uri, password: String?): Int = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val document = if (password != null) PDDocument.load(inputStream, password) else PDDocument.load(inputStream)
            val count = document.numberOfPages
            document.close()
            count
        } ?: 0
    } catch (e: Exception) { 0 }
}
