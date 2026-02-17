package com.paperknifeplus.app.ui.components

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
    fileName: String, 
    path: String = "", 
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
        
        Spacer(Modifier.height(48.dp))
        
        // Minimal Display of Processed File
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = fileName, 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Box(Modifier.size(4.dp).background(Color(0xFF06D6A0), CircleShape))
                Spacer(Modifier.width(6.dp))
                Text("Saved to Storage", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(Modifier.height(64.dp))

        // New Future Button
        OutlinedButton(
            onClick = { /* Future functionality */ },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(0.2f))
        ) {
            Icon(Icons.Default.Visibility, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Text("PREVIEW (COMING SOON)", fontWeight = FontWeight.Bold, color = Color.Gray)
        }
        
        Spacer(Modifier.height(12.dp))
        
        Button(
            onClick = onProcessMore, 
            modifier = Modifier.fillMaxWidth().height(56.dp), 
            shape = RoundedCornerShape(16.dp), 
            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
        ) {
            Text("PROCESS MORE FILES", fontWeight = FontWeight.Black, color = Color.White)
        }
        
        Spacer(Modifier.height(12.dp))
        
        TextButton(onClick = onDone) {
            Text("BACK TO TOOLS", fontWeight = FontWeight.Bold, color = Color.Gray)
        }
    }
}
