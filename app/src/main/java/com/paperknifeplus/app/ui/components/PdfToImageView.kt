package com.paperknifeplus.app.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BurstMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.LocalImageLoader
import com.paperknifeplus.app.data.image.PdfPageFetcher
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PdfToImageView(
    onBack: () -> Unit,
    onOpenPreview: (Uri, String, Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val accentColor = Color(0xFF14B8A6)

    var currentState by remember { mutableStateOf<ToolState>(ToolState.SELECTING) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var unlockPassword by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf("") }
    
    var pageCount by remember { mutableIntStateOf(0) }
    var selectedPages by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var format by remember { mutableStateOf("JPEG") }
    var quality by remember { mutableStateOf("Standard") }
    
    var progressCount by remember { mutableIntStateOf(0) }
    var processingTime by remember { mutableStateOf("") }
    var showLoadingWarning by remember { mutableStateOf(false) }
    var lightboxPage by remember { mutableStateOf<Int?>(null) }
    var fileToUnlock by remember { mutableStateOf<String?>(null) }
    var isFileLoading by remember { mutableStateOf(false) }

    // Use Shared Global Loader (MainActivity)
    val imageLoader = coil.compose.LocalImageLoader.current

    LaunchedEffect(currentState) {
        if (currentState == ToolState.PROCESSING) {
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

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let { saveUri ->
            currentState = ToolState.PROCESSING
            val startTime = System.currentTimeMillis()
            scope.launch(Dispatchers.IO) {
                try {
                    convertPdfToImages(context, selectedUri!!, saveUri, if (unlockPassword.isEmpty()) null else unlockPassword, selectedPages.toList().sorted(), format, quality) { current, total ->
                        progressCount = current
                    }
                    val endTime = System.currentTimeMillis()
                    val timeStr = String.format("%.1fs", (endTime - startTime) / 1000.0)
                    withContext(Dispatchers.Main) {
                        processingTime = timeStr
                        SessionManager.addEntry(fileName, "PDF to Image", "${selectedPages.size} images", Icons.Outlined.BurstMode)
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

    LaunchedEffect(initialUri) {
        if (initialUri != null) {
            selectedUri = initialUri
            val details = getUriDetails(context, initialUri)
            fileName = details.name
            fileSize = details.size
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
                            Text("PDF to Image", fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Text("NATIVE EXPORT ENGINE", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
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
        Box(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            when (currentState) {
                ToolState.SELECTING -> {
                    SelectionGrid(
                        onSelect = { pickLauncher.launch("application/pdf") }, 
                        isDark = isDark,
                        icon = Icons.Outlined.BurstMode,
                        title = "Tap to select PDF",
                        subtitle = "CONVERT DOCUMENT TO IMAGES",
                        accentColor = accentColor,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                ToolState.CONFIGURING -> {
                    Column(Modifier.fillMaxSize()) {
                        Spacer(Modifier.height(16.dp))
                        
                        // Settings Panel
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("EXPORT CONFIGURATION", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 1.2.sp)
                                Spacer(Modifier.height(12.dp))
                                
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Column(Modifier.weight(1f)) {
                                        Text("FORMAT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                        Spacer(Modifier.height(8.dp))
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            listOf("JPG", "PNG", "WebP").forEach { f ->
                                                val isSel = format == f
                                                Surface(
                                                    onClick = { format = f },
                                                    modifier = Modifier.weight(1f).height(32.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSel) accentColor else Color.Gray.copy(0.1f)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(f, fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (isSel) Color.White else Color.Gray)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    Column(Modifier.weight(1f)) {
                                        Text("QUALITY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                        Spacer(Modifier.height(8.dp))
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            listOf("Standard", "HD", "Ultra HD").forEach { q ->
                                                val isSel = quality == q
                                                Surface(
                                                    onClick = { quality = q },
                                                    modifier = Modifier.weight(1f).height(32.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSel) accentColor else Color.Gray.copy(0.1f)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(q.replace(" HD", "+"), fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (isSel) Color.White else Color.Gray)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Text("SELECT PAGES TO EXPORT", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 1.sp, modifier = Modifier.padding(start = 4.dp))
                        Spacer(Modifier.height(8.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(bottom = 100.dp)
                            ) {
                                items(pageCount, key = { it }) { index ->
                                    val isSelected = selectedPages.contains(index)
                                    PdfPageItem(
                                        uri = selectedUri!!,
                                        index = index,
                                        password = unlockPassword.ifEmpty { null },
                                        imageLoader = imageLoader,
                                        accentColor = accentColor,
                                        onClick = { lightboxPage = index },
                                        modifier = Modifier.border(
                                            BorderStroke(2.dp, if (isSelected) accentColor else Color.Transparent), 
                                            RoundedCornerShape(12.dp)
                                        )
                                    ) {
                                        // Selection Checkbox
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(if (isSelected) accentColor.copy(alpha = 0.1f) else Color.Transparent)
                                                .clickable { selectedPages = if (isSelected) selectedPages - index else selectedPages + index }
                                        )

                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                                .size(24.dp),
                                            color = if (isSelected) accentColor else Color.Black.copy(0.3f),
                                            shape = CircleShape
                                        ) {
                                            Icon(
                                                Icons.Filled.Check, 
                                                null, 
                                                tint = Color.White, 
                                                modifier = Modifier.padding(4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { saveLauncher.launch(fileName.replace(".pdf", ".zip")) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).height(56.dp),
                            enabled = selectedPages.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Convert ${selectedPages.size} Pages to $format", fontWeight = FontWeight.Black)
                        }
                    }
                }
                ToolState.PROCESSING -> {
                    ProcessingStateView(
                        accentColor = accentColor,
                        uri = selectedUri,
                        password = unlockPassword.ifEmpty { null },
                        text = "Rendering page $progressCount of ${selectedPages.size}...",
                        current = progressCount,
                        total = selectedPages.size,
                        showWarning = showLoadingWarning
                    )
                }
                ToolState.SUCCESS -> {
                    SuccessView(
                        message = "Images Exported",
                        subMessage = "${selectedPages.size} pages saved to ZIP",
                        processingTime = processingTime,
                        onDone = onBack,
                        onProcessMore = { selectedUri = null; currentState = ToolState.SELECTING },
                        showPreviewButton = false,
                        accentColor = accentColor
                    )
                }
                else -> {}
            }

            if (fileToUnlock != null) {
                LockedFilePrompt(
                    fileName = fileToUnlock!!,
                    onDismiss = { fileToUnlock = null; selectedUri = null; currentState = ToolState.SELECTING },
                    onUnlocked = { pass ->
                        unlockPassword = pass
                        isFileLoading = true
                        scope.launch(Dispatchers.IO) {
                            val count = getPageCount(context, selectedUri!!, pass)
                            if (count > 0) {
                                withContext(Dispatchers.Main) { 
                                    pageCount = count
                                    selectedPages = (0 until count).toSet()
                                    currentState = ToolState.CONFIGURING
                                    fileToUnlock = null
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
                    accentColor = accentColor,
                    isLoading = isFileLoading
                )
            }
        }
    }

    if (lightboxPage != null && selectedUri != null) {
        PageLightbox(
            uri = selectedUri!!,
            initialPage = lightboxPage!!,
            totalCount = pageCount,
            password = unlockPassword.ifEmpty { null },
            onDismiss = { lightboxPage = null },
            selectedPages = selectedPages,
            onToggleSelection = { index ->
                selectedPages = if (selectedPages.contains(index)) selectedPages - index else selectedPages + index
            }
        )
    }
}
