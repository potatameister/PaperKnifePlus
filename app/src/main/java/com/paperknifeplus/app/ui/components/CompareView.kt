package com.paperknifeplus.app.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareView(
    initialUri: Uri? = null,
    onBack: () -> Unit,
    onOpenPreview: (Uri, String, Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val accentColor = Color(0xFFF59E0B)

    var fileA by remember { mutableStateOf(initialUri) }
    var fileB by remember { mutableStateOf<Uri?>(null) }
    var nameA by remember { mutableStateOf("") }
    var nameB by remember { mutableStateOf("") }
    var passA by remember { mutableStateOf("") }
    var passB by remember { mutableStateOf("") }
    
    var isComparing by remember { mutableStateOf(false) }
    var fileToUnlock by remember { mutableStateOf<Pair<Uri, String>?>(null) } // Uri, target ("A" or "B")
    var isFileLoading by remember { mutableStateOf(false) }

    val pickALauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> 
        uri?.let { 
            isFileLoading = true
            scope.launch(Dispatchers.IO) {
                val isEnc = checkIsEncryptedLocal(context, it)
                withContext(Dispatchers.Main) {
                    if (isEnc) {
                        fileToUnlock = it to "A"
                    } else {
                        fileA = it
                        nameA = getUriDetails(context, it).name
                    }
                    isFileLoading = false
                }
            }
        }
    }

    val pickBLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> 
        uri?.let { 
            isFileLoading = true
            scope.launch(Dispatchers.IO) {
                val isEnc = checkIsEncryptedLocal(context, it)
                withContext(Dispatchers.Main) {
                    if (isEnc) {
                        fileToUnlock = it to "B"
                    } else {
                        fileB = it
                        nameB = getUriDetails(context, it).name
                    }
                    isFileLoading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { if (isComparing) isComparing = false else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Compare", fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Text(if (isComparing) "SYNCHRONIZED VIEW" else "VISUAL DIFFERENCE TOOL", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
                    }
                    if (!isComparing && (fileA != null || fileB != null)) {
                        TextButton(onClick = { fileA = null; fileB = null; nameA = ""; nameB = ""; passA = ""; passB = "" }) {
                            Text("RESET", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                        }
                    } else if (isComparing) {
                        TextButton(onClick = { isComparing = false }) {
                            Text("CHANGE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = accentColor)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (isFileLoading) {
                LoadingStateView(accentColor, false, "Analyzing document...")
            } else if (!isComparing) {
                Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                    Spacer(Modifier.height(24.dp))
                    Text("SELECT DOCUMENTS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(16.dp))
                    
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CompareFileCard("ORIGINAL (A)", nameA, fileA != null, accentColor, Modifier.weight(1f)) { pickALauncher.launch("application/pdf") }
                        CompareFileCard("REVISED (B)", nameB, fileB != null, accentColor, Modifier.weight(1f)) { pickBLauncher.launch("application/pdf") }
                    }
                    
                    Spacer(Modifier.height(32.dp))
                    
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            Icon(Icons.Filled.Compare, null, modifier = Modifier.size(48.dp).alpha(0.1f))
                            Spacer(Modifier.height(16.dp))
                            Text("Pick two files to begin comparison.", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        }
                    }
                    
                    Button(
                        onClick = { isComparing = true },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp).height(60.dp),
                        enabled = fileA != null && fileB != null,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White)
                    ) {
                        Text("START COMPARISON", fontWeight = FontWeight.Black)
                    }
                }
            } else {
                ComparisonViewer(fileA!!, fileB!!, nameA, nameB, passA, passB)
            }

            if (fileToUnlock != null) {
                val (uri, target) = fileToUnlock!!
                LockedFilePrompt(
                    fileName = getUriDetails(context, uri).name,
                    onDismiss = { fileToUnlock = null },
                    onUnlocked = { pass ->
                        isFileLoading = true
                        scope.launch(Dispatchers.IO) {
                            val count = getPageCount(context, uri, pass)
                            withContext(Dispatchers.Main) {
                                if (count > 0) {
                                    if (target == "A") {
                                        fileA = uri
                                        nameA = getUriDetails(context, uri).name
                                        passA = pass
                                    } else {
                                        fileB = uri
                                        nameB = getUriDetails(context, uri).name
                                        passB = pass
                                    }
                                    fileToUnlock = null
                                } else {
                                    Toast.makeText(context, "Invalid Password", Toast.LENGTH_SHORT).show()
                                }
                                isFileLoading = false
                            }
                        }
                    },
                    accentColor = accentColor,
                    isLoading = isFileLoading
                )
            }
        }
    }
}

