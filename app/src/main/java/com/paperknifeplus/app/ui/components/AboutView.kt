package com.paperknifeplus.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
                            "license" -> "License"
                            else -> "About"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Text("OFFLINE EDITION", fontSize = 8.sp, fontWeight = FontWeight.Black, color = PaperPink, letterSpacing = 1.sp)
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Logo(modifier = Modifier.size(64.dp), partColor = PaperPink)
                Spacer(Modifier.height(16.dp))
                Text("PaperKnife+", fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("OFFLINE ARCHITECTURE", fontSize = 9.sp, color = PaperPink, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("How it works", fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Unlike other PDF tools, PaperKnife+ processes everything on your device. Your documents never touch a server, ensuring 100% privacy and fast speed. No internet required, no data uploaded.",
                        fontSize = 13.sp, lineHeight = 18.sp, color = Color.Gray
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
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SocialIcon(Icons.Filled.Code, "GitHub", Color(0xFF24292E)) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/potatameister")))
                    }
                    SocialIcon(Icons.Filled.Public, "X", Color(0xFF000000)) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://x.com/potatameister")))
                    }
                    SocialIcon(Icons.Filled.Forum, "Discord", Color(0xFF5865F2)) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/7538nAWYx")))
                    }
                }
            }
        }

        item {
            AboutSection("LEGAL") {
                AboutMenuItem(Icons.Filled.Code, "Open Source Libraries", "The tech powering our engine") { onNavigate("libraries") }
                AboutMenuItem(Icons.Filled.Description, "Apache License 2.0", "Read the legal terms") { onNavigate("license") }
            }
        }
        
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("VERSION 1.0", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            }
        }
        
        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
fun SocialIcon(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }.padding(4.dp)) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color.copy(alpha = 0.1f), CircleShape)
                .border(1.dp, color.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
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
            
            SupportActionCard("Buy Me a Coffee", "Instant support for the developer", Icons.Filled.Coffee, Color(0xFFFFDD00)) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/potatameister")))
            }
            
            Spacer(Modifier.height(12.dp))

            SupportActionCard("GitHub Sponsors", "Join our developer community", Icons.Filled.Favorite, Color(0xFFEA4AAA)) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sponsors/potatameister")))
            }
        }
        
        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
fun SupportActionCard(title: String, subtitle: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = if (color == Color(0xFFFFDD00)) Color.Black else Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Text(subtitle, fontSize = 11.sp, color = Color.Gray)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = Color.Gray)
        }
    }
}

@Composable
fun LibrariesPage() {
    val libraries = listOf(
        "PDFBox-Android" to "The native heart of our engine. Apache 2.0",
        "Jetpack Compose" to "Modern UI toolkit for native Android.",
        "Coil" to "High-performance image loading engine.",
        "Material 3" to "Modern design system implementation.",
        "Kotlin Coroutines" to "Powering our multi-threaded rendering engine."
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("POWERED BY OPEN SOURCE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 1.2.sp)
        }
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
fun HallOfFamePage(onNavigate: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Icon(Icons.Filled.Star, null, modifier = Modifier.size(64.dp).alpha(0.1f), tint = PaperPink)
            Spacer(Modifier.height(16.dp))
            Text("The Legends", fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text("PEOPLE WHO MADE THIS POSSIBLE", fontSize = 8.sp, fontWeight = FontWeight.Black, color = PaperPink, letterSpacing = 1.5.sp)
        }
        
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HallSlot("Kalyan", "Me-Kalyan", Modifier.weight(1f))
                HallSlot("For the Planet!", "Plantbased4Future", Modifier.weight(1f))
            }
        }

        item {
            Surface(
                onClick = { onNavigate("support") },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                shape = RoundedCornerShape(20.dp),
                color = PaperPink.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, PaperPink.copy(alpha = 0.1f))
            ) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("JOIN THE LEGENDS", fontWeight = FontWeight.Black, fontSize = 12.sp, color = PaperPink)
                    Text("Support the project to see your name here!", fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
        }
        
        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
fun HallSlot(name: String, username: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.Gray.copy(0.1f)),
        modifier = modifier.aspectRatio(1.2f)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(name, fontSize = 14.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(username, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PaperPink, textAlign = TextAlign.Center)
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
                    Apache License
                    Version 2.0, January 2004
                    http://www.apache.org/licenses/

                    Licensed under the Apache License, Version 2.0 (the "License");
                    you may not use this file except in compliance with the License.
                    You may obtain a copy of the License at

                        http://www.apache.org/licenses/LICENSE-2.0

                    Unless required by applicable law or agreed to in writing, software
                    distributed under the License is distributed on an "AS IS" BASIS,
                    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
                    See the License for the specific language governing permissions and
                    limitations under the License.
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
