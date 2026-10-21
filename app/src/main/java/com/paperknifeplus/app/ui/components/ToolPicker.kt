package com.paperknifeplus.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperknifeplus.app.ui.theme.PaperPink

@Composable
fun ToolPickerContent(onToolClick: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val allTools = ToolCatalog.all
    val filteredTools = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else allTools.filter {
            it.name.contains(searchQuery, true) ||
                it.description.contains(searchQuery, true) ||
                it.subtitle.contains(searchQuery, true) ||
                it.category.contains(searchQuery, true)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.88f)
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
    ) {
        val allColumns = if (maxWidth >= 520.dp) 3 else 2
        val quickColumns = if (maxWidth >= 520.dp) 6 else 3

        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    text = "Choose a tool",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Start a new task",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(48.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Search, null, tint = Color.Gray, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text("Search tools...", color = Color.Gray, fontSize = 13.sp)
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(PaperPink),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Filled.Close, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (searchQuery.isNotBlank()) {
                if (filteredTools.isEmpty()) {
                    EmptySearchState(Modifier.weight(1f))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            SectionLabel("${filteredTools.size} RESULT${if (filteredTools.size == 1) "" else "S"}")
                        }
                        items(filteredTools.chunked(allColumns)) { rowTools ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowTools.forEach { tool ->
                                    AllToolCard(tool, isDark, Modifier.weight(1f)) { onToolClick(tool.id) }
                                }
                                repeat(allColumns - rowTools.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { SectionLabel("QUICK ACCESS") }
                    items(ToolCatalog.essentials.chunked(quickColumns)) { rowTools ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowTools.forEach { tool ->
                                QuickToolCard(tool, isDark, Modifier.weight(1f)) { onToolClick(tool.id) }
                            }
                            repeat(quickColumns - rowTools.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    ToolCatalog.categories.forEach { category ->
                        val catTools = allTools.filter { it.category == category }
                        if (catTools.isNotEmpty()) {
                            item {
                                CategoryHeader(
                                    title = category,
                                    subtitle = ToolCatalog.categoryDescriptions[category] ?: "",
                                    color = catTools.first().color
                                )
                            }
                            items(catTools.chunked(allColumns)) { rowTools ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowTools.forEach { tool ->
                                        AllToolCard(tool, isDark, Modifier.weight(1f)) { onToolClick(tool.id) }
                                    }
                                    repeat(allColumns - rowTools.size) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        color = Color.Gray,
        letterSpacing = 1.4.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp, start = 2.dp)
    )
}

@Composable
private fun CategoryHeader(title: String, subtitle: String, color: Color) {
    Row(
        modifier = Modifier.padding(top = 14.dp, bottom = 2.dp, start = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = color,
            letterSpacing = 1.4.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = subtitle,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
    }
}

@Composable
private fun QuickToolCard(
    tool: Tool,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(94.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) tool.color.copy(alpha = 0.08f) else tool.bgColor.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, tool.color.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(tool.color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon ?: Icons.Filled.Build,
                    contentDescription = null,
                    tint = tool.color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = tool.name,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = tool.subtitle.uppercase(),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                letterSpacing = 0.2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AllToolCard(
    tool: Tool,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF0F0F12) else Color.White,
        border = BorderStroke(
            1.dp,
            if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (isDark) tool.color.copy(alpha = 0.15f) else tool.bgColor,
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon ?: Icons.Filled.Build,
                    contentDescription = null,
                    tint = tool.color,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tool.name,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tool.description,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptySearchState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = Color.Gray.copy(alpha = 0.4f),
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "No tools found",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Try a different search term.",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}
