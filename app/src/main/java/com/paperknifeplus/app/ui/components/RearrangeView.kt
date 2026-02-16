package com.paperknifeplus.app.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomroush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RearrangeView(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var pageOrder by remember { mutableStateOf(listOf<Int>()) }
    var thumbnails by remember { mutableStateOf<Map<Int, Bitmap>>(emptyMap()) }
    var isProcessing by remember { mutableStateOf(false) }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedUri = it
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openFileDescriptor(it, "r")?.use { pfd ->
                        val renderer = PdfRenderer(pfd)
                        val count = renderer.pageCount
                        val order = (0 until count).toList()
                        val thumbs = mutableMapOf<Int, Bitmap>()
                        for (i in 0 until minOf(count, 20)) {
                            val page = renderer.openPage(i)
                            val bitmap = Bitmap.createBitmap(page.width/4, page.height/4, Bitmap.Config.ARGB_8888)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            thumbs[i] = bitmap
                            page.close()
                        }
                        withContext(Dispatchers.Main) { pageOrder = order; thumbnails = thumbs }
                        renderer.close()
                    }
                } catch (e: Exception) { /* Log error */ }
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
                        val newDocument = PDDocument()
                        pageOrder.forEach { oldIndex -> newDocument.addPage(document.getPage(oldIndex)) }
                        context.contentResolver.openOutputStream(saveUri)?.use { outputStream -> newDocument.save(outputStream) }
                        newDocument.close(); document.close()
                    }
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Rearranged!", Toast.LENGTH_LONG).show(); onBack() }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
                } finally { isProcessing = false }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back") }
            Text(text = "Rearrange PDF", style = MaterialTheme.typography.titleLarge)
        }

        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            if (selectedUri == null) {
                Box(modifier = Modifier.fillMaxSize().clickable { pickLauncher.launch("application/pdf") }, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.ViewQuilt, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(text = "Select PDF to Rearrange", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.weight(1f)) {
                    items(pageOrder.size) { index ->
                        val originalPageIndex = pageOrder[index]
                        Card(modifier = Modifier.padding(4.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                thumbnails[originalPageIndex]?.let { Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = Modifier.aspectRatio(0.7f), contentScale = ContentScale.Crop) }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    IconButton(onClick = { if (index > 0) { val m = pageOrder.toMutableList(); val t = m[index]; m[index] = m[index-1]; m[index-1] = t; pageOrder = m } }) { Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Move Left", modifier = Modifier.size(16.dp)) }
                                    IconButton(onClick = { if (index < pageOrder.size-1) { val m = pageOrder.toMutableList(); val t = m[index]; m[index] = m[index+1]; m[index+1] = t; pageOrder = m } }) { Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Move Right", modifier = Modifier.size(16.dp)) }
                                }
                            }
                        }
                    }
                }
                Button(onClick = { saveLauncher.launch("rearranged.pdf") }, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), enabled = !isProcessing) {
                    if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    else Text(text = "Save New Order")
                }
            }
        }
    }
}
