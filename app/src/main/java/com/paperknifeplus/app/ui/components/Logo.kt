package com.paperknifeplus.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp

@Composable
fun PaperKnifeLogo(size: Int = 24, partColor: Color = Color.Black) {
    val roseColor = Color(0xFFF43F5E)
    Canvas(modifier = Modifier.size(size.dp)) {
        val width = size.dp.toPx()
        val height = size.dp.toPx()
        val scale = width / 24f

        // Top Part (Rose)
        val topPath = Path().apply {
            moveTo(4f * scale, 4f * scale)
            lineTo(21f * scale, 12f * scale)
            lineTo(9f * scale, 12f * scale)
            close()
        }
        drawPath(topPath, SolidColor(roseColor))

        // Bottom Part (Variable color)
        val bottomPath = Path().apply {
            moveTo(4f * scale, 20f * scale)
            lineTo(21f * scale, 12f * scale)
            lineTo(9f * scale, 12f * scale)
            close()
        }
        drawPath(bottomPath, SolidColor(partColor))
    }
}
