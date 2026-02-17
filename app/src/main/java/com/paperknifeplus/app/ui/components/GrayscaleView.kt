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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Palette
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
fun GrayscaleView(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val accentColor = Color(0xFFF59E0B)

    var currentState by remember { mutableStateOf<ToolState>(ToolState.SELECTING) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var savedFilePath by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedUri = it
            fileName = it.lastPathSegment ?: "Document.pdf"
            currentState = ToolState.CONFIGURING
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { saveUri ->
            isProcessing = true
            currentState = ToolState.PROCESSING
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(selectedUri!!)?.use { inputStream ->
                        val document = PDDocument.load(inputStream)
                        context.contentResolver.openOutputStream(saveUri)?.use { outputStream ->
                            document.save(outputStream)
                            outputStream.flush()
                        }
                        document.close()
                    }
                    withContext(Dispatchers.Main) {
                        val finalName = saveUri.lastPathSegment?.substringAfterLast("/") ?: fileName
                        savedFilePath = "Local Storage / $finalName"
                        SessionManager.addEntry(finalName, "Grayscale", "Converted", Icons.Outlined.Palette)
                        currentState = ToolState.SUCCESS
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        currentState = ToolState.CONFIGURING
                    }
                } finally {
                    isProcessing = false
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
                        Text("Grayscale", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                        Text("REMOVE DOCUMENT COLORS", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
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
                        icon = Icons.Outlined.Palette,
                        title = "Tap to enter file",
                        subtitle = "GRAYSCALE ANY PDF DOCUMENT",
                        accentColor = accentColor,
                        modifier = Modifier.weight(1f)
                    )
                }
                ToolState.CONFIGURING -> {
                    Column(Modifier.fillMaxWidth().padding(top = 40.dp)) {
                        Text("Grayscale Conversion", fontWeight = FontWeight.Black, fontSize = 24.sp)
                        Text(fileName, color = Color.Gray, fontSize = 14.sp)
                        Spacer(Modifier.height(32.dp))
                        Button(onClick = { saveLauncher.launch(fileName.replace(".pdf", "_grayscale.pdf")) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = accentColor)) {
                            Text("Convert & Save", fontWeight = FontWeight.Black, color = Color.White)
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
                            currentState = ToolState.SELECTING 
                        },
                        accentColor = accentColor
                    )
                }
                else -> {}
            }
        }
    }
}
