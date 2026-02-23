package com.paperknifeplus.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
fun ToolPickerContent(initialExpanded: Boolean = false, onToolClick: (String) -> Unit) {
    var isExpanded by remember { mutableStateOf(initialExpanded) }
    
    val quickTools = remember {
        listOf(
            Tool("merge", "Merge", "COMBINE", Icons.Outlined.Layers, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            Tool("split", "Split", "EXTRACT", Icons.Outlined.ContentCut, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            Tool("rearrange", "Rearrange", "ORGANIZE", Icons.Outlined.SwapVert, "Edit", Color(0xFF10B981), Color(0xFFECFDF5)),
            Tool("compress", "Compress", "OPTIMIZE", Icons.Outlined.Bolt, "Optimize", Color(0xFFF59E0B), Color(0xFFFFFBEB)),
            Tool("sign", "Sign", "SIGNATURE", Icons.Outlined.Draw, "Edit", Color(0xFF6366F1), Color(0xFFEEF2FF)),
            Tool("watermark", "Watermark", "BRANDING", Icons.Outlined.BrandingWatermark, "Security", Color(0xFF8B5CF6), Color(0xFFF5F3FF)),
            Tool("pdf2img", "To Image", "EXPORT", Icons.Outlined.BurstMode, "Convert", Color(0xFFEC4899), Color(0xFFFDF2F8)),
            Tool("img2pdf", "To PDF", "BUILD", Icons.Outlined.PictureAsPdf, "Convert", Color(0xFF14B8A6), Color(0xFFF0FDFA))
        )
    }

    val allTools = remember {
        listOf(
            Tool("merge", "Merge", "Combine PDFs", Icons.Outlined.Layers, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            Tool("split", "Split", "Extract pages", Icons.Outlined.ContentCut, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            Tool("delete", "Delete", "Remove pages", Icons.Outlined.Delete, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            Tool("rearrange", "Rearrange", "Sort pages", Icons.Outlined.SwapVert, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            Tool("rotate", "Rotate", "Fix orientation", Icons.Outlined.RotateRight, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            Tool("sign", "Sign", "Add signature", Icons.Outlined.Draw, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            Tool("watermark", "Watermark", "Add overlay", Icons.Outlined.TextFields, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            Tool("page-numbers", "Numbers", "Add pagination", Icons.Outlined.FormatListNumbered, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            Tool("bookmarks", "Bookmarks", "Edit bookmarks", Icons.Outlined.BookmarkBorder, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            Tool("compress", "Compress", "Small size", Icons.Outlined.Bolt, "Optimize", Color(0xFFF59E0B), Color(0xFFFFFBEB)),
            Tool("grayscale", "Grayscale", "Gray tones", Icons.Outlined.Palette, "Optimize", Color(0xFFF59E0B), Color(0xFFFFFBEB)),
            Tool("repair", "Repair", "Fix corruption", Icons.Outlined.Build, "Optimize", Color(0xFFF59E0B), Color(0xFFFFFBEB)),
            Tool("compare", "Compare", "Visual diff", Icons.Outlined.Compare, "Optimize", Color(0xFFF59E0B), Color(0xFFFFFBEB)),
            Tool("protect", "Lock", "Password", Icons.Outlined.Lock, "Secure", Color(0xFF6366F1), Color(0xFFEEF2FF)),
            Tool("unlock", "Unlock", "Remove pass", Icons.Outlined.LockOpen, "Secure", Color(0xFF6366F1), Color(0xFFEEF2FF)),
            Tool("metadata", "Metadata", "Edit props", Icons.Outlined.Fingerprint, "Secure", Color(0xFF6366F1), Color(0xFFEEF2FF)),
            Tool("pdf2img", "PDF to Image", "High-res export", Icons.Outlined.BurstMode, "Convert", Color(0xFF14B8A6), Color(0xFFF0FDFA)),
            Tool("img2pdf", "Image to PDF", "Build from photos", Icons.Outlined.PictureAsPdf, "Convert", Color(0xFF14B8A6), Color(0xFFF0FDFA)),
            Tool("pdf2zip", "PDF to ZIP", "Archive for sharing", Icons.Outlined.FolderZip, "Convert", Color(0xFF14B8A6), Color(0xFFF0FDFA)),
            Tool("extract-images", "Extract Image", "Extract images from PDFs", Icons.Outlined.Collections, "Convert", Color(0xFF14B8A6), Color(0xFFF0FDFA)),
            Tool("pdf2text", "PDF to Text", "Extract plain text", Icons.Outlined.Article, "Convert", Color(0xFF14B8A6), Color(0xFFF0FDFA))
        )
    }

    val groupedTools = remember(allTools) { allTools.groupBy { it.category } }
    val isDark = MaterialTheme.colorScheme.background == Color.Black

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp)) {
                Text(if (isExpanded) "ALL ENGINES" else "QUICK TOOLS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 1.2.sp)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { isExpanded = !isExpanded }) {
                    Text(if (isExpanded) "SHOW LESS" else "SEE MORE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = PaperPink)
                }
            }
        }

        if (!isExpanded) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    quickTools.take(4).forEach { tool ->
                        ToolPickerItem(tool, isDark, Modifier.weight(1f), onToolClick)
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    quickTools.drop(4).forEach { tool ->
                        ToolPickerItem(tool, isDark, Modifier.weight(1f), onToolClick)
                    }
                }
            }
        } else {
            groupedTools.keys.forEach { category ->
                item {
                    Text(category.uppercase(), fontSize = 8.sp, fontWeight = FontWeight.Black, color = PaperPink, letterSpacing = 1.5.sp)
                }
                val toolsInCategory = groupedTools[category] ?: emptyList()
                items(toolsInCategory.chunked(4)) { rowTools ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        rowTools.forEach { tool ->
                            ToolPickerItem(tool, isDark, Modifier.weight(1f), onToolClick)
                        }
                        repeat(4 - rowTools.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
fun ToolPickerItem(tool: Tool, isDark: Boolean, modifier: Modifier = Modifier, onClick: (String) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable { onClick(tool.id) }
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) tool.color.copy(alpha = 0.15f) else tool.bgColor,
            border = BorderStroke(1.dp, tool.color.copy(0.1f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(tool.icon ?: Icons.Filled.Build, null, modifier = Modifier.size(24.dp), tint = tool.color)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(tool.name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}
