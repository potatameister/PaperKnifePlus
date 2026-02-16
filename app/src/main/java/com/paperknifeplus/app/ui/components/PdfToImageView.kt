package com.paperknifeplus.app.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToImageView(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var pageCount by remember { mutableStateOf(0) }
    var isProcessing by remember { mutableStateOf(false) }

    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedUri = it
            try {
                context.contentResolver.openFileDescriptor(it, "r")?.use { pfd ->
                    val renderer = PdfRenderer(pfd)
                    pageCount = renderer.pageCount
                    renderer.close()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error opening PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { saveUri ->
        saveUri?.let { uri ->
            isProcessing = true
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        ZipOutputStream(outputStream).use { zipOut ->
                            context.contentResolver.openFileDescriptor(selectedUri!!, "r")?.use { pfd ->
                                val renderer = PdfRenderer(pfd)
                                for (i in 0 until renderer.pageCount) {
                                    val page = renderer.openPage(i)
                                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    
                                    val zipEntry = ZipEntry("page_${i + 1}.jpg")
                                    zipOut.putNextEntry(zipEntry)
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, zipOut)
                                    zipOut.closeEntry()
                                    page.close()
                                    bitmap.recycle()
                                }
                                renderer.close()
                            }
                        }
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Images Exported!", Toast.LENGTH_LONG).show()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF to Images") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            if (selectedUri == null) {
                Box(
                    modifier = Modifier.fillMaxSize().clickable { pickLauncher.launch("application/pdf") },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PictureAsPdf, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Select PDF to Convert", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Text("Ready to export $pageCount pages as images.", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { saveLauncher.launch("images.zip") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing
                ) {
                    if (isProcessing) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
                    else Text("Export as ZIP")
                }
            }
        }
    }
}
