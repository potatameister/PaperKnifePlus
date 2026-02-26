package com.paperknifeplus.app.ui.components

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.vector.ImageVector

data class ActivityEntry(
    val id: String,
    val name: String,
    val tool: String,
    val size: String,
    val icon: ImageVector,
    val uri: Uri? = null,
    val pageCount: Int = 0
)

data class Tool(
    val id: String,
    val name: String,
    val description: String,
    val subtitle: String = "",
    val icon: ImageVector? = null,
    val category: String, // Edit, Secure, Convert, Optimize
    val color: androidx.compose.ui.graphics.Color,
    val bgColor: androidx.compose.ui.graphics.Color
)

object SessionManager {
    val history = mutableStateListOf<ActivityEntry>()
    
    fun addEntry(name: String, tool: String, size: String, icon: ImageVector, uri: Uri? = null, pageCount: Int = 0) {
        history.add(0, ActivityEntry(
            id = System.currentTimeMillis().toString(),
            name = name,
            tool = tool,
            size = size,
            icon = icon,
            uri = uri,
            pageCount = pageCount
        ))
    }

    fun purgeHistory(retentionDays: Int) {
        if (retentionDays <= 0) return
        val cutoff = System.currentTimeMillis() - (retentionDays.toLong() * 24 * 60 * 60 * 1000)
        history.removeAll { entry ->
            val timestamp = entry.id.toLongOrNull() ?: 0L
            timestamp < cutoff
        }
    }

    fun clearHistory() {
        history.clear()
    }
}
