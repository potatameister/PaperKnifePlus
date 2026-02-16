package com.paperknifeplus.app.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SplitView(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var pageCount by remember { mutableStateOf(0) }
    var selectedPages by remember { mutableStateOf(setOf<Int>()) }
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
                        val thumbs = mutableMapOf<Int, Bitmap>()
                        for (i in 0 until minOf(count, 20)) {
                            val page = renderer.openPage(i)
                            val bitmap = Bitmap.createBitmap(page.width/4, page.height/4, Bitmap.Config.ARGB_8888)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            thumbs[i] = bitmap
                            page.close()
                        }
                        withContext(Dispatchers.Main) {
                            pageCount = count
                            thumbnails = thumbs
                            selectedPages = (0 until count).toSet()
                        }
                        renderer.close()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
                }
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
                        selectedPages.sorted().forEach { index -> newDocument.addPage(document.getPage(index)) }
                        context.contentResolver.openOutputStream(saveUri)?.use { outputStream -> newDocument.save(outputStream) }
                        newDocument.close()
                        document.close()
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Split successful!", Toast.LENGTH_LONG).show()
                        onBack()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    LaunchedEffect(Unit) { PDFBoxResourceLoader.init(context) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Text(text = "Split PDF", style = MaterialTheme.typography.titleLarge)
        }

        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            if (selectedUri == null) {
                Box(modifier = Modifier.fillMaxSize().clickable { pickLauncher.launch("application/pdf") }, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color(0xFFE91E63))
                        Text(text = "Select PDF to Split", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = selectedUri?.lastPathSegment ?: "Document", style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            Text(text = "$pageCount Pages", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { selectedUri = null }) { Icon(imageVector = Icons.Default.Close, contentDescription = "Close") }
                    }
                }

                LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.weight(1f)) {
                    items(pageCount) { index ->
                        val isSelected = selectedPages.contains(index)
                        Box(modifier = Modifier.aspectRatio(0.75f).padding(4.dp).clip(RoundedCornerShape(8.dp)).border(2.dp, if (isSelected) Color(0xFFE91E63) else Color.Transparent, RoundedCornerShape(8.dp)).clickable { selectedPages = if (isSelected) selectedPages - index else selectedPages + index }) {
                            thumbnails[index]?.let { Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                            if (isSelected) Box(modifier = Modifier.fillMaxSize().background(Color(0xFFE91E63).copy(alpha = 0.2f)), contentAlignment = Alignment.Center) { Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFE91E63)) }
                            Text(text = "${index + 1}", modifier = Modifier.align(Alignment.BottomStart).padding(4.dp).background(Color.Black.copy(alpha = 0.6f)).padding(horizontal = 4.dp), color = Color.White, fontSize = 10.sp)
                        }
                    }
                }

                Button(
                    onClick = { saveLauncher.launch("split.pdf") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    enabled = selectedPages.isNotEmpty() && !isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                ) {
                    if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    else Text(text = "Extract ${selectedPages.size} Pages")
                }
            }
        }
    }
}
