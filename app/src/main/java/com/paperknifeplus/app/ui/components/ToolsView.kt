package com.paperknifeplus.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
        // EDIT
        Tool("merge", "Merge PDF", "Combine multiple PDFs", Icons.Default.Layers, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
        Tool("split", "Split PDF", "Extract specific pages", Icons.Default.ContentCut, "Edit", Color(0xFF3B82F6), Color(0xFFEFF6FF)),
        Tool("rotate", "Rotate PDF", "Fix orientation", Icons.Default.RotateRight, "Edit", Color(0xFFF97316), Color(0xFFFFF7ED)),
        Tool("rearrange", "Rearrange", "Reorder pages", Icons.AutoMirrored.Filled.List, "Edit", Color(0xFF10B981), Color(0xFFECFDF5)),
        Tool("page-numbers", "Page Numbers", "Add numbering", Icons.Default.FormatListNumbered, "Edit", Color(0xFF0EA5E9), Color(0xFFF0F9FF)),
        Tool("watermark", "Watermark", "Add text overlay", Icons.Default.TypeSpecimen, "Edit", Color(0xFFA855F7), Color(0xFFFAF5FF)),
        Tool("signature", "Signature", "Sign documents", Icons.Default.Draw, "Edit", Color(0xFFEC4899), Color(0xFFFDF2F8)),
        
        // OPTIMIZE
        Tool("compress", "Compress", "Optimize size", Icons.Default.Bolt, "Optimize", Color(0xFFF59E0B), Color(0xFFFFFBEB)),
        Tool("grayscale", "Grayscale", "Remove colors", Icons.Default.Palette, "Optimize", Color(0xFF71717A), Color(0xFFF4F4F5)),
        Tool("repair", "Repair PDF", "Fix corrupted files", Icons.Default.Build, "Optimize", Color(0xFFEF4444), Color(0xFFFEF2F2)),
        
        // SECURE
        Tool("protect", "Protect PDF", "Add password", Icons.Default.Lock, "Secure", Color(0xFF6366F1), Color(0xFFEEF2FF)),
        Tool("unlock", "Unlock PDF", "Remove password", Icons.Default.LockOpen, "Secure", Color(0xFF8B5CF6), Color(0xFFF5F3FF)),
        Tool("metadata", "Metadata", "Edit properties", Icons.Default.Fingerprint, "Secure", Color(0xFF06B6D4), Color(0xFFECFEFF)),
        
        // CONVERT
        Tool("pdf2img", "PDF to Image", "Export as JPG", Icons.Default.PictureAsPdf, "Convert", Color(0xFF84CC16), Color(0xFFF7FEE7)),
        Tool("img2pdf", "Image to PDF", "Photos to PDF", Icons.Default.Image, "Convert", Color(0xFF14B8A6), Color(0xFFF0FDFA)),
        Tool("extract-images", "Extract Images", "Save all images", Icons.Default.PhotoLibrary, "Convert", Color(0xFFEAB308), Color(0xFFFEFCE8)),
        Tool("pdf2text", "PDF to Text", "Extract plain text", Icons.Default.TextFields, "Convert", Color(0xFF2563EB), Color(0xFFEFF6FF))
    )

    val categories = listOf("Edit", "Optimize", "Secure", "Convert")

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "All Tools",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-1.5).sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        categories.forEach { category ->
            item {
                Text(
                    text = "${category.uppercase()} TOOLS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Gray,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp, start = 4.dp)
                )
            }
            
            items(allTools.filter { it.category == category }) { tool ->
                ListToolItem(tool = tool, onClick = { onToolClick(tool.id) })
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun ListToolItem(tool: Tool, onClick: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF09090B) else Color.White
        ),
        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) tool.color.copy(alpha = 0.12f) else tool.bgColor
            ) {
                Icon(
                    imageVector = tool.icon ?: Icons.Default.Build,
                    contentDescription = null,
                    tint = tool.color,
                    modifier = Modifier.padding(12.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tool.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = tool.description,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 0.2.sp
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
