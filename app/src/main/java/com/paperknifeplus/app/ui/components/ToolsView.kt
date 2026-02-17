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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperknifeplus.app.ui.theme.PaperPink

@Composable
fun ToolsView(onToolClick: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val allTools = listOf(
        // EDIT - Rose
        Tool("merge", "Merge PDF", "Combine multiple PDFs", Icons.Outlined.Layers, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
        Tool("split", "Split PDF", "Extract specific pages", Icons.Outlined.ContentCut, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
        Tool("rotate", "Rotate PDF", "Fix orientation", Icons.Outlined.RotateRight, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
        Tool("rearrange", "Rearrange", "Reorder pages", Icons.Outlined.List, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
        Tool("page-numbers", "Page Numbers", "Add numbering", Icons.Outlined.FormatListNumbered, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
        Tool("watermark", "Watermark", "Add text overlay", Icons.Outlined.TextFields, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
        Tool("signature", "Signature", "Sign documents", Icons.Outlined.Draw, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
        
        // OPTIMIZE - Amber
        Tool("compress", "Compress", "Optimize size", Icons.Outlined.Bolt, "Optimize", Color(0xFFF59E0B), Color(0xFFFFFBEB)),
        Tool("grayscale", "Grayscale", "Remove colors", Icons.Outlined.Palette, "Optimize", Color(0xFFF59E0B), Color(0xFFFFFBEB)),
        Tool("repair", "Repair PDF", "Fix corrupted files", Icons.Outlined.Build, "Optimize", Color(0xFFF59E0B), Color(0xFFFFFBEB)),
        
        // SECURE - Indigo
        Tool("protect", "Protect PDF", "Add password", Icons.Outlined.Lock, "Secure", Color(0xFF6366F1), Color(0xFFEEF2FF)),
        Tool("unlock", "Unlock PDF", "Remove password", Icons.Outlined.LockOpen, "Secure", Color(0xFF6366F1), Color(0xFFEEF2FF)),
        Tool("metadata", "Metadata", "Edit properties", Icons.Outlined.Fingerprint, "Secure", Color(0xFF6366F1), Color(0xFFEEF2FF)),
        
        // CONVERT - Teal
        Tool("pdf2img", "PDF to Image", "Export as JPG", Icons.Outlined.BurstMode, "Convert", Color(0xFF14B8A6), Color(0xFFF0FDFA)),
        Tool("img2pdf", "Image to PDF", "Photos to PDF", Icons.Outlined.PictureAsPdf, "Convert", Color(0xFF14B8A6), Color(0xFFF0FDFA)),
        Tool("extract-images", "Extract Images", "Save all images", Icons.Outlined.Collections, "Convert", Color(0xFF14B8A6), Color(0xFFF0FDFA)),
        Tool("pdf2text", "PDF to Text", "Extract plain text", Icons.Outlined.Article, "Convert", Color(0xFF14B8A6), Color(0xFFF0FDFA))
    )

    val filteredTools = allTools.filter { 
        it.name.contains(searchQuery, ignoreCase = true) || 
        it.description.contains(searchQuery, ignoreCase = true) 
    }

    val categories = listOf("Edit", "Optimize", "Secure", "Convert")

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(PaperPink.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.GridView, null, tint = PaperPink, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "All Tools",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-1.5).sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.size(8.dp).background(PaperPink, CircleShape))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search tools...", fontSize = 14.sp, fontWeight = FontWeight.Medium) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null, tint = Color.Gray) } },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = PaperPink,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp) // More compact spacing
        ) {
            categories.forEach { category ->
                val categoryTools = filteredTools.filter { it.category == category }
                if (categoryTools.isNotEmpty()) {
                    item {
                        Text(
                            text = "${category.uppercase()} TOOLS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Gray,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
                        )
                    }
                    
                    items(categoryTools) { tool ->
                        ListToolItem(tool = tool, onClick = { onToolClick(tool.id) })
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun ListToolItem(tool: Tool, onClick: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp) // More compact height
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp), // Slightly smaller corner radius for compact look
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF09090B) else Color.White
        ),
        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(0.03f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(36.dp), // Smaller icon surface
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
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tool.name,
                    fontSize = 13.sp, // Slightly smaller font
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = tool.description,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 0.2.sp,
                    maxLines = 1
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray.copy(alpha = 0.2f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
