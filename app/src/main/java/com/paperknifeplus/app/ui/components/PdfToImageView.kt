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
import androidx.compose.material.icons.outlined.BurstMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Composable
fun PdfToImageView(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val accentColor = Color(0xFF14B8A6)

    var currentState by remember { mutableStateOf<ToolState>(ToolState.SELECTING) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var unlockPassword by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf("") }
    var pageCount by remember { mutableStateOf(0) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isFileLoading by remember { mutableStateOf(false) }
    var processingTime by remember { mutableStateOf("") }
    var savedFilePath by remember { mutableStateOf("") }

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
                        currentState = ToolState.UNLOCKING
                        isFileLoading = false
                    }
                } else {
                    val bitmap = loadPreview(context, it, null)
                    try {
                        context.contentResolver.openFileDescriptor(it, "r")?.use { pfd ->
                            val renderer = PdfRenderer(pfd)
                            pageCount = renderer.pageCount
                            renderer.close()
                        }
                    } catch (e: Exception) { }
                    
                    withContext(Dispatchers.Main) {
                        previewBitmap = bitmap
                        currentState = ToolState.CONFIGURING
                        isFileLoading = false
                    }
                }
            }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let { saveUri ->
            currentState = ToolState.PROCESSING
            val startTime = System.currentTimeMillis()
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(saveUri)?.use { outputStream ->
                        ZipOutputStream(outputStream).use { zipOut ->
                            // Use PDFBox to load even if password is required
                            context.contentResolver.openInputStream(selectedUri!!)?.use { inputStream ->
                                val document = if (unlockPassword.isNotEmpty()) PDDocument.load(inputStream, unlockPassword) else PDDocument.load(inputStream)
                                
                                // We need a way to render pages to images. 
                                // PDFBox-Android's PDFRenderer is available.
                                val renderer = com.tom_roush.pdfbox.rendering.PDFRenderer(document)
                                for (i in 0 until document.numberOfPages) {
                                    val bitmap = renderer.renderImage(i, 2.0f) // 2x scale for quality
                                    val entry = ZipEntry("page_${i + 1}.jpg")
                                    zipOut.putNextEntry(entry)
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, zipOut)
                                    zipOut.closeEntry()
                                    bitmap.recycle()
                                }
                                document.close()
                            }
                            zipOut.flush()
                        }
                        outputStream.flush()
                    }
                    val endTime = System.currentTimeMillis()
                    val timeStr = String.format("%.1fs", (endTime - startTime) / 1000.0)
                    
                    withContext(Dispatchers.Main) {
                        val finalName = saveUri.lastPathSegment?.substringAfterLast("/") ?: fileName.replace(".pdf", ".zip")
                        savedFilePath = "Local Storage / $finalName"
                        processingTime = timeStr
                        SessionManager.addEntry(finalName, "PDF to Image", "$pageCount images", Icons.Outlined.BurstMode)
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
                        Text("PDF to Image", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                        Text("CONVERT PAGES TO JPEG ZIP", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            if (isFileLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentColor)
                }
            } else {
                when (currentState) {
                    ToolState.SELECTING -> {
                        SelectionGrid(
                            onSelect = { pickLauncher.launch("application/pdf") }, 
                            isDark = isDark,
                            icon = Icons.Outlined.BurstMode,
                            title = "Tap to enter file",
                            subtitle = "CONVERT PDF TO IMAGE ARCHIVE",
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
                                    val bitmap = loadPreview(context, selectedUri!!, unlockPassword)
                                    if (bitmap != null) {
                                        previewBitmap = bitmap
                                        withContext(Dispatchers.Main) { 
                                            currentState = ToolState.CONFIGURING
                                            isFileLoading = false 
                                        }
                                    } else {
                                        val isValid = verifyPasswordLocal(context, selectedUri!!, unlockPassword)
                                        if (isValid) {
                                            withContext(Dispatchers.Main) { 
                                                currentState = ToolState.CONFIGURING
                                                isFileLoading = false 
                                            }
                                        } else {
                                            withContext(Dispatchers.Main) { 
                                                Toast.makeText(context, "Invalid Password", Toast.LENGTH_SHORT).show()
                                                isFileLoading = false 
                                            }
                                        }
                                    }
                                }
                            },
                            onCancel = { selectedUri = null; currentState = ToolState.SELECTING },
                            accentColor = accentColor
                        )
                    }
                    ToolState.CONFIGURING -> {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            Spacer(Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth().height(240.dp),
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, Color.Gray.copy(0.1f))
                            ) {
                                if (previewBitmap != null) {
                                    Image(bitmap = previewBitmap!!.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                } else {
                                    Box(Modifier.fillMaxSize().background(Color.Gray.copy(0.1f)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Outlined.BurstMode, null, modifier = Modifier.size(48.dp).alpha(0.2f))
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(fileName, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                            Text(fileSize, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally))
                            
                            Spacer(Modifier.height(32.dp))
                            Text("READY TO EXPORT", fontSize = 10.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.5.sp)
                            Text("This will convert each page of the PDF into a high-quality JPEG and package them into a ZIP file.", fontSize = 12.sp, color = Color.Gray)
                            
                            Spacer(Modifier.height(32.dp))
                            
                            Button(
                                onClick = { 
                                    val defaultName = fileName.replace(".pdf", "", true) + "-images.zip"
                                    saveLauncher.launch(defaultName) 
                                }, 
                                modifier = Modifier.fillMaxWidth().height(60.dp), 
                                shape = RoundedCornerShape(20.dp), 
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Text("EXPORT AS IMAGES (ZIP)", fontWeight = FontWeight.Black, color = Color.White)
                            }
                            TextButton(onClick = { selectedUri = null; currentState = ToolState.SELECTING }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                Text("CHANGE FILE", color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(100.dp))
                        }
                    }
                    ToolState.PROCESSING -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = accentColor)
                                Spacer(Modifier.height(16.dp))
                                Text("Generating images...", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    ToolState.SUCCESS -> {
                        SuccessView(
                            fileName = fileName.replace(".pdf", ".zip"),
                            path = savedFilePath,
                            processingTime = processingTime,
                            onDone = onBack,
                            onProcessMore = { 
                                selectedUri = null
                                unlockPassword = ""
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
}
