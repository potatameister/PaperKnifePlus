package com.paperknifeplus.app.ui.components

import androidx.compose.ui.graphics.vector.ImageVector

data class Tool(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector? = null,
    val category: String, // Edit, Secure, Convert, Optimize
    val color: androidx.compose.ui.graphics.Color,
    val bgColor: androidx.compose.ui.graphics.Color
)
