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
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import com.tomroush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RearrangeView(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var pageOrder by remember { mutableStateOf(listOf<Int>()) }
    var thumbnails by remember { mutableStateOf<Map<Int, Bitmap>>(emptyMap()) }
    var isProcessing by remember { mutableStateOf(false) }

    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
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
                        withContext(Dispatchers.Main) {
                            pageOrder = order
                            thumbnails = thumbs
                        }
                        renderer.close()
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { saveUri ->
        saveUri?.let { uri ->
            isProcessing = true
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(selectedUri!!)?.use { inputStream ->
                        val document = PDDocument.load(inputStream)
                        val newDocument = PDDocument()
                        pageOrder.forEach { oldIndex ->
                            newDocument.addPage(document.getPage(oldIndex))
                        }
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            newDocument.save(outputStream)
                        }
                        newDocument.close()
                        document.close()
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Rearranged Successfully!", Toast.LENGTH_LONG).show()
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
                title = { Text("Rearrange Pages") },
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
                        Icon(Icons.Default.ViewQuilt, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Select PDF to Rearrange", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Text("Tap arrows to move pages", fontWeight = FontWeight.Bold)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.weight(1f).padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(pageOrder) { index, originalPageIndex ->
                        Card(shape = RoundedCornerShape(8.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.aspectRatio(0.7f).background(Color.LightGray)) {
                                    thumbnails[originalPageIndex]?.let { bitmap ->
                                        Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    }
                                    Box(Modifier.align(Alignment.BottomEnd).background(Color.Black.copy(0.6f)).padding(4.dp)) {
                                        Text("${originalPageIndex + 1}", color = Color.White, fontSize = 10.sp)
                                    }
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    IconButton(
                                        onClick = { 
                                            if (index > 0) {
                                                val mutable = pageOrder.toMutableList()
                                                val temp = mutable[index]
                                                mutable[index] = mutable[index - 1]
                                                mutable[index - 1] = temp
                                                pageOrder = mutable
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) { Icon(Icons.Default.ArrowBack, null, Modifier.size(16.dp)) } // Left
                                    
                                    IconButton(
                                        onClick = {
                                            if (index < pageOrder.size - 1) {
                                                val mutable = pageOrder.toMutableList()
                                                val temp = mutable[index]
                                                mutable[index] = mutable[index + 1]
                                                mutable[index + 1] = temp
                                                pageOrder = mutable
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) { Icon(Icons.Default.ArrowForward, null, Modifier.size(16.dp)) } // Right
                                }
                            }
                        }
                    }
                }
                Button(
                    onClick = { saveLauncher.launch("rearranged.pdf") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing
                ) {
                    if (isProcessing) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
                    else Text("Save New Order")
                }
            }
        }
    }
}
