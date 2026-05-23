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
fun ImageToPdfView(
    onBack: () -> Unit,
    onOpenPreview: (Uri, String, Int) -> Unit
) {
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

    var outputUri by remember { mutableStateOf<Uri?>(null) }

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
                        outputUri = saveUri
                        SessionManager.addEntry(getUriDetails(context, saveUri).name, "Image to PDF", "${selectedUris.size} images", Icons.Filled.Description, saveUri, selectedUris.size)
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
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Image to PDF", fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Text("PHOTO TO DOCUMENT CONVERTER", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
                        }
                        if (selectedUris.isNotEmpty() && currentState == ToolState.CONFIGURING) {
                            TextButton(onClick = { pickLauncher.launch("image/*") }) {
                                Icon(Icons.Filled.Add, null, modifier = Modifier.size(14.dp), tint = accentColor)
                                Spacer(Modifier.width(4.dp))
                                Text("ADD", fontSize = 11.sp, fontWeight = FontWeight.Black, color = accentColor)
                            }
                        }
                    }
                }
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
                            icon = Icons.Outlined.PictureAsPdf,
                            title = "Tap to select images",
                            subtitle = "JPG, PNG, OR WEBP",
                            accentColor = accentColor,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        var selectedIndex by remember { mutableStateOf<Int?>(null) }
                        
                        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("REORDER ASSETS", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 1.sp)
                                Text("${selectedUris.size} IMAGES SELECTED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = accentColor)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilterChip(
                                    selected = pageSize == "Fit",
                                    onClick = { pageSize = "Fit" },
                                    label = { Text("ORIGINAL", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accentColor, selectedLabelColor = Color.White)
                                )
                                Spacer(Modifier.width(4.dp))
                                FilterChip(
                                    selected = pageSize == "A4",
                                    onClick = { pageSize = "A4" },
                                    label = { Text("A4 PAPER", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accentColor, selectedLabelColor = Color.White)
                                )
                            }
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            itemsIndexed(selectedUris) { index, uri ->
                                val isSelected = selectedIndex == index
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(0.707f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5))
                                        .border(BorderStroke(2.dp, if (isSelected) accentColor else Color.Transparent), RoundedCornerShape(16.dp))
                                        .clickable { selectedIndex = if (isSelected) null else index }
                                ) {
                                    Image(painter = rememberAsyncImagePainter(model = uri), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    
                                    if (isSelected) {
                                        Box(modifier = Modifier.fillMaxSize().background(accentColor.copy(alpha = 0.1f)))
                                        Surface(
                                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(24.dp),
                                            color = accentColor,
                                            shape = CircleShape
                                        ) {
                                            Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.padding(4.dp))
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                                        color = Color.Black.copy(0.6f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("${index + 1}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                    }
                                    
                                    IconButton(
                                        onClick = { selectedUris = selectedUris.filterIndexed { i, _ -> i != index }; if (isSelected) selectedIndex = null },
                                        modifier = Modifier.align(Alignment.TopStart).padding(4.dp).size(28.dp).background(Color.Black.copy(0.4f), CircleShape)
                                    ) {
                                        Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }

                        // REORDER CONTROLS (Floating style)
                        if (selectedIndex != null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            val current = selectedIndex!!
                                            if (current > 0) {
                                                val list = selectedUris.toMutableList()
                                                val item = list.removeAt(current)
                                                list.add(current - 1, item)
                                                selectedUris = list
                                                selectedIndex = current - 1
                                            }
                                        },
                                        enabled = selectedIndex!! > 0
                                    ) {
                                        Icon(Icons.Filled.ArrowBackIos, null, tint = if (selectedIndex!! > 0) accentColor else Color.Gray)
                                    }

                                    Text("POSITION: ${selectedIndex!! + 1}", fontSize = 10.sp, fontWeight = FontWeight.Black)

                                    IconButton(
                                        onClick = {
                                            val current = selectedIndex!!
                                            if (current < selectedUris.size - 1) {
                                                val list = selectedUris.toMutableList()
                                                val item = list.removeAt(current)
                                                list.add(current + 1, item)
                                                selectedUris = list
                                                selectedIndex = current + 1
                                            }
                                        },
                                        enabled = selectedIndex!! < selectedUris.size - 1
                                    ) {
                                        Icon(Icons.Filled.ArrowForwardIos, null, tint = if (selectedIndex!! < selectedUris.size - 1) accentColor else Color.Gray)
                                    }
                                }
                            }
                        }
                        
                        Button(
                            onClick = { saveLauncher.launch("images.pdf") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
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
                        onPreview = { outputUri?.let { uri -> onOpenPreview(uri, "Created PDF", selectedUris.size) } },
                        accentColor = accentColor
                    )
                }
                else -> {}
            }
        }
    }
}
