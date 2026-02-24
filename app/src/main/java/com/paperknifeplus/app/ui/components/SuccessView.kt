package com.paperknifeplus.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperknifeplus.app.ui.theme.PaperPink

@Composable
fun SuccessView(
    message: String = "Task Complete",
    subMessage: String = "Saved successfully",
    processingTime: String = "", 
    onDone: () -> Unit, 
    onProcessMore: () -> Unit,
    onPreview: (() -> Unit)? = null,
    showPreviewButton: Boolean = true,
    accentColor: Color = PaperPink
) {
    Column(
        modifier = Modifier.fillMaxSize(), 
        horizontalAlignment = Alignment.CenterHorizontally, 
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(100.dp), 
            shape = CircleShape, 
            color = Color(0xFF06D6A0).copy(alpha = 0.1f)
        ) {
            Icon(
                Icons.Filled.Check, 
                null, 
                tint = Color(0xFF06D6A0), 
                modifier = Modifier.padding(24.dp).size(48.dp)
            )
        }
        
        Spacer(Modifier.height(32.dp))
        
        Text(message, fontWeight = FontWeight.Black, fontSize = 28.sp, letterSpacing = (-0.5).sp)
        
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 8.dp)) {
            if (subMessage.isNotBlank()) {
                Text(subMessage, fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            }
            if (processingTime.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = Color(0xFF06D6A0).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "PROCESSED IN $processingTime", 
                        fontSize = 9.sp, 
                        fontWeight = FontWeight.Black, 
                        color = Color(0xFF06D6A0),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
        
        Spacer(Modifier.height(64.dp))
        
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onProcessMore, 
                modifier = Modifier.fillMaxWidth().height(60.dp), 
                shape = RoundedCornerShape(20.dp), 
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("PROCESS MORE FILES", fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 0.5.sp)
            }

            if (showPreviewButton) {
                OutlinedButton(
                    onClick = { onPreview?.invoke() },
                    enabled = onPreview != null,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (onPreview != null) accentColor.copy(alpha = 0.3f) else Color.Gray.copy(0.2f))
                ) {
                    Icon(
                        Icons.Filled.Visibility, 
                        null, 
                        modifier = Modifier.size(18.dp), 
                        tint = if (onPreview != null) accentColor else Color.Gray
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (onPreview != null) "OPEN PREVIEW" else "PREVIEW (UNAVAILABLE)", 
                        fontWeight = FontWeight.Bold, 
                        color = if (onPreview != null) accentColor else Color.Gray
                    )
                }
            }
            
            TextButton(
                onClick = onDone,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("BACK TO TOOLS CATALOG", fontWeight = FontWeight.Black, color = Color.Gray, fontSize = 11.sp, letterSpacing = 1.sp)
            }
        }
    }
}
