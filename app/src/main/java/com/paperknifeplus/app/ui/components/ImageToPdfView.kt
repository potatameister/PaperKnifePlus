package com.paperknifeplus.app.ui.components

import android.graphics.BitmapFactory
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.paperknifeplus.app.ui.theme.PaperPink
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
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
    var processingTime by remember { mutableStateOf("") }
    var showLoadingWarning by remember { mutableStateOf(false) }
    
    var progressCount by remember { mutableIntStateOf(0) }

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
                    val document = PDDocument()
                    selectedUris.forEachIndexed { index, imgUri ->
                        progressCount = index + 1
                        context.contentResolver.openInputStream(imgUri)?.use { inputStream ->
                            val bitmap = BitmapFactory.decodeStream(inputStream)
                            val pdImage = JPEGFactory.createFromImage(document, bitmap)
                            val page = PDPage(PDRectangle(pdImage.width.toFloat(), pdImage.height.toFloat()))
                            document.addPage(page)
                            PDPageContentStream(document, page).use { contentStream ->
                                contentStream.drawImage(pdImage, 0f, 0f)
                            }
                            bitmap.recycle()
                        }
                    }
                    saveAndFlush(context, document, saveUri)
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
            if (currentState == ToolState.CONFIGURING) {
                FloatingActionButton(
                    onClick = { pickLauncher.launch("image/*") },
                    containerColor = accentColor,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) { Icon(Icons.Default.AddPhotoAlternate, "Add") }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            when (currentState) {
                ToolState.CONFIGURING -> {
                    if (selectedUris.isEmpty()) {
                        Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.PictureAsPdf, null, modifier = Modifier.size(64.dp).alpha(0.1f))
                                Spacer(Modifier.height(16.dp))
                                Text("No images selected.", fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text("TAP THE + BUTTON TO START", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray.copy(alpha = 0.5f), letterSpacing = 1.sp)
                            }
                        }
                    } else {
                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { Spacer(Modifier.height(16.dp)) }
                            items(selectedUris) { uri ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF09090B) else Color.White),
                                    border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(0.03f))
                                ) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(Modifier.size(50.dp), shape = RoundedCornerShape(12.dp)) {
                                            Image(painter = rememberAsyncImagePainter(model = uri), contentDescription = null, contentScale = ContentScale.Crop)
                                        }
                                        Spacer(Modifier.width(16.dp))
                                        Text("Image", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        IconButton(onClick = { selectedUris = selectedUris - uri }) { 
                                            Icon(Icons.Default.DeleteOutline, null, tint = Color.Gray, modifier = Modifier.size(20.dp)) 
                                        }
                                    }
                                }
                            }
                            item { Spacer(Modifier.height(100.dp)) }
                        }
                        
                        Button(
                            onClick = { saveLauncher.launch("images.pdf") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).height(56.dp),
                            enabled = selectedUris.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Create PDF from ${selectedUris.size} Images", fontWeight = FontWeight.Black)
                        }
                    }
                }
                ToolState.PROCESSING -> {
                    LoadingStateView(accentColor, showLoadingWarning, "Encoding image $progressCount of ${selectedUris.size}...")
                }
                ToolState.SUCCESS -> {
                    SuccessView(
                        processingTime = processingTime,
                        onDone = onBack,
                        onProcessMore = { 
                            selectedUris = emptyList()
                            currentState = ToolState.CONFIGURING 
                        },
                        accentColor = accentColor
                    )
                }
                else -> {}
            }
        }
    }
}
