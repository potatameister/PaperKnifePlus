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
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProtectView(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background == Color.Black

    var currentState by remember { mutableStateOf<ToolState>(ToolState.SELECTING) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var unlockPassword by remember { mutableStateOf("") }
    var protectPassword by remember { mutableStateOf("") }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var savedFilePath by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedUri = it
            fileName = it.lastPathSegment ?: "Document.pdf"
            scope.launch(Dispatchers.IO) {
                val isEncrypted = checkIsEncrypted(context, it)
                if (isEncrypted) {
                    currentState = ToolState.UNLOCKING
                } else {
                    val bitmap = loadPreview(context, it, null)
                    withContext(Dispatchers.Main) {
                        previewBitmap = bitmap
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
                        val document = if (unlockPassword.isNotEmpty()) {
                            PDDocument.load(inputStream, unlockPassword)
                        } else {
                            PDDocument.load(inputStream)
                        }
                        
                        val ap = AccessPermission()
                        val spp = StandardProtectionPolicy(protectPassword, protectPassword, ap)
                        spp.encryptionKeyLength = 128
                        document.protect(spp)
                        
                        context.contentResolver.openOutputStream(saveUri)?.use { outputStream -> 
                            document.save(outputStream)
                            outputStream.flush()
                        }
                        document.close()
                    }
                    withContext(Dispatchers.Main) {
                        savedFilePath = saveUri.path ?: "Local Storage"
                        SessionManager.addEntry(fileName, "Protect", "Encrypted", Icons.Outlined.Lock)
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
                        Text("Protect", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                        Text("ENCRYPT YOUR DOCUMENT", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF6366F1), letterSpacing = 1.sp)
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
                        icon = Icons.Outlined.Security,
                        title = "Tap to enter file",
                        subtitle = "PROTECT ANY PDF DOCUMENT",
                        accentColor = Color(0xFF6366F1),
                        modifier = Modifier.weight(1f)
                    )
                }
                ToolState.UNLOCKING -> {
                    UnlockPrompt(
                        fileName = fileName,
                        password = unlockPassword,
                        onPasswordChange = { unlockPassword = it },
                        onUnlock = {
                            scope.launch(Dispatchers.IO) {
                                val bitmap = loadPreview(context, selectedUri!!, unlockPassword)
                                if (bitmap != null) {
                                    previewBitmap = bitmap
                                    withContext(Dispatchers.Main) { currentState = ToolState.CONFIGURING }
                                } else {
                                    withContext(Dispatchers.Main) { Toast.makeText(context, "Invalid Password", Toast.LENGTH_SHORT).show() }
                                }
                            }
                        },
                        onCancel = { selectedUri = null; currentState = ToolState.SELECTING }
                    )
                }
                ToolState.CONFIGURING -> {
                    ProtectConfiguringView(
                        preview = previewBitmap,
                        password = protectPassword,
                        onPasswordChange = { protectPassword = it },
                        onProtect = { saveLauncher.launch(fileName.replace(".pdf", "_protected.pdf")) },
                        onChangeFile = { selectedUri = null; currentState = ToolState.SELECTING }
                    )
                }
                ToolState.PROCESSING -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF6366F1))
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
                            protectPassword = ""
                            previewBitmap = null
                            currentState = ToolState.SELECTING 
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProtectConfiguringView(preview: Bitmap?, password: String, onPasswordChange: (String) -> Unit, onProtect: () -> Unit, onChangeFile: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth().height(240.dp),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.Gray.copy(0.1f))
        ) {
            if (preview != null) {
                Image(bitmap = preview.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Box(Modifier.fillMaxSize().background(Color.Gray.copy(0.1f)), contentAlignment = Alignment.Center) {
                    Text("No Preview Available", color = Color.Gray)
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        Text("SET PROTECTION", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF6366F1), letterSpacing = 1.5.sp)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("New Password", fontWeight = FontWeight.Bold) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(focusedIndicatorColor = Color(0xFF6366F1), cursorColor = Color(0xFF6366F1))
        )
        
        Spacer(Modifier.height(16.dp))
        Surface(color = Color(0xFFFFF1F2), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = Color(0xFFF43F5E), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("PaperKnife+ cannot recover lost passwords. Ensure you keep it safe.", fontSize = 11.sp, color = Color(0xFF9F1239), fontWeight = FontWeight.Bold, lineHeight = 16.sp)
            }
        }
        
        Spacer(Modifier.height(32.dp))
        Button(onClick = onProtect, modifier = Modifier.fillMaxWidth().height(60.dp), enabled = password.isNotBlank(), shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))) {
            Text("Protect & Save", fontWeight = FontWeight.Black)
        }
        TextButton(onClick = onChangeFile, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("CHANGE FILE", color = Color.Gray, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(100.dp))
    }
}

private suspend fun checkIsEncrypted(context: android.content.Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            PDDocument.load(inputStream).use { doc -> doc.isEncrypted }
        } ?: false
    } catch (e: com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException) {
        true
    } catch (e: Exception) {
        false
    }
}

private suspend fun loadPreview(context: android.content.Context, uri: Uri, password: String?): Bitmap? = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            val renderer = PdfRenderer(pfd)
            val page = renderer.openPage(0)
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            bitmap
        }
    } catch (e: Exception) {
        null
    }
}