@Composable
fun ComparisonViewer(uriA: Uri, uriB: Uri, nameA: String, nameB: String, passA: String, passB: String) {
    val context = LocalContext.current
    var pageCountA by remember { mutableIntStateOf(0) }
    var pageCountB by remember { mutableIntStateOf(0) }
    val imageLoader = coil.compose.LocalImageLoader.current
    
    val listState = rememberLazyListState()
    var lightboxData by remember { mutableStateOf<Triple<Uri, Int, Int>?>(null) }
    var lightboxPass by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialUri) {
        if (initialUri != null) {
            isFileLoading = true
            scope.launch(Dispatchers.IO) {
                val isEncrypted = checkIsEncryptedLocal(context, initialUri)
                withContext(Dispatchers.Main) {
                    if (isEncrypted) {
                        fileToUnlock = initialUri to "A"
                    } else {
                        fileA = initialUri
                        nameA = getUriDetails(context, initialUri).name
                    }
                    isFileLoading = false
                }
            }
        }
    }
    
    LaunchedEffect(uriA, uriB) {
        pageCountA = getPageCount(context, uriA, passA.ifEmpty { null })
        pageCountB = getPageCount(context, uriB, passB.ifEmpty { null })
    }

    val maxPages = remember(pageCountA, pageCountB) { max(pageCountA, pageCountB) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
                                password = passA.ifEmpty { null },
                                imageLoader = imageLoader,
                                onClick = { 
                                    lightboxData = Triple(uriA, index, pageCountA)
                                    lightboxPass = passA.ifEmpty { null }
                                },
                                scale = 2.0f // BOOSTED RESOLUTION
                            )
                        } else {
                            Box(Modifier.fillMaxWidth().aspectRatio(0.707f).background(Color.Gray.copy(0.05f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                Text("END OF FILE A", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray.copy(0.4f))
                            }
                        }
                    }

                    // File B Column
                    Box(Modifier.weight(1f).padding(8.dp)) {
                        if (index < pageCountB) {
                            PdfPageItem(
                                uri = uriB,
                                index = index,
                                password = passB.ifEmpty { null },
                                imageLoader = imageLoader,
                                onClick = { 
                                    lightboxData = Triple(uriB, index, pageCountB)
                                    lightboxPass = passB.ifEmpty { null }
                                },
                                scale = 2.0f // BOOSTED RESOLUTION
                            )
                        } else {
                            Box(Modifier.fillMaxWidth().aspectRatio(0.707f).background(Color.Gray.copy(0.05f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                Text("END OF FILE B", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray.copy(0.4f))
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

    lightboxData?.let { (uri, page, total) ->
        PageLightbox(
            uri = uri,
            initialPage = page,
            totalCount = total,
            password = lightboxPass,
            onDismiss = { lightboxData = null; lightboxPass = null }
        )
    }
}

@Composable
fun CompareFileCard(label: String, name: String, isSelected: Boolean, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(24.dp),
        color = if (isSelected) color.copy(0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
        border = BorderStroke(1.dp, if (isSelected) color.copy(0.3f) else Color.Transparent)
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(
                if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.AddCircle, 
                null, 
                tint = if (isSelected) color else Color.Gray.copy(0.5f),
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(label, fontSize = 8.sp, fontWeight = FontWeight.Black, color = if (isSelected) color else Color.Gray, letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
            Text(if (isSelected) name else "Select File", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
