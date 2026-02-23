package com.paperknifeplus.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperknifeplus.app.ui.theme.PaperPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutView(initialPage: String = "main", onBack: () -> Unit) {
    val context = LocalContext.current
    var currentSubPage by remember { mutableStateOf(initialPage) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (currentSubPage == "main") onBack() else currentSubPage = "main" },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = when(currentSubPage) {
                            "support" -> "Support Us"
                            "libraries" -> "Open Source"
                            "hall" -> "Hall of Fame"
                            else -> "About"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Text("PLATINUM EDITION", fontSize = 8.sp, fontWeight = FontWeight.Black, color = PaperPink, letterSpacing = 1.sp)
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when(currentSubPage) {
                "main" -> AboutMain(onNavigate = { currentSubPage = it })
                "support" -> SupportPage()
                "libraries" -> LibrariesPage()
                "hall" -> HallOfFamePage()
            }
        }
    }
}

@Composable
fun AboutMain(onNavigate: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Logo(modifier = Modifier.size(64.dp), partColor = PaperPink)
                Spacer(Modifier.height(16.dp))
                Text("PaperKnife+", fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("v1.0.0 (Platinum)", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }

        item {
            AboutSection("COMMUNITY") {
                AboutMenuItem(Icons.Filled.Favorite, "Support PaperKnife+", "Keep us ad-free and open") { onNavigate("support") }
                AboutMenuItem(Icons.Filled.Star, "Hall of Fame", "Our amazing supporters") { onNavigate("hall") }
            }
        }

        item {
            AboutSection("LEGAL & CREDITS") {
                AboutMenuItem(Icons.Filled.Code, "Open Source Libraries", "The tech powering our engine") { onNavigate("libraries") }
                AboutMenuItem(Icons.Filled.Shield, "Privacy Policy", "100% Local processing details") { /* Link */ }
                AboutMenuItem(Icons.Filled.Description, "License", "Apache License 2.0") { /* Link */ }
            }
        }
        
        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
fun SupportPage() {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                "PaperKnife+ is a mission to build the world's most powerful, private, and 100% free PDF utility. No ads, no tracking, no server costs.",
                fontSize = 15.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PaperPink.copy(0.1f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, PaperPink.copy(0.2f))
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("THE PLAY STORE PLAN", fontWeight = FontWeight.Black, fontSize = 12.sp, color = PaperPink)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "We need your help to reach the Google Play Store! Your support covers developer fees, hosting for the website, and ensures PaperKnife+ stays updated forever.",
                        fontSize = 13.sp, lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Text("WAYS TO HELP", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(8.dp))
            
            // BUY ME A COFFEE (TOP)
            Surface(
                onClick = { 
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/potatameister")))
                    } catch (e: Exception) {}
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFFFDD00).copy(alpha = 0.1f),
                border = BorderStroke(1.dp, Color(0xFFFFDD00).copy(alpha = 0.2f))
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).background(Color(0xFFFFDD00), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Coffee, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Buy Me a Coffee", fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Text("Instant support for the developer", fontSize = 11.sp, color = Color.Gray)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = Color.Gray)
                }
            }
            
            Spacer(Modifier.height(12.dp))

            // GITHUB SPONSORS
            Surface(
                onClick = { 
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sponsors/potatameister")))
                    } catch (e: Exception) {}
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFEA4AAA).copy(alpha = 0.1f),
                border = BorderStroke(1.dp, Color(0xFFEA4AAA).copy(alpha = 0.2f))
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).background(Color(0xFFEA4AAA), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Favorite, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("GitHub Sponsors", fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Text("Join our developer community", fontSize = 11.sp, color = Color.Gray)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = Color.Gray)
                }
            }
        }
        
        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
fun LibrariesPage() {
    val libraries = listOf(
        "PDFBox-Android" to "The native heart of our engine. Apache 2.0",
        "Jetpack Compose" to "Modern UI toolkit for native Android.",
        "Coil" to "High-performance image loading & Nitro cache.",
        "Material 3" to "Modern design system implementation.",
        "Kotlin Coroutines" to "Powering our multi-threaded Nitro rendering."
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(libraries) { lib ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(lib.first, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(lib.second, fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
        
        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
fun HallOfFamePage() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Star, null, modifier = Modifier.size(64.dp).alpha(0.1f), tint = PaperPink)
        Spacer(Modifier.height(24.dp))
        Text("Our First Supporters", fontWeight = FontWeight.Black, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text("Your name could be here!", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(32.dp))
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HallSlot("Support to join", Modifier.weight(1f))
            HallSlot("Support to join", Modifier.weight(1f))
        }
        
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun HallSlot(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(0.2f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.Gray.copy(0.1f)),
        modifier = modifier.aspectRatio(1.5f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray.copy(0.5f))
        }
    }
}

@Composable
fun AboutSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 1.2.sp, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(0.2f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.Gray.copy(0.1f))
        ) {
            Column { content() }
        }
    }
}

@Composable
fun AboutMenuItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(36.dp).background(PaperPink.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = PaperPink)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, fontSize = 11.sp, color = Color.Gray)
        }
        Icon(Icons.Filled.ChevronRight, null, modifier = Modifier.size(16.dp), tint = Color.Gray.copy(0.5f))
    }
}
