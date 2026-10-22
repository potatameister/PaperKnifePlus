package com.paperknifeplus.app.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.LocalImageLoader
import coil.compose.rememberAsyncImagePainter
import com.paperknifeplus.app.ui.theme.PaperPink
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CompressView(
    initialUri: Uri? = null,
    initialPassword: String? = null,
    onBack: () -> Unit,
    onOpenPreview: (Uri, String, Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val accentColor = Color(0xFFF59E0B)

    var currentState by remember { mutableStateOf<ToolState>(ToolState.SELECTING) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var outputUri by remember { mutableStateOf<Uri?>(null) }
    var unlockPassword by remember { mutableStateOf(initialPassword ?: "") }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf("") }
    var fileSizeOld by remember { mutableLongStateOf(0L) }
    var isFileLoading by remember { mutableStateOf(false) }
    var processingTime by remember { mutableStateOf("") }
    var spaceSavedText by remember { mutableStateOf("") }
    
    var compressionLevel by remember { mutableStateOf("Recommended") }
    var pageCount by remember { mutableIntStateOf(0) }
    var progressPage by remember { mutableIntStateOf(0) }
    var showLoadingWarning by remember { mutableStateOf(false) }
    var fileToUnlock by remember { mutableStateOf<String?>(null) }

    fun handleFileSelection(uri: Uri, password: String? = null) {
        selectedUri = uri
        val details = getUriDetails(context, uri)
        fileName = details.name
        fileSize = details.size
        fileSizeOld = details.sizeBytes
        
        isFileLoading = true
        scope.launch(Dispatchers.IO) {
            val isEncrypted = checkIsEncryptedLocal(context, uri)
            if (isEncrypted && password == null) {
                withContext(Dispatchers.Main) {
                    fileToUnlock = fileName
                    isFileLoading = false
                }
            } else {
                val count = getPageCount(context, uri, password)
                withContext(Dispatchers.Main) {
                    pageCount = count
                    currentState = ToolState.CONFIGURING
                    isFileLoading = false
                }
            }
        }
    }

    LaunchedEffect(initialUri) {
        initialUri?.let { handleFileSelection(it, initialPassword) }
    }

    LaunchedEffect(isFileLoading, currentState) {
        if (isFileLoading || currentState == ToolState.PROCESSING) {
            delay(5000)
            showLoadingWarning = true
        } else {
            showLoadingWarning = false
        }
    }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { handleFileSelection(it) }
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { saveUri ->
            currentState = ToolState.PROCESSING
            val startTime = System.currentTimeMillis()
            scope.launch(Dispatchers.IO) {
                try {
                    compressPdf(context, selectedUri!!, saveUri, if (unlockPassword.isEmpty()) null else unlockPassword, compressionLevel) { current, total ->
                        progressPage = current
                    }
                    val endTime = System.currentTimeMillis()
                    val timeStr = String.format("%.1fs", (endTime - startTime) / 1000.0)
                    
                    val newDetails = getUriDetails(context, saveUri)
                    val spaceSaved = if (fileSizeOld > 0) ((fileSizeOld - newDetails.sizeBytes).toFloat() / fileSizeOld * 100).toInt().coerceAtLeast(0) else 0
                    val finalCount = getPageCount(context, saveUri, null)
                    
                    withContext(Dispatchers.Main) {
                        processingTime = timeStr
                        outputUri = saveUri
                        fileName = getUriDetails(context, saveUri).name
                        SessionManager.addEntry(fileName, "Compress", "Optimized", ToolCatalog.icon("compress"), saveUri, finalCount)
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
                ToolTopBar(
                    toolId = "compress",
                    onBack = onBack,
                    actionLabel = "CHANGE",
                    onAction = { selectedUri = null; currentState = ToolState.SELECTING },
                    showAction = selectedUri != null && currentState == ToolState.CONFIGURING,
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            if (isFileLoading) {
                LoadingStateView(accentColor, showLoadingWarning, "Analyzing file structure...")
            } else {
                when (currentState) {
                    ToolState.SELECTING -> {
                        SelectionGrid(
                            onSelect = { pickLauncher.launch("application/pdf") }, 
                            isDark = isDark,
                            icon = ToolCatalog.icon("compress"),
                            title = "Tap to enter file",
                            subtitle = "OPTIMIZE ANY PDF DOCUMENT",
                            accentColor = accentColor,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    ToolState.CONFIGURING -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(Modifier.height(12.dp))

                            ToolFileCard(
                                fileName = fileName,
                                fileSize = fileSize,
                                pageCount = pageCount,
                                icon = ToolCatalog.icon("compress"),
                                accentColor = accentColor
                            )

                            Spacer(Modifier.height(16.dp))

                            Box(modifier = Modifier.fillMaxWidth(0.5f).aspectRatio(0.707f)) {
                                UnifiedPdfPreview(
                                    uri = selectedUri!!,
                                    pageCount = pageCount,
                                    mode = PreviewMode.COVER,
                                    password = null,
                                    accentColor = accentColor
                                )
                            }

                            Spacer(Modifier.height(16.dp))
                            Text("COMPRESSION LEVEL", fontSize = 10.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.5.sp)
                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Basic", "Recommended", "Extreme").forEach { level ->
                                    val isSelected = compressionLevel == level
                                    Surface(
                                        onClick = { compressionLevel = level },
                                        color = if (isSelected) accentColor else Color.Transparent,
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) accentColor else Color.Gray.copy(alpha = 0.15f)
                                        ),
                                        modifier = Modifier.weight(1f).height(46.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = level,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = when (compressionLevel) {
                                    "Extreme" -> "Smallest file, lower image quality"
                                    "Recommended" -> "Best balance of size and quality"
                                    else -> "Keeps high visual fidelity"
                                },
                                modifier = Modifier.fillMaxWidth(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(Modifier.weight(1f))

                            Button(
                                onClick = {
                                    val defaultName = fileName.replace(".pdf", "", true) + "-compressed.pdf"
                                    saveLauncher.launch(defaultName)
                                },
                                modifier = Modifier.fillMaxWidth().height(60.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White)
                            ) {
                                Text("COMPRESS & SAVE PDF", fontWeight = FontWeight.Black, color = Color.White)
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                    ToolState.PROCESSING -> {
                        ProcessingStateView(
                            accentColor = accentColor,
                            uri = selectedUri,
                            password = if (unlockPassword.isEmpty()) null else unlockPassword,
                            text = "Optimizing document size...",
                            current = progressPage,
                            total = pageCount,
                            showWarning = showLoadingWarning
                        )
                    }
                    ToolState.SUCCESS -> {
                        SuccessView(
                            message = "Compression Complete",
                            subMessage = "", // REMOVED 0B BUG
                            processingTime = processingTime,
                            onDone = onBack,
                            onProcessMore = { 
                                selectedUri = null
                                outputUri = null
                                unlockPassword = ""
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
                            val decryptedUri = decryptToCache(context, selectedUri!!, pass)
                            if (decryptedUri != null) {
                                val count = getPageCount(context, decryptedUri, null)
                                withContext(Dispatchers.Main) { 
                                    if (count > 0) {
                                        unlockPassword = pass
                                        selectedUri = decryptedUri
                                        pageCount = count
                                        currentState = ToolState.CONFIGURING
                                        fileToUnlock = null
                                    } else {
                                        Toast.makeText(context, "Invalid Password", Toast.LENGTH_SHORT).show()
                                    }
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
}
