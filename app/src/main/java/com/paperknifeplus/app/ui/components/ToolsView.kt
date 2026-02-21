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
            Tool("delete", "Delete", "Remove pages", Icons.Outlined.Delete, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            Tool("rearrange", "Rearrange", "Sort pages", Icons.Outlined.SwapVert, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            Tool("rotate", "Rotate", "Fix orientation", Icons.Outlined.RotateRight, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            Tool("sign", "Sign", "Add signature", Icons.Outlined.Draw, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            Tool("watermark", "Watermark", "Add overlay", Icons.Outlined.TextFields, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            Tool("page-numbers", "Numbers", "Add pagination", Icons.Outlined.FormatListNumbered, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            Tool("bookmarks", "Bookmarks", "Edit bookmarks", Icons.Outlined.BookmarkBorder, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            
            // OPTIMIZE - Amber
            Tool("compress", "Compress", "Small size", Icons.Outlined.Bolt, "Optimize", Color(0xFFF59E0B), Color(0xFFFFFBEB)),
            Tool("grayscale", "Grayscale", "Gray tones", Icons.Outlined.Palette, "Optimize", Color(0xFFF59E0B), Color(0xFFFFFBEB)),
            Tool("repair", "Repair", "Fix corruption", Icons.Outlined.Build, "Optimize", Color(0xFFF59E0B), Color(0xFFFFFBEB)),
            Tool("compare", "Compare", "Visual diff", Icons.Outlined.Compare, "Optimize", Color(0xFFF59E0B), Color(0xFFFFFBEB)),
            
            // SECURE - Indigo
            Tool("protect", "Lock", "Password", Icons.Outlined.Lock, "Secure", Color(0xFF6366F1), Color(0xFFEEF2FF)),
            Tool("unlock", "Unlock", "Remove pass", Icons.Outlined.LockOpen, "Secure", Color(0xFF6366F1), Color(0xFFEEF2FF)),
            Tool("metadata", "Metadata", "Edit props", Icons.Outlined.Fingerprint, "Secure", Color(0xFF6366F1), Color(0xFFEEF2FF)),
            
            // CONVERT - Teal
            Tool("pdf2img", "To Image", "Export JPG", Icons.Outlined.BurstMode, "Convert", Color(0xFF14B8A6), Color(0xFFF0FDFA)),
            Tool("img2pdf", "To PDF", "Photos to PDF", Icons.Outlined.PictureAsPdf, "Convert", Color(0xFF14B8A6), Color(0xFFF0FDFA)),
            Tool("pdf2zip", "To ZIP", "Archive PDF", Icons.Outlined.FolderZip, "Convert", Color(0xFF14B8A6), Color(0xFFF0FDFA)),
            Tool("extract-images", "Extract", "Save photos", Icons.Outlined.Collections, "Convert", Color(0xFF14B8A6), Color(0xFFF0FDFA)),
            Tool("pdf2text", "To Text", "Plain text", Icons.Outlined.Article, "Convert", Color(0xFF14B8A6), Color(0xFFF0FDFA))
        )
    }

    val filteredTools by remember {
        derivedStateOf {
            allTools.filter { 
                it.name.contains(searchQuery, ignoreCase = true) || 
                it.description.contains(searchQuery, ignoreCase = true) 
            }
        }
    }

    val categories = remember { listOf("Edit", "Optimize", "Secure", "Convert") }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        // Search Header
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildAnnotatedString {
                        append("Tools")
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
                    Icon(Icons.Outlined.GridView, null, modifier = Modifier.padding(8.dp).size(20.dp), tint = PaperPink)
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
                            Text("Search 20+ tools...", color = Color.Gray, fontSize = 14.sp)
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

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            categories.forEach { category ->
                val categoryTools = filteredTools.filter { it.category == category }
                if (categoryTools.isNotEmpty()) {
                    item(key = "header_$category", span = { GridItemSpan(3) }) {
                        Text(
                            text = category.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Gray.copy(alpha = 0.6f),
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp)
                        )
                    }
                    
                    items(categoryTools, key = { it.id }) { tool ->
                        PremiumGridItem(tool = tool, onClick = { onToolClick(tool.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumGridItem(tool: Tool, onClick: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val containerColor = remember(isDark) { if (isDark) Color(0xFF0F0F12) else Color.White }
    val borderColor = remember(isDark) { if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(0.04f) }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(if (isDark) tool.color.copy(alpha = 0.15f) else tool.bgColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon ?: Icons.Filled.Build,
                    contentDescription = null,
                    tint = tool.color,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = tool.name,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.2).sp,
                maxLines = 1
            )
            
            Text(
                text = tool.description.uppercase(),
                fontSize = 6.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray.copy(alpha = 0.5f),
                letterSpacing = 0.4.sp,
                maxLines = 1
            )
        }
    }
}
