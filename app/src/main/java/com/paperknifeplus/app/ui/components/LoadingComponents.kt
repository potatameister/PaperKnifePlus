package com.paperknifeplus.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Bitmap

@Composable
fun LoadingStateView(accentColor: Color, showWarning: Boolean, text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            CircularProgressIndicator(color = accentColor)
            Spacer(Modifier.height(24.dp))
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            
            AnimatedVisibility(visible = showWarning, enter = fadeIn(), exit = fadeOut()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(16.dp))
                    Surface(color = accentColor.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Info, null, tint = accentColor, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Processing may take a moment depending on the device.", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProcessingStateView(
    accentColor: Color, 
    preview: Bitmap?,
    text: String,
    current: Int,
    total: Int,
    showWarning: Boolean
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Card(
                modifier = Modifier.size(180.dp, 240.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(0.1f))
            ) {
                if (preview != null) {
                    Image(bitmap = preview.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Box(Modifier.fillMaxSize().background(Color.Gray.copy(0.1f)))
                }
            }
            
            Spacer(Modifier.height(32.dp))
            CircularProgressIndicator(color = accentColor)
            Spacer(Modifier.height(24.dp))
            
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.Black)
            
            if (total > 0) {
                Spacer(Modifier.height(8.dp))
                Text("Page $current of $total", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            }

            AnimatedVisibility(visible = showWarning, enter = fadeIn(), exit = fadeOut()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(24.dp))
                    Surface(color = accentColor.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Info, null, tint = accentColor, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("This is a complex operation. Please keep the app open.", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}
