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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperknifeplus.app.ui.theme.PaperPink
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RearrangeView(
    initialUri: Uri? = null,
    initialPassword: String? = null,
    onBack: () -> Unit,
    onOpenPreview: (Uri, String, Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val accentColor = Color(0xFFF43F5E)

    var currentState by remember { mutableStateOf<ToolState>(ToolState.SELECTING) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var outputUri by remember { mutableStateOf<Uri?>(null) }
    var unlockPassword by remember { mutableStateOf(initialPassword ?: "") }
    var fileName by remember { mutableStateOf("") }
    var pageOrder by remember { mutableStateOf<List<Int>>(emptyList()) }
    var isFileLoading by remember { mutableStateOf(false) }
    var processingTime by remember { mutableStateOf("") }
    var fileToUnlock by remember { mutableStateOf<String?>(null) }
    
    // NEW REARRANGE STATE
    var selectedPageIndex by remember { mutableStateOf<Int?>(null) }
    var showMoveToDialog by remember { mutableStateOf(false) }
    var moveToInput by remember { mutableStateOf("") }

    fun handleFileSelection(uri: Uri, password: String? = null) {
        selectedUri = uri
        val details = getUriDetails(context, uri)
        fileName = details.name
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
                    pageOrder = (0 until count).toList()
                    currentState = ToolState.CONFIGURING
                    isFileLoading = false
                }
            }
        }
    }

    LaunchedEffect(initialUri) {
        initialUri?.let { handleFileSelection(it, initialPassword) }
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
                        val newDocument = PDDocument()
                        pageOrder.forEach { index -> newDocument.addPage(document.getPage(index)) }
                        saveAndFlush(context, newDocument, saveUri)
                        document.close()
                    }
                    val endTime = System.currentTimeMillis()
                    withContext(Dispatchers.Main) {
                        processingTime = String.format("%.1fs", (endTime - startTime) / 1000.0)
                        outputUri = saveUri
                        fileName = getUriDetails(context, saveUri).name
                        SessionManager.addEntry(fileName, "Rearrange", "Reordered pages", Icons.Filled.GridView, saveUri, pageOrder.size)
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
                            Text("Rearrange", fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Text("MOVE PAGES TO NEW SLOTS", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
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
                LoadingStateView(accentColor, false, "Preparing Rearrange Grid...")
            } else {
                when (currentState) {
                    ToolState.SELECTING -> {
                        SelectionGrid(
                            onSelect = { pickLauncher.launch("application/pdf") }, 
                            isDark = isDark,
                            icon = Icons.Filled.SwapVert,
                            title = "Tap to enter file",
                            subtitle = "REORDER PAGES RELIABLY",
                            accentColor = accentColor,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    ToolState.CONFIGURING -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(fileName, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1)
                                    Text("${pageOrder.size} PAGES • TAP TO SELECT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                }
                                IconButton(onClick = { selectedUri = null; currentState = ToolState.SELECTING; selectedPageIndex = null }) { 
                                    Icon(Icons.Filled.Close, "Clear", tint = Color.Gray, modifier = Modifier.size(20.dp)) 
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                UnifiedPdfPreview(
                                    uri = selectedUri!!,
                                    pageCount = pageOrder.size,
                                    mode = PreviewMode.REORDER,
                                    password = null,
                                    accentColor = accentColor,
                                    pageOrder = pageOrder,
                                    selectedPages = selectedPageIndex?.let { setOf(it) } ?: emptySet(),
                                    onToggleSelection = { index -> 
                                        selectedPageIndex = if (selectedPageIndex == index) null else index 
                                    }
                                )
                            }

                            // CONTROLS BAR
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            selectedPageIndex?.let { current ->
                                                if (current > 0) {
                                                    val newList = pageOrder.toMutableList()
                                                    val item = newList.removeAt(current)
                                                    newList.add(current - 1, item)
                                                    pageOrder = newList
                                                    selectedPageIndex = current - 1
                                                }
                                            }
                                        },
                                        enabled = selectedPageIndex != null && selectedPageIndex!! > 0
                                    ) {
                                        Icon(Icons.Filled.ArrowBackIos, "Move Back", tint = if (selectedPageIndex != null && selectedPageIndex!! > 0) accentColor else Color.Gray)
                                    }

                                    Button(
                                        onClick = { showMoveToDialog = true },
                                        enabled = selectedPageIndex != null,
                                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("MOVE TO...", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    }

                                    IconButton(
                                        onClick = {
                                            selectedPageIndex?.let { current ->
                                                if (current < pageOrder.size - 1) {
                                                    val newList = pageOrder.toMutableList()
                                                    val item = newList.removeAt(current)
                                                    newList.add(current + 1, item)
                                                    pageOrder = newList
                                                    selectedPageIndex = current + 1
                                                }
                                            }
                                        },
                                        enabled = selectedPageIndex != null && selectedPageIndex!! < pageOrder.size - 1
                                    ) {
                                        Icon(Icons.Filled.ArrowForwardIos, "Move Forward", tint = if (selectedPageIndex != null && selectedPageIndex!! < pageOrder.size - 1) accentColor else Color.Gray)
                                    }
                                }
                            }

                            Button(
                                onClick = { saveLauncher.launch(fileName.replace(".pdf", "") + "-reordered.pdf") },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).height(60.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text(text = "SAVE NEW ORDER", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                            }
                        }
                    }
                    ToolState.PROCESSING -> {
                        ProcessingStateView(
                            accentColor = accentColor,
                            uri = selectedUri,
                            text = "Applying new order...",
                            current = 0,
                            total = 0,
                            showWarning = false
                        )
                    }
                    ToolState.SUCCESS -> {
                        SuccessView(
                            message = "Rearrange Complete",
                            subMessage = "Page order updated successfully",
                            processingTime = processingTime,
                            onDone = onBack,
                            onProcessMore = { 
                                selectedUri = null
                                selectedPageIndex = null
                                currentState = ToolState.SELECTING 
                            },
                            onPreview = {
                                outputUri?.let { uri -> onOpenPreview(uri, fileName, pageOrder.size) }
                            },
                            accentColor = accentColor
                        )
                    }
                    else -> {}
                }
            }

            if (showMoveToDialog) {
                AlertDialog(
                    onDismissRequest = { showMoveToDialog = false },
                    title = { Text("Move Page to Position", fontWeight = FontWeight.Black) },
                    text = {
                        Column {
                            Text("Current position: ${selectedPageIndex!! + 1}", fontSize = 12.sp, color = Color.Gray)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = moveToInput,
                                onValueChange = { if (it.all { c -> c.isDigit() }) moveToInput = it },
                                label = { Text("Target Page (1-${pageOrder.size})") },
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val target = moveToInput.toIntOrNull()?.let { it - 1 }
                            if (target != null && target in 0 until pageOrder.size && selectedPageIndex != null) {
                                val newList = pageOrder.toMutableList()
                                val item = newList.removeAt(selectedPageIndex!!)
                                newList.add(target, item)
                                pageOrder = newList
                                selectedPageIndex = target
                                showMoveToDialog = false
                                moveToInput = ""
                            } else {
                                Toast.makeText(context, "Invalid Page Number", Toast.LENGTH_SHORT).show()
                            }
                        }) { Text("MOVE", color = accentColor, fontWeight = FontWeight.Black) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showMoveToDialog = false; moveToInput = "" }) { Text("CANCEL", color = Color.Gray) }
                    }
                )
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
                                    pageOrder = (0 until count).toList()
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
