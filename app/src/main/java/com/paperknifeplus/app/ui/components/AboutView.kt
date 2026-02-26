package com.paperknifeplus.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import com.paperknifeplus.app.R
import com.paperknifeplus.app.ui.theme.PaperPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutView(initialPage: String = "main", isFromSettings: Boolean = false, onBack: () -> Unit) {
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
                    onClick = { 
                        if (currentSubPage == "main") {
                            onBack()
                        } else {
                            if (isFromSettings && (currentSubPage == "hall" || currentSubPage == "support")) {
                                onBack()
                            } else {
                                currentSubPage = "main"
                            }
                        }
                    },
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
                            "license" -> "License"
                            else -> "About"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Text("SECURE PDF ENGINE", fontSize = 8.sp, fontWeight = FontWeight.Black, color = PaperPink, letterSpacing = 1.sp)
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when(currentSubPage) {
                "main" -> AboutMain(onNavigate = { currentSubPage = it })
                "support" -> SupportPage()
                "libraries" -> LibrariesPage()
                "hall" -> HallOfFamePage(onNavigate = { currentSubPage = it })
                "license" -> LicensePage()
            }
        }
    }
}

@Composable
fun AboutMain(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Logo(modifier = Modifier.size(64.dp), partColor = if (isDark) Color.White else Color.Black)
                Spacer(Modifier.height(16.dp))
                Text("PaperKnife+", fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("NATIVE PDF WORKSHOP", fontSize = 9.sp, color = PaperPink, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("How it works", fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "PaperKnife+ is built on a simple but powerful idea: your documents belong to you, and they should never have to leave your device to be modified.\n\nTraditional PDF tools often require you to upload your files to their servers. This means your private contracts, bank statements, or personal letters are being sent over the internet and processed on a computer you don't control. Even if they promise to delete the data, the risk remains.\n\nWe took a different path. We built a 'Native Engine' that brings the workshop to your phone. When you open a PDF in PaperKnife+, our app reads the file's structure directly from your storage. When you sign a document, merge files, or extract images, all the heavy lifting is done by your phone's processor. It's like having a professional printing press and a secure vault built right into your pocket.\n\nThis approach doesn't just protect your privacy; it makes the app incredibly fast and reliable. Because there's no uploading or downloading, tools work instantly. You can edit a document on a plane, in a remote area without a signal, or in a high-security environment with zero connectivity. Your data starts on your device and ends on your device—exactly where it belongs.",
                        fontSize = 13.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
                    Spacer(Modifier.height(20.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Security, null, tint = PaperPink, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("PRIVACY BY DESIGN", fontWeight = FontWeight.Black, fontSize = 11.sp, color = PaperPink, letterSpacing = 1.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "• 100% local processing — files never leave your device\n• No servers, no cloud, no uploads\n• No tracking, no analytics, no telemetry\n• Works completely offline\n• Your files, your processor, your device",
                        fontSize = 12.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Code, null, tint = PaperPink, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("FULLY OPEN SOURCE", fontWeight = FontWeight.Black, fontSize = 11.sp, color = PaperPink, letterSpacing = 1.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "• All code available on GitHub\n• Anyone can audit the source\n• No hidden tracking or data collection\n• Community-driven development\n• Security researchers can verify claims",
                        fontSize = 12.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        item {
            AboutSection("COMMUNITY") {
                AboutMenuItem(Icons.Filled.Favorite, "Support PaperKnife+", "Help us reach the Play Store") { onNavigate("support") }
                AboutMenuItem(Icons.Filled.Star, "Hall of Fame", "Our amazing supporters") { onNavigate("hall") }
            }
        }

        item {
            AboutSection("CONNECT") {
                val isDarkTheme = MaterialTheme.colorScheme.background == Color.Black
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SocialIcon(painterResource(R.drawable.ic_github), "GitHub", if (isDarkTheme) Color.White else Color(0xFF24292E)) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/potatameister")))
                    }
                    SocialIcon(painterResource(R.drawable.ic_x), "X", if (isDarkTheme) Color.White else Color(0xFF000000)) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://x.com/potatameister")))
                    }
                    SocialIcon(painterResource(R.drawable.ic_discord), "Discord", Color(0xFF5865F2)) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/7538nAWYx")))
                    }
                }
            }
        }

        item {
            AboutSection("LEGAL") {
                AboutMenuItem(Icons.Filled.Code, "Open Source Libraries", "The tech powering our engine") { onNavigate("libraries") }
                AboutMenuItem(Icons.Filled.Description, "GPL v3", "Read the legal terms") { onNavigate("license") }
            }
        }
        
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PaperKnife+ V1.0", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            }
        }
        
        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
fun SocialIcon(painter: androidx.compose.ui.graphics.painter.Painter, label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }.padding(4.dp)) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color.copy(alpha = 0.1f), CircleShape)
                .border(1.dp, color.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(painter, null, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
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
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = PaperPink.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, PaperPink.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Store, null, tint = PaperPink, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("THE PLAY STORE PLAN", fontWeight = FontWeight.Black, fontSize = 11.sp, color = PaperPink, letterSpacing = 1.sp)
                    }
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
            
            val isDark = MaterialTheme.colorScheme.background == Color.Black
            val coffeeBg = if (isDark) Color(0xFF3D3520) else Color(0xFFFFF9C4)
            val coffeeBorder = if (isDark) Color(0xFF5D5020) else Color(0xFFFFEB3B)
            val coffeeIconBg = if (isDark) Color(0xFF5D5020) else Color(0xFFFFEB3B)
            
            Surface(
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/potatameister"))) },
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = coffeeBg,
                border = BorderStroke(1.dp, coffeeBorder)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).background(coffeeIconBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Coffee, null, tint = if (isDark) Color(0xFFFFE082) else Color(0xFF795548), modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Buy Me a Coffee", fontWeight = FontWeight.Black, fontSize = 15.sp, color = if (isDark) Color(0xFFFFE082) else MaterialTheme.colorScheme.onSurface)
                        Text("Instant support for the developer", fontSize = 11.sp, color = Color.Gray)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = Color.Gray)
                }
            }
        }
        
        item {
            Surface(
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sponsors/potatameister"))) },
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFEA4AAA).copy(alpha = 0.1f),
                border = BorderStroke(1.dp, Color(0xFFEA4AAA).copy(alpha = 0.3f))
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).background(Color(0xFFEA4AAA), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Settings, null, tint = PaperPink, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("CORE ENGINE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = PaperPink, letterSpacing = 1.2.sp)
            }
        }
        
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier.size(36.dp).background(PaperPink.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Description, null, tint = PaperPink, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("PDFBox-Android", fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("[Apache 2.0]", fontSize = 9.sp, color = PaperPink, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("The native heart of our engine", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }
        
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier.size(36.dp).background(PaperPink.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Bolt, null, tint = PaperPink, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Kotlin Coroutines", fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("[Apache 2.0]", fontSize = 9.sp, color = PaperPink, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Powering our multi-threaded rendering engine", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }
        
        item {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Palette, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("UI & DESIGN", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF8B5CF6), letterSpacing = 1.2.sp)
            }
        }
        
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier.size(36.dp).background(Color(0xFF8B5CF6).copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.TouchApp, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Jetpack Compose", fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("[Apache 2.0]", fontSize = 9.sp, color = Color(0xFF8B5CF6), fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Modern UI toolkit for native Android", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }
        
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier.size(36.dp).background(Color(0xFF8B5CF6).copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Style, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Material 3", fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("[Apache 2.0]", fontSize = 9.sp, color = Color(0xFF8B5CF6), fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Modern design system implementation", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }
        
        item {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Image, null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("MEDIA", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981), letterSpacing = 1.2.sp)
            }
        }
        
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier.size(36.dp).background(Color(0xFF10B981).copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Coil", fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("[MIT]", fontSize = 9.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("High-performance image loading engine", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }
        
        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
fun HallOfFamePage(onNavigate: (String) -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "star")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Star, null,
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .alpha(glowAlpha),
                    tint = PaperPink.copy(alpha = 0.4f)
                )
                Icon(
                    Icons.Filled.Star, null,
                    modifier = Modifier
                        .size(64.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale },
                    tint = PaperPink
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("The Legends", fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text("PEOPLE WHO MADE THIS POSSIBLE", fontSize = 8.sp, fontWeight = FontWeight.Black, color = PaperPink, letterSpacing = 1.5.sp)
        }
        
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HallSlot("Kalyan", "Me-Kalyan", Color(0xFFF43F5E), Modifier.weight(1f))
                HallSlot("For the Planet!", "Plantbased4Future", Color(0xFF10B981), Modifier.weight(1f))
            }
        }

        item {
            Surface(
                onClick = { onNavigate("support") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = PaperPink.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(20.dp),
                color = PaperPink.copy(alpha = 0.08f),
                border = BorderStroke(1.5.dp, PaperPink.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Celebration, null, tint = PaperPink, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("JOIN THE LEGENDS", fontWeight = FontWeight.Black, fontSize = 13.sp, color = PaperPink)
                        Text("Support the project to see your name here!", fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        
        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
fun HallSlot(name: String, username: String, accentColor: Color, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
        modifier = modifier.aspectRatio(1.2f)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Favorite, null, tint = accentColor.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(name, fontSize = 14.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Spacer(Modifier.height(2.dp))
            Text(username, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun LicensePage() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp)
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    """
                    GNU GENERAL PUBLIC LICENSE
                    Version 3, 29 June 2007
                    
                    Copyright (C) 2007 Free Software Foundation, Inc.
                    https://www.gnu.org/licenses/gpl-3.0.html
                    
                    This program is free software: you can redistribute it
                    and/or modify it under the terms of the GNU General Public
                    License as published by the Free Software Foundation,
                    either version 3 of the License
                    any later version.
                    
                    This program is distributed in the hope that it will be
                    useful, but WITHOUT ANY WARRANTY; without even the implied
                    warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
                    PURPOSE. See the GNU General Public License for more details.
                    
                    You should have received a copy of the GNU General Public
                    License along with this program. If not, see
                    https://www.gnu.org/licenses/.
                    """.trimIndent(),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(20.dp),
                    color = Color.Gray
                )
            }
        }
        item { Spacer(Modifier.height(100.dp)) }
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
fun AboutMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
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
