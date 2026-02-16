package com.paperknifeplus.app.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.tomroush.pdfbox.pdmodel.PDDocument
import com.tomroush.pdfbox.pdmodel.PDPage
import com.tomroush.pdfbox.pdmodel.PDPageContentStream
import com.tomroush.pdfbox.pdmodel.common.PDRectangle
import com.tomroush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tomroush.pdfbox.pdmodel.graphics.image.PDImageXObject
import android.graphics.BitmapFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToPdfView(onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }

    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris -> selectedUris = selectedUris + uris }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { saveUri ->
            isProcessing = true
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
                    }
                }
                context.contentResolver.openOutputStream(saveUri)?.use { outputStream ->
                    document.save(outputStream)
                }
                document.close()
                Toast.makeText(context, "PDF Created!", Toast.LENGTH_LONG).show()
                onBack()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Images to PDF") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { pickLauncher.launch("image/*") }) {
                Icon(Icons.Default.AddPhotoAlternate, "Add Image")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            if (selectedUris.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No images selected. Tap + to add.")
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(selectedUris) { uri ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = rememberAsyncImagePainter(uri),
                                    contentDescription = null,
                                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(16.dp))
                                Text("Image ${selectedUris.indexOf(uri) + 1}", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { selectedUris = selectedUris - uri }) {
                                    Icon(Icons.Default.Delete, "Remove")
                                }
                            }
                        }
                    }
                }
                Button(
                    onClick = { saveLauncher.launch("images.pdf") },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    enabled = selectedUris.isNotEmpty() && !isProcessing
                ) {
                    if (isProcessing) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
                    else Text("Create PDF")
                }
            }
        }
    }
}
