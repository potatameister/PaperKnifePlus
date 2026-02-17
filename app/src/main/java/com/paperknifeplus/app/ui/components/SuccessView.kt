package com.paperknifeplus.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperknifeplus.app.ui.theme.PaperPink

@Composable
fun SuccessView(
    fileName: String, 
    path: String, 
    processingTime: String = "", 
    onDone: () -> Unit, 
    onProcessMore: () -> Unit,
    accentColor: Color = PaperPink
) {
    Column(
        modifier = Modifier.fillMaxSize(), 
        horizontalAlignment = Alignment.CenterHorizontally, 
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp), 
            shape = CircleShape, 
            color = Color(0xFF06D6A0).copy(alpha = 0.1f)
        ) {
            Icon(Icons.Default.Check, null, tint = Color(0xFF06D6A0), modifier = Modifier.padding(20.dp))
        }
        
        Spacer(Modifier.height(24.dp))
        
        Text("Task Complete", fontWeight = FontWeight.Black, fontSize = 24.sp)
        
        if (processingTime.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                "PROCESSED IN $processingTime", 
                fontSize = 9.sp, 
                fontWeight = FontWeight.Black, 
                color = Color(0xFF06D6A0),
                letterSpacing = 1.sp
            )
        }
        
        Spacer(Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
        ) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.FileOpen, null, modifier = Modifier.size(32.dp).alpha(0.3f))
                Spacer(Modifier.height(12.dp))
                Text(
                    text = fileName, 
                    fontWeight = FontWeight.Bold, 
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    maxLines = 1
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).background(Color(0xFF06D6A0), CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text("Saved successfully", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = path, 
                        fontSize = 10.sp, 
                        color = Color.Gray, 
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
        
        Spacer(Modifier.height(48.dp))
        
        Button(
            onClick = onProcessMore, 
            modifier = Modifier.fillMaxWidth().height(56.dp), 
            shape = RoundedCornerShape(16.dp), 
            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
        ) {
            Text("PROCESS MORE FILES", fontWeight = FontWeight.Black, color = Color.White)
        }
        
        Spacer(Modifier.height(12.dp))
        
        OutlinedButton(
            onClick = onDone, 
            modifier = Modifier.fillMaxWidth().height(56.dp), 
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
        ) {
            Text("DONE", fontWeight = FontWeight.Bold, color = accentColor)
        }
    }
}
