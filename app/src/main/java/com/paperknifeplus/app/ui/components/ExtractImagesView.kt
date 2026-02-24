package com.paperknifeplus.app.ui.components

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PhotoLibrary
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
import com.tom_roush.pdfbox.pdmodel.PDResources
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.pdmodel.graphics.form.PDFormXObject
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Composable
fun ExtractImagesView(
    onBack: () -> Unit,
    onOpenPreview: (Uri, String, Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val accentColor = Color(0xFF14B8A6)

    var currentState by remember { mutableStateOf<ToolState>(ToolState.SELECTING) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var unlockPassword by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf("") }
    var pageCount by remember { mutableIntStateOf(0) }
    var isFileLoading by remember { mutableStateOf(false) }
    var processingTime by remember { mutableStateOf("") }
    var showLoadingWarning by remember { mutableStateOf(false) }
    var fileToUnlock by remember { mutableStateOf<String?>(null) }
    var extractedCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(isFileLoading, currentState) {
        if (isFileLoading || currentState == ToolState.PROCESSING) {
            delay(5000)
            showLoadingWarning = true
        } else {
            showLoadingWarning = false
        }
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
                            context.contentResolver.openInputStream(selectedUri!!)?.use { inputStream ->
                                val document = if (unlockPassword.isNotEmpty()) PDDocument.load(inputStream, unlockPassword) else PDDocument.load(inputStream)
                                var imageCount = 0
                                val processedObjects = mutableSetOf<String>()

                                fun extractFromResources(resources: PDResources?, pIdx: Int) {
                                    if (resources == null) return
                                    for (name in resources.xObjectNames) {
                                        try {
                                            val xobject = resources.getXObject(name)
                                            val cosObj = xobject.cosObject.toString()
                                            if (cosObj in processedObjects) continue
                                            processedObjects.add(cosObj)

                                            if (xobject is PDImageXObject) {
                                                imageCount++
                                                scope.launch(Dispatchers.Main) { extractedCount = imageCount }
                                                
                                                val suffix = xobject.suffix ?: "jpg"
                                                val entry = ZipEntry("asset_${imageCount}_p${pIdx + 1}.$suffix")
                                                zipOut.putNextEntry(entry)
                                                
                                                try {
                                                    // Try Bitmap conversion for standard viewers compatibility
                                                    val bitmap = xobject.image
                                                    if (bitmap != null) {
                                                        val format = if (suffix.lowercase() == "png") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                                                        bitmap.compress(format, 90, zipOut)
                                                        bitmap.recycle()
                                                    } else {
                                                        throw Exception("Bitmap null")
                                                    }
                                                } catch (e: Exception) {
                                                    // FALLBACK: Raw stream dump for JPX or complex encodings
                                                    // Use createRawInputStream to get actual data bytes
                                                    xobject.cosObject.createRawInputStream().use { imageStream ->
                                                        imageStream.copyTo(zipOut)
                                                    }
                                                }
                                                
                                                zipOut.closeEntry()
                                            } else if (xobject is PDFormXObject) {
                                                extractFromResources(xobject.resources, pIdx)
                                            }
                                        } catch (e: Exception) { }
                                    }
                                }

                                document.pages.forEachIndexed { pIdx, page ->
                                    extractFromResources(page.resources, pIdx)
                                }
                                
                                document.close()
                                if (imageCount == 0) throw Exception("No images found in PDF")
                            }
                            zipOut.finish()
                            zipOut.flush()
                        }
                        outputStream.flush()
                    }
                    val endTime = System.currentTimeMillis()
                    val timeStr = String.format("%.1fs", (endTime - startTime) / 1000.0)
                    withContext(Dispatchers.Main) {
                        processingTime = timeStr
                        SessionManager.addEntry(fileName, "Extract Image", "$extractedCount assets", Icons.Filled.Collections)
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
                            Text("Extract Image", fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Text("EXTRACT ASSETS FROM PDFS", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
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
                LoadingStateView(accentColor, showLoadingWarning, "Preparing document...")
            } else {
                when (currentState) {
                    ToolState.SELECTING -> {
                        SelectionGrid(
                            onSelect = { pickLauncher.launch("application/pdf") }, 
                            isDark = isDark,
                            icon = Icons.Outlined.PhotoLibrary,
                            title = "Tap to enter file",
                            subtitle = "EXTRACT ASSETS FROM PDF",
                            accentColor = accentColor,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    ToolState.CONFIGURING -> {
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(Modifier.height(16.dp))
                            
                            Box(modifier = Modifier.fillMaxWidth(0.6f).aspectRatio(0.707f)) {
                                UnifiedPdfPreview(
                                    uri = selectedUri!!,
                                    pageCount = pageCount,
                                    mode = PreviewMode.COVER,
                                    password = unlockPassword.ifEmpty { null },
                                    accentColor = accentColor
                                )
                            }
                            
                            Spacer(Modifier.height(12.dp))
                            Text(fileName, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                            Text("$fileSize • $pageCount PAGES", fontSize = 10.sp, color = Color.Gray)
                            
                            Spacer(Modifier.height(24.dp))
                            Text("READY TO STRIP ASSETS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.5.sp)
                            Text("Strip and save all raw image files from the document.", fontSize = 12.sp, color = Color.Gray)
                            
                            Spacer(Modifier.height(32.dp))
                            
                            Button(
                                onClick = { 
                                    val safeName = fileName.replace(Regex("[^a-zA-Z0-9.-]"), "_").replace(".pdf", "", true) + "-assets.zip"
                                    saveLauncher.launch(safeName) 
                                }, 
                                modifier = Modifier.fillMaxWidth().height(60.dp), 
                                shape = RoundedCornerShape(20.dp), 
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Text("EXTRACT IMAGES (ZIP)", fontWeight = FontWeight.Black, color = Color.White)
                            }
                            Spacer(Modifier.height(100.dp))
                        }
                    }
                    ToolState.PROCESSING -> {
                        ProcessingStateView(
                            accentColor = accentColor,
                            uri = selectedUri,
                            password = unlockPassword.ifEmpty { null },
                            text = "Stripping image assets...",
                            current = extractedCount,
                            total = 0, 
                            showWarning = showLoadingWarning
                        )
                    }
                    ToolState.SUCCESS -> {
                        SuccessView(
                            message = "Extraction Complete",
                            subMessage = "Found $extractedCount assets in document",
                            processingTime = processingTime,
                            onDone = onBack,
                            onProcessMore = { 
                                selectedUri = null
                                unlockPassword = ""
                                extractedCount = 0
                                currentState = ToolState.SELECTING 
                            },
                            showPreviewButton = false,
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
