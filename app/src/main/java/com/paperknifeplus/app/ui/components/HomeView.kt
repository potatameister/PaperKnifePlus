package com.paperknifeplus.app.ui.components

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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeView(onToolClick: (String) -> Unit) {
    val tools = listOf(
        // Edit
        Tool("merge", "Merge", "Combine files", Icons.Default.CallMerge, "Edit", Color(0xFFE3F2FD)),
        Tool("split", "Split", "Extract pages", Icons.Default.CallSplit, "Edit", Color(0xFFF1F8E9)),
        Tool("rotate", "Rotate", "Fix orientation", Icons.Default.RotateRight, "Edit", Color(0xFFFFF3E0)),
        Tool("rearrange", "Rearrange", "Reorder pages", Icons.Default.ViewQuilt, "Edit", Color(0xFFF3E5F5)),
        
        // Secure
        Tool("protect", "Protect", "Add password", Icons.Default.Lock, "Secure", Color(0xFFFFEBEE)),
        Tool("unlock", "Unlock", "Remove security", Icons.Default.LockOpen, "Secure", Color(0xFFE0F2F1)),
        
        // Convert
        Tool("img2pdf", "Img to PDF", "Photos to PDF", Icons.Default.Image, "Convert", Color(0xFFFFF8E1)),
        Tool("pdf2img", "PDF to Img", "Export images", Icons.Default.PictureAsPdf, "Convert", Color(0xFFE8EAF6)),
        Tool("pdf2text", "PDF to Text", "Extract text", Icons.Default.TextFields, "Convert", Color(0xFFFBE9E7))
    )

    val categories = listOf("Edit", "Secure", "Convert")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PaperKnifeLogo(size = 28, partColor = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "PaperKnife+",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        Text(
            "Privacy-first PDF utility",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 100.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            categories.forEach { category ->
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                items(tools.filter { it.category == category }) { tool ->
                    BentoCard(tool, onClick = { onToolClick(tool.id) })
                }
            }
        }
    }
}

@Composable
fun BentoCard(tool: Tool, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = tool.color),
        modifier = Modifier
            .aspectRatio(1.1f)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Icon(
                imageVector = tool.icon ?: Icons.Default.Build,
                contentDescription = null,
                tint = Color.Black.copy(alpha = 0.8f),
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.TopStart)
            )
            
            Column(
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Text(
                    text = tool.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Text(
                    text = tool.description,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    color = Color.Black.copy(alpha = 0.6f)
                )
            }
        }
    }
}
