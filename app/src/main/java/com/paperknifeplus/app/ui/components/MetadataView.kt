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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
fun MetadataView(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val accentColor = Color(0xFF6366F1)

    var currentState by remember { mutableStateOf<ToolState>(ToolState.SELECTING) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var unlockPassword by remember { mutableStateOf("") }
    
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var keywords by remember { mutableStateOf("") }
    var creator by remember { mutableStateOf("") }
    var producer by remember { mutableStateOf("") }
    
    var savedFilePath by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedUri = it
            fileName = it.lastPathSegment ?: "Document.pdf"
            scope.launch(Dispatchers.IO) {
                val isEncrypted = checkIsEncryptedLocal(context, it)
                if (isEncrypted) {
                    currentState = ToolState.UNLOCKING
                } else {
                    loadMetadata(context, it, null) { t, a, s, k, c, p ->
                        title = t; author = a; subject = s; keywords = k; creator = c; producer = p
                        currentState = ToolState.CONFIGURING
                    }
                }
            }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { saveUri ->
            currentState = ToolState.PROCESSING
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(selectedUri!!)?.use { inputStream ->
                        val document = if (unlockPassword.isNotEmpty()) PDDocument.load(inputStream, unlockPassword) else PDDocument.load(inputStream)
                        val info = document.documentInformation
                        info.title = title
                        info.author = author
                        info.subject = subject
                        info.keywords = keywords
                        info.creator = creator
                        info.producer = producer
                        
                        context.contentResolver.openOutputStream(saveUri)?.use { outputStream -> 
                            document.save(outputStream)
                            outputStream.flush()
                        }
                        document.close()
                    }
                    withContext(Dispatchers.Main) {
                        val finalName = saveUri.path?.substringAfterLast("/") ?: fileName
                        savedFilePath = "Local Storage / $finalName"
                        SessionManager.addEntry(finalName, "Metadata", "Edited", Icons.Outlined.Fingerprint)
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
            if (currentState != ToolState.SUCCESS) {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), CircleShape)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Metadata", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                        Text("EDIT DOCUMENT PROPERTIES", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            when (currentState) {
                ToolState.SELECTING -> {
                    SelectionGrid(
                        onSelect = { pickLauncher.launch("application/pdf") }, 
                        isDark = isDark,
                        icon = Icons.Outlined.Fingerprint,
                        title = "Tap to enter file",
                        subtitle = "EDIT PDF PROPERTIES",
                        accentColor = accentColor,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                ToolState.UNLOCKING -> {
                    LockedFilePrompt(
                        fileName = fileName,
                        password = unlockPassword,
                        onPasswordChange = { unlockPassword = it },
                        onUnlock = {
                            scope.launch(Dispatchers.IO) {
                                loadMetadata(context, selectedUri!!, unlockPassword) { t, a, s, k, c, p ->
                                    title = t; author = a; subject = s; keywords = k; creator = c; producer = p
                                    currentState = ToolState.CONFIGURING
                                }
                            }
                        },
                        onCancel = { selectedUri = null; currentState = ToolState.SELECTING },
                        accentColor = accentColor
                    )
                }
                ToolState.CONFIGURING -> {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        SettingsGroup("DOCUMENT CORE") {
                            MetadataEditField("Title", title, accentColor) { title = it }
                            MetadataEditField("Author", author, accentColor) { author = it }
                            MetadataEditField("Subject", subject, accentColor) { subject = it }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        SettingsGroup("ADDITIONAL INFO") {
                            MetadataEditField("Keywords", keywords, accentColor) { keywords = it }
                            MetadataEditField("Creator", creator, accentColor) { creator = it }
                            MetadataEditField("Producer", producer, accentColor) { producer = it }
                        }
                        
                        Spacer(Modifier.height(32.dp))
                        
                        Button(
                            onClick = { saveLauncher.launch(fileName.replace(".pdf", "_meta.pdf")) }, 
                            modifier = Modifier.fillMaxWidth().height(60.dp), 
                            shape = RoundedCornerShape(20.dp), 
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text("Save Metadata", fontWeight = FontWeight.Black, color = Color.White)
                        }
                        
                        TextButton(onClick = { selectedUri = null; currentState = ToolState.SELECTING }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("CHANGE FILE", color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(100.dp))
                    }
                }
                ToolState.PROCESSING -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accentColor)
                    }
                }
                ToolState.SUCCESS -> {
                    SuccessView(
                        fileName = fileName,
                        path = savedFilePath,
                        onDone = onBack,
                        onProcessMore = { 
                            selectedUri = null
                            unlockPassword = ""
                            currentState = ToolState.SELECTING 
                        },
                        accentColor = accentColor
                    )
                }
            }
        }
    }
}

@Composable
fun MetadataEditField(label: String, value: String, accentColor: Color, onValueChange: (String) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 1.sp)
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                focusedIndicatorColor = accentColor,
                cursorColor = accentColor
            ),
            singleLine = true
        )
    }
}

private suspend fun loadMetadata(
    context: android.content.Context, 
    uri: Uri, 
    password: String?, 
    onSuccess: suspend (String, String, String, String, String, String) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val document = if (password != null) PDDocument.load(inputStream, password) else PDDocument.load(inputStream)
            val info = document.documentInformation
            val t = info.title ?: ""
            val a = info.author ?: ""
            val s = info.subject ?: ""
            val k = info.keywords ?: ""
            val c = info.creator ?: ""
            val p = info.producer ?: ""
            document.close()
            onSuccess(t, a, s, k, c, p)
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Incorrect password or error", Toast.LENGTH_SHORT).show()
        }
    }
}

private suspend fun checkIsEncryptedLocal(context: android.content.Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val doc = PDDocument.load(inputStream)
            val isEnc = doc.isEncrypted
            doc.close()
            isEnc
        } ?: false
    } catch (e: com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException) {
        true
    } catch (e: Exception) {
        if (e.message?.contains("encrypted", ignoreCase = true) == true) true else false
    }
}
