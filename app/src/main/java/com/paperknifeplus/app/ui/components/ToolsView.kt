package com.paperknifeplus.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperknifeplus.app.ui.theme.PaperPink

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items

@Composable
fun ToolsView(onToolClick: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val allTools = remember {
        listOf(
            // EDIT - Rose
            Tool("merge", "Merge", "Combine PDFs", Icons.Outlined.Layers, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            Tool("split", "Split", "Extract pages", Icons.Outlined.ContentCut, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            Tool("rotate", "Rotate", "Fix orientation", Icons.Outlined.RotateRight, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            Tool("rearrange", "Reorder", "Reorder pages", Icons.Outlined.List, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            Tool("delete", "Delete", "Remove pages", Icons.Outlined.Delete, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            Tool("page-numbers", "Numbers", "Add numbering", Icons.Outlined.FormatListNumbered, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            Tool("watermark", "Stamp", "Add overlay", Icons.Outlined.TextFields, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            
            // OPTIMIZE - Amber
            Tool("compress", "Compress", "Optimize size", Icons.Outlined.Bolt, "Optimize", Color(0xFFF59E0B), Color(0xFFFFFBEB)),
            Tool("grayscale", "B&W", "Remove colors", Icons.Outlined.Palette, "Optimize", Color(0xFFF59E0B), Color(0xFFFFFBEB)),
            Tool("repair", "Repair", "Fix corruption", Icons.Outlined.Build, "Optimize", Color(0xFFF59E0B), Color(0xFFFFFBEB)),
            
            // SECURE - Indigo
            Tool("protect", "Lock", "Add password", Icons.Outlined.Lock, "Secure", Color(0xFF6366F1), Color(0xFFEEF2FF)),
            Tool("unlock", "Unlock", "Remove password", Icons.Outlined.LockOpen, "Secure", Color(0xFF6366F1), Color(0xFFEEF2FF)),
            Tool("metadata", "Metadata", "Edit properties", Icons.Outlined.Fingerprint, "Secure", Color(0xFF6366F1), Color(0xFFEEF2FF)),
            
            // CONVERT - Teal
            Tool("pdf2img", "To Image", "Export as JPG", Icons.Outlined.BurstMode, "Convert", Color(0xFF14B8A6), Color(0xFFF0FDFA)),
            Tool("img2pdf", "To PDF", "Photos to PDF", Icons.Outlined.PictureAsPdf, "Convert", Color(0xFF14B8A6), Color(0xFFF0FDFA)),
            Tool("extract-images", "Extract", "Save photos", Icons.Outlined.Collections, "Convert", Color(0xFF14B8A6), Color(0xFFF0FDFA)),
            Tool("pdf2text", "To Text", "Plain text", Icons.Outlined.Article, "Convert", Color(0xFF14B8A6), Color(0xFFF0FDFA))
        )
    }

    val filteredTools = remember(searchQuery, allTools) {
        allTools.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.description.contains(searchQuery, ignoreCase = true) 
        }
    }

    val categories = remember { listOf("Edit", "Optimize", "Secure", "Convert") }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(PaperPink.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.GridView, null, tint = PaperPink, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = buildAnnotatedString {
                        append("Engines")
                        withStyle(SpanStyle(color = PaperPink)) { append(".") }
                    },
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-1.5).sp,
                    fontSize = 32.sp
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search tools...", fontSize = 14.sp, fontWeight = FontWeight.Medium) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(20.dp)) },
                trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) } },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = PaperPink,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                )
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                val categoryTools = filteredTools.filter { it.category == category }
                if (categoryTools.isNotEmpty()) {
                    item(key = category, span = { GridItemSpan(3) }) {
                        Text(
                            text = "${category.uppercase()} ENGINES",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Gray,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 4.dp)
                        )
                    }
                    
                    items(categoryTools, key = { it.id }) { tool ->
                        GridToolItem(tool = tool, onClick = { onToolClick(tool.id) })
                    }
                }
            }

            item(key = "footer_spacer", span = { GridItemSpan(3) }) {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun GridToolItem(tool: Tool, onClick: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val containerColor = remember(isDark) { if (isDark) Color(0xFF0C0C0E) else Color.White }
    val borderColor = remember(isDark) { if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(0.04f) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (isDark) tool.color.copy(alpha = 0.12f) else tool.bgColor
            ) {
                Icon(
                    imageVector = tool.icon ?: Icons.Default.Build,
                    contentDescription = null,
                    tint = tool.color,
                    modifier = Modifier.padding(8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = tool.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.2).sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1
            )
            
            Text(
                text = tool.category.uppercase(),
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray.copy(alpha = 0.6f),
                letterSpacing = 0.5.sp
            )
        }
    }
}
