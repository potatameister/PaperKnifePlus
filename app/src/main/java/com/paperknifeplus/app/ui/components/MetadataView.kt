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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Fingerprint
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
fun MetadataView(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.background == Color.Black

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedUri = uri
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(it)?.use { inputStream ->
                        val document = PDDocument.load(inputStream)
                        val info = document.documentInformation
                        withContext(Dispatchers.Main) {
                            title = info.title ?: ""
                            author = info.author ?: ""
                            subject = info.subject ?: ""
                        }
                        document.close()
                    }
                } catch (e: Exception) { }
            }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { saveUri ->
            isProcessing = true
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(selectedUri!!)?.use { inputStream ->
                        val document = PDDocument.load(inputStream)
                        val info = document.documentInformation
                        info.title = title
                        info.author = author
                        info.subject = subject
                        context.contentResolver.openOutputStream(saveUri)?.use { outputStream -> document.save(outputStream) }
                        document.close()
                    }
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Metadata Updated!", Toast.LENGTH_LONG).show(); onBack() }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
                } finally { isProcessing = false }
            }
        }
    }

    LaunchedEffect(Unit) { PDFBoxResourceLoader.init(context) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), CircleShape)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Metadata", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                    Text("EDIT DOCUMENT PROPERTIES", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF6366F1), letterSpacing = 1.sp)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
            if (selectedUri == null) {
                Box(modifier = Modifier.height(400.dp).fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(if (isDark) Color(0xFF09090B) else Color.White).border(BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(0.03f)), RoundedCornerShape(32.dp)).clickable { pickLauncher.launch("application/pdf") }, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Outlined.Fingerprint, contentDescription = null, modifier = Modifier.size(64.dp).alpha(0.1f))
                        Spacer(Modifier.height(16.dp))
                        Text("Select PDF to Edit", fontWeight = FontWeight.Black, color = Color.Gray)
                    }
                }
            } else {
                SettingsGroup("PROPERTIES") {
                    MetadataField("Title", title) { title = it }
                    MetadataField("Author", author) { author = it }
                    MetadataField("Subject", subject) { subject = it }
                }
                Spacer(Modifier.height(24.dp))
                Button(onClick = { saveLauncher.launch("updated_metadata.pdf") }, modifier = Modifier.fillMaxWidth().height(56.dp), enabled = !isProcessing, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)), shape = RoundedCornerShape(20.dp)) {
                    if (isProcessing) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
                    else Text("Save Changes", fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { selectedUri = null }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(20.dp)) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MetadataField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 1.sp)
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent
            ),
            singleLine = true
        )
    }
}
