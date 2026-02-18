package com.paperknifeplus.app.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperknifeplus.app.ui.theme.PaperPink
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CompressView(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val accentColor = Color(0xFFF59E0B)

    var currentState by remember { mutableStateOf<ToolState>(ToolState.SELECTING) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var unlockPassword by remember { mutableStateOf("") }
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
            fileSizeOld = details.sizeBytes
            
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
                    withContext(Dispatchers.Main) {
                        pageCount = count
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
                    compressPdf(context, selectedUri!!, saveUri, if (unlockPassword.isEmpty()) null else unlockPassword, compressionLevel) { current, total ->
                        progressPage = current
                    }
                    val endTime = System.currentTimeMillis()
                    val timeStr = String.format("%.1fs", (endTime - startTime) / 1000.0)
                    
                    val newDetails = getUriDetails(context, saveUri)
                    val spaceSaved = if (fileSizeOld > 0) ((fileSizeOld - newDetails.sizeBytes).toFloat() / fileSizeOld * 100).toInt().coerceAtLeast(0) else 0
                    
                    withContext(Dispatchers.Main) {
                        processingTime = timeStr
                        spaceSavedText = "REDUCED BY $spaceSaved% (${newDetails.size})"
                        SessionManager.addEntry(fileName, "Compress", "Optimized", Icons.Outlined.Bolt)
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
                    Column {
                        Text("Compress", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                        Text("OPTIMIZE DOCUMENT SIZE", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            if (isFileLoading) {
                LoadingStateView(accentColor, showLoadingWarning, "Analyzing file structure...")
            } else {
                when (currentState) {
                    ToolState.SELECTING -> {
                        SelectionGrid(
                            onSelect = { pickLauncher.launch("application/pdf") }, 
                            isDark = isDark,
                            icon = Icons.Outlined.Bolt,
                            title = "Tap to enter file",
                            subtitle = "OPTIMIZE ANY PDF DOCUMENT",
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
                                    if (count > 0) {
                                        withContext(Dispatchers.Main) {
                                            pageCount = count
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
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            Spacer(Modifier.height(16.dp))
                            
                            // UNIFIED PREVIEW: COVER MODE
                            UnifiedPdfPreview(
                                uri = selectedUri!!,
                                pageCount = pageCount,
                                mode = PreviewMode.COVER,
                                password = if (unlockPassword.isEmpty()) null else unlockPassword,
                                accentColor = accentColor
                            )
                            
                            Spacer(Modifier.height(16.dp))
                            Text(fileName, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                            Text(fileSize, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally))
                            
                            Spacer(Modifier.height(32.dp))
                            Text("OPTIMIZATION LEVEL", fontSize = 10.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.5.sp)
                            Spacer(Modifier.height(12.dp))
                            
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Basic", "Recommended", "Extreme").forEach { level ->
                                    val isSelected = compressionLevel == level
                                    Surface(
                                        onClick = { compressionLevel = level },
                                        color = if (isSelected) accentColor.copy(alpha = 0.1f) else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, if (isSelected) accentColor else Color.Gray.copy(0.1f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(selected = isSelected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = accentColor))
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text(level, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(
                                                    text = when(level) {
                                                        "Extreme" -> "Max reduction, lower image quality"
                                                        "Recommended" -> "Great balance of size and quality"
                                                        else -> "Maintains high visual fidelity"
                                                    },
                                                    fontSize = 10.sp, color = Color.Gray
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(32.dp))
                            Button(
                                onClick = { 
                                    val defaultName = fileName.replace(".pdf", "", true) + "-compressed.pdf"
                                    saveLauncher.launch(defaultName) 
                                }, 
                                modifier = Modifier.fillMaxWidth().height(60.dp), 
                                shape = RoundedCornerShape(20.dp), 
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Text("COMPRESS & SAVE PDF", fontWeight = FontWeight.Black, color = Color.White)
                            }
                            TextButton(onClick = { selectedUri = null; currentState = ToolState.SELECTING }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                Text("CHANGE FILE", color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(100.dp))
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
                            subMessage = spaceSavedText,
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
}
