package com.paperknifeplus.app.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
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
import kotlin.math.max

@Composable
fun SynchronizedComparison(
    originalUri: Uri,
    modifiedUri: Uri,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    accentColor: Color = PaperPink
) {
    val context = LocalContext.current
    var pageCountA by remember { mutableIntStateOf(0) }
    var pageCountB by remember { mutableIntStateOf(0) }
    val imageLoader = coil.compose.LocalImageLoader.current
    
    val listState = rememberLazyListState()
    
    LaunchedEffect(originalUri, modifiedUri) {
        pageCountA = getPageCount(context, originalUri, null)
        pageCountB = getPageCount(context, modifiedUri, null)
    }

    val maxPages = remember(pageCountA, pageCountB) { max(pageCountA, pageCountB) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), CircleShape)) {
                    Icon(Icons.Filled.Edit, "Back to Edit", modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Comparison Preview", fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text("BEFORE (LEFT) VS AFTER (RIGHT)", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
                }
                
                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("COMMIT", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
            
            Divider(color = Color.Gray.copy(alpha = 0.1f))

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(maxPages) { index ->
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        // Original Column
                        Box(Modifier.weight(1f).padding(8.dp)) {
                            if (index < pageCountA) {
                                PdfPageItem(
                                    uri = originalUri,
                                    index = index,
                                    password = null,
                                    imageLoader = imageLoader,
                                    onClick = { },
                                    scale = 0.6f
                                )
                            }
                        }

                        // Divider
                        Box(Modifier.fillMaxHeight().width(1.dp).background(Color.Gray.copy(0.1f)))

                        // Modified Column
                        Box(Modifier.weight(1f).padding(8.dp)) {
                            if (index < pageCountB) {
                                PdfPageItem(
                                    uri = modifiedUri,
                                    index = index,
                                    password = null,
                                    imageLoader = imageLoader,
                                    onClick = { },
                                    scale = 0.6f
                                )
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
