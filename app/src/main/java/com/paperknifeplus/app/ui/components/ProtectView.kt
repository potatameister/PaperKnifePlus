package com.paperknifeplus.app.ui.components

import android.graphics.Bitmap
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
import androidx.compose.material.icons.outlined.*
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
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProtectView(
    initialUri: Uri? = null,
    onBack: () -> Unit,
    onOpenPreview: (Uri, String, Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val accentColor = Color(0xFF6366F1)

    var currentState by remember { mutableStateOf<ToolState>(ToolState.SELECTING) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var outputUri by remember { mutableStateOf<Uri?>(null) }
    var unlockPassword by remember { mutableStateOf("") }
    var protectPassword by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf("") }
    var pageCount by remember { mutableIntStateOf(0) }
    var isFileLoading by remember { mutableStateOf(false) }
    var processingTime by remember { mutableStateOf("") }
    var showLoadingWarning by remember { mutableStateOf(false) }
    var fileToUnlock by remember { mutableStateOf<String?>(null) }

    fun handleFileSelection(uri: Uri) {
        selectedUri = uri
        val details = getUriDetails(context, uri)
        fileName = details.name
        fileSize = details.size
        isFileLoading = true
        scope.launch(Dispatchers.IO) {
            val isEncrypted = checkIsEncryptedLocal(context, uri)
            if (isEncrypted) {
                withContext(Dispatchers.Main) {
                    fileToUnlock = fileName
                    isFileLoading = false
                }
            } else {
                val count = getPageCount(context, uri, null)
                withContext(Dispatchers.Main) {
                    pageCount = count
                    currentState = ToolState.CONFIGURING
                    isFileLoading = false
                }
            }
        }
    }

    LaunchedEffect(initialUri) {
        initialUri?.let { handleFileSelection(it) }
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
                    context.contentResolver.openInputStream(selectedUri!!)?.use { inputStream ->
                        val document = if (unlockPassword.isNotEmpty()) {
                            PDDocument.load(inputStream, unlockPassword)
                        } else {
                            PDDocument.load(inputStream)
                        }
                        
                        val ap = AccessPermission()
                        val spp = StandardProtectionPolicy(protectPassword, protectPassword, ap)
                        spp.encryptionKeyLength = 128
                        document.protect(spp)
                        
                        saveAndFlush(context, document, saveUri)
                    }
                    val endTime = System.currentTimeMillis()
                    val timeStr = String.format("%.1fs", (endTime - startTime) / 1000.0)
                    withContext(Dispatchers.Main) {
                        processingTime = timeStr
                        outputUri = saveUri
                        SessionManager.addEntry(fileName, "Protect", "Encrypted", Icons.Outlined.Lock, saveUri, pageCount)
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
                        Text("Protect", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                        Text("ENCRYPT YOUR DOCUMENT", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
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
                            icon = Icons.Outlined.Security,
                            title = "Tap to enter file",
                            subtitle = "PROTECT ANY PDF DOCUMENT",
                            accentColor = accentColor,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    ToolState.CONFIGURING -> {
                        ProtectConfiguringView(
                            uri = selectedUri!!,
                            pageCount = pageCount,
                            fileName = fileName,
                            fileSize = fileSize,
                            password = protectPassword,
                            unlockPassword = unlockPassword,
                            onPasswordChange = { protectPassword = it },
                            onProtect = { 
                                val defaultName = fileName.replace(".pdf", "", true) + "-protected.pdf"
                                saveLauncher.launch(defaultName) 
                            },
                            onChangeFile = { selectedUri = null; currentState = ToolState.SELECTING },
                            accentColor = accentColor
                        )
                    }
                    ToolState.PROCESSING -> {
                        ProcessingStateView(
                            accentColor = accentColor,
                            uri = selectedUri,
                            password = unlockPassword.ifEmpty { null },
                            text = "Applying encryption policy...",
                            current = 0,
                            total = 0,
                            showWarning = showLoadingWarning
                        )
                    }
                    ToolState.SUCCESS -> {
                        SuccessView(
                            message = "Protect Complete",
                            subMessage = "Document encrypted with password",
                            processingTime = processingTime,
                            onDone = onBack,
                            onProcessMore = { 
                                selectedUri = null
                                unlockPassword = ""
                                protectPassword = ""
                                currentState = ToolState.SELECTING 
                            },
                            onPreview = { outputUri?.let { uri -> onOpenPreview(uri, fileName, pageCount) } },
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
                            val count = getPageCount(context, selectedUri!!, pass)
                            if (count > 0) {
                                withContext(Dispatchers.Main) { 
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

@Composable
fun ProtectConfiguringView(
    uri: Uri,
    pageCount: Int,
    fileName: String,
    fileSize: String,
    password: String, 
    unlockPassword: String,
    onPasswordChange: (String) -> Unit, 
    onProtect: () -> Unit, 
    onChangeFile: () -> Unit, 
    accentColor: Color
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(16.dp))
        
        // UNIFIED PREVIEW: COVER MODE
        UnifiedPdfPreview(
            uri = uri,
            pageCount = pageCount,
            mode = PreviewMode.COVER,
            password = if (unlockPassword.isEmpty()) null else unlockPassword,
            accentColor = accentColor
        )
        
        Spacer(Modifier.height(16.dp))
        Text(fileName, fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Row(modifier = Modifier.align(Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            Text(fileSize, fontSize = 11.sp, color = Color.Gray)
            Spacer(Modifier.width(8.dp))
            Text("• $pageCount PAGES", fontSize = 11.sp, color = Color.Gray)
        }
        
        Spacer(Modifier.height(24.dp))
        Text("SET PROTECTION", fontSize = 10.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(12.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("New Password", fontWeight = FontWeight.Bold) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = accentColor,
                cursorColor = accentColor,
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent
            )
        )
        
        Spacer(Modifier.height(16.dp))
        Surface(color = Color(0xFFFFF1F2), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, null, tint = Color(0xFFF43F5E), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("PaperKnife+ cannot recover lost passwords. Ensure you keep it safe.", fontSize = 11.sp, color = Color(0xFF9F1239), fontWeight = FontWeight.Bold, lineHeight = 16.sp)
            }
        }
        
        Spacer(Modifier.height(32.dp))
        Button(onClick = onProtect, modifier = Modifier.fillMaxWidth().height(60.dp), enabled = password.isNotBlank(), shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = accentColor)) {
            Text("Protect & Save", fontWeight = FontWeight.Black, color = Color.White)
        }
        TextButton(onClick = onChangeFile, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("CHANGE FILE", color = Color.Gray, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(100.dp))
    }
}
