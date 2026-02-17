package com.paperknifeplus.app.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
fun RepairView(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.background == Color.Black

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> selectedUri = uri }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { saveUri ->
            isProcessing = true
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(selectedUri!!)?.use { inputStream ->
                        val document = PDDocument.load(inputStream)
                        // Note: Loading and saving fresh usually fixes many structural issues
                        context.contentResolver.openOutputStream(saveUri)?.use { outputStream ->
                            document.save(outputStream)
                        }
                        document.close()
                    }
                    withContext(Dispatchers.Main) { Toast.makeText(context, "PDF Repaired!", Toast.LENGTH_LONG).show(); onBack() }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
                } finally { isProcessing = false }
            }
        }
    }

    LaunchedEffect(Unit) { PDFBoxResourceLoader.init(context) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), CircleShape)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Repair PDF", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                    Text("FIX STRUCTURAL ISSUES", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFFF59E0B), letterSpacing = 1.sp)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            if (selectedUri == null) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(if (isDark) Color(0xFF09090B) else Color.White).border(BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(0.03f)), RoundedCornerShape(32.dp)).clickable { pickLauncher.launch("application/pdf") }, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Outlined.Build, contentDescription = null, modifier = Modifier.size(64.dp).alpha(0.1f))
                        Spacer(Modifier.height(16.dp))
                        Text("Select PDF to Repair", fontWeight = FontWeight.Black, color = Color.Gray)
                    }
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF09090B) else Color.White), border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(0.03f))) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(40.dp), shape = RoundedCornerShape(10.dp), color = Color(0xFFF59E0B).copy(alpha = 0.1f)) {
                            Icon(Icons.Default.PictureAsPdf, null, tint = Color(0xFFF59E0B), modifier = Modifier.padding(10.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(selectedUri?.lastPathSegment ?: "Document", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        IconButton(onClick = { selectedUri = null }) { Icon(Icons.Default.Close, null, tint = Color.Gray) }
                    }
                }
                Button(onClick = { saveLauncher.launch("repaired.pdf") }, modifier = Modifier.fillMaxWidth().height(56.dp), enabled = !isProcessing, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)), shape = RoundedCornerShape(20.dp)) {
                    if (isProcessing) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
                    else Text("Repair & Save", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
