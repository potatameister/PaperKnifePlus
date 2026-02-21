package com.paperknifeplus.app.ui.components

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import kotlinx.coroutines.launch
import kotlin.math.max

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
    
    var isComparing by remember { mutableStateOf(false) }

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
            if (!isComparing) {
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
        }
    ) { padding ->
        if (!isComparing) {
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Select two files to compare side-by-side", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                
                CompareFileCard("FILE A (ORIGINAL)", nameA, fileA != null, accentColor) { pickALauncher.launch("application/pdf") }
                
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.CompareArrows, null, modifier = Modifier.padding(6.dp), tint = Color.Gray)
                    }
                }
                
                CompareFileCard("FILE B (REVISED)", nameB, fileB != null, accentColor) { pickBLauncher.launch("application/pdf") }
                
                Spacer(Modifier.weight(1f))
                
                Button(
                    onClick = { isComparing = true },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).height(60.dp),
                    enabled = fileA != null && fileB != null,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("START COMPARISON", fontWeight = FontWeight.Black)
                }
            }
        } else {
            ComparisonViewer(fileA!!, fileB!!, nameA, nameB, onBack = { isComparing = false })
        }
    }
}

@Composable
fun ComparisonViewer(uriA: Uri, uriB: Uri, nameA: String, nameB: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var pageCountA by remember { mutableIntStateOf(0) }
    var pageCountB by remember { mutableIntStateOf(0) }
    val imageLoader = coil.compose.LocalImageLoader.current
    
    val listState = rememberLazyListState()
    
    LaunchedEffect(uriA, uriB) {
        pageCountA = getPageCount(context, uriA, null)
        pageCountB = getPageCount(context, uriB, null)
    }

    val maxPages = remember(pageCountA, pageCountB) { max(pageCountA, pageCountB) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.Close, null) }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("Synchronized View", fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text("SCROLLING BOTH FILES", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFFF59E0B))
                }
            }
            
            Divider(color = Color.Gray.copy(alpha = 0.1f))

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(maxPages) { index ->
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        // File A Column
                        Box(Modifier.weight(1f).padding(8.dp)) {
                            if (index < pageCountA) {
                                PdfPageItem(
                                    uri = uriA,
                                    index = index,
                                    password = null,
                                    imageLoader = imageLoader,
                                    onClick = { },
                                    scale = 0.6f
                                )
                            } else {
                                Box(Modifier.fillMaxSize().background(Color.Gray.copy(0.05f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                    Text("END OF FILE A", fontSize = 8.sp, color = Color.Gray)
                                }
                            }
                        }

                        // Divider
                        Box(Modifier.fillMaxHeight().width(1.dp).background(Color.Gray.copy(0.1f)))

                        // File B Column
                        Box(Modifier.weight(1f).padding(8.dp)) {
                            if (index < pageCountB) {
                                PdfPageItem(
                                    uri = uriB,
                                    index = index,
                                    password = null,
                                    imageLoader = imageLoader,
                                    onClick = { },
                                    scale = 0.6f
                                )
                            } else {
                                Box(Modifier.fillMaxSize().background(Color.Gray.copy(0.05f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                    Text("END OF FILE B", fontSize = 8.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                    if (index < maxPages - 1) {
                        Divider(color = Color.Gray.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}
