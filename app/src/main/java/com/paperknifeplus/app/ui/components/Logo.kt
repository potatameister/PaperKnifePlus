package com.paperknifeplus.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import com.paperknifeplus.app.ui.theme.PaperPink

import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp

@Composable
fun Logo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        // Red Arrow Logo (Matching screenshot style)
        val path = Path().apply {
            moveTo(0f, height * 0.1f)
            lineTo(width, height * 0.5f)
            lineTo(0f, height * 0.9f)
            lineTo(width * 0.3f, height * 0.5f)
            close()
        }
        drawPath(path, SolidColor(PaperPink))
    }
}

// Legacy support
@Composable
fun PaperKnifeLogo(size: Int = 24, partColor: androidx.compose.ui.graphics.Color = PaperPink) {
    Logo(modifier = Modifier.size(size.dp))
}
