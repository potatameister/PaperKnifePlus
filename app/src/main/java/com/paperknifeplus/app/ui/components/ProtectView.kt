package com.paperknifeplus.app.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.sp
import com.tomroush.pdfbox.pdmodel.PDDocument
import com.tomroush.pdfbox.pdmodel.encryption.AccessPermission
import com.tomroush.pdfbox.pdmodel.encryption.StandardProtectionPolicy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectView(onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var password by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedUri = uri }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { saveUri ->
            isProcessing = true
            try {
                context.contentResolver.openInputStream(selectedUri!!)?.use { inputStream ->
                    val document = PDDocument.load(inputStream)
                    val ap = AccessPermission()
                    val spp = StandardProtectionPolicy(password, password, ap)
                    spp.encryptionKeyLength = 128
                    document.protect(spp)
                    
                    context.contentResolver.openOutputStream(saveUri)?.use { outputStream ->
                        document.save(outputStream)
                    }
                    document.close()
                }
                Toast.makeText(context, "File protected!", Toast.LENGTH_LONG).show()
                onBack()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Protect PDF") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
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
                        .border(2.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(32.dp))
                        .clickable { pickLauncher.launch("application/pdf") },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color(0xFFFF9800))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Select PDF to Secure", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFFFF9800))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(selectedUri?.lastPathSegment ?: "Document", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
                        IconButton(onClick = { selectedUri = null }) { Icon(Icons.Default.Close, contentDescription = null) }
                    }
                }

                Text("Set Password", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Enter strong password") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { saveLauncher.launch("protected_document.pdf") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = password.isNotEmpty() && !isProcessing,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) {
                    if (isProcessing) CircularProgressIndicator(color = Color.White)
                    else Text("Protect & Save PDF", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
