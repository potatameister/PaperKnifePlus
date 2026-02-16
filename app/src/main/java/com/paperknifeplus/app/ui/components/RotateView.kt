package com.paperknifeplus.app.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RotateView(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var rotationAngle by remember { mutableStateOf(0) }
    var isProcessing by remember { mutableStateOf(false) }

    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedUri = uri }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { saveUri ->
        saveUri?.let { uri ->
            isProcessing = true
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(selectedUri!!)?.use { inputStream ->
                        val document = PDDocument.load(inputStream)
                        for (i in 0 until document.numberOfPages) {
                            val page = document.getPage(i)
                            page.rotation = (page.rotation + rotationAngle) % 360
                        }
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            document.save(outputStream)
                        }
                        document.close()
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Rotated Successfully!", Toast.LENGTH_LONG).show()
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
                title = { Text("Rotate PDF Pages") },
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
                        Icon(Icons.Default.RotateRight, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Select PDF to Rotate", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Text("Select Rotation Angle", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf(90, 180, 270).forEach { angle ->
                        FilterChip(
                            selected = rotationAngle == angle,
                            onClick = { rotationAngle = angle },
                            label = { Text("$angle°") },
                            leadingIcon = {
                                if (rotationAngle == angle) Icon(Icons.Default.Check, null)
                            }
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = { saveLauncher.launch("rotated_document.pdf") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = rotationAngle != 0 && !isProcessing
                ) {
                    if (isProcessing) CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Rotate & Save")
                }
            }
        }
    }
}
