package com.paperknifeplus.app.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tomroush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RotateView(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var rotationAngle by remember { mutableStateOf(0) }
    var isProcessing by remember { mutableStateOf(false) }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> selectedUri = uri }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { saveUri ->
            isProcessing = true
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(selectedUri!!)?.use { inputStream ->
                        val document = PDDocument.load(inputStream)
                        for (i in 0 until document.numberOfPages) {
                            val page = document.getPage(i)
                            val currentRotation = page.rotation
                            page.rotation = (currentRotation + rotationAngle) % 360
                        }
                        context.contentResolver.openOutputStream(saveUri)?.use { outputStream ->
                            document.save(outputStream)
                        }
                        document.close()
                    }
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Rotated!", Toast.LENGTH_LONG).show(); onBack() }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
                } finally { isProcessing = false }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Text("Rotate PDF", style = MaterialTheme.typography.titleLarge)
        }

        Column(Modifier.weight(1f).padding(24.dp)) {
            if (selectedUri == null) {
                Box(Modifier.fillMaxSize().clickable { pickLauncher.launch("application/pdf") }, Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.RotateRight, null, Modifier.size(48.dp), MaterialTheme.colorScheme.primary)
                        Text("Select PDF to Rotate", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf(90, 180, 270).forEach { angle ->
                        Button(
                            onClick = { rotationAngle = angle },
                            colors = ButtonDefaults.buttonColors(containerColor = if (rotationAngle == angle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        ) { Text("$angle°", color = if (rotationAngle == angle) Color.White else Color.Black) }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { saveLauncher.launch("rotated.pdf") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = rotationAngle != 0 && !isProcessing
                ) {
                    if (isProcessing) CircularProgressIndicator(Modifier.size(24.dp))
                    else Text("Rotate & Save")
                }
            }
        }
    }
}
