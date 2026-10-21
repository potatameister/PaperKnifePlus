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

    val allTools = ToolCatalog.all
    val categories = ToolCatalog.categories
    val catDescriptions = ToolCatalog.categoryDescriptions
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
                ToolCatalog.essentials.chunked(3).forEach { rowTools ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowTools.forEach { tool ->
                            ModernToolItem(tool, isDark, Modifier.weight(1f), onToolClick)
                        }
                        repeat(3 - rowTools.size) {
                            Spacer(Modifier.weight(1f))
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
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = catColor,
                                    letterSpacing = 1.5.sp,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    catDescriptions[category] ?: "",
                                    fontSize = 10.sp,
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
                    fontSize = 10.sp,
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
