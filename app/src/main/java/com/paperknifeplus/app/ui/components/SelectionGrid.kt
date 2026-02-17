package com.paperknifeplus.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperknifeplus.app.ui.theme.PaperPink

@Composable
fun SelectionGrid(
    onSelect: () -> Unit, 
    isDark: Boolean, 
    icon: ImageVector, 
    title: String, 
    subtitle: String,
    accentColor: Color = Color.Gray,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(if (isDark) Color(0xFF09090B) else Color.White)
            .border(BorderStroke(1.dp, Color.Gray.copy(0.1f)), RoundedCornerShape(32.dp))
            .clickable { onSelect() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon, 
                contentDescription = null, 
                modifier = Modifier.size(64.dp).alpha(0.1f), 
                tint = accentColor
            )
            Spacer(Modifier.height(16.dp))
            Text(title, fontWeight = FontWeight.Black, fontSize = 18.sp, color = if (isDark) Color.LightGray else Color.DarkGray)
            Text(
                subtitle, 
                fontSize = 10.sp, 
                fontWeight = FontWeight.Black, 
                color = Color.Gray.copy(alpha = 0.6f), 
                letterSpacing = 1.sp
            )
        }
    }
}
