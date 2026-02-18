package com.paperknifeplus.app.ui.components

import android.graphics.Bitmap
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
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BurstMode
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
import kotlinx.coroutines.delay
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
    
    var pageCount by remember { mutableIntStateOf(0) }
    var selectedPages by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var format by remember { mutableStateOf("JPEG") }
    var quality by remember { mutableStateOf("Standard") }
    
    var thumbnails by remember { mutableStateOf<Map<Int, Bitmap>>(emptyMap()) }
    var progressCount by remember { mutableIntStateOf(0) }
    var processingTime by remember { mutableStateOf("") }
    var showLoadingWarning by remember { mutableStateOf(false) }

    LaunchedEffect(currentState) {
        if (currentState == ToolState.PROCESSING) {
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
            scope.launch(Dispatchers.IO) {
                val isEncrypted = checkIsEncryptedLocal(context, it)
                if (isEncrypted) {
                    withContext(Dispatchers.Main) { currentState = ToolState.UNLOCKING }
                } else {
                    val count = getPageCountLocal(context, it, null)
                    val thumbs = mutableMapOf<Int, Bitmap>()
                    for (i in 0 until minOf(count, 30)) {
                        val bitmap = renderPageToBitmap(context, it, i, null, 0.3f)
                        if (bitmap != null) thumbs[i] = bitmap
                    }
                    withContext(Dispatchers.Main) {
                        pageCount = count
                        selectedPages = (0 until count).toSet()
                        thumbnails = thumbs
                        currentState = ToolState.CONFIGURING
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
                    convertPdfToImages(context, selectedUri!!, saveUri, if (unlockPassword.isEmpty()) null else unlockPassword, selectedPages.toList().sorted(), format, quality) { current, total ->
                        progressCount = current
                    }
                    val endTime = System.currentTimeMillis()
                    val timeStr = String.format("%.1fs", (endTime - startTime) / 1000.0)
                    withContext(Dispatchers.Main) {
                        processingTime = timeStr
                        SessionManager.addEntry(fileName, "PDF to Image", "${selectedPages.size} images", Icons.Outlined.BurstMode)
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
                        Text("PDF to Image", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                        Text("EXTRACT PAGES AS IMAGES", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
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
                        icon = Icons.Outlined.BurstMode,
                        title = "Tap to select PDF",
                        subtitle = "CONVERT DOCUMENT TO IMAGES",
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
                            scope.launch(Dispatchers.IO) {
                                val count = getPageCountLocal(context, selectedUri!!, unlockPassword)
                                val thumbs = mutableMapOf<Int, Bitmap>()
                                for (i in 0 until minOf(count, 20)) {
                                    val bitmap = renderPageToBitmap(context, selectedUri!!, i, unlockPassword, 0.3f)
                                    if (bitmap != null) thumbs[i] = bitmap
                                }
                                if (count > 0) {
                                    withContext(Dispatchers.Main) {
                                        pageCount = count
                                        selectedPages = (0 until count).toSet()
                                        thumbnails = thumbs
                                        currentState = ToolState.CONFIGURING
                                    }
                                } else {
                                    withContext(Dispatchers.Main) { 
                                        Toast.makeText(context, "Invalid Password", Toast.LENGTH_SHORT).show()
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
                        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("SELECT PAGES & EXPORT SETTINGS", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 1.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilterChip(
                                    selected = format == "JPEG",
                                    onClick = { format = "JPEG" },
                                    label = { Text("JPG", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accentColor, selectedLabelColor = Color.White)
                                )
                                Spacer(Modifier.width(4.dp))
                                FilterChip(
                                    selected = quality == "HD",
                                    onClick = { quality = if (quality == "HD") "Standard" else "HD" },
                                    label = { Text("HD", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accentColor, selectedLabelColor = Color.White)
                                )
                            }
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(pageCount) { index ->
                                val isSelected = selectedPages.contains(index)
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(0.8f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5))
                                        .border(BorderStroke(2.dp, if (isSelected) accentColor else Color.Transparent), RoundedCornerShape(12.dp))
                                        .clickable { 
                                            selectedPages = if (isSelected) selectedPages - index else selectedPages + index 
                                        }
                                ) {
                                    thumbnails[index]?.let { Image(bitmap = it.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                                    if (isSelected) {
                                        Surface(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp), color = accentColor, shape = CircleShape) {
                                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp).padding(2.dp))
                                        }
                                    }
                                    Surface(modifier = Modifier.align(Alignment.BottomStart).padding(6.dp), color = Color.Black.copy(0.5f), shape = RoundedCornerShape(4.dp)) {
                                        Text("${index + 1}", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp))
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { saveLauncher.launch(fileName.replace(".pdf", ".zip")) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).height(56.dp),
                            enabled = selectedPages.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Convert ${selectedPages.size} Pages to $format", fontWeight = FontWeight.Black)
                        }
                    }
                }
                ToolState.PROCESSING -> {
                    ProcessingStateView(
                        accentColor = accentColor,
                        preview = thumbnails[0],
                        text = "Rendering page $progressCount of ${selectedPages.size}...",
                        current = progressCount,
                        total = selectedPages.size,
                        showWarning = showLoadingWarning
                    )
                }
                ToolState.SUCCESS -> {
                    SuccessView(
                        processingTime = processingTime,
                        onDone = onBack,
                        onProcessMore = { selectedUri = null; currentState = ToolState.SELECTING },
                        accentColor = accentColor
                    )
                }
                else -> {}
            }
        }
    }
}

private suspend fun getPageCountLocal(context: android.content.Context, uri: Uri, password: String?): Int = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val document = if (password != null) PDDocument.load(inputStream, password) else PDDocument.load(inputStream)
            val count = document.numberOfPages
            document.close()
            count
        } ?: 0
    } catch (e: Exception) { 0 }
}
