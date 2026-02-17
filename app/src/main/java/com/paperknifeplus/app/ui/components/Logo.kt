package com.paperknifeplus.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.paperknifeplus.app.ui.theme.PaperPink

@Composable
fun Logo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        // Correct Sharp PaperKnife Arrow Logo
        val path = Path().apply {
            moveTo(0f, height * 0.15f)
            lineTo(width, height * 0.5f)
            lineTo(0f, height * 0.85f)
            lineTo(width * 0.35f, height * 0.5f)
            close()
        }
        drawPath(path, SolidColor(PaperPink))
    }
}

@Composable
fun PaperKnifeLogo(size: Int = 24) {
    Logo(modifier = Modifier.size(size.dp))
}
