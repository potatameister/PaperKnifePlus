package com.paperknifeplus.app.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun UnlockView(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var password by remember { mutableStateOf("") }
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
                        val document = PDDocument.load(inputStream, password)
                        document.isAllSecurityToBeRemoved = true
                        
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            document.save(outputStream)
                        }
                        document.close()
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Unlocked successfully!", Toast.LENGTH_LONG).show()
                        onBack()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Incorrect Password or Error: ${e.message}", Toast.LENGTH_LONG).show()
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
                title = { Text("Unlock PDF") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp)) {
            if (selectedUri == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.5f)
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { pickLauncher.launch("application/pdf") },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.LockOpen, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Select Protected PDF", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Text("Enter Password to Unlock", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                )
                Button(
                    onClick = { saveLauncher.launch("unlocked_document.pdf") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = password.isNotEmpty() && !isProcessing
                ) {
                    if (isProcessing) CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Unlock & Save")
                }
            }
        }
    }
}
