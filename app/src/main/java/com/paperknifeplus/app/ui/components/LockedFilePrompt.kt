package com.paperknifeplus.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LockedFilePrompt(
    fileName: String, 
    password: String, 
    onPasswordChange: (String) -> Unit, 
    onUnlock: () -> Unit, 
    onCancel: () -> Unit,
    accentColor: Color = Color(0xFF6366F1)
) {
    Column(Modifier.fillMaxWidth().padding(top = 40.dp)) {
        Text("Encrypted File", fontWeight = FontWeight.Black, fontSize = 24.sp)
        Text(fileName, color = Color.Gray, fontSize = 14.sp)
        
        Spacer(Modifier.height(32.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Enter current password") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = accentColor,
                cursorColor = accentColor,
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent
            )
        )
        
        Spacer(Modifier.height(24.dp))
        
        Button(
            onClick = onUnlock, 
            modifier = Modifier.fillMaxWidth().height(56.dp), 
            shape = RoundedCornerShape(16.dp), 
            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
        ) {
            Text("Unlock to Proceed", fontWeight = FontWeight.Black, color = Color.White)
        }
        
        TextButton(
            onClick = onCancel, 
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("CANCEL", color = Color.Gray, fontWeight = FontWeight.Bold)
        }
    }
}
