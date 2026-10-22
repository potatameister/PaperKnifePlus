package com.paperknifeplus.app.ui.components

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperknifeplus.app.ui.theme.PaperPink
import kotlinx.coroutines.launch

@Composable
fun HomeView(
    isDarkMode: Boolean,
    onThemeToggle: () -> Unit,
    onToolClick: (String) -> Unit,
    onOpenPreview: (Uri, String, Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pickLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val details = getUriDetails(context, it)
            scope.launch {
                val count = getPageCount(context, it, null)
                onOpenPreview(it, details.name, count)
            }
        }
    }

    val coreEngines = ToolCatalog.core

    val history = SessionManager.history

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val homeColumns = if (maxWidth >= 700.dp) 4 else 2
        LazyVerticalGrid(
            columns = GridCells.Fixed(homeColumns),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = 8.dp,
                end = 20.dp,
                bottom = 96.dp
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "header") {
                HomeHeader(isDarkMode, onThemeToggle)
            }

            item(span = { GridItemSpan(maxLineSpan) }, key = "hero") {
                HeroCard(onSelectPdf = { pickLauncher.launch("application/pdf") })
            }

            item(span = { GridItemSpan(maxLineSpan) }, key = "history") {
                MiniHistoryBar(history, onHistoryClick = { onToolClick("history") }, onOpenPreview = onOpenPreview)
            }

            item(span = { GridItemSpan(maxLineSpan) }, key = "label") {
                Row(
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp, start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CORE TOOLS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        letterSpacing = 1.2.sp
                    )
                }
            }

            items(coreEngines, key = { it.id }) { tool ->
                BentoCard(tool = tool, onClick = { onToolClick(tool.id) })
            }

            item(span = { GridItemSpan(maxLineSpan) }, key = "more") {
                MoreToolsCard(onClick = { onToolClick("tools") })
            }

            item(span = { GridItemSpan(maxLineSpan) }, key = "footer") {
                Spacer(modifier = Modifier.height(12.dp))

                // PaperKnife-style support card
                val supportDark = MaterialTheme.colorScheme.background == Color.Black
                val supportCard = if (supportDark) Color.White else Color(0xFF09090B)
                val supportText = if (supportDark) Color.Black else Color.White
                val supportSub = if (supportDark) Color(0xFF52525B) else Color(0xFFA1A1AA)
                val supportIconBg = if (supportDark) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.10f)

                Surface(
                    onClick = {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            Uri.parse("https://potatameister.github.io/support")
                        )
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = supportCard,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).background(supportIconBg, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Support PaperKnife",
                                tint = Color(0xFFF43F5E),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Support PaperKnife+",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = supportText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "HELP FUEL DEVELOPMENT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = supportSub,
                                letterSpacing = 1.sp
                            )
                        }
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = supportSub,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(220.dp))
            }
        }
    }
}

@Composable
fun HomeHeader(isDarkMode: Boolean, onThemeToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Logo(
                modifier = Modifier.size(20.dp),
                partColor = if (isDarkMode) Color.White else Color(0xFF18181B)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = buildAnnotatedString {
                    append("PaperKnife")
                    withStyle(SpanStyle(color = PaperPink)) { append("+") }
                },
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-0.5).sp
            )
        }

        Surface(
            onClick = onThemeToggle,
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Filled.Lightbulb else Icons.Filled.Bedtime,
                    contentDescription = "Toggle Theme",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
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
            .height(170.dp)
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(28.dp), spotColor = PaperPink.copy(alpha = 0.15f))
            .clickable(onClick = onSelectPdf),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = if (isDark) {
                            listOf(Color.White, Color.White, PaperPink.copy(alpha = 0.1f))
                        } else {
                            listOf(Color(0xFF121212), Color(0xFF121212), PaperPink.copy(alpha = 0.2f))
                        },
                        start = Offset(0f, 1000f),
                        end = Offset(1000f, 0f)
                    )
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Text(
                    text = "Select PDF",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor,
                    letterSpacing = (-0.5).sp,
                    lineHeight = 28.sp
                )
                Text(
                    text = "TAP TO LOAD FROM DEVICE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = subTextColor,
                    letterSpacing = 0.5.sp
                )
            }

            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.TopStart),
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, if (isDark) Color.Black.copy(alpha = 0.05f) else Color.White.copy(0.05f))
            ) {
                Icon(
                    Icons.Outlined.Description,
                    contentDescription = null,
                    tint = if (isDark) Color.Black else Color.White,
                    modifier = Modifier.padding(10.dp)
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
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

@Composable
fun MiniHistoryBar(history: List<ActivityEntry>, onHistoryClick: () -> Unit, onOpenPreview: (Uri, String, Int) -> Unit) {
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDark) Color(0xFF09090B) else Color.White)
            .border(BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(0.03f)), RoundedCornerShape(20.dp))
            .clickable(onClick = onHistoryClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.History, null, modifier = Modifier.size(11.dp), tint = Color.Gray)
                Spacer(Modifier.width(6.dp))
                Text("RECENT ACTIVITY", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), letterSpacing = 0.8.sp)
            }
            if (history.isNotEmpty()) {
                Text("VIEW ALL", fontSize = 10.sp, fontWeight = FontWeight.Black, color = PaperPink, letterSpacing = 0.8.sp)
            }
        }

        if (history.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "NO RECENT SESSIONS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray.copy(alpha = 0.3f)
            )
        } else {
            Spacer(Modifier.height(8.dp))
            history.take(2).forEach { entry ->
                val entryColor = remember(entry.tool) { ToolCatalog.colorFor(entry.tool) }
                Row(
                    modifier = Modifier
                        .padding(vertical = 3.dp)
                        .clickable(enabled = entry.uri != null) {
                            entry.uri?.let { onOpenPreview(it, entry.name, entry.pageCount) }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(entry.icon, null, modifier = Modifier.size(12.dp), tint = entryColor.copy(alpha = 0.6f))
                    Spacer(Modifier.width(10.dp))
                    Text(entry.name, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1)
                    Text(entry.tool.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = entryColor.copy(alpha = 0.75f))
                }
            }
        }
    }
}

@Composable
fun BentoCard(tool: Tool, onClick: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val containerColor = remember(isDark) { if (isDark) Color(0xFF0A0A0A) else Color.White }
    val borderColor = remember(isDark) { if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(0.03f) }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 92.dp),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(if (isDark) tool.color.copy(alpha = 0.12f) else tool.bgColor, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon ?: Icons.Filled.Build,
                    contentDescription = null,
                    tint = tool.color,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tool.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.3).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = tool.description.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    letterSpacing = 0.2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun MoreToolsCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
        shape = RoundedCornerShape(20.dp),
        color = PaperPink
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.GridView,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "More Tools",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "FULL CATALOG",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White.copy(alpha = 0.7f),
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
