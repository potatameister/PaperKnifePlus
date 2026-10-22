package com.paperknifeplus.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperknifeplus.app.ui.theme.PaperPink
import java.util.Collections

private const val MAX_HOME_TOOLS = 8

/**
 * Lets users choose, remove and reorder the tools shown in Home's CORE TOOLS grid.
 * The current four tools remain the default.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeToolsSheet(
    selectedIds: List<String>,
    onDismiss: () -> Unit,
    onChanged: (List<String>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selected = selectedIds.mapNotNull { ToolCatalog.byId[it] }
    val available = ToolCatalog.all.filter { it.id !in selectedIds }
    val isDark = MaterialTheme.colorScheme.background == Color.Black

    fun move(index: Int, delta: Int) {
        val target = index + delta
        if (target in selectedIds.indices) {
            val mutable = selectedIds.toMutableList()
            Collections.swap(mutable, index, target)
            onChanged(mutable)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray.copy(alpha = 0.2f)) },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Customize Home",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "${selected.size} of $MAX_HOME_TOOLS core tools",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                TextButton(onClick = { onChanged(ToolCatalog.core.map { it.id }) }) {
                    Text(
                        text = "RESET",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = PaperPink,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            Text(
                text = "Choose up to $MAX_HOME_TOOLS tools. The default four can be replaced or reduced.",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "shown-label") {
                    SectionLabel("Shown on Home")
                }
                items(selected, key = { "shown-${it.id}" }) { tool ->
                    SelectedToolRow(
                        tool = tool,
                        isDark = isDark,
                        canMoveUp = selected.indexOf(tool) > 0,
                        canMoveDown = selected.indexOf(tool) < selected.lastIndex,
                        canRemove = selected.size > 1,
                        onMoveUp = { move(selected.indexOf(tool), -1) },
                        onMoveDown = { move(selected.indexOf(tool), 1) },
                        onRemove = {
                            onChanged(selectedIds.filterNot { it == tool.id })
                        }
                    )
                }

                item(key = "available-label") {
                    SectionLabel("Add more tools")
                }
                if (available.isEmpty()) {
                    item(key = "all-added") {
                        Text(
                            text = "All tools are already on Home.",
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    items(available, key = { "available-${it.id}" }) { tool ->
                        AvailableToolRow(
                            tool = tool,
                            isDark = isDark,
                            canAdd = selected.size < MAX_HOME_TOOLS,
                            onAdd = { onChanged(selectedIds + tool.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedToolRow(
    tool: Tool,
    isDark: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canRemove: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    ToolPickerSurface(tool = tool, isDark = isDark, modifier = Modifier.height(64.dp)) {
        Row(
            modifier = Modifier.fillMaxSize().padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolIcon(tool, isDark)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tool.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tool.category.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = tool.color,
                    letterSpacing = 0.6.sp
                )
            }
            IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = "Move up",
                    tint = if (canMoveUp) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else Color.Gray.copy(alpha = 0.2f),
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Move down",
                    tint = if (canMoveDown) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else Color.Gray.copy(alpha = 0.2f),
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onRemove, enabled = canRemove, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = Icons.Filled.RemoveCircleOutline,
                    contentDescription = "Remove from Home",
                    tint = if (canRemove) MaterialTheme.colorScheme.error.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.2f),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@Composable
private fun AvailableToolRow(
    tool: Tool,
    isDark: Boolean,
    canAdd: Boolean,
    onAdd: () -> Unit
) {
    ToolPickerSurface(
        tool = tool,
        isDark = isDark,
        modifier = Modifier.height(60.dp).clickable(enabled = canAdd) { onAdd() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolIcon(tool, isDark)
            Spacer(Modifier.width(10.dp))
            Text(
                text = tool.name,
                modifier = Modifier.weight(1f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = if (canAdd) "Add to Home" else "Home is full",
                tint = if (canAdd) tool.color else Color.Gray.copy(alpha = 0.25f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ToolPickerSurface(
    tool: Tool,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF0F0F12) else Color.White,
        border = BorderStroke(
            1.dp,
            if (isDark) Color.White.copy(alpha = 0.06f) else tool.color.copy(alpha = 0.10f)
        )
    ) {
        content()
    }
}

@Composable
private fun ToolIcon(tool: Tool, isDark: Boolean) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(
                if (isDark) tool.color.copy(alpha = 0.14f) else tool.bgColor,
                RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = tool.icon ?: Icons.Filled.Add,
            contentDescription = null,
            tint = tool.color,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        letterSpacing = 1.2.sp
    )
}
