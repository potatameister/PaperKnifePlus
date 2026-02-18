package com.paperknifeplus.app.ui.components

import android.graphics.Bitmap
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
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PictureAsPdf
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
import coil.compose.rememberAsyncImagePainter
import com.paperknifeplus.app.ui.theme.PaperPink
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ImageToPdfView(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val accentColor = Color(0xFF14B8A6)

    var currentState by remember { mutableStateOf<ToolState>(ToolState.CONFIGURING) }
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var pageSize by remember { mutableStateOf("Fit") }
    
    var progressCount by remember { mutableIntStateOf(0) }
    var processingTime by remember { mutableStateOf("") }
    var showLoadingWarning by remember { mutableStateOf(false) }

    LaunchedEffect(currentState) {
        if (currentState == ToolState.PROCESSING) {
            delay(5000)
            showLoadingWarning = true
        } else {
            showLoadingWarning = false
        }
    }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -> 
        selectedUris = selectedUris + uris 
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { saveUri ->
            currentState = ToolState.PROCESSING
            val startTime = System.currentTimeMillis()
            scope.launch(Dispatchers.IO) {
                try {
                    convertImagesToPdf(context, selectedUris, saveUri, pageSize) { current, total ->
                        progressCount = current
                    }
                    val endTime = System.currentTimeMillis()
                    val timeStr = String.format("%.1fs", (endTime - startTime) / 1000.0)
                    withContext(Dispatchers.Main) {
                        processingTime = timeStr
                        SessionManager.addEntry("Created PDF", "Image to PDF", "${selectedUris.size} images", Icons.Default.PictureAsPdf)
                        currentState = ToolState.SUCCESS
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        currentState = ToolState.CONFIGURING
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) { PDFBoxResourceLoader.init(context) }

    Scaffold(
        topBar = {
            if (currentState != ToolState.SUCCESS && currentState != ToolState.PROCESSING) {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), CircleShape)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Image to PDF", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                        Text("CONVERT PHOTOS TO DOCUMENT", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentState == ToolState.CONFIGURING && selectedUris.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { pickLauncher.launch("image/*") },
                    containerColor = accentColor,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) { Icon(Icons.Default.Add, "Add") }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            when (currentState) {
                ToolState.CONFIGURING -> {
                    if (selectedUris.isEmpty()) {
                        SelectionGrid(
                            onSelect = { pickLauncher.launch("image/*") },
                            isDark = isDark,
                            icon = Icons.Default.AddPhotoAlternate,
                            title = "Tap to select images",
                            subtitle = "JPG, PNG, OR WEBP",
                            accentColor = accentColor,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("REORDER BY TAPPING ARROWS", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 1.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilterChip(
                                    selected = pageSize == "Fit",
                                    onClick = { pageSize = "Fit" },
                                    label = { Text("Original Size", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accentColor, selectedLabelColor = Color.White)
                                )
                                Spacer(Modifier.width(4.dp))
                                FilterChip(
                                    selected = pageSize == "A4",
                                    onClick = { pageSize = "A4" },
                                    label = { Text("A4 Paper", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accentColor, selectedLabelColor = Color.White)
                                )
                            }
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(selectedUris) { index, uri ->
                                Box(modifier = Modifier.aspectRatio(0.8f).clip(RoundedCornerShape(12.dp)).background(if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5))) {
                                    Image(painter = rememberAsyncImagePainter(model = uri), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    
                                    // Subtle Controls
                                    Surface(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp).clickable { selectedUris = selectedUris.filterIndexed { i, _ -> i != index } }, color = Color.Black.copy(0.5f), shape = CircleShape) {
                                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.padding(4.dp))
                                    }

                                    Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        if (index > 0) {
                                            IconButton(
                                                onClick = { val list = selectedUris.toMutableList(); val tmp = list[index]; list[index] = list[index-1]; list[index-1] = tmp; selectedUris = list },
                                                modifier = Modifier.size(24.dp).background(Color.Black.copy(0.4f), CircleShape)
                                            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(12.dp)) }
                                        }
                                        
                                        if (index < selectedUris.size - 1) {
                                            IconButton(
                                                onClick = { val list = selectedUris.toMutableList(); val tmp = list[index]; list[index] = list[index+1]; list[index+1] = tmp; selectedUris = list },
                                                modifier = Modifier.size(24.dp).background(Color.Black.copy(0.4f), CircleShape)
                                            ) { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(12.dp)) }
                                        }
                                    }
                                }
                            }
                        }
                        
                        Button(
                            onClick = { saveLauncher.launch("images.pdf") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Create PDF from ${selectedUris.size} Images", fontWeight = FontWeight.Black)
                        }
                    }
                }
                ToolState.PROCESSING -> {
                    ProcessingStateView(
                        accentColor = accentColor,
                        preview = null,
                        text = "Packaging high-res document...",
                        current = progressCount,
                        total = selectedUris.size,
                        showWarning = showLoadingWarning
                    )
                }
                ToolState.SUCCESS -> {
                    SuccessView(
                        message = "PDF Created",
                        subMessage = "Images merged into document",
                        processingTime = processingTime,
                        onDone = onBack,
                        onProcessMore = { selectedUris = emptyList(); currentState = ToolState.CONFIGURING },
                        accentColor = accentColor
                    )
                }
                else -> {}
            }
        }
    }
}
