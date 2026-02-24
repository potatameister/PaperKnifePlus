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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperknifeplus.app.ui.theme.PaperPink
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageNumbersView(
    onBack: () -> Unit,
    onOpenPreview: (Uri, String, Int) -> Unit
) {
    val context = LocalContext.current
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
    
    var selectedPages by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var position by remember { mutableStateOf("Bottom Right") }
    var fontSize by remember { mutableFloatStateOf(12f) }
    var format by remember { mutableStateOf("Page {n}") }
    var numberColor by remember { mutableStateOf(Color.Black) }
    
    var showPreviewOverlay by remember { mutableStateOf(true) }

    fun handleFileSelection(uri: Uri) {
        selectedUri = uri
        val details = getUriDetails(context, uri)
        fileName = details.name
        isFileLoading = true
        scope.launch(Dispatchers.IO) {
            val isEnc = checkIsEncryptedLocal(context, uri)
            withContext(Dispatchers.Main) {
                if (isEnc) {
                    fileToUnlock = fileName
                    isFileLoading = false
                } else {
                    val count = getPageCount(context, uri, null)
                    pageCount = count
                    selectedPages = (0 until count).toSet() 
                    currentState = ToolState.CONFIGURING
                    isFileLoading = false
                }
            }
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
                    context.contentResolver.openInputStream(selectedUri!!)?.use { inputStream ->
                        val document = if (unlockPassword.isNotEmpty()) PDDocument.load(inputStream, unlockPassword) else PDDocument.load(inputStream)
                        if (document.isEncrypted) document.isAllSecurityToBeRemoved = true
                        
                        selectedPages.forEach { pageIdx ->
                            if (pageIdx < document.numberOfPages) {
                                val page = document.getPage(pageIdx)
                                PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                                    val rect = page.mediaBox
                                    val text = format.replace("{n}", (pageIdx + 1).toString())
                                    val font = PDType1Font.HELVETICA_BOLD
                                    val textWidth = font.getStringWidth(text) / 1000 * fontSize
                                    
                                    val margin = 30f
                                    val x = when {
                                        position.contains("Left") -> margin
                                        position.contains("Center") -> (rect.width - textWidth) / 2
                                        else -> rect.width - textWidth - margin
                                    }
                                    val y = when {
                                        position.contains("Top") -> rect.height - margin - fontSize
                                        else -> margin
                                    }
                                    
                                    cs.beginText()
                                    cs.setFont(font, fontSize)
                                    // NITRO: Proper color conversion for PDFBox
                                    cs.setNonStrokingColor(numberColor.red, numberColor.green, numberColor.blue)
                                    cs.newLineAtOffset(x, y)
                                    cs.showText(text)
                                    cs.endText()
                                }
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
                            Text("Page Numbers", fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Text("ADD PAGINATION TO DOCUMENT", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isFileLoading) {
                LoadingStateView(accentColor, false, "Preparing document...")
            } else {
                when (currentState) {
                    ToolState.SELECTING -> {
                        SelectionGrid(
                            onSelect = { pickLauncher.launch("application/pdf") }, 
                            isDark = isDark,
                            icon = Icons.Filled.FormatListNumbered,
                            title = "Tap to enter file",
                            subtitle = "ADD PAGE NUMBERS",
                            accentColor = accentColor,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)
                        )
                    }
                    ToolState.CONFIGURING -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(Modifier.height(12.dp))
                            
                            // Header Stats & Selection
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("${selectedPages.size} / $pageCount PAGES", fontSize = 10.sp, fontWeight = FontWeight.Black, color = accentColor)
                                }
                                Row {
                                    TextButton(onClick = { selectedPages = (0 until pageCount).toSet() }) {
                                        Text("SELECT ALL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                    }
                                    TextButton(onClick = { selectedPages = emptySet() }) {
                                        Text("CLEAR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                UnifiedPdfPreview(
                                    uri = selectedUri!!,
                                    pageCount = pageCount,
                                    mode = PreviewMode.GRID,
                                    password = unlockPassword.ifEmpty { null }, 
                                    accentColor = accentColor,
                                    showIndexNumbers = false, // HIDE DEFAULT GRID NUMBERS
                                    selectedPages = selectedPages,
                                    onToggleSelection = { index ->
                                        selectedPages = if (selectedPages.contains(index)) selectedPages - index else selectedPages + index
                                    },
                                    itemOverlay = { index ->
                                        if (showPreviewOverlay && selectedPages.contains(index)) {
                                            val text = format.replace("{n}", (index + 1).toString())
                                            Box(Modifier.fillMaxSize(), contentAlignment = when {
                                                position.contains("Bottom") && position.contains("Left") -> Alignment.BottomStart
                                                position.contains("Bottom") && position.contains("Center") -> Alignment.BottomCenter
                                                position.contains("Bottom") && position.contains("Right") -> Alignment.BottomEnd
                                                position.contains("Top") && position.contains("Left") -> Alignment.TopStart
                                                position.contains("Top") && position.contains("Center") -> Alignment.TopCenter
                                                position.contains("Top") && position.contains("Right") -> Alignment.TopEnd
                                                else -> Alignment.BottomEnd
                                            }) {
                                                // OVERLAY PNG-STYLE TEXT (NO BOX)
                                                Text(
                                                    text = text, 
                                                    color = numberColor, 
                                                    fontSize = 8.sp, 
                                                    fontWeight = FontWeight.Black, 
                                                    modifier = Modifier.padding(6.dp)
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                            
                            Spacer(Modifier.height(12.dp))
                            
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text("PREVIEW PLACEMENT", fontSize = 9.sp, fontWeight = FontWeight.Black, color = accentColor, modifier = Modifier.weight(1f))
                                        Switch(
                                            checked = showPreviewOverlay, 
                                            onCheckedChange = { showPreviewOverlay = it }, 
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = accentColor,
                                                uncheckedThumbColor = Color.Gray,
                                                uncheckedTrackColor = Color.Gray.copy(0.2f)
                                            )
                                        )
                                    }
                                    
                                    Spacer(Modifier.height(12.dp))
                                    
                                    // Formatting & Color
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = format,
                                            onValueChange = { format = it },
                                            label = { Text("Format ({n})", fontSize = 10.sp) },
                                            modifier = Modifier.weight(1.5f),
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = accentColor,
                                                focusedLabelColor = accentColor
                                            )
                                        )
                                        
                                        // Color Picker (Presets)
                                        Surface(
                                            modifier = Modifier.weight(1f).height(56.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(0.3f)),
                                            onClick = {
                                                numberColor = when(numberColor) {
                                                    Color.Black -> Color.White
                                                    Color.White -> Color.Red
                                                    Color.Red -> Color.Blue
                                                    else -> Color.Black
                                                }
                                            }
                                        ) {
                                            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                                Box(Modifier.size(16.dp).clip(CircleShape).background(numberColor).border(1.dp, Color.Gray.copy(0.3f), CircleShape))
                                                Spacer(Modifier.width(8.dp))
                                                Text("COLOR", fontSize = 9.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                    }
                                    
                                    Spacer(Modifier.height(8.dp))
                                    
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(0.3f)),
                                        onClick = { 
                                            position = when(position) {
                                                "Bottom Right" -> "Bottom Left"
                                                "Bottom Left" -> "Bottom Center"
                                                "Bottom Center" -> "Top Right"
                                                "Top Right" -> "Top Left"
                                                "Top Left" -> "Top Center"
                                                else -> "Bottom Right"
                                            }
                                        }
                                    ) {
                                        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text("POSITION:", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                                            Spacer(Modifier.width(8.dp))
                                            Text(position.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
                                        }
                                    }
                                }
                            }
                            
                            Button(
                                onClick = { saveLauncher.launch(fileName.replace(".pdf", "") + "-numbered.pdf") },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(60.dp),
                                enabled = selectedPages.isNotEmpty(),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Text("APPLY PAGE NUMBERS", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    ToolState.PROCESSING -> {
                        ProcessingStateView(
                            accentColor = accentColor,
                            uri = selectedUri,
                            password = unlockPassword.ifEmpty { null },
                            text = "Applying numbering...",
                            current = 0,
                            total = 0,
                            showWarning = false
                        )
                    }
                    ToolState.SUCCESS -> {
                        SuccessView(
                            message = "Numbering Complete",
                            subMessage = "Page numbers added successfully",
                            processingTime = processingTime,
                            onDone = onBack,
                            onProcessMore = { 
                                selectedUri = null
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
                            val count = getPageCount(context, selectedUri!!, pass)
                            withContext(Dispatchers.Main) { 
                                if (count > 0) {
                                    unlockPassword = pass
                                    pageCount = count
                                    selectedPages = (0 until count).toSet()
                                    currentState = ToolState.CONFIGURING
                                    fileToUnlock = null
                                } else {
                                    Toast.makeText(context, "Invalid Password", Toast.LENGTH_SHORT).show()
                                }
                                isFileLoading = false
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
