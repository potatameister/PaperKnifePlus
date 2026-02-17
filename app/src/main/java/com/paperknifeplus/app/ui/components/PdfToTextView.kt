package com.paperknifeplus.app.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperknifeplus.app.ui.theme.PaperPink
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToTextView(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val accentColor = Color(0xFF14B8A6)
    
    var currentState by remember { mutableStateOf<ToolState>(ToolState.SELECTING) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var unlockPassword by remember { mutableStateOf("") }
    var extractedText by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf("") }
    var isFileLoading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
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
                    val text = extractTextFromPdf(context, it, null)
                    withContext(Dispatchers.Main) {
                        extractedText = text
                        currentState = ToolState.CONFIGURING
                        isFileLoading = false
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) { PDFBoxResourceLoader.init(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("PDF to Text", fontWeight = FontWeight.Black, fontSize = 18.sp)
                        if (currentState == ToolState.CONFIGURING) Text(fileName, fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (extractedText.isNotBlank() && currentState == ToolState.CONFIGURING) {
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(extractedText))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Outlined.ContentCopy, "Copy", tint = accentColor)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            if (isFileLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentColor)
                }
            } else {
                when (currentState) {
                    ToolState.SELECTING -> {
                        SelectionGrid(
                            onSelect = { launcher.launch("application/pdf") }, 
                            isDark = isDark,
                            icon = Icons.Outlined.Description,
                            title = "Tap to enter file",
                            subtitle = "EXTRACT PLAIN TEXT CONTENT",
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
                                    val text = extractTextFromPdf(context, selectedUri!!, unlockPassword)
                                    withContext(Dispatchers.Main) {
                                        extractedText = text
                                        currentState = ToolState.CONFIGURING
                                        isFileLoading = false
                                    }
                                }
                            },
                            onCancel = { selectedUri = null; currentState = ToolState.SELECTING },
                            accentColor = accentColor
                        )
                    }
                    ToolState.CONFIGURING -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "EXTRACTED CONTENT",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Gray,
                                    letterSpacing = 1.5.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                if (extractedText.isBlank()) {
                                    Text(
                                        "NO TEXT FOUND",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = PaperPink
                                    )
                                }
                            }
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                                    .background(
                                        if (isDark) Color(0xFF09090B) else Color(0xFFF8F9FA),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .border(BorderStroke(1.dp, Color.Gray.copy(0.1f)), RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                            ) {
                                val scrollState = rememberScrollState()
                                Text(
                                    text = extractedText.ifBlank { "This PDF contains no extractable text. It might be a scanned image. Use OCR tool (coming soon) for these files." },
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    modifier = Modifier.verticalScroll(scrollState),
                                    color = if (extractedText.isBlank()) Color.Gray else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            Button(
                                onClick = { selectedUri = null; extractedText = ""; currentState = ToolState.SELECTING },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Text("SELECT ANOTHER", fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }
                    ToolState.PROCESSING -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = accentColor)
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

private suspend fun extractTextFromPdf(context: android.content.Context, uri: Uri, password: String?): String = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val document = if (password != null) PDDocument.load(inputStream, password) else PDDocument.load(inputStream)
            val stripper = PDFTextStripper()
            stripper.sortByPosition = true
            val text = stripper.getText(document) ?: ""
            document.close()
            text
        } ?: "Error opening file"
    } catch (e: Exception) {
        e.printStackTrace()
        "Error extracting text: ${e.localizedMessage}"
    }
}
