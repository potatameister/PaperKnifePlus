package com.paperknifeplus.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperknifeplus.app.ui.theme.PaperPink

@Composable
fun ToolsView(onToolClick: (String) -> Unit) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isListView by rememberSaveable { mutableStateOf(true) }

    val categories = ToolCatalog.categories

    val groupedTools = remember(searchQuery) {
        ToolCatalog.all
            .filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true) ||
                    it.subtitle.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true)
            }
            .groupBy { it.category }
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        ToolsHeader(
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            isListView = isListView,
            onToggleView = { isListView = !isListView }
        )

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            val columns = when {
                maxWidth >= 900.dp -> 5
                maxWidth >= 600.dp -> 4
                else -> 3
            }
            val horizontalPadding = if (maxWidth >= 600.dp) 24.dp else 16.dp
            val contentModifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 1100.dp)
                .fillMaxWidth()

            if (groupedTools.isEmpty()) {
                EmptyToolsState(searchQuery)
            } else {
                Crossfade(
                    targetState = isListView,
                    animationSpec = tween(180),
                    label = "tools-view"
                ) { listMode ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (listMode) {
                            LazyColumn(
                                modifier = contentModifier,
                                contentPadding = PaddingValues(
                                    start = horizontalPadding,
                                    end = horizontalPadding,
                                    top = 8.dp,
                                    bottom = 200.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categories.forEach { category ->
                                    val categoryTools = groupedTools[category] ?: emptyList()
                                    if (categoryTools.isNotEmpty()) {
                                        item(key = "header_list_$category") {
                                            CategoryHeader(category)
                                        }
                                        items(categoryTools, key = { it.id }) { tool ->
                                            PremiumListItem(
                                                tool = tool,
                                                onClick = { onToolClick(tool.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(columns),
                                modifier = contentModifier,
                                contentPadding = PaddingValues(
                                    start = horizontalPadding,
                                    end = horizontalPadding,
                                    top = 8.dp,
                                    bottom = 200.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                categories.forEach { category ->
                                    val categoryTools = groupedTools[category] ?: emptyList()
                                    if (categoryTools.isNotEmpty()) {
                                        item(
                                            key = "header_grid_$category",
                                            span = { GridItemSpan(maxLineSpan) }
                                        ) {
                                            CategoryHeader(category)
                                        }
                                        items(categoryTools, key = { it.id }) { tool ->
                                            PremiumGridItem(
                                                tool = tool,
                                                onClick = { onToolClick(tool.id) }
                                            )
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
}

@Composable
private fun ToolsHeader(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    isListView: Boolean,
    onToggleView: () -> Unit
) {
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
                onClick = onToggleView,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = CircleShape,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isListView) Icons.Outlined.GridView else Icons.Outlined.FormatListBulleted,
                    contentDescription = if (isListView) "Switch to grid view" else "Switch to list view",
                    modifier = Modifier.padding(8.dp).size(20.dp),
                    tint = PaperPink
                )
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
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(PaperPink),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Close, null, tint = Color.Gray, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(category: String) {
    Text(
        text = category.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        color = Color.Gray.copy(alpha = 0.65f),
        letterSpacing = 1.4.sp,
        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp, start = 4.dp)
    )
}

@Composable
private fun PremiumGridItem(tool: Tool, onClick: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val containerColor = if (isDark) Color(0xFF0F0F12) else Color.White
    val borderColor = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f)

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (isDark) tool.color.copy(alpha = 0.15f) else tool.bgColor,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon ?: Icons.Filled.Build,
                    contentDescription = null,
                    tint = tool.color,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = tool.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.2).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = tool.description.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                letterSpacing = 0.3.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PremiumListItem(tool: Tool, onClick: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val containerColor = if (isDark) Color(0xFF0F0F12) else Color.White
    val borderColor = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f)

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (isDark) tool.color.copy(alpha = 0.15f) else tool.bgColor,
                        RoundedCornerShape(13.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon ?: Icons.Filled.Build,
                    contentDescription = null,
                    tint = tool.color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tool.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tool.description,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Gray.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun EmptyToolsState(query: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = Color.Gray.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No tools found",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (query.isBlank()) "No tools available." else "Nothing matches \"$query\".",
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}
