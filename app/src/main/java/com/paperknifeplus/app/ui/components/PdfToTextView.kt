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
fun PdfToTextView(onBack: () -> Unit) {
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
                    withContext(Dispatchers.Main) { 
                        currentState = ToolState.PROCESSING 
                        isFileLoading = false
                    }
                    processText(context, it, null) { text, time ->
                        extractedText = text
                        processingTime = time
                        currentState = ToolState.SUCCESS
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
                        Text("PDF to Text", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                        Text("EXTRACT RAW CONTENT", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
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
                ToolState.PROCESSING -> {
                    LoadingStateView(accentColor, showLoadingWarning, "Reading text layers...")
                }
                ToolState.SUCCESS -> {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("EXTRACTED CONTENT", fontSize = 10.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.5.sp)
                            Row {
                                IconButton(onClick = { 
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("PDF Text", extractedText)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Filled.ContentCopy, null, tint = accentColor)
                                }
                                IconButton(onClick = { saveTxtLauncher.launch(fileName.replace(".pdf", ".txt")) }) {
                                    Icon(Icons.Filled.Save, null, tint = accentColor)
                                }
                            }
                        }
                        
                        Surface(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            color = if (isDark) Color(0xFF09090B) else Color.White,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color.Gray.copy(0.1f))
                        ) {
                            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
                                if (extractedText.trim().isEmpty()) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Filled.Build, null, modifier = Modifier.size(48.dp).alpha(0.2f))
                                            Text("No text found. Document might be a scan.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                                        }
                                    }
                                } else {
                                    Text(text = extractedText, fontSize = 12.sp, fontFamily = FontFamily.Monospace, lineHeight = 18.sp)
                                }
                            }
                        }
                        
                        Button(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Done", fontWeight = FontWeight.Black)
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
                            withContext(Dispatchers.Main) { 
                                fileToUnlock = null
                            }
                            processText(context, selectedUri!!, pass) { text, time ->
                                extractedText = text
                                processingTime = time
                                withContext(Dispatchers.Main) { 
                                    currentState = ToolState.SUCCESS 
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
