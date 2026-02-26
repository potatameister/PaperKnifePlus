package com.paperknifeplus.app.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperknifeplus.app.ui.theme.PaperPink

@Composable
fun HistoryView(onItemClick: (Uri, String, Int) -> Unit) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    
    val historyItems = SessionManager.history

    val filteredItems = remember(searchQuery, historyItems.size) {
        historyItems.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.tool.contains(searchQuery, ignoreCase = true) 
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Standardized Header
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildAnnotatedString {
                        append("History")
                        withStyle(SpanStyle(color = PaperPink)) { append(".") }
                    },
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-1).sp
                )
                
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Outlined.History, null, modifier = Modifier.padding(8.dp).size(20.dp), tint = PaperPink)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Filled.Search, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text("Search sessions...", color = Color.Gray, fontSize = 14.sp)
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
        
        Text(
            text = "ALL LOCAL SESSIONS",
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            color = Color.Gray.copy(alpha = 0.6f),
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp, start = 24.dp)
        )

        if (filteredItems.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.History, null, modifier = Modifier.size(64.dp).alpha(0.05f))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (searchQuery.isEmpty()) "No recent activity" else "No results for \"$searchQuery\"", 
                        color = Color.Gray, 
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    HistoryItem(item, 
                        onClick = { item.uri?.let { uri -> onItemClick(uri, item.name, item.pageCount) } },
                        onDownload = {
                            try {
                                // Use sharing as a "Save to Files" mechanism which is most reliable on Android
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(android.content.Intent.EXTRA_STREAM, item.uri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(android.content.Intent.createChooser(intent, "Save or Share PDF"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot export file", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
                item(key = "footer_spacer") { Spacer(modifier = Modifier.height(200.dp)) }
            }
        }
    }
}

@Composable
fun HistoryItem(entry: ActivityEntry, onClick: () -> Unit, onDownload: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val entryColor = remember(entry.tool) { 
        when {
            entry.tool.contains("Merge", true) || entry.tool.contains("Split", true) || entry.tool.contains("Delete", true) || entry.tool.contains("Rearrange", true) -> Color(0xFFF43F5E)
            entry.tool.contains("Compress", true) || entry.tool.contains("ZIP", true) || entry.tool.contains("Grayscale", true) -> Color(0xFFF59E0B)
            entry.tool.contains("Protect", true) || entry.tool.contains("Lock", true) || entry.tool.contains("Unlock", true) || entry.tool.contains("Metadata", true) -> Color(0xFF8B5CF6)
            entry.tool.contains("Convert", true) || entry.tool.contains("PDF to", true) || entry.tool.contains("Img to", true) || entry.tool.contains("Extract", true) -> Color(0xFF10B981)
            else -> PaperPink
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(enabled = entry.uri != null) { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF09090B) else Color.White
        ),
        border = BorderStroke(1.dp, if (isDark) entryColor.copy(alpha = 0.1f) else entryColor.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = entryColor.copy(alpha = 0.1f)
            ) {
                Icon(entry.icon, null, tint = entryColor, modifier = Modifier.padding(10.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Text("${entry.tool.uppercase()} • ${entry.size}", fontSize = 11.sp, color = Color.Gray)
            }
            
            IconButton(onClick = onDownload, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Share, null, tint = entryColor.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
            }
            
            Spacer(Modifier.width(12.dp))

            // REMADE OPEN BUTTON: Modern pill style
            Surface(
                onClick = onClick,
                modifier = Modifier.height(32.dp),
                shape = CircleShape,
                color = entryColor,
                shadowElevation = 4.dp
            ) {
                Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "OPEN",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
