package com.paperknifeplus.app.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun UnlockView(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val accentColor = Color(0xFF6366F1)

    var currentState by remember { mutableStateOf<ToolState>(ToolState.SELECTING) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var password by remember { mutableStateOf("") }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var savedFilePath by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedUri = it
            fileName = it.lastPathSegment?.substringAfterLast("/") ?: "Document.pdf"
            if (!fileName.endsWith(".pdf", true)) fileName += ".pdf"
            
            scope.launch(Dispatchers.IO) {
                val isEncrypted = checkIsEncryptedLocal(context, it)
                if (isEncrypted) {
                    currentState = ToolState.UNLOCKING
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "File is not encrypted", Toast.LENGTH_SHORT).show()
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
                        val document = PDDocument.load(inputStream, password)
                        document.isAllSecurityToBeRemoved = true
                        context.contentResolver.openOutputStream(saveUri)?.use { outputStream -> 
                            document.save(outputStream)
                            outputStream.flush()
                        }
                        document.close()
                    }
                    withContext(Dispatchers.Main) {
                        val finalName = saveUri.lastPathSegment?.substringAfterLast("/") ?: fileName
                        savedFilePath = "Local Storage / $finalName"
                        SessionManager.addEntry(finalName, "Unlock", "Decrypted", Icons.Outlined.LockOpen)
                        currentState = ToolState.SUCCESS
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        currentState = ToolState.UNLOCKING
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
                        Text("Unlock", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                        Text("REMOVE PDF RESTRICTIONS", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            when (currentState) {
                ToolState.SELECTING -> {
                    SelectionGrid(
                        onSelect = { pickLauncher.launch("application/pdf") }, 
                        isDark = isDark,
                        icon = Icons.Outlined.LockOpen,
                        title = "Tap to select locked file",
                        subtitle = "REMOVE PASSWORD PROTECTION",
                        accentColor = accentColor,
                        modifier = Modifier.weight(1f)
                    )
                }
                ToolState.UNLOCKING -> {
                    LockedFilePrompt(
                        fileName = fileName,
                        password = password,
                        onPasswordChange = { password = it },
                        onUnlock = {
                            scope.launch(Dispatchers.IO) {
                                val bitmap = loadPreview(context, selectedUri!!, password)
                                if (bitmap != null) {
                                    previewBitmap = bitmap
                                    withContext(Dispatchers.Main) { currentState = ToolState.CONFIGURING }
                                } else {
                                    val isValid = verifyPasswordLocal(context, selectedUri!!, password)
                                    if (isValid) {
                                        withContext(Dispatchers.Main) { currentState = ToolState.CONFIGURING }
                                    } else {
                                        withContext(Dispatchers.Main) { Toast.makeText(context, "Invalid Password", Toast.LENGTH_SHORT).show() }
                                    }
                                }
                            }
                        },
                        onCancel = { selectedUri = null; currentState = ToolState.SELECTING },
                        accentColor = accentColor
                    )
                }
                ToolState.CONFIGURING -> {
                    Column(Modifier.fillMaxSize()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().height(240.dp),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, Color.Gray.copy(0.1f))
                        ) {
                            if (previewBitmap != null) {
                                Image(bitmap = previewBitmap!!.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            } else {
                                Box(Modifier.fillMaxSize().background(Color.Gray.copy(0.1f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.LockOpen, null, modifier = Modifier.size(48.dp).alpha(0.2f))
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Text("File Unlocked", fontWeight = FontWeight.Black, fontSize = 20.sp)
                        Text("Ready to save without restrictions.", color = Color.Gray, fontSize = 14.sp)
                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = { saveLauncher.launch(fileName.replace(".pdf", "_unlocked.pdf")) }, 
                            modifier = Modifier.fillMaxWidth().height(60.dp), 
                            shape = RoundedCornerShape(20.dp), 
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text("SAVE UNRESTRICTED PDF", fontWeight = FontWeight.Black, color = Color.White)
                        }
                        TextButton(onClick = { selectedUri = null; currentState = ToolState.SELECTING }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("CHANGE FILE", color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
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
                            password = ""
                            previewBitmap = null
                            currentState = ToolState.SELECTING 
                        },
                        accentColor = accentColor
                    )
                }
            }
        }
    }
}

private suspend fun verifyPasswordLocal(context: android.content.Context, uri: Uri, password: String): Boolean = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            PDDocument.load(inputStream, password).use { doc -> !doc.isEncrypted || true }
        } ?: false
    } catch (e: Exception) {
        false
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

private suspend fun loadPreview(context: android.content.Context, uri: Uri, password: String?): Bitmap? = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            val renderer = android.graphics.pdf.PdfRenderer(pfd)
            val page = renderer.openPage(0)
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            bitmap
        }
    } catch (e: Exception) {
        null
    }
}
