package com.paperknifeplus.app.ui.components

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
import androidx.compose.material.icons.outlined.FolderZip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.util.zip.ZipOutputStream

@Composable
fun PdfToZipView(
    onBack: () -> Unit,
    onOpenPreview: (Uri, String, Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val accentColor = Color(0xFF14B8A6)

    var currentState by remember { mutableStateOf<ToolState>(ToolState.SELECTING) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf("") }
    var pageCount by remember { mutableIntStateOf(0) }
    var processingTime by remember { mutableStateOf("") }
    var progressCount by remember { mutableIntStateOf(0) }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedUri = it
            val details = getUriDetails(context, it)
            fileName = details.name
            fileSize = details.size
            scope.launch {
                pageCount = getPageCount(context, it, null)
                currentState = ToolState.CONFIGURING
            }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let { saveUri ->
            currentState = ToolState.PROCESSING
            val startTime = System.currentTimeMillis()
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(saveUri)?.use { os ->
                        ZipOutputStream(os).use { zipOut ->
                            context.contentResolver.openInputStream(selectedUri!!)?.use { inputStream ->
                                val document = PDDocument.load(inputStream)
                                val total = document.numberOfPages
                                for (i in 0 until total) {
                                    withContext(Dispatchers.Main) { progressCount = i + 1 }
                                    val singlePageDoc = PDDocument()
                                    singlePageDoc.addPage(document.getPage(i))
                                    
                                    val entryName = fileName.replace(".pdf", "", true) + "_page_${i + 1}.pdf"
                                    zipOut.putNextEntry(ZipEntry(entryName))
                                    singlePageDoc.save(zipOut)
                                    zipOut.closeEntry()
                                    singlePageDoc.close()
                                }
                                document.close()
                            }
                        }
                    }
                    val endTime = System.currentTimeMillis()
                    val timeStr = String.format("%.1fs", (endTime - startTime) / 1000.0)
                    withContext(Dispatchers.Main) {
                        processingTime = timeStr
                        SessionManager.addEntry(fileName, "PDF to Pages ZIP", "$pageCount pages archived", Icons.Outlined.FolderZip)
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
                        Text("PDF to ZIP", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                        Text("PAGES TO ARCHIVE", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
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
                        icon = Icons.Outlined.FolderZip,
                        title = "Tap to enter file",
                        subtitle = "CREATE PAGES ARCHIVE",
                        accentColor = accentColor,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                ToolState.CONFIGURING -> {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        Spacer(Modifier.height(16.dp))
                        
                        UnifiedPdfPreview(
                            uri = selectedUri!!,
                            pageCount = pageCount,
                            mode = PreviewMode.COVER,
                            accentColor = accentColor
                        )
                        
                        Spacer(Modifier.height(32.dp))
                        Text("ARCHIVE INFO", fontSize = 10.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.5.sp)
                        Text("This tool will split '$fileName' into $pageCount separate PDF files (one for each page) and bundle them into a single ZIP archive for easier distribution.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                        
                        Spacer(Modifier.weight(1f))
                        
                        Button(
                            onClick = { saveLauncher.launch(fileName.replace(".pdf", "", true) + "-pages.zip") }, 
                            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).height(60.dp), 
                            shape = RoundedCornerShape(20.dp), 
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text("CONVERT & SAVE ZIP", fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }
                ToolState.PROCESSING -> {
                    ProcessingStateView(
                        accentColor = accentColor,
                        uri = selectedUri,
                        text = "Archiving page $progressCount of $pageCount...",
                        current = progressCount,
                        total = pageCount,
                        showWarning = false
                    )
                }
                ToolState.SUCCESS -> {
                    SuccessView(
                        message = "Archive Created",
                        subMessage = "$pageCount individual pages saved to ZIP",
                        processingTime = processingTime,
                        onDone = onBack,
                        onProcessMore = { selectedUri = null; currentState = ToolState.SELECTING },
                        showPreviewButton = false,
                        accentColor = accentColor
                    )
                }
                else -> {}
            }
        }
    }
}
