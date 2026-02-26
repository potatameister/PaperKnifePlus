package com.paperknifeplus.app.ui.components

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperknifeplus.app.ui.theme.PaperPink

@Composable
fun ToolPickerContent(onToolClick: (String) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    
    val allTools = remember {
        listOf(
            Tool("merge", "Merge", "Join PDFs", "Join Files", Icons.Outlined.Layers, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            Tool("split", "Split", "Extract pages", "Extract Pages", Icons.Outlined.ContentCut, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            Tool("delete", "Delete", "Remove pages", "Wipe Pages", Icons.Outlined.Delete, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            Tool("rearrange", "Rearrange", "Sort pages", "Sort Order", Icons.Outlined.SwapVert, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            Tool("rotate", "Rotate", "Fix orientation", "Fix Angle", Icons.Outlined.RotateRight, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            Tool("sign", "Sign", "Add signature", "Add Signature", Icons.Outlined.Draw, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            Tool("watermark", "Watermark", "Add overlay", "Custom Overlay", Icons.Outlined.TextFields, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            Tool("page-numbers", "Numbers", "Add pagination", "Add Numbers", Icons.Outlined.FormatListNumbered, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            
            Tool("compress", "Compress", "Small size", "Reduce Size", Icons.Outlined.Bolt, "Optimize", Color(0xFFF59E0B), Color(0xFFFFFBEB)),
            Tool("grayscale", "Grayscale", "Gray tones", "Black & White", Icons.Outlined.Palette, "Optimize", Color(0xFFF59E0B), Color(0xFFFFFBEB)),
            Tool("repair", "Repair", "Fix corruption", "Fix Defects", Icons.Outlined.Build, "Optimize", Color(0xFFF59E0B), Color(0xFFFFFBEB)),
            Tool("compare", "Compare", "Visual diff", "Side-by-Side", Icons.Outlined.Compare, "Optimize", Color(0xFFF59E0B), Color(0xFFFFFBEB)),
            
            Tool("protect", "Lock", "Password", "Set Pass", Icons.Outlined.Lock, "Secure", Color(0xFF8B5CF6), Color(0xFFF5F3FF)),
            Tool("unlock", "Unlock", "Remove pass", "Remove Pass", Icons.Outlined.LockOpen, "Secure", Color(0xFF8B5CF6), Color(0xFFF5F3FF)),
            Tool("metadata", "Metadata", "Edit props", "Edit Metadata", Icons.Outlined.Fingerprint, "Secure", Color(0xFF8B5CF6), Color(0xFFF5F3FF)),
            
            Tool("pdf2img", "PDF to Img", "High-res export", "Export Assets", Icons.Outlined.BurstMode, "Convert", Color(0xFF10B981), Color(0xFFECFDF5)),
            Tool("img2pdf", "Img to PDF", "Build from photos", "Build PDF", Icons.Outlined.PictureAsPdf, "Convert", Color(0xFF10B981), Color(0xFFECFDF5)),
            Tool("extract-images", "Extract", "Strip assets", "Strip Raw", Icons.Outlined.Collections, "Convert", Color(0xFF10B981), Color(0xFFECFDF5)),
            Tool("pdf2text", "PDF to Text", "Extract text", "Plain Text", Icons.AutoMirrored.Outlined.Notes, "Convert", Color(0xFF10B981), Color(0xFFECFDF5))
        )
    }

    val essentialIds = listOf("merge", "split", "compress", "sign", "protect", "pdf2img")
    val categories = listOf("Edit", "Optimize", "Secure", "Convert")
    val catDescriptions = mapOf(
        "Edit" to "Organize & Modify",
        "Optimize" to "Shrink & Repair",
        "Secure" to "Protect & Unlock",
        "Convert" to "Export Assets"
    )
    val isDark = MaterialTheme.colorScheme.background == Color.Black

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .animateContentSize()
    ) {
        // Mode Title & Toggle
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (isExpanded) "ALL ENGINES" else "ESSENTIALS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color.Gray,
                letterSpacing = 1.2.sp
            )
            
            TextButton(onClick = { isExpanded = !isExpanded }) {
                Text(
                    if (isExpanded) "SHOW LESS" else "MORE TOOLS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = PaperPink
                )
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = PaperPink
                )
            }
        }

        if (!isExpanded) {
            // Mode 1: Compact Essentials (Grid of 6)
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                essentialIds.chunked(3).forEach { rowIds ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowIds.forEach { id ->
                            val tool = allTools.find { it.id == id }
                            if (tool != null) {
                                ModernToolItem(tool, isDark, Modifier.weight(1f), onToolClick)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        } else {
            // Mode 2: Full Categorized List
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                categories.forEach { category ->
                    item {
                        val catTools = allTools.filter { it.category == category }
                        val catColor = catTools.firstOrNull()?.color ?: PaperPink
                        
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    category.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = catColor,
                                    letterSpacing = 1.5.sp,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    catDescriptions[category] ?: "",
                                    fontSize = 8.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            
                            catTools.chunked(2).forEach { rowTools ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    rowTools.forEach { tool ->
                                        ModernToolItem(tool, isDark, Modifier.weight(1f), onToolClick)
                                    }
                                    if (rowTools.size == 1) Spacer(Modifier.weight(1f))
                                }
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
        // FIXED: Navbar bleed prevention
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun ModernToolItem(tool: Tool, isDark: Boolean, modifier: Modifier = Modifier, onClick: (String) -> Unit) {
    Surface(
        onClick = { onClick(tool.id) },
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) tool.color.copy(alpha = 0.08f) else tool.bgColor.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, tool.color.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(tool.color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(tool.icon ?: Icons.Filled.Build, null, modifier = Modifier.size(18.dp), tint = tool.color)
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    tool.name, 
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black, 
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.2).sp,
                    maxLines = 1
                )
                Text(
                    tool.subtitle.uppercase(), 
                    fontSize = 6.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = Color.Gray, 
                    letterSpacing = 0.5.sp,
                    maxLines = 1
                )
            }
        }
    }
}
