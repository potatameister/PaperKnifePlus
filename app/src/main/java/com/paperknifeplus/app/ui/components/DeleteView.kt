package com.paperknifeplus.app.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DeleteView(
    initialUri: Uri? = null,
    onBack: () -> Unit,
    onOpenPreview: (Uri, String, Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val accentColor = Color(0xFFF43F5E) // Consistent Rose accent

    var currentState by remember { mutableStateOf<ToolState>(ToolState.SELECTING) }
    var selectedUri by remember { mutableStateOf(initialUri) }
    var outputUri by remember { mutableStateOf<Uri?>(null) }
    var unlockPassword by remember { mutableStateOf("") }
    var rangeText by remember { mutableStateOf("") }
    var pagesToDeleteSet by remember { mutableStateOf<Set<Int>>(emptySet()) }
    
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf("") }
    var pageCount by remember { mutableIntStateOf(0) }
    var progressCount by remember { mutableIntStateOf(0) }
    var isFileLoading by remember { mutableStateOf(false) }
    var processingTime by remember { mutableStateOf("") }
    var showLoadingWarning by remember { mutableStateOf(false) }
    var fileToUnlock by remember { mutableStateOf<String?>(null) }
    var showRangeInput by remember { mutableStateOf(false) }

    LaunchedEffect(isFileLoading, currentState) {
        if (isFileLoading || currentState == ToolState.PROCESSING) {
            delay(5000)
            showLoadingWarning = true
        } else {
            showLoadingWarning = false
        }
    }

    // Range Parser
    fun parseRange(input: String, max: Int): Set<Int> {
        val pages = mutableSetOf<Int>()
        try {
            input.split(",").forEach { part ->
                if (part.contains("-")) {
                    val split = part.split("-")
                    val start = split[0].trim().toInt().coerceIn(1, max)
                    val end = split[1].trim().toInt().coerceIn(1, max)
                    for (i in start..end) pages.add(i - 1)
                } else {
                    val p = part.trim().toIntOrNull()
                    if (p != null && p in 1..max) pages.add(p - 1)
                }
            }
        } catch (e: Exception) {}
        return pages
    }

    // Set to Range String
    fun generateRangeString(pages: Set<Int>): String {
        if (pages.isEmpty()) return ""
        val sorted = pages.toList().sorted()
        val result = mutableListOf<String>()
        var start = sorted[0]
        var prev = start
        
        for (i in 1 until sorted.size) {
            if (sorted[i] == prev + 1) {
                prev = sorted[i]
            } else {
                if (start == prev) result.add("${start + 1}")
                else result.add("${start + 1}-${prev + 1}")
                start = sorted[i]
                prev = start
            }
        }
        if (start == prev) result.add("${start + 1}")
        else result.add("${start + 1}-${prev + 1}")
        
        return result.joinToString(", ")
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
                        pagesToDeleteSet = emptySet()
                        rangeText = ""
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
                                withContext(Dispatchers.Main) { progressCount = 0 }
                                context.contentResolver.openInputStream(selectedUri!!)?.use { inputStream ->
                                    val document = if (unlockPassword.isNotEmpty()) PDDocument.load(inputStream, unlockPassword) else PDDocument.load(inputStream)
                                    val newDocument = PDDocument()
                                    for (i in 0 until document.numberOfPages) {
                                        if (!pagesToDeleteSet.contains(i)) {
                                            newDocument.addPage(document.getPage(i))
                                        }
                                        if (pagesToDeleteSet.contains(i)) {
                                            withContext(Dispatchers.Main) { progressCount++ }
                                        }
                                    }
                                    saveAndFlush(context, newDocument, saveUri)
                                    document.close()
                                }
            
                    val endTime = System.currentTimeMillis()
                    val timeStr = String.format("%.1fs", (endTime - startTime) / 1000.0)
                    val finalCount = getPageCount(context, saveUri, null)
                    withContext(Dispatchers.Main) {
                        processingTime = timeStr
                        outputUri = saveUri
                        SessionManager.addEntry(fileName, "Delete", "${pagesToDeleteSet.size} pages removed", Icons.Filled.Delete, saveUri, finalCount)
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
            val isEncrypted = checkIsEncryptedLocal(context, initialUri)
            if (isEncrypted) {
                fileToUnlock = fileName
                isFileLoading = false
            } else {
                val count = getPageCount(context, initialUri, null)
                pageCount = count
                pagesToDeleteSet = emptySet()
                rangeText = ""
                isFileLoading = false
                currentState = ToolState.READY
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
                        Text("Delete", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                        Text("REMOVE PAGES FROM PDF", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isFileLoading) {
                LoadingStateView(accentColor, showLoadingWarning, "Preparing document...")
            } else {
                when (currentState) {
                    ToolState.SELECTING -> {
                        SelectionGrid(
                            onSelect = { pickLauncher.launch("application/pdf") }, 
                            isDark = isDark,
                            icon = Icons.Filled.Delete,
                            title = "Tap to enter file",
                            subtitle = "DELETE PAGES INSTANTLY",
                            accentColor = accentColor,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)
                        )
                    }
                    ToolState.CONFIGURING -> {
                        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                            // Header Info
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(fileName, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1)
                                    Text("${pagesToDeleteSet.size} / $pageCount MARKED FOR DELETION", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                }
                                
                                TextButton(onClick = { showRangeInput = !showRangeInput }) {
                                    Icon(if (showRangeInput) Icons.Filled.KeyboardArrowUp else Icons.Filled.Create, null, modifier = Modifier.size(16.dp), tint = accentColor)
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (showRangeInput) "HIDE RANGE" else "ENTER RANGE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = accentColor)
                                }
                            }

                            // Batch Selection Row (Hides when manual range input is open to save space)
                            AnimatedVisibility(visible = !showRangeInput) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { 
                                            val all = (0 until pageCount).toSet()
                                            pagesToDeleteSet = all
                                            rangeText = generateRangeString(all)
                                        },
                                        modifier = Modifier.weight(1f).height(36.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.1f), contentColor = accentColor),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("SELECT ALL", fontSize = 10.sp, fontWeight = FontWeight.Black)
                                    }
                                    Button(
                                        onClick = { 
                                            pagesToDeleteSet = emptySet()
                                            rangeText = ""
                                        },
                                        modifier = Modifier.weight(1f).height(36.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.1f), contentColor = Color.Gray),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("CLEAR ALL", fontSize = 10.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }

                            if (showRangeInput) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(Modifier.padding(16.dp)) {
                                        OutlinedTextField(
                                            value = rangeText,
                                            onValueChange = { 
                                                rangeText = it
                                                pagesToDeleteSet = parseRange(it, pageCount)
                                            },
                                            label = { Text("Example: 2, 4-6, 9") },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = accentColor,
                                                cursorColor = accentColor
                                            )
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text("Tip: Pages selected in grid will be removed.", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                UnifiedPdfPreview(
                                    uri = selectedUri!!,
                                    pageCount = pageCount,
                                    mode = PreviewMode.GRID,
                                    password = null, 
                                    accentColor = accentColor,
                                    selectedPages = pagesToDeleteSet,
                                    onToggleSelection = { index ->
                                        val newSet = if (pagesToDeleteSet.contains(index)) pagesToDeleteSet - index else pagesToDeleteSet + index
                                        pagesToDeleteSet = newSet
                                        rangeText = generateRangeString(newSet)
                                    }
                                )
                            }
                            
                            Button(
                                onClick = { 
                                    val defaultName = fileName.replace(".pdf", "", true) + "-cleaned.pdf"
                                    saveLauncher.launch(defaultName) 
                                }, 
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).height(60.dp), 
                                enabled = pagesToDeleteSet.isNotEmpty() && pagesToDeleteSet.size < pageCount,
                                shape = RoundedCornerShape(20.dp), 
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White)
                            ) {
                                Text("DELETE ${pagesToDeleteSet.size} PAGES", fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }
                    ToolState.PROCESSING -> {
                        ProcessingStateView(
                            accentColor = accentColor,
                            uri = selectedUri,
                            password = unlockPassword.ifEmpty { null },
                            text = "Removing selected pages...",
                            current = progressCount,
                            total = pagesToDeleteSet.size,
                            showWarning = showLoadingWarning
                        )
                    }
                    ToolState.SUCCESS -> {
                        SuccessView(
                            message = "Delete Complete",
                            subMessage = "Selected pages were removed",
                            processingTime = processingTime,
                            onDone = onBack,
                            onProcessMore = { 
                                selectedUri = null
                                outputUri = null
                                unlockPassword = ""
                                currentState = ToolState.SELECTING 
                            },
                            onPreview = {
                                outputUri?.let { uri ->
                                    scope.launch {
                                        val count = getPageCount(context, uri, null)
                                        onOpenPreview(uri, fileName, count)
                                    }
                                }
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
                        unlockPassword = pass
                        isFileLoading = true
                        scope.launch(Dispatchers.IO) {
                            val decryptedUri = decryptToCache(context, selectedUri!!, pass)
                            if (decryptedUri != null) {
                                val count = getPageCount(context, decryptedUri, null)
                                withContext(Dispatchers.Main) { 
                                    selectedUri = decryptedUri
                                    pageCount = count
                                    currentState = ToolState.CONFIGURING
                                    isFileLoading = false 
                                    fileToUnlock = null
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
