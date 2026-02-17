package com.paperknifeplus.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperknifeplus.app.ui.theme.PaperPink

@Composable
fun HomeView(
    isDarkMode: Boolean,
    onThemeToggle: () -> Unit,
    onToolClick: (String) -> Unit
) {
    val coreEngines = listOf(
        Tool("merge", "Merge", "COMBINE", Icons.Default.Layers, "Edit", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
        Tool("compress", "Compress", "OPTIMIZE", Icons.Default.Bolt, "Optimize", Color(0xFFF59E0B), Color(0xFFFFFBEB)),
        Tool("split", "Split", "EXTRACT", Icons.Default.ContentCut, "Edit", Color(0xFF3B82F6), Color(0xFFEFF6FF)),
        Tool("protect", "Protect", "SECURE", Icons.Default.Lock, "Secure", Color(0xFF6366F1), Color(0xFFEEF2FF)),
        Tool("rotate", "Rotate", "ORIENTATION", Icons.Default.RotateRight, "Edit", Color(0xFFF97316), Color(0xFFFFF7ED)),
        Tool("rearrange", "Rearrange", "REORDER", Icons.AutoMirrored.Filled.List, "Edit", Color(0xFF10B981), Color(0xFFECFDF5))
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Premium Background Hue (Top Right)
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.TopEnd)
                .offset(x = 150.dp, y = (-100).dp)
                .background(Brush.radialGradient(listOf(PaperPink.copy(alpha = 0.12f), Color.Transparent)))
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Header moved into scrollable area
            item(span = { GridItemSpan(2) }) {
                HomeHeader(isDarkMode, onThemeToggle)
            }

            // Hero Card
            item(span = { GridItemSpan(2) }) {
                HeroCard(onSelectPdf = { /* Action */ })
            }

            // Section Title
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CORE ENGINES",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Gray,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            // Bento Grid Items
            items(coreEngines) { tool ->
                BentoCard(tool = tool, onClick = { onToolClick(tool.id) })
            }

            // More Engines
            item(span = { GridItemSpan(2) }) {
                MoreEnginesCard(onClick = { onToolClick("tools") })
            }
            
            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "PaperKnife v1.0.9",
                    modifier = Modifier.fillMaxWidth().alpha(0.2f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun HomeHeader(isDarkMode: Boolean, onThemeToggle: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp)
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Logo(modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "PaperKnife",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.8).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .graphicsLayer { alpha = dotAlpha }
                            .background(Color(0xFF06D6A0), CircleShape)
                    )
                }
                Text(
                    text = "SECURE ENGINE",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black,
                    color = PaperPink,
                    letterSpacing = 1.2.sp
                )
            }
        }
        
        IconButton(
            onClick = onThemeToggle,
            modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(0.4f), CircleShape)
        ) {
            Icon(
                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "Toggle Theme",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun HeroCard(onSelectPdf: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val containerColor = if (isDark) Color.White else Color(0xFF121212)
    val textColor = if (isDark) Color.Black else Color.White
    val subTextColor = if (isDark) Color.DarkGray else Color.Gray

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(32.dp), spotColor = PaperPink.copy(alpha = 0.2f))
            .clickable(onClick = onSelectPdf),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Internal Glow
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 20.dp, y = (-20).dp)
                    .background(Brush.radialGradient(listOf(PaperPink.copy(alpha = 0.15f), Color.Transparent)))
            )

            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Text(
                    text = "Select PDF",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor,
                    letterSpacing = (-0.5).sp,
                    lineHeight = 32.sp
                )
                Text(
                    text = "TAP TO LOAD FROM DEVICE STORAGE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = subTextColor,
                    letterSpacing = 0.5.sp
                )
            }
            
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.TopStart),
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, if (isDark) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.05f))
            ) {
                Icon(
                    Icons.Default.FileUpload,
                    contentDescription = null,
                    tint = if (isDark) Color.Black else Color.White,
                    modifier = Modifier.padding(12.dp)
                )
            }
            
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .clickable { onSelectPdf() },
                color = PaperPink,
                shape = CircleShape
            ) {
                Text(
                    text = "START SESSION",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun BentoCard(tool: Tool, onClick: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF0A0A0A) else Color.White
        ),
        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle Tool Background Icon
            Icon(
                imageVector = tool.icon ?: Icons.Default.Build,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 10.dp, y = 10.dp)
                    .alpha(0.03f),
                tint = tool.color
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) tool.color.copy(alpha = 0.12f) else tool.bgColor
                ) {
                    Icon(
                        imageVector = tool.icon ?: Icons.Default.Build,
                        contentDescription = null,
                        tint = tool.color,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                
                Column {
                    Text(
                        text = tool.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.3).sp,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = tool.description,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 0.2.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MoreEnginesCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PaperPink)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Icon(
                        Icons.Default.GridView,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "More Engines",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "FULL CATALOG",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White.copy(alpha = 0.7f),
                        letterSpacing = 1.sp
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
