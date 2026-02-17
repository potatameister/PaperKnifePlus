package com.paperknifeplus.app.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperknifeplus.app.ui.theme.PaperPink
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToTextView(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineManager()
    val clipboardManager = LocalClipboardManager.current
    
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var extractedText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedUri = uri
        uri?.let {
            isProcessing = true
            scope.launch {
                val text = extractTextFromPdf(context, it)
                extractedText = text
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF to Text", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (extractedText.isNotEmpty()) {
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(extractedText))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Outlined.ContentCopy, "Copy")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            if (selectedUri == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clickable { launcher.launch("application/pdf") },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp).alpha(0.1f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("TAP TO SELECT PDF", fontWeight = FontWeight.Bold, color = Color.Gray)
                    }
                }
            } else {
                if (isProcessing) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PaperPink)
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            "EXTRACTED CONTENT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Gray,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(
                                    if (MaterialTheme.colorScheme.background == Color.Black) Color(0xFF09090B) else Color(0xFFF8F9FA),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp)
                        ) {
                            val scrollState = rememberScrollState()
                            Text(
                                text = extractedText.ifEmpty { "No text found in PDF." },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                modifier = Modifier.verticalScroll(scrollState)
                            )
                        }
                        
                        Button(
                            onClick = { selectedUri = null; extractedText = "" },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PaperPink)
                        ) {
                            Text("SELECT ANOTHER", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

private suspend fun extractTextFromPdf(context: android.content.Context, uri: Uri): String = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            PDDocument.load(inputStream).use { document ->
                val stripper = PDFTextStripper()
                stripper.sortByPosition = true
                stripper.getText(document)
            }
        } ?: "Error opening file"
    } catch (e: Exception) {
        e.printStackTrace()
        "Error extracting text: ${e.localizedMessage}"
    }
}

@Composable
fun rememberCoroutineManager() = rememberCoroutineScope()
