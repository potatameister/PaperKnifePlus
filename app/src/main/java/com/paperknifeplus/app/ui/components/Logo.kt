package com.paperknifeplus.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.paperknifeplus.app.ui.theme.PaperPink

@Composable
fun Logo(
    modifier: Modifier = Modifier,
    partColor: Color = if (isSystemInDarkTheme()) Color.White else Color(0xFF18181B)
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val scale = width / 24f
        
        // Exact PaperKnife Logo Paths from Reference
        
        // Top Part (Iconic Red)
        val topPath = Path().apply {
            moveTo(4f * scale, 4f * scale)
            lineTo(21f * scale, 12f * scale)
            lineTo(9f * scale, 12f * scale)
            close()
        }
        drawPath(topPath, SolidColor(PaperPink))

        // Bottom Part (Contrast Color)
        val bottomPath = Path().apply {
            moveTo(4f * scale, 20f * scale)
            lineTo(21f * scale, 12f * scale)
            lineTo(9f * scale, 12f * scale)
            close()
        }
        drawPath(bottomPath, SolidColor(partColor))
    }
}

@Composable
fun PaperKnifeLogo(size: Int = 24) {
    Logo(modifier = Modifier.size(size.dp))
}
