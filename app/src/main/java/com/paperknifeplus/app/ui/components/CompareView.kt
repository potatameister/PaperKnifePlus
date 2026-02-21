package com.paperknifeplus.app.ui.components

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperknifeplus.app.ui.theme.PaperPink
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareView(
    onBack: () -> Unit,
    onOpenPreview: (Uri, String, Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val accentColor = Color(0xFFF59E0B)

    var fileA by remember { mutableStateOf<Uri?>(null) }
    var fileB by remember { mutableStateOf<Uri?>(null) }
    var nameA by remember { mutableStateOf("") }
    var nameB by remember { mutableStateOf("") }
    
    var isConfiguring by remember { mutableStateOf(false) }

    val pickALauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri -> 
        uri?.let { 
            fileA = it
            nameA = getUriDetails(context, it).name
        }
    }

    val pickBLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri -> 
        uri?.let { 
            fileB = it
            nameB = getUriDetails(context, it).name
        }
    }

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
                    Text("Compare", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                    Text("VISUAL DIFFERENCE TOOL", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Select two files to compare side-by-side", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            
            CompareFileCard("FILE A", nameA, fileA != null, accentColor) { pickALauncher.launch("application/pdf") }
            
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.CompareArrows, null, modifier = Modifier.padding(6.dp), tint = Color.Gray)
                }
            }
            
            CompareFileCard("FILE B", nameB, fileB != null, accentColor) { pickBLauncher.launch("application/pdf") }
            
            Spacer(Modifier.weight(1f))
            
            Button(
                onClick = { isConfiguring = true },
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).height(60.dp),
                enabled = fileA != null && fileB != null,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("START COMPARISON", fontWeight = FontWeight.Black)
            }
        }
    }

    if (isConfiguring) {
        ComparisonViewer(fileA!!, fileB!!, nameA, nameB, onBack = { isConfiguring = false })
    }
}

@Composable
fun ComparisonViewer(uriA: Uri, uriB: Uri, nameA: String, nameB: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var pageCountA by remember { mutableIntStateOf(0) }
    var pageCountB by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(uriA, uriB) {
        pageCountA = getPageCount(context, uriA, null)
        pageCountB = getPageCount(context, uriB, null)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.Close, null) }
                Text("Side-by-Side Comparison", fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 12.dp))
            }
            
            Row(Modifier.fillMaxWidth().weight(1f)) {
                Box(Modifier.weight(1f).fillMaxHeight().background(Color.Black.copy(0.05f))) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(nameA, fontSize = 9.sp, fontWeight = FontWeight.Black, maxLines = 1, modifier = Modifier.padding(4.dp))
                        UnifiedPdfPreview(uri = uriA, pageCount = pageCountA, mode = PreviewMode.GRID)
                    }
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(Color.Gray.copy(0.2f)))
                Box(Modifier.weight(1f).fillMaxHeight().background(Color.Black.copy(0.05f))) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(nameB, fontSize = 9.sp, fontWeight = FontWeight.Black, maxLines = 1, modifier = Modifier.padding(4.dp))
                        UnifiedPdfPreview(uri = uriB, pageCount = pageCountB, mode = PreviewMode.GRID)
                    }
                }
            }
        }
    }
}

@Composable
fun CompareFileCard(label: String, name: String, isSelected: Boolean, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(100.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) color.copy(0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
        border = BorderStroke(1.dp, if (isSelected) color.copy(0.3f) else Color.Transparent)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.Center) {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Black, color = if (isSelected) color else Color.Gray, letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
            Text(if (isSelected) name else "Tap to select file", fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
        }
    }
}
