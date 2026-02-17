package com.paperknifeplus.app.ui.components

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.vector.ImageVector

data class ActivityEntry(
    val id: String,
    val name: String,
    val tool: String,
    val size: String,
    val icon: ImageVector
)

data class Tool(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector? = null,
    val category: String, // Edit, Secure, Convert, Optimize
    val color: androidx.compose.ui.graphics.Color,
    val bgColor: androidx.compose.ui.graphics.Color
)

object SessionManager {
    val history = mutableStateListOf<ActivityEntry>()
    
    fun addEntry(name: String, tool: String, size: String, icon: ImageVector) {
        history.add(0, ActivityEntry(
            id = System.currentTimeMillis().toString(),
            name = name,
            tool = tool,
            size = size,
            icon = icon
        ))
    }
}
