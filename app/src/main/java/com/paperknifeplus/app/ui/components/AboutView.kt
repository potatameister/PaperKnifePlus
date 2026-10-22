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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import com.paperknifeplus.app.R
import com.paperknifeplus.app.ui.theme.PaperPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutView(initialPage: String = "main", isFromSettings: Boolean = false, onBack: () -> Unit) {
    var currentSubPage by remember { mutableStateOf(initialPage) }
    val title = when (currentSubPage) {
        "hall" -> "Hall of Fame"
        "libraries" -> "Open Source"
        "license" -> "License"
        else -> "About"
    }
    val subtitle = when (currentSubPage) {
        "hall" -> "OUR SUPPORTERS"
        "libraries" -> "BUILT WITH"
        "license" -> "LEGAL"
        else -> "PRIVATE PDF WORKSHOP"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val fromSettingsRoot = isFromSettings && currentSubPage == initialPage
                        if (currentSubPage == "main" || fromSettingsRoot) {
                            onBack()
                        } else {
                            currentSubPage = "main"
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.4).sp)
                        Text(
                            subtitle,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            letterSpacing = 1.2.sp
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (currentSubPage) {
                "main" -> AboutMain(onNavigate = { currentSubPage = it })
                "hall" -> HallOfFamePage()
                "libraries" -> LibrariesPage()
                "license" -> LicensePage()
            }
        }
    }
}

@Composable
fun AboutMain(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val accent = Color(0xFF14B8A6)

    fun openUrl(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Logo(modifier = Modifier.size(64.dp), partColor = if (isDark) Color.White else Color.Black)
                Spacer(Modifier.height(14.dp))
                Text("PaperKnife+", fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.8).sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "PRIVATE PDF WORKSHOP",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = accent,
                    letterSpacing = 1.8.sp
                )
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        "V1.2 · ${ToolCatalog.all.size} tools · 100% offline",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.06f))
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(accent.copy(alpha = 0.10f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Security, null, tint = accent, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Built for privacy", fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Text("Your files never leave this device.", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No accounts, no uploads, no tracking. Every tool runs entirely on your phone using the native PDF engine. Your documents stay private, and the app works even without a connection.",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniFeature("Offline", Icons.Filled.WifiOff, accent, Modifier.weight(1f))
                        MiniFeature("No tracking", Icons.Filled.VisibilityOff, accent, Modifier.weight(1f))
                        MiniFeature("Open source", Icons.Filled.Code, accent, Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            AboutSection("COMMUNITY") {
                AboutMenuItem(
                    Icons.Filled.Favorite,
                    "Support PaperKnife+",
                    "Help keep the workshop free",
                    accentColor = Color(0xFFE11D48)
                ) {
                    openUrl("https://potatameister.github.io/support")
                }
                AboutMenuItem(
                    Icons.Filled.Star,
                    "Hall of Fame",
                    "21 supporters who made this possible",
                    accentColor = Color(0xFFFFC107)
                ) { onNavigate("hall") }
            }
        }

        item {
            AboutSection("CONNECT") {
                val socialDark = MaterialTheme.colorScheme.background == Color.Black
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SocialIcon(painterResource(R.drawable.ic_github), "GitHub", if (socialDark) Color.White else Color(0xFF24292E)) {
                        openUrl("https://github.com/potatameister")
                    }
                    SocialIcon(painterResource(R.drawable.ic_x), "X", if (socialDark) Color.White else Color(0xFF000000)) {
                        openUrl("https://x.com/potatameister")
                    }
                    SocialIcon(painterResource(R.drawable.ic_discord), "Discord", Color(0xFF5865F2)) {
                        openUrl("https://discord.gg/7538nAWYx")
                    }
                }
            }
        }

        item {
            AboutSection("LEGAL") {
                AboutMenuItem(
                    Icons.Filled.Code,
                    "Open Source Libraries",
                    "The tech powering the engine",
                    accentColor = accent
                ) { onNavigate("libraries") }
                AboutMenuItem(
                    Icons.Filled.Description,
                    "GPL v3",
                    "Read the license",
                    accentColor = accent
                ) { onNavigate("license") }
            }
        }

        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "PaperKnife+ V1.2",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Made with care, entirely offline.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                )
            }
        }
    }
}

@Composable
private fun MiniFeature(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = tint.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = tint, maxLines = 1)
        }
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
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
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
                            Text("[Apache 2.0]", fontSize = 10.sp, color = PaperPink, fontWeight = FontWeight.Bold)
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
                            Text("[Apache 2.0]", fontSize = 10.sp, color = PaperPink, fontWeight = FontWeight.Bold)
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
                            Text("[Apache 2.0]", fontSize = 10.sp, color = Color(0xFF8B5CF6), fontWeight = FontWeight.Bold)
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
                            Text("[Apache 2.0]", fontSize = 10.sp, color = Color(0xFF8B5CF6), fontWeight = FontWeight.Bold)
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
                            Text("[MIT]", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
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
fun HallOfFamePage() {
    val context = LocalContext.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val columns = if (maxWidth >= 700.dp) 3 else 2
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "hall-header") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFFFFC107).copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Hall of Fame", fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.6).sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${SUPPORTER_NAMES.size} people who made PaperKnife+ possible.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            itemsIndexed(
                SUPPORTER_NAMES,
                key = { index, _ -> "supporter-$index" }
            ) { index, name ->
                SupporterCard(
                    name = name,
                    color = SUPPORTER_COLORS[index % SUPPORTER_COLORS.size]
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }, key = "hall-cta") {
                Surface(
                    onClick = {
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://potatameister.github.io/support"))
                            )
                        } catch (_: Exception) {
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFFFC107).copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, Color(0xFFFFC107).copy(alpha = 0.28f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Celebration,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Become a supporter",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFF59E0B)
                            )
                            Text(
                                "Help keep PaperKnife+ free and offline.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private val SUPPORTER_NAMES = listOf(
    "Kalyan",
    "For the Planet!",
    "1260er",
    "Sushant Sangle",
    "Bittu Shaw",
    "@chetly968",
    "Andreil",
    "Csaba Gal",
    "Jason",
    "Vishnumahanthy Mohan",
    "Maurin",
    "Moh",
    "Balboc",
    "Winkewinke",
    "Akalollo",
    "Loïc",
    "Johannes",
    "yogs",
    "Vincent",
    "onyks",
    "Hana."
)

private val SUPPORTER_COLORS = listOf(
    Color(0xFFF59E0B),
    Color(0xFF14B8A6),
    Color(0xFF6366F1),
    Color(0xFFF43F5E),
    Color(0xFF06B6D4),
    Color(0xFF8B5CF6),
    Color(0xFF10B981),
    Color(0xFFF97316),
    Color(0xFF0EA5E9),
    Color(0xFFEC4899)
)

@Composable
private fun SupporterCard(name: String, color: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(116.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.20f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.13f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.firstOrNull()?.uppercase() ?: "?",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = color
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "SUPPORTER",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                letterSpacing = 0.8.sp
            )
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
        Text(
            title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = Color.Gray,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.1f))
        ) {
            Column { content() }
        }
    }
}

@Composable
fun AboutMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(accentColor.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = accentColor)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, fontSize = 11.sp, color = Color.Gray)
        }
        Icon(
            Icons.Filled.ChevronRight,
            null,
            modifier = Modifier.size(16.dp),
            tint = Color.Gray.copy(alpha = 0.5f)
        )
    }
}
