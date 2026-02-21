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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    
    var selectedPages by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var position by remember { mutableStateOf("Bottom Right") }
    var fontSize by remember { mutableFloatStateOf(12f) }
    var format by remember { mutableStateOf("Page {n}") }
    
    // Pro Feature: Compare Toggle
    var showPreviewOverlay by remember { mutableStateOf(true) }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedUri = it
            val details = getUriDetails(context, it)
            fileName = details.name
            isFileLoading = true
            scope.launch(Dispatchers.IO) {
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

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { saveUri ->
            currentState = ToolState.PROCESSING
            val startTime = System.currentTimeMillis()
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(selectedUri!!)?.use { inputStream ->
                        val document = PDDocument.load(inputStream)
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
                                    cs.setNonStrokingColor(android.graphics.Color.BLACK)
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
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), CircleShape)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Page Numbers", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                        Text("ADD PAGINATION TO DOCUMENT", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isFileLoading) {
                LoadingStateView(accentColor, false, "Analyzing document...")
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
                        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(fileName, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1)
                                    Text("${selectedPages.size} / $pageCount PAGES SELECTED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("PREVIEW", fontSize = 9.sp, fontWeight = FontWeight.Black, color = if (showPreviewOverlay) accentColor else Color.Gray)
                                    Switch(checked = showPreviewOverlay, onCheckedChange = { showPreviewOverlay = it }, colors = SwitchDefaults.colors(checkedThumbColor = accentColor))
                                }
                            }

                            // Format Options Row
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
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
                                    Column(Modifier.padding(12.dp)) {
                                        Text("POSITION", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                                        Text(position, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    onClick = { 
                                        format = when(format) {
                                            "Page {n}" -> "{n}"
                                            "{n}" -> "- {n} -"
                                            "- {n} -" -> "P. {n}"
                                            else -> "Page {n}"
                                        }
                                    }
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text("FORMAT", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                                        Text(format.replace("{n}", "1"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Box(modifier = Modifier.weight(1f)) {
                                UnifiedPdfPreview(
                                    uri = selectedUri!!,
                                    pageCount = pageCount,
                                    mode = PreviewMode.GRID,
                                    password = null, 
                                    accentColor = accentColor,
                                    selectedPages = selectedPages,
                                    onToggleSelection = { index ->
                                        selectedPages = if (selectedPages.contains(index)) selectedPages - index else selectedPages + index
                                    },
                                    itemOverlay = { index ->
                                        if (showPreviewOverlay && selectedPages.contains(index)) {
                                            val text = format.replace("{n}", (index + 1).toString())
                                            Box(
                                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                                contentAlignment = when {
                                                    position.contains("Left") -> Alignment.TopStart
                                                    position.contains("Center") -> Alignment.TopCenter
                                                    else -> Alignment.TopEnd
                                                }
                                            ) {
                                                // Adjust alignment for bottom positions
                                                val finalAlign = when {
                                                    position.contains("Bottom") && position.contains("Left") -> Alignment.BottomStart
                                                    position.contains("Bottom") && position.contains("Center") -> Alignment.BottomCenter
                                                    position.contains("Bottom") && position.contains("Right") -> Alignment.BottomEnd
                                                    position.contains("Top") && position.contains("Left") -> Alignment.TopStart
                                                    position.contains("Top") && position.contains("Center") -> Alignment.TopCenter
                                                    position.contains("Top") && position.contains("Right") -> Alignment.TopEnd
                                                    else -> Alignment.BottomEnd
                                                }
                                                
                                                Box(Modifier.fillMaxSize(), contentAlignment = finalAlign) {
                                                    Surface(
                                                        color = Color.White,
                                                        shape = RoundedCornerShape(2.dp),
                                                        border = BorderStroke(0.5.dp, Color.Black.copy(0.2f)),
                                                        modifier = Modifier.padding(2.dp)
                                                    ) {
                                                        Text(
                                                            text = text,
                                                            color = Color.Black,
                                                            fontSize = 7.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                                
                                // REMOVED the old floating label overlay
                            }
                            
                            Button(
                                onClick = { saveLauncher.launch(fileName.replace(".pdf", "") + "-numbered.pdf") },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).height(60.dp),
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
        }
    }
}
