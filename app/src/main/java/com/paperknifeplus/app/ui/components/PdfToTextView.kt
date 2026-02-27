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
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperknifeplus.app.ui.theme.PaperPink
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PdfToTextView(
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
    var extractedText by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var processingTime by remember { mutableStateOf("") }
    var showLoadingWarning by remember { mutableStateOf(false) }
    var fileToUnlock by remember { mutableStateOf<String?>(null) }
    var isFileLoading by remember { mutableStateOf(false) }
    var pageCount by remember { mutableIntStateOf(0) }

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

    val saveTxtLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let { saveUri ->
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(saveUri)?.use { it.write(extractedText.toByteArray()) }
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Saved as TXT", Toast.LENGTH_SHORT).show() }
                } catch (e: Exception) { }
            }
        }
    }

    fun startExtraction() {
        currentState = ToolState.PROCESSING
        scope.launch(Dispatchers.IO) {
            processText(context, selectedUri!!, unlockPassword.ifEmpty { null }) { text, time ->
                extractedText = text
                processingTime = time
                withContext(Dispatchers.Main) {
                    SessionManager.addEntry(fileName, "To Text", "${text.length} chars", Icons.Outlined.Description)
                    currentState = ToolState.SUCCESS
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
                            Text("PDF to Text", fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Text("EXTRACT RAW CONTENT", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
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
            if (isFileLoading) {
                LoadingStateView(accentColor, showLoadingWarning, "Preparing document...")
            } else {
                when (currentState) {
                    ToolState.SELECTING -> {
                        SelectionGrid(
                            onSelect = { pickLauncher.launch("application/pdf") }, 
                            isDark = isDark,
                            icon = Icons.Outlined.Description,
                            title = "Tap to select PDF",
                            subtitle = "EXTRACT TEXT DATA",
                            accentColor = accentColor,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    ToolState.CONFIGURING -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(Modifier.height(16.dp))
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(0.85f)) {
                                UnifiedPdfPreview(
                                    uri = selectedUri!!,
                                    pageCount = pageCount,
                                    mode = PreviewMode.COVER,
                                    password = unlockPassword.ifEmpty { null },
                                    accentColor = accentColor
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(fileName, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                            
                            Spacer(Modifier.height(24.dp))
                            Text("READY TO EXTRACT", fontSize = 10.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.5.sp)
                            Text("Scan PDF text layers and output a plain .txt file.", fontSize = 12.sp, color = Color.Gray)
                            
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = { startExtraction() },
                                modifier = Modifier.fillMaxWidth().height(60.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("EXTRACT TEXT", fontWeight = FontWeight.Black)
                            }
                            Spacer(Modifier.height(32.dp))
                        }
                    }
                    ToolState.PROCESSING -> {
                        ProcessingStateView(
                            accentColor = accentColor,
                            uri = selectedUri,
                            password = unlockPassword.ifEmpty { null },
                            text = "Reading text layers...",
                            current = 0,
                            total = pageCount,
                            showWarning = showLoadingWarning
                        )
                    }
                    ToolState.SUCCESS -> {
                        Column(Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween, 
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("EXTRACTED CONTENT", fontSize = 10.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.5.sp)
                                    Text("${extractedText.length} CHARACTERS FOUND", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                }
                                Row {
                                    IconButton(onClick = { 
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("PDF Text", extractedText)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }, modifier = Modifier.background(accentColor.copy(alpha = 0.1f), CircleShape).size(36.dp)) {
                                        Icon(Icons.Filled.ContentCopy, null, tint = accentColor, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    IconButton(onClick = { saveTxtLauncher.launch(fileName.replace(".pdf", ".txt")) }, modifier = Modifier.background(accentColor.copy(alpha = 0.1f), CircleShape).size(36.dp)) {
                                        Icon(Icons.Filled.Save, null, tint = accentColor, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            
                            Surface(
                                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                                color = if (isDark) Color(0xFF09090B) else Color.White,
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, Color.Gray.copy(0.1f))
                            ) {
                                Box(Modifier.padding(20.dp)) {
                                    if (extractedText.trim().isEmpty()) {
                                        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Filled.FindInPage, null, modifier = Modifier.size(48.dp).alpha(0.2f), tint = accentColor)
                                            Text("No text detected. The document might be a high-res scan.", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
                                        }
                                    } else {
                                        Column(Modifier.verticalScroll(rememberScrollState())) {
                                            Text(
                                                text = extractedText, 
                                                fontSize = 13.sp, 
                                                fontFamily = FontFamily.Monospace, 
                                                lineHeight = 20.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Button(
                                onClick = onBack,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp)
                                    .height(56.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White)
                            ) {
                                Text("DONE", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    else -> {}
                }
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
                            withContext(Dispatchers.Main) { 
                                fileToUnlock = null
                                if (count > 0) {
                                    pageCount = count
                                    currentState = ToolState.CONFIGURING
                                    isFileLoading = false
                                } else {
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

private suspend fun processText(context: android.content.Context, uri: Uri, password: String?, onResult: suspend (String, String) -> Unit) = withContext(Dispatchers.IO) {
    val startTime = System.currentTimeMillis()
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val document = if (password != null) PDDocument.load(inputStream, password) else PDDocument.load(inputStream)
            val stripper = PDFTextStripper()
            val totalPages = document.numberOfPages
            val builder = StringBuilder()
            
            for (i in 1..totalPages) {
                stripper.startPage = i
                stripper.endPage = i
                val pageText = stripper.getText(document)
                builder.append("--- PAGE $i ---\n")
                builder.append(pageText)
                builder.append("\n\n")
            }
            
            document.close()
            val time = String.format("%.1fs", (System.currentTimeMillis() - startTime) / 1000.0)
            onResult(builder.toString(), time)
        }
    } catch (e: Exception) {
        onResult("Error: ${e.message}", "0s")
    }
}
