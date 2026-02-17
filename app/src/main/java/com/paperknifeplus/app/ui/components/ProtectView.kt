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
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
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
    val isDark = MaterialTheme.colorScheme.background == Color.Black

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
                    Text("Protect", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                    Text("ENCRYPT YOUR DOCUMENT", fontSize = 8.sp, fontWeight = FontWeight.Black, color = PaperPink, letterSpacing = 1.sp)
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
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(64.dp).alpha(0.1f))
                        Spacer(Modifier.height(16.dp))
                        Text("Select PDF to Secure", fontWeight = FontWeight.Black, color = Color.Gray)
                        Text("TAP TO BROWSE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray.copy(alpha = 0.5f), letterSpacing = 1.sp)
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF09090B) else Color.White),
                    border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f))
                ) {
                    Column(Modifier.padding(24.dp)) {
                        Text("Encryption", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Set Password", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = PaperPink,
                                cursorColor = PaperPink
                            )
                        )
                    }
                }
                
                Button(
                    onClick = { saveLauncher.launch("protected.pdf") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = password.isNotEmpty() && !isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = PaperPink),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    if (isProcessing) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
                    else Text("Protect & Save", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
