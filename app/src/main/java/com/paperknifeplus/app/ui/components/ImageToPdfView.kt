package com.paperknifeplus.app.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.tomroush.pdfbox.pdmodel.PDDocument
import com.tomroush.pdfbox.pdmodel.PDPage
import com.tomroush.pdfbox.pdmodel.PDPageContentStream
import com.tomroush.pdfbox.pdmodel.common.PDRectangle
import com.tomroush.pdfbox.pdmodel.graphics.image.LosslessFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ImageToPdfView(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -> selectedUris = selectedUris + uris }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { saveUri ->
            isProcessing = true
            scope.launch(Dispatchers.IO) {
                try {
                    val document = PDDocument()
                    selectedUris.forEach { imgUri ->
                        context.contentResolver.openInputStream(imgUri)?.use { inputStream ->
                            val bitmap = BitmapFactory.decodeStream(inputStream)
                            val pdImage = LosslessFactory.createFromImage(document, bitmap)
                            val page = PDPage(PDRectangle(pdImage.width.toFloat(), pdImage.height.toFloat()))
                            document.addPage(page)
                            PDPageContentStream(document, page).use { contentStream ->
                                contentStream.drawImage(pdImage, 0f, 0f)
                            }
                            bitmap.recycle()
                        }
                    }
                    context.contentResolver.openOutputStream(saveUri)?.use { outputStream -> document.save(outputStream) }
                    document.close()
                    withContext(Dispatchers.Main) { Toast.makeText(context, "PDF Created!", Toast.LENGTH_LONG).show(); onBack() }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
                } finally { isProcessing = false }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Text("Image to PDF", style = MaterialTheme.typography.titleLarge)
        }

        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            if (selectedUris.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No images selected.") }
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(selectedUris) { uri ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Image(rememberAsyncImagePainter(uri), null, Modifier.size(60.dp), contentScale = ContentScale.Crop)
                                Spacer(Modifier.width(16.dp))
                                Text("Image", Modifier.weight(1f))
                                IconButton(onClick = { selectedUris = selectedUris - uri }) { Icon(Icons.Default.Delete, null) }
                            }
                        }
                    }
                }
                Button(onClick = { saveLauncher.launch("images.pdf") }, Modifier.fillMaxWidth().padding(vertical = 16.dp), enabled = !isProcessing) {
                    if (isProcessing) CircularProgressIndicator(Modifier.size(24.dp))
                    else Text("Create PDF")
                }
            }
        }
        
        FloatingActionButton(onClick = { pickLauncher.launch("image/*") }, Modifier.align(Alignment.End).padding(32.dp)) { Icon(Icons.Default.AddPhotoAlternate, "Add") }
    }
}
