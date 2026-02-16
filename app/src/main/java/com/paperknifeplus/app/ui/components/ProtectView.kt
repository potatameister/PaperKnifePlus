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
import com.tomroush.pdfbox.pdmodel.encryption.AccessPermission
import com.tomroush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProtectView(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var password by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> selectedUri = uri }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { saveUri ->
            isProcessing = true
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(selectedUri!!)?.use { inputStream ->
                        val document = PDDocument.load(inputStream)
                        val ap = AccessPermission()
                        val spp = StandardProtectionPolicy(password, password, ap)
                        spp.encryptionKeyLength = 128
                        document.protect(spp)
                        context.contentResolver.openOutputStream(saveUri)?.use { outputStream -> document.save(outputStream) }
                        document.close()
                    }
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Protected!", Toast.LENGTH_LONG).show(); onBack() }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
                } finally { isProcessing = false }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Text("Protect PDF", style = MaterialTheme.typography.titleLarge)
        }

        Column(Modifier.weight(1f).padding(24.dp)) {
            if (selectedUri == null) {
                Box(Modifier.fillMaxSize().clickable { pickLauncher.launch("application/pdf") }, Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Lock, null, Modifier.size(48.dp), Color(0xFFFF9800))
                        Text("Select PDF to Secure", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Set Password") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { saveLauncher.launch("protected.pdf") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = password.isNotEmpty() && !isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) {
                    if (isProcessing) CircularProgressIndicator(Modifier.size(24.dp))
                    else Text("Protect & Save")
                }
            }
        }
    }
}
