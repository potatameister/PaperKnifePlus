package com.paperknifeplus.app.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperknifeplus.app.ui.theme.PaperPink
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
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
    val isDark = MaterialTheme.colorScheme.background == Color.Black

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedUri = it
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openFileDescriptor(it, "r")?.use { pfd ->
                        val renderer = PdfRenderer(pfd)
                        val count = renderer.pageCount
                        val thumbs = mutableMapOf<Int, Bitmap>()
                        for (i in 0 until minOf(count, 50)) {
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

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Split PDF", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                    Text("EXTRACT OR REMOVE PAGES", fontSize = 8.sp, fontWeight = FontWeight.Black, color = PaperPink, letterSpacing = 1.sp)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            if (selectedUri == null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .background(if (isDark) Color(0xFF09090B) else Color.White)
                        .border(BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f)), RoundedCornerShape(32.dp))
                        .clickable { pickLauncher.launch("application/pdf") },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(64.dp).alpha(0.1f))
                        Spacer(Modifier.height(16.dp))
                        Text("Select PDF to Split", fontWeight = FontWeight.Black, color = Color.Gray)
                        Text("TAP TO BROWSE FILES", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray.copy(alpha = 0.5f), letterSpacing = 1.sp)
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF09090B) else Color.White),
                    border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(40.dp), shape = RoundedCornerShape(10.dp), color = PaperPink.copy(alpha = 0.1f)) {
                            Icon(Icons.Default.PictureAsPdf, null, tint = PaperPink, modifier = Modifier.padding(10.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = selectedUri?.lastPathSegment ?: "Document", fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                            Text(text = "$pageCount Pages • ${selectedPages.size} Selected", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { selectedUri = null }) { Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(20.dp)) }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pageCount) { index ->
                        val isSelected = selectedPages.contains(index)
                        Box(
                            modifier = Modifier
                                .aspectRatio(0.75f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5))
                                .border(
                                    2.dp, 
                                    if (isSelected) PaperPink else Color.Transparent, 
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedPages = if (isSelected) selectedPages - index else selectedPages + index }
                        ) {
                            thumbnails[index]?.let { 
                                Image(
                                    bitmap = it.asImageBitmap(), 
                                    contentDescription = null, 
                                    modifier = Modifier.fillMaxSize(), 
                                    contentScale = ContentScale.Crop 
                                ) 
                            }
                            
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(PaperPink.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = PaperPink, modifier = Modifier.size(28.dp))
                                }
                            }
                            
                            Surface(
                                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "${index + 1}", 
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), 
                                    color = Color.White, 
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = { saveLauncher.launch("extracted.pdf") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .height(56.dp),
                    enabled = selectedPages.isNotEmpty() && !isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = PaperPink),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    else Text(text = "Extract ${selectedPages.size} Pages", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                }
            }
        }
    }
}
