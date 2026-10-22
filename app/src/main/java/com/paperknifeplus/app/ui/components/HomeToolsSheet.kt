package com.paperknifeplus.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val MAX_HOME_TOOLS = 8

/**
 * Full-screen Home tool editor.
 *
 * Tapping a tool adds or removes it. Selection order becomes Home order.
 * The default four remain available through RESET.
 */
@Composable
fun HomeToolsSheet(
    selectedIds: List<String>,
    onDismiss: () -> Unit,
    onChanged: (List<String>) -> Unit
) {
    BackHandler { onDismiss() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 12.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Home tools",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-0.5).sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "Choose up to $MAX_HOME_TOOLS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = "${selectedIds.size}/$MAX_HOME_TOOLS",
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }
                        }
                    }
                    val isDefault = selectedIds == ToolCatalog.core.map { it.id }
                    TextButton(
                        onClick = { onChanged(ToolCatalog.core.map { it.id }) },
                        enabled = !isDefault
                    ) {
                        Text(
                            text = "RESET",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDefault) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            },
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(ToolCatalog.all, key = { it.id }) { tool ->
                        val index = selectedIds.indexOf(tool.id)
                        val isSelected = index >= 0
                        val canToggle = if (isSelected) true else selectedIds.size < MAX_HOME_TOOLS

                        HomeToolCard(
                            tool = tool,
                            isSelected = isSelected,
                            order = index + 1,
                            enabled = canToggle,
                            onClick = {
                                if (isSelected) {
                                    onChanged(selectedIds.filterNot { it == tool.id })
                                } else {
                                    onChanged(selectedIds + tool.id)
                                }
                            }
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.background
                    )
                ) {
                    Text("DONE", fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 0.8.sp)
                }
            }
        }
}

@Composable
private fun HomeToolCard(
    tool: Tool,
    isSelected: Boolean,
    order: Int,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        } else if (isDark) {
            Color(0xFF0F0F12)
        } else {
            Color.White
        },
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
            }
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize().padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            if (isDark) tool.color.copy(alpha = 0.14f) else tool.bgColor,
                            RoundedCornerShape(11.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tool.icon ?: Icons.Filled.Add,
                        contentDescription = null,
                        tint = tool.color,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f).padding(end = 20.dp)) {
                    Text(
                        text = tool.name,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = tool.category.uppercase(),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = tool.color,
                        letterSpacing = 0.6.sp,
                        maxLines = 1
                    )
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(18.dp)
                        .background(MaterialTheme.colorScheme.onSurface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = order.toString(),
                        fontSize = 8.5.sp,
                        lineHeight = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.background,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
