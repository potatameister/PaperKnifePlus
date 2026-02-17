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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
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
fun RearrangeView(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val accentColor = Color(0xFFF43F5E)

    var currentState by remember { mutableStateOf<ToolState>(ToolState.SELECTING) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var unlockPassword by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf("") }
    var pageOrder by remember { mutableStateOf<List<Int>>(emptyList()) }
    var thumbnails by remember { mutableStateOf<Map<Int, Bitmap>>(emptyMap()) }
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
                    try {
                        context.contentResolver.openFileDescriptor(it, "r")?.use { pfd ->
                            val renderer = PdfRenderer(pfd)
                            val count = renderer.pageCount
                            val thumbs = mutableMapOf<Int, Bitmap>()
                            for (i in 0 until minOf(count, 50)) {
                                val page = renderer.openPage(i)
                                val bitmap = Bitmap.createBitmap(page.width/4, page.height/4, Bitmap.Config.ARGB_8888)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                thumbs[i] = bitmap
                                page.close()
                            }
                            withContext(Dispatchers.Main) {
                                pageOrder = (0 until count).toList()
                                thumbnails = thumbs
                                currentState = ToolState.CONFIGURING
                                isFileLoading = false
                            }
                            renderer.close()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            isFileLoading = false
                        }
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
                    context.contentResolver.openInputStream(selectedUri!!)?.use { inputStream ->
                        val document = if (unlockPassword.isNotEmpty()) PDDocument.load(inputStream, unlockPassword) else PDDocument.load(inputStream)
                        val newDocument = PDDocument()
                        pageOrder.forEach { index -> newDocument.addPage(document.getPage(index)) }
                        context.contentResolver.openOutputStream(saveUri)?.use { outputStream -> 
                            newDocument.save(outputStream)
                            outputStream.flush()
                        }
                        newDocument.close()
                        document.close()
                    }
                    val endTime = System.currentTimeMillis()
                    val timeStr = String.format("%.1fs", (endTime - startTime) / 1000.0)
                    
                    withContext(Dispatchers.Main) {
                        val finalName = saveUri.lastPathSegment?.substringAfterLast("/") ?: fileName
                        savedFilePath = "Local Storage / $finalName"
                        processingTime = timeStr
                        SessionManager.addEntry(finalName, "Rearrange", "${pageOrder.size} pages", Icons.Default.ViewQuilt)
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
                        Text("Rearrange", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                        Text("REORDER DOCUMENT PAGES", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
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
                            icon = Icons.Default.ViewQuilt,
                            title = "Tap to enter file",
                            subtitle = "REARRANGE ANY PDF DOCUMENT",
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
                                    try {
                                        context.contentResolver.openFileDescriptor(selectedUri!!, "r")?.use { pfd ->
                                            val renderer = PdfRenderer(pfd)
                                            val count = renderer.pageCount
                                            val thumbs = mutableMapOf<Int, Bitmap>()
                                            for (i in 0 until minOf(count, 50)) {
                                                val page = renderer.openPage(i)
                                                val bitmap = Bitmap.createBitmap(page.width/4, page.height/4, Bitmap.Config.ARGB_8888)
                                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                                thumbs[i] = bitmap
                                                page.close()
                                            }
                                            withContext(Dispatchers.Main) {
                                                pageOrder = (0 until count).toList()
                                                thumbnails = thumbs
                                                currentState = ToolState.CONFIGURING
                                                isFileLoading = false
                                            }
                                            renderer.close()
                                        }
                                    } catch (e: Exception) {
                                        // If native renderer fails, we might still be able to rearrange with PDFBox
                                        // but we can't show thumbnails easily without rendering.
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
                        Column(modifier = Modifier.fillMaxSize()) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF09090B) else Color.White),
                                border = BorderStroke(1.dp, Color.Gray.copy(0.1f))
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(Modifier.size(40.dp), shape = RoundedCornerShape(10.dp), color = accentColor.copy(alpha = 0.1f)) {
                                        Icon(Icons.Default.PictureAsPdf, null, tint = accentColor, modifier = Modifier.padding(10.dp))
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = fileName, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                                        Text(text = "${pageOrder.size} Pages", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    }
                                    IconButton(onClick = { selectedUri = null; currentState = ToolState.SELECTING }) { 
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(20.dp)) 
                                    }
                                }
                            }

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(pageOrder.size) { i ->
                                    val index = pageOrder[i]
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(0.75f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5))
                                    ) {
                                        thumbnails[index]?.let { 
                                            Image(
                                                bitmap = it.asImageBitmap(), 
                                                contentDescription = null, 
                                                modifier = Modifier.fillMaxSize(), 
                                                contentScale = ContentScale.Crop 
                                            ) 
                                        }
                                        
                                        Row(
                                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            IconButton(
                                                onClick = { if (i > 0) { val list = pageOrder.toMutableList(); val temp = list[i]; list[i] = list[i-1]; list[i-1] = temp; pageOrder = list } },
                                                modifier = Modifier.size(28.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(14.dp), tint = Color.White) }
                                            
                                            IconButton(
                                                onClick = { if (i < pageOrder.size - 1) { val list = pageOrder.toMutableList(); val temp = list[i]; list[i] = list[i+1]; list[i+1] = temp; pageOrder = list } },
                                                modifier = Modifier.size(28.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                            ) { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(14.dp), tint = Color.White) }
                                        }
                                        
                                        Surface(
                                            modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                                            color = Color.Black.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "${index + 1}", 
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), 
                                                color = Color.White, 
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = { 
                                    val defaultName = fileName.replace(".pdf", "", true) + "-rearranged.pdf"
                                    saveLauncher.launch(defaultName) 
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp)
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(text = "Save New Order", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                            }
                        }
                    }
                    ToolState.PROCESSING -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = accentColor)
                                Spacer(Modifier.height(16.dp))
                                Text("Saving order...", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    ToolState.SUCCESS -> {
                        SuccessView(
                            fileName = fileName,
                            path = savedFilePath,
                            processingTime = processingTime,
                            onDone = onBack,
                            onProcessMore = { 
                                selectedUri = null
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
