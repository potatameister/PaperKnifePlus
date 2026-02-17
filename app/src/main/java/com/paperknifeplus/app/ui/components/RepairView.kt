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
import androidx.compose.material.icons.outlined.Build
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
fun RepairView(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background == Color.Black

    var currentState by remember { mutableStateOf(ToolState.SELECTING) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var savedFilePath by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedUri = it
            fileName = it.lastPathSegment ?: "Document.pdf"
            currentState = ToolState.CONFIGURING
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { saveUri ->
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
                        savedFilePath = saveUri.path ?: "Local Storage"
                        SessionManager.addEntry(fileName, "Repair", "Repaired", Icons.Outlined.Build)
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
                        Text("Repair PDF", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                        Text("FIX STRUCTURAL ISSUES", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFFF59E0B), letterSpacing = 1.sp)
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
                        icon = Icons.Outlined.Build,
                        title = "Tap to enter file",
                        subtitle = "REPAIR ANY PDF DOCUMENT",
                        accentColor = Color(0xFFF59E0B)
                    )
                }
                ToolState.CONFIGURING -> {
                    Column(Modifier.fillMaxWidth().padding(top = 40.dp)) {
                        Text("Ready to Repair", fontWeight = FontWeight.Black, fontSize = 24.sp)
                        Text(fileName, color = Color.Gray, fontSize = 14.sp)
                        Spacer(Modifier.height(32.dp))
                        Button(onClick = { saveLauncher.launch(fileName.replace(".pdf", "_repaired.pdf")) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))) {
                            Text("Repair & Save", fontWeight = FontWeight.Black)
                        }
                        TextButton(onClick = { selectedUri = null; currentState = ToolState.SELECTING }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("CHANGE FILE", color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                ToolState.PROCESSING -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFF59E0B))
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
                        }
                    )
                }
                else -> {}
            }
        }
    }
}
