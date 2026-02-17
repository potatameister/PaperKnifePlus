package com.paperknifeplus.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
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
fun HistoryView() {
    val historyItems = listOf(
        ActivityEntry("1", "Resume_Final.pdf", "Compress", "1.2 MB", Icons.Outlined.Description),
        ActivityEntry("2", "Invoice_Feb.pdf", "Merge", "0.8 MB", Icons.Outlined.Description),
        ActivityEntry("3", "Tax_Return_2025.pdf", "Protect", "2.4 MB", Icons.Outlined.Description),
        ActivityEntry("4", "Trip_Photos.pdf", "Img2Pdf", "15.6 MB", Icons.Outlined.Description)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(PaperPink.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.History, null, tint = PaperPink, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "History",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-1.5).sp
            )
        }
        
        Text(
            text = "ALL LOCAL SESSIONS",
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = Color.Gray,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(top = 24.dp, bottom = 16.dp, start = 4.dp)
        )

        if (historyItems.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No recent activity", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(historyItems) { item ->
                    HistoryItem(item)
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
fun HistoryItem(entry: ActivityEntry) {
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF09090B) else Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = PaperPink.copy(alpha = 0.1f)
            ) {
                Icon(entry.icon, null, tint = PaperPink, modifier = Modifier.padding(10.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("${entry.tool} • ${entry.size}", fontSize = 11.sp, color = Color.Gray)
            }
            Text(
                "DONE",
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF06D6A0),
                modifier = Modifier
                    .background(Color(0xFF06D6A0).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
