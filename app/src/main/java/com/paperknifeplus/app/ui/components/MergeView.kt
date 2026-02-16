@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
import com.tomroush.pdfbox.multipdf.PDFMergerUtility
import com.tomroush.pdfbox.util.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MergeView(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    
    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        selectedUris = selectedUris + uris
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { saveUri ->
        saveUri?.let { uri ->
            isProcessing = true
            scope.launch(Dispatchers.IO) {
                try {
                    val merger = PDFMergerUtility()
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
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

    LaunchedEffect(Unit) {
        PDFBoxResourceLoader.init(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Merge PDFs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { pickLauncher.launch("application/pdf") }) {
                Icon(Icons.Default.Add, contentDescription = "Add PDF")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (selectedUris.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No PDFs selected. Tap + to add.")
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(selectedUris) { uri ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = uri.lastPathSegment ?: "PDF File",
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { selectedUris = selectedUris - uri }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { saveLauncher.launch("merged_document.pdf") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    enabled = selectedUris.size > 1 && !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Merge ${selectedUris.size} Files & Save")
                    }
                }
            }
        }
    }
}
