package com.paperknifeplus.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeView(onToolClick: (String) -> Unit) {
    val tools = listOf(
        Tool("merge", "Merge", "Combine files", Icons.Default.CallMerge, "Edit", Color(0xFFE3F2FD)),
        Tool("split", "Split", "Extract pages", Icons.Default.CallSplit, "Edit", Color(0xFFF1F8E9)),
        Tool("rotate", "Rotate", "Fix orientation", Icons.Default.RotateRight, "Edit", Color(0xFFFFF3E0)),
        Tool("rearrange", "Rearrange", "Reorder pages", Icons.Default.ViewQuilt, "Edit", Color(0xFFF3E5F5)),
        Tool("protect", "Protect", "Add password", Icons.Default.Lock, "Secure", Color(0xFFFFEBEE)),
        Tool("unlock", "Unlock", "Remove security", Icons.Default.LockOpen, "Secure", Color(0xFFE0F2F1)),
        Tool("img2pdf", "Img to PDF", "Photos to PDF", Icons.Default.Image, "Convert", Color(0xFFFFF8E1)),
        Tool("pdf2img", "PDF to Img", "Export images", Icons.Default.PictureAsPdf, "Convert", Color(0xFFE8EAF6))
    )

    val categories = listOf("Edit", "Secure", "Convert")

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PaperKnifeLogo(size = 28, partColor = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.width(12.dp))
            Text("PaperKnife+", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        }
        
        Text("Privacy-first PDF utility", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 100.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            categories.forEach { category ->
                item(span = { GridItemSpan(2) }) {
                    Text(category.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                }
                items(tools.filter { it.category == category }) { tool ->
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = tool.color),
                        modifier = Modifier.aspectRatio(1.1f).clickable { onToolClick(tool.id) }
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Icon(tool.icon ?: Icons.Default.Build, null, tint = Color.Black.copy(0.8f), modifier = Modifier.size(32.dp).align(Alignment.TopStart))
                            Column(Modifier.align(Alignment.BottomStart)) {
                                Text(tool.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                                Text(tool.description, fontSize = 11.sp, color = Color.Black.copy(0.6f))
                            }
                        }
                    }
                }
            }
        }
    }
}
