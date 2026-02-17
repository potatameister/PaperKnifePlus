package com.paperknifeplus.app.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
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
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MergeView(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    
    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -> 
        selectedUris = selectedUris + uris 
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { saveUri ->
            isProcessing = true
            scope.launch(Dispatchers.IO) {
                try {
                    val merger = PDFMergerUtility()
                    context.contentResolver.openOutputStream(saveUri)?.use { outputStream ->
                        selectedUris.forEach { pdfUri ->
                            context.contentResolver.openInputStream(pdfUri)?.use { inputStream ->
                                merger.addSource(inputStream)
                            }
                        }
                        merger.destinationStream = outputStream
                        merger.mergeDocuments(null)
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Merged successfully!", Toast.LENGTH_LONG).show()
                        onBack()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Merge PDFs", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                    Text("COMBINE MULTIPLE DOCUMENTS", fontSize = 8.sp, fontWeight = FontWeight.Black, color = PaperPink, letterSpacing = 1.sp)
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { pickLauncher.launch("application/pdf") },
                containerColor = PaperPink,
                contentColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) { Icon(Icons.Default.Add, "Add") }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            if (selectedUris.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Layers, null, modifier = Modifier.size(64.dp).alpha(0.1f))
                        Spacer(Modifier.height(16.dp))
                        Text("No PDFs selected.", fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text("TAP THE + BUTTON TO START", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray.copy(alpha = 0.5f), letterSpacing = 1.sp)
                    }
                }
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(selectedUris) { uri ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF09090B) else Color.White),
                            border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f))
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(Modifier.size(36.dp), shape = RoundedCornerShape(8.dp), color = PaperPink.copy(alpha = 0.1f)) {
                                    Icon(Icons.Default.PictureAsPdf, null, tint = PaperPink, modifier = Modifier.padding(8.dp))
                                }
                                Spacer(Modifier.width(16.dp))
                                Text(uri.lastPathSegment ?: "PDF Document", Modifier.weight(1f), maxLines = 1, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                IconButton(onClick = { selectedUris = selectedUris - uri }) { 
                                    Icon(Icons.Default.DeleteOutline, null, tint = Color.Gray, modifier = Modifier.size(20.dp)) 
                                }
                            }
                        }
                    }
                }
                
                Button(
                    onClick = { saveLauncher.launch("merged.pdf") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .height(56.dp),
                    enabled = selectedUris.size > 1 && !isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = PaperPink),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    if (isProcessing) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
                    else Text("Merge ${selectedUris.size} Files", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                }
            }
        }
    }
}
