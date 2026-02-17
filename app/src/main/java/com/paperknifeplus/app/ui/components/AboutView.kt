package com.paperknifeplus.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Update
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
fun AboutView() {
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Logo(modifier = Modifier.size(28.dp), partColor = if (isDark) Color.White else Color.Black)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "About",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-1.5).sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "WHAT IS PAPERKNIFE+ ?",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = PaperPink,
            letterSpacing = 1.5.sp
        )
        
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF09090B) else Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color.White.copy(0.05f) else Color.Black.copy(0.03f))
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = "PaperKnife+ is the native Android successor to the original PaperKnife. Built from the ground up with Kotlin and Jetpack Compose, it brings desktop-class PDF manipulation to your thumb.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(16.dp))
                FeatureRow(Icons.Outlined.Security, "100% Offline", "Processing happens entirely on your device. Your data never leaves your hand.")
                FeatureRow(Icons.Outlined.Update, "Native Performance", "Zero-server architecture powered by the high-performance PDFBox-Android engine.")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "HOW IT WORKS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = PaperPink,
            letterSpacing = 1.5.sp
        )

        val toolGuide = listOf(
            "Merge" to "Combines multiple PDF files into one continuous document while maintaining layout integrity.",
            "Protect" to "Applies standard 128-bit AES encryption to your document, requiring a password for any future access.",
            "Unlock" to "Removes all security restrictions and passwords from a PDF, provided you know the original password.",
            "Text Extraction" to "Parses the underlying data layer of the PDF to recover raw plain text for easy copying."
        )

        toolGuide.forEach { (title, desc) ->
            Column(Modifier.padding(vertical = 12.dp, horizontal = 4.dp)) {
                Text(title, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Text(desc, fontSize = 12.sp, color = Color.Gray, lineHeight = 18.sp)
            }
        }

        Spacer(Modifier.height(120.dp))
    }
}

@Composable
fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Row(Modifier.padding(vertical = 8.dp)) {
        Icon(icon, null, tint = PaperPink, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(desc, fontSize = 11.sp, color = Color.Gray, lineHeight = 16.sp)
        }
    }
}
