package com.paperknifeplus.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
fun SuccessView(fileName: String, path: String, onDone: () -> Unit, onProcessMore: () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Surface(Modifier.size(80.dp), shape = CircleShape, color = Color(0xFF06D6A0).copy(alpha = 0.1f)) {
            Icon(Icons.Default.Check, null, tint = Color(0xFF06D6A0), modifier = Modifier.padding(20.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Task Complete", fontWeight = FontWeight.Black, fontSize = 24.sp)
        Spacer(Modifier.height(8.dp))
        Text(fileName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text("is successfully saved at", fontSize = 12.sp, color = Color.Gray)
        Text(path, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        
        Spacer(Modifier.height(48.dp))
        
        Button(onClick = onProcessMore, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = PaperPink)) {
            Text("PROCESS MORE FILES", fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) {
            Text("DONE", fontWeight = FontWeight.Bold)
        }
    }
}
