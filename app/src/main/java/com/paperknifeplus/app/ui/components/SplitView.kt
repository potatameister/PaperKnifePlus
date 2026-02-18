package com.paperknifeplus.app.ui.components

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
fun SplitView(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val accentColor = Color(0xFFF43F5E)

    var currentState by remember { mutableStateOf<ToolState>(ToolState.SELECTING) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var unlockPassword by remember { mutableStateOf("") }
    var range by remember { mutableStateOf("1-2") }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf("") }
    var pageCount by remember { mutableIntStateOf(0) }
    var isFileLoading by remember { mutableStateOf(false) }
    var processingTime by remember { mutableStateOf("") }
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
                    context.contentResolver.openInputStream(selectedUri!!)?.use { inputStream ->
                        val document = if (unlockPassword.isNotEmpty()) PDDocument.load(inputStream, unlockPassword) else PDDocument.load(inputStream)
                        val newDocument = PDDocument()
                        val parts = range.split(",").flatMap { part ->
                            if (part.contains("-")) {
                                val split = part.split("-")
                                (split[0].toInt()..split[1].toInt()).toList()
                            } else listOf(part.trim().toInt())
                        }
                        parts.forEach { pageNum ->
                            if (pageNum <= document.numberOfPages) {
                                newDocument.addPage(document.getPage(pageNum - 1))
                            }
                        }
                        saveAndFlush(context, newDocument, saveUri)
                        document.close()
                    }
                    val endTime = System.currentTimeMillis()
                    val timeStr = String.format("%.1fs", (endTime - startTime) / 1000.0)
                    withContext(Dispatchers.Main) {
                        processingTime = timeStr
                        SessionManager.addEntry(fileName, "Split", "Extracted pages", Icons.Default.ContentCut)
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
                        Text("Split", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                        Text("EXTRACT PAGES FROM PDF", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
                    }
                }
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
                            icon = Icons.Default.ContentCut,
                            title = "Tap to enter file",
                            subtitle = "SPLIT PDF INTO PARTS",
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
                            
                            UnifiedPdfPreview(
                                uri = selectedUri!!,
                                pageCount = pageCount,
                                mode = PreviewMode.COVER,
                                password = if (unlockPassword.isEmpty()) null else unlockPassword,
                                accentColor = accentColor
                            )
                            
                            Spacer(Modifier.height(12.dp))
                            Text(fileName, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                            Text(fileSize, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally))
                            
                            Spacer(Modifier.height(32.dp))
                            Text("PAGE RANGE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.5.sp)
                            Spacer(Modifier.height(12.dp))
                            
                            OutlinedTextField(
                                value = range,
                                onValueChange = { range = it },
                                label = { Text("Example: 1-5, 8, 11-13") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = accentColor,
                                    cursorColor = accentColor,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent
                                )
                            )
                            
                            Spacer(Modifier.height(32.dp))
                            
                            Button(
                                onClick = { saveLauncher.launch(fileName.replace(".pdf", "", true) + "-split.pdf") }, 
                                modifier = Modifier.fillMaxWidth().height(60.dp), 
                                shape = RoundedCornerShape(20.dp), 
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Text("EXTRACT & SAVE", fontWeight = FontWeight.Black, color = Color.White)
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
                            password = unlockPassword.ifEmpty { null },
                            text = "Extracting specified pages...",
                            current = 0,
                            total = 0,
                            showWarning = showLoadingWarning
                        )
                    }
                    ToolState.SUCCESS -> {
                        SuccessView(
                            message = "Split Complete",
                            subMessage = "Selected pages saved successfully",
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
