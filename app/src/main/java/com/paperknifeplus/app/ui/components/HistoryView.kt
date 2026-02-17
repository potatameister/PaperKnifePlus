package com.paperknifeplus.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    var searchQuery by remember { mutableStateOf("") }
    
    // Real apps would load this from a database. 
    // Currently using empty list as requested to avoid placeholders.
    val historyItems = emptyList<ActivityEntry>()

    val filteredItems = historyItems.filter { 
        it.name.contains(searchQuery, ignoreCase = true) || 
        it.tool.contains(searchQuery, ignoreCase = true) 
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
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
            
            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search sessions...", fontSize = 14.sp, fontWeight = FontWeight.Medium) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null, tint = Color.Gray) } },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = PaperPink,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )
        }
        
        Text(
            text = "ALL LOCAL SESSIONS",
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = Color.Gray,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp, start = 24.dp)
        )

        if (filteredItems.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.History, null, modifier = Modifier.size(64.dp).alpha(0.05f))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (searchQuery.isEmpty()) "No recent activity" else "No results for \"$searchQuery\"", 
                        color = Color.Gray, 
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredItems) { item ->
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
