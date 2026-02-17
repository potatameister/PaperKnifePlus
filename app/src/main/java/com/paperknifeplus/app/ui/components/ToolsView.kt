package com.paperknifeplus.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ToolsView(onToolClick: (String) -> Unit) {
    val allTools = listOf(
        Tool("merge", "Merge PDF", "Combine multiple PDFs", Icons.Default.Layers, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
        Tool("split", "Split PDF", "Extract specific pages", Icons.Default.ContentCut, "Edit", Color(0xFF3B82F6), Color(0xFFEFF6FF)),
        Tool("compress", "Compress", "Optimize file size", Icons.Default.Bolt, "Optimize", Color(0xFFF59E0B), Color(0xFFFFFBEB)),
        Tool("protect", "Protect PDF", "Add password security", Icons.Default.Lock, "Secure", Color(0xFF6366F1), Color(0xFFEEF2FF)),
        Tool("unlock", "Unlock PDF", "Remove PDF password", Icons.Default.LockOpen, "Secure", Color(0xFF8B5CF6), Color(0xFFF5F3FF)),
        Tool("rotate", "Rotate PDF", "Fix page orientation", Icons.Default.RotateRight, "Edit", Color(0xFFF97316), Color(0xFFFFF7ED)),
        Tool("rearrange", "Rearrange", "Reorder PDF pages", Icons.AutoMirrored.Filled.List, "Edit", Color(0xFF10B981), Color(0xFFECFDF5)),
        Tool("watermark", "Watermark", "Add text overlay", Icons.Default.TypeSpecimen, "Edit", Color(0xFFA855F7), Color(0xFFFAF5FF)),
        Tool("metadata", "Metadata", "Edit PDF properties", Icons.Default.Info, "Secure", Color(0xFF06B6D4), Color(0xFFECFEFF)),
        Tool("img2pdf", "Image to PDF", "Convert photos to PDF", Icons.Default.Image, "Convert", Color(0xFF14B8A6), Color(0xFFF0FDFA)),
        Tool("pdf2img", "PDF to Image", "Export pages as JPG", Icons.Default.PictureAsPdf, "Convert", Color(0xFF84CC16), Color(0xFFF7FEE7)),
        Tool("signature", "Signature", "Sign your documents", Icons.Default.Edit, "Edit", Color(0xFFEC4899), Color(0xFFFDF2F8)),
        Tool("grayscale", "Grayscale", "Remove PDF colors", Icons.Default.Palette, "Optimize", Color(0xFF71717A), Color(0xFFF4F4F5)),
        Tool("repair", "Repair PDF", "Fix corrupted files", Icons.Default.Build, "Optimize", Color(0xFFEF4444), Color(0xFFFEF2F2))
    )

    val categories = listOf("Edit", "Optimize", "Secure", "Convert")

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(2) }) {
            Text(
                text = "Tool Catalog",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        categories.forEach { category ->
            item(span = { GridItemSpan(2) }) {
                Text(
                    text = category.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.Gray,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 4.dp)
                )
            }
            
            items(allTools.filter { it.category == category }) { tool ->
                SmallBentoCard(tool = tool, onClick = { onToolClick(tool.id) })
            }
        }

        item(span = { GridItemSpan(2) }) {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun SmallBentoCard(tool: Tool, onClick: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF09090B) else Color.White
        ),
        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (isDark) tool.color.copy(alpha = 0.15f) else tool.bgColor
            ) {
                Icon(
                    imageVector = tool.icon ?: Icons.Default.Build,
                    contentDescription = null,
                    tint = tool.color,
                    modifier = Modifier.padding(7.dp)
                )
            }
            
            Column {
                Text(
                    text = tool.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = tool.description,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    maxLines = 1,
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
}
