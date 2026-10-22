package com.paperknifeplus.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared tool-screen top bar.
 *
 * Title, subtitle and accent color come from [ToolCatalog], so every tool
 * header stays in sync with Home, the Tools page and the task picker.
 */
@Composable
fun ToolTopBar(
    toolId: String,
    onBack: () -> Unit,
    subtitleOverride: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    showAction: Boolean = true,
    actionEnabled: Boolean = true
) {
    val tool = ToolCatalog.byId[toolId]
    val accentColor = tool?.color ?: MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tool?.name ?: "",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitleOverride ?: tool?.headerSubtitle ?: "",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = accentColor,
                    letterSpacing = 1.sp
                )
            }
            if (showAction && actionLabel != null && onAction != null) {
                TextButton(onClick = onAction, enabled = actionEnabled) {
                    Text(
                        text = actionLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = if (actionEnabled) accentColor else Color.Gray
                    )
                }
            }
        }
    }
}
