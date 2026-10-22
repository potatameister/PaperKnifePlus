package com.paperknifeplus.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperknifeplus.app.ui.theme.PaperPink

/**
 * Full-screen task launcher used by the center "+" button.
 */
@Composable
fun ToolLauncher(
    onDismiss: () -> Unit,
    onToolClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { }
    ) {
        ToolPickerBody(
            modifier = Modifier.fillMaxSize(),
            showClose = true,
            onClose = onDismiss,
            onToolClick = onToolClick
        )
    }
}

/**
 * Sheet variant kept for the in-viewer "open in another tool" action.
 */
@Composable
fun ToolPickerContent(onToolClick: (String) -> Unit) {
    ToolPickerBody(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .navigationBarsPadding(),
        showClose = false,
        onClose = {},
        onToolClick = onToolClick
    )
}

@Composable
private fun ToolPickerBody(
    modifier: Modifier,
    showClose: Boolean,
    onClose: () -> Unit,
    onToolClick: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val allTools = ToolCatalog.all
    val activeCategory = selectedCategory
    val filteredTools = remember(query, activeCategory) {
        allTools.filter { tool ->
            val inCategory = activeCategory == null || tool.category == activeCategory
            val matches = query.isBlank() ||
                tool.name.contains(query, true) ||
                tool.description.contains(query, true) ||
                tool.subtitle.contains(query, true) ||
                tool.category.contains(query, true) ||
                tool.id.contains(query, true)
            inCategory && matches
        }
    }
    val isSearching = query.isNotBlank()
    val isDark = MaterialTheme.colorScheme.background == Color.Black

    BoxWithConstraints(modifier = modifier) {
        val columns = if (maxWidth >= 700.dp) 3 else 2

        Column(modifier = Modifier.fillMaxSize()) {
            LauncherHeader(
                toolCount = allTools.size,
                query = query,
                onQueryChange = { query = it },
                showClose = showClose,
                onClose = onClose
            )

            CategoryFilterRow(
                selectedCategory = selectedCategory,
                onSelect = { selectedCategory = it }
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 10.dp,
                    bottom = 32.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when {
                    isSearching -> {
                        if (filteredTools.isEmpty()) {
                            item(
                                span = { GridItemSpan(maxLineSpan) },
                                key = "empty"
                            ) {
                                EmptySearchState()
                            }
                        } else {
                            item(
                                span = { GridItemSpan(maxLineSpan) },
                                key = "result-count"
                            ) {
                                ResultLabel(
                                    count = filteredTools.size,
                                    category = activeCategory
                                )
                            }
                            items(filteredTools, key = { it.id }) { tool ->
                                ToolGridCard(
                                    tool = tool,
                                    isDark = isDark,
                                    onClick = { onToolClick(tool.id) }
                                )
                            }
                        }
                    }

                    activeCategory != null -> {
                        item(
                            span = { GridItemSpan(maxLineSpan) },
                            key = "category-intro"
                        ) {
                            CategoryIntro(
                                category = activeCategory,
                                count = filteredTools.size
                            )
                        }
                        items(filteredTools, key = { it.id }) { tool ->
                            ToolGridCard(
                                tool = tool,
                                isDark = isDark,
                                onClick = { onToolClick(tool.id) }
                            )
                        }
                    }

                    else -> {
                        item(
                            span = { GridItemSpan(maxLineSpan) },
                            key = "popular-label"
                        ) {
                            SectionLabel("Popular")
                        }
                        item(
                            span = { GridItemSpan(maxLineSpan) },
                            key = "popular-row"
                        ) {
                            PopularToolsRow(
                                isDark = isDark,
                                onToolClick = onToolClick
                            )
                        }

                        ToolCatalog.categories.forEach { category ->
                            val categoryTools = allTools.filter { it.category == category }
                            if (categoryTools.isNotEmpty()) {
                                item(
                                    span = { GridItemSpan(maxLineSpan) },
                                    key = "header-$category"
                                ) {
                                    CategoryHeader(
                                        category = category,
                                        count = categoryTools.size
                                    )
                                }
                                items(categoryTools, key = { it.id }) { tool ->
                                    ToolGridCard(
                                        tool = tool,
                                        isDark = isDark,
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

@Composable
private fun LauncherHeader(
    toolCount: Int,
    query: String,
    onQueryChange: (String) -> Unit,
    showClose: Boolean,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (showClose) Modifier.statusBarsPadding() else Modifier)
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "New task",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.6).sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$toolCount tools · 100% offline",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    letterSpacing = 0.2.sp
                )
            }

            if (showClose) {
                Surface(
                    onClick = onClose,
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close tool picker",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                    modifier = Modifier.size(19.dp)
                )
                Spacer(Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search tools...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
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
                if (query.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .clickable { onQueryChange("") },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    selectedCategory: String?,
    onSelect: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CategoryChip(
            label = "All",
            selected = selectedCategory == null,
            color = PaperPink,
            onClick = { onSelect(null) }
        )
        ToolCatalog.categories.forEach { category ->
            CategoryChip(
                label = category,
                selected = selectedCategory == category,
                color = categoryColor(category),
                onClick = { onSelect(category) }
            )
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) color else Color.Transparent,
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        border = if (selected) null else BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PopularToolsRow(
    isDark: Boolean,
    onToolClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ToolCatalog.essentials.forEach { tool ->
            PopularToolCard(
                tool = tool,
                isDark = isDark,
                onClick = { onToolClick(tool.id) }
            )
        }
    }
}

@Composable
private fun PopularToolCard(
    tool: Tool,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(104.dp)
            .height(94.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (isDark) tool.color.copy(alpha = 0.10f) else tool.bgColor.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, tool.color.copy(alpha = 0.16f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(tool.color.copy(alpha = 0.16f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = tool.icon ?: Icons.Filled.Build,
                    contentDescription = null,
                    tint = tool.color,
                    modifier = Modifier.size(17.dp)
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
        }
    }
}

@Composable
private fun ToolGridCard(
    tool: Tool,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(98.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) Color(0xFF0F0F12) else Color.White,
        border = BorderStroke(
            1.dp,
            if (isDark) Color.White.copy(alpha = 0.06f) else tool.color.copy(alpha = 0.10f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
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
                androidx.compose.material3.Icon(
                    imageVector = tool.icon ?: Icons.Filled.Build,
                    contentDescription = null,
                    tint = tool.color,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(Modifier.height(7.dp))
            Text(
                text = tool.name,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = tool.description,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        modifier = Modifier.padding(start = 2.dp, top = 2.dp),
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        letterSpacing = 1.3.sp
    )
}

@Composable
private fun ResultLabel(count: Int, category: String?) {
    val suffix = if (count == 1) "RESULT" else "RESULTS"
    val scope = category?.let { " IN ${it.uppercase()}" } ?: ""
    Text(
        text = "$count $suffix$scope",
        modifier = Modifier.padding(start = 2.dp, top = 2.dp),
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        letterSpacing = 1.3.sp
    )
}

@Composable
private fun CategoryHeader(category: String, count: Int) {
    val color = categoryColor(category)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp, top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = category.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = color,
            letterSpacing = 1.3.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$count",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        )
    }
}

@Composable
private fun CategoryIntro(category: String, count: Int) {
    val color = categoryColor(category)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp, top = 2.dp, bottom = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(color, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = category,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$count tools",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = ToolCatalog.categoryDescriptions[category] ?: "",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun EmptySearchState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "No tools found",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "Try a different word or category.",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }
    }
}

private fun categoryColor(category: String): Color =
    ToolCatalog.all.firstOrNull { it.category == category }?.color ?: PaperPink
