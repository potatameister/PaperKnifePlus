package com.paperknifeplus.app.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MergeView(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    
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

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                Text("Merge PDFs", style = MaterialTheme.typography.titleLarge)
            }
            
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                if (selectedUris.isEmpty()) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No PDFs selected.") }
                } else {
                    LazyColumn(Modifier.weight(1f)) {
                        items(selectedUris) { uri ->
                            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(uri.lastPathSegment ?: "PDF", Modifier.weight(1f), maxLines = 1)
                                    IconButton(onClick = { selectedUris = selectedUris - uri }) { Icon(Icons.Default.Delete, null) }
                                }
                            }
                        }
                    }
                    Button(
                        onClick = { saveLauncher.launch("merged.pdf") },
                        Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        enabled = selectedUris.size > 1 && !isProcessing
                    ) {
                        if (isProcessing) CircularProgressIndicator(Modifier.size(24.dp))
                        else Text("Merge ${selectedUris.size} Files")
                    }
                }
            }
        }
        
        FloatingActionButton(
            onClick = { pickLauncher.launch("application/pdf") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(32.dp)
        ) { Icon(Icons.Default.Add, "Add") }
    }
}
