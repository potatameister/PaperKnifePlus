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
                            val contentStream = PDPageContentStream(document, page)
                            contentStream.drawImage(pdImage, 0f, 0f)
                            contentStream.close()
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

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back") }
            Text(text = "Image to PDF", style = MaterialTheme.typography.titleLarge)
        }

        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            if (selectedUris.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text = "No images selected.") }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(selectedUris) { uri ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Image(painter = rememberAsyncImagePainter(model = uri), contentDescription = null, modifier = Modifier.size(60.dp), contentScale = ContentScale.Crop)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(text = "Image", modifier = Modifier.weight(1f))
                                IconButton(onClick = { selectedUris = selectedUris - uri }) { Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove") }
                            }
                        }
                    }
                }
                Button(onClick = { saveLauncher.launch("images.pdf") }, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), enabled = !isProcessing) {
                    if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    else Text(text = "Create PDF")
                }
            }
        }
        
        FloatingActionButton(onClick = { pickLauncher.launch("image/*") }, modifier = Modifier.align(Alignment.End).padding(32.dp)) { Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = "Add") }
    }
}
