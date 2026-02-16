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

@Composable
fun PdfToImageView(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var pageCount by remember { mutableStateOf(0) }
    var isProcessing by remember { mutableStateOf(false) }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedUri = it
            try {
                context.contentResolver.openFileDescriptor(it, "r")?.use { pfd ->
                    val renderer = PdfRenderer(pfd)
                    pageCount = renderer.pageCount
                    renderer.close()
                }
            } catch (e: Exception) { /* Error */ }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let { saveUri ->
            isProcessing = true
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(saveUri)?.use { outputStream ->
                        ZipOutputStream(outputStream).use { zipOut ->
                            context.contentResolver.openFileDescriptor(selectedUri!!, "r")?.use { pfd ->
                                val renderer = PdfRenderer(pfd)
                                for (i in 0 until renderer.pageCount) {
                                    val page = renderer.openPage(i)
                                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    val entry = ZipEntry("page_${i + 1}.jpg")
                                    zipOut.putNextEntry(entry)
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, zipOut)
                                    zipOut.closeEntry()
                                    page.close(); bitmap.recycle()
                                }
                                renderer.close()
                            }
                        }
                    }
                    withContext(Dispatchers.Main) { Toast.makeText(context, "ZIP Exported!", Toast.LENGTH_LONG).show(); onBack() }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
                } finally { isProcessing = false }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Text("PDF to Image ZIP", style = MaterialTheme.typography.titleLarge)
        }

        Column(Modifier.weight(1f).padding(24.dp)) {
            if (selectedUri == null) {
                Box(Modifier.fillMaxSize().clickable { pickLauncher.launch("application/pdf") }, Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PictureAsPdf, null, Modifier.size(48.dp), MaterialTheme.colorScheme.primary)
                        Text("Select PDF to Export", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Text("Export $pageCount pages as JPEG images inside a ZIP.", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(24.dp))
                Button(onClick = { saveLauncher.launch("images.zip") }, modifier = Modifier.fillMaxWidth(), enabled = !isProcessing) {
                    if (isProcessing) CircularProgressIndicator(Modifier.size(24.dp))
                    else Text("Export ZIP")
                }
            }
        }
    }
}
