package com.paperknifeplus.app.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperknifeplus.app.ui.theme.PaperPink
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Bookmark(
    val title: String,
    val pageIndex: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksView(
    onBack: () -> Unit,
    onOpenPreview: (Uri, String, Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val accentColor = Color(0xFFF43F5E)

    var currentState by remember { mutableStateOf<ToolState>(ToolState.SELECTING) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var outputUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var pageCount by remember { mutableIntStateOf(0) }
    var isFileLoading by remember { mutableStateOf(false) }
    var processingTime by remember { mutableStateOf("") }
    var unlockPassword by remember { mutableStateOf("") }
    var fileToUnlock by remember { mutableStateOf<String?>(null) }
    
    var bookmarks by remember { mutableStateOf<List<Bookmark>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }

    fun extractBookmarks(uri: Uri, pass: String?) {
        scope.launch(Dispatchers.IO) {
            try {
                val document = PdDocumentPool.acquire(context, uri, pass)
                if (document != null) {
                    val outline = document.documentCatalog.documentOutline
                    val list = mutableListOf<Bookmark>()
                    
                    fun resolvePage(item: PDOutlineItem): Int {
                        try {
                            val dest = item.destination
                            if (dest is com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination) {
                                return document.pages.indexOf(dest.page)
                            }
                        } catch (e: Exception) {}
                        return 0
                    }

                    fun traverse(item: PDOutlineItem?) {
                        var current = item
                        while (current != null) {
                            list.add(Bookmark(current.title, resolvePage(current)))
                            traverse(current.firstChild)
                            current = current.nextSibling
                        }
                    }
                    
                    if (outline != null) traverse(outline.firstChild)
                    
                    withContext(Dispatchers.Main) {
                        bookmarks = list
                        pageCount = document.numberOfPages
                        currentState = ToolState.CONFIGURING
                        isFileLoading = false
                        fileToUnlock = null
                    }
                } else {
                    withContext(Dispatchers.Main) { 
                        fileToUnlock = getUriDetails(context, uri).name
                        isFileLoading = false 
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { isFileLoading = false }
            }
        }
    }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedUri = it
            val details = getUriDetails(context, it)
            fileName = details.name
            isFileLoading = true
            extractBookmarks(it, null)
        }
    }

    val extractLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { saveUri ->
            currentState = ToolState.PROCESSING
            val startTime = System.currentTimeMillis()
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(selectedUri!!)?.use { inputStream ->
                        val document = if (unlockPassword.isNotEmpty()) PDDocument.load(inputStream, unlockPassword) else PDDocument.load(inputStream)
                        val target = PDDocument()
                        
                        // Extract only pages that have bookmarks
                        val bookmarkedIndices = bookmarks.map { it.pageIndex }.toSet().sorted()
                        bookmarkedIndices.forEach { idx ->
                            if (idx in 0 until document.numberOfPages) {
                                target.addPage(document.getPage(idx))
                            }
                        }
                        
                        saveAndFlush(context, target, saveUri)
                        document.close()
                    }
                    withContext(Dispatchers.Main) {
                        processingTime = String.format("%.1fs", (System.currentTimeMillis() - startTime) / 1000.0)
                        outputUri = saveUri
                        currentState = ToolState.SUCCESS
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Extraction failed: ${e.message}", Toast.LENGTH_LONG).show()
                        currentState = ToolState.CONFIGURING
                    }
                }
            }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { saveUri ->
            currentState = ToolState.PROCESSING
            val startTime = System.currentTimeMillis()
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(selectedUri!!)?.use { inputStream ->
                        val document = if (unlockPassword.isNotEmpty()) PDDocument.load(inputStream, unlockPassword) else PDDocument.load(inputStream)
                        val outline = PDDocumentOutline()
                        document.documentCatalog.documentOutline = outline
                        
                        bookmarks.forEach { bm ->
                            val item = PDOutlineItem()
                            item.title = bm.title
                            val dest = com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitWidthDestination()
                            dest.page = document.getPage(bm.pageIndex)
                            item.destination = dest
                            outline.addLast(item)
                        }
                        
                        saveAndFlush(context, document, saveUri)
                    }
                    val endTime = System.currentTimeMillis()
                    withContext(Dispatchers.Main) {
                        processingTime = String.format("%.1fs", (endTime - startTime) / 1000.0)
                        outputUri = saveUri
                        currentState = ToolState.SUCCESS
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        currentState = ToolState.CONFIGURING
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            if (currentState != ToolState.SUCCESS && currentState != ToolState.PROCESSING) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Bookmarks", fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Text("EDIT DOCUMENT NAVIGATION", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
                        }
                        if (selectedUri != null && currentState == ToolState.CONFIGURING) {
                            TextButton(onClick = { selectedUri = null; currentState = ToolState.SELECTING }) {
                                Text("CHANGE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isFileLoading) {
                LoadingStateView(accentColor, false, "Reading outlines...")
            } else {
                when (currentState) {
                    ToolState.SELECTING -> {
                        SelectionGrid(
                            onSelect = { pickLauncher.launch("application/pdf") }, 
                            isDark = isDark,
                            icon = Icons.Filled.BookmarkBorder,
                            title = "Tap to enter file",
                            subtitle = "MANAGE PDF BOOKMARKS",
                            accentColor = accentColor,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)
                        )
                    }
                    ToolState.CONFIGURING -> {
                        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("${bookmarks.size} BOOKMARKS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                Button(onClick = { showAddDialog = true }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = accentColor)) {
                                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("ADD", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                            }

                            if (bookmarks.isEmpty()) {
                                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("No bookmarks found", color = Color.Gray, fontSize = 14.sp)
                                }
                            } else {
                                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(bookmarks) { bm ->
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        ) {
                                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.Bookmark, null, tint = accentColor, modifier = Modifier.size(20.dp))
                                                Spacer(Modifier.width(16.dp))
                                                Column(Modifier.weight(1f)) {
                                                    Text(bm.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text("Page ${bm.pageIndex + 1}", fontSize = 10.sp, color = Color.Gray)
                                                }
                                                IconButton(onClick = { bookmarks = bookmarks - bm }) {
                                                    Icon(Icons.Filled.Delete, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Row(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = { saveLauncher.launch(fileName.replace(".pdf", "") + "-nav.pdf") },
                                    modifier = Modifier.weight(1f).height(60.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                                ) {
                                    Text("SAVE PDF", fontWeight = FontWeight.Black)
                                }
                                OutlinedButton(
                                    onClick = { extractLauncher.launch(fileName.replace(".pdf", "") + "-extract.pdf") },
                                    modifier = Modifier.weight(1f).height(60.dp),
                                    enabled = bookmarks.isNotEmpty(),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
                                ) {
                                    Text("EXTRACT PAGES", fontWeight = FontWeight.Black, color = accentColor)
                                }
                            }
                        }
                    }
                    ToolState.PROCESSING -> {
                        ProcessingStateView(
                            accentColor = accentColor,
                            uri = selectedUri,
                            password = unlockPassword.ifEmpty { null },
                            text = "Updating document...",
                            current = 0,
                            total = 0,
                            showWarning = false
                        )
                    }
                    ToolState.SUCCESS -> {
                        SuccessView(
                            message = "Task Complete",
                            subMessage = "Document updated successfully",
                            processingTime = processingTime,
                            onDone = onBack,
                            onProcessMore = { 
                                selectedUri = null
                                bookmarks = emptyList()
                                currentState = ToolState.SELECTING 
                            },
                            onPreview = { outputUri?.let { uri -> onOpenPreview(uri, fileName, pageCount) } },
                            accentColor = accentColor
                        )
                    }
                    else -> {}
                }
            }

            if (fileToUnlock != null) {
                LockedFilePrompt(
                    fileName = fileToUnlock!!,
                    onDismiss = { fileToUnlock = null; selectedUri = null; currentState = ToolState.SELECTING },
                    onUnlocked = { pass ->
                        isFileLoading = true
                        scope.launch(Dispatchers.IO) {
                            val count = getPageCount(context, selectedUri!!, pass)
                            withContext(Dispatchers.Main) { 
                                if (count > 0) {
                                    unlockPassword = pass
                                    extractBookmarks(selectedUri!!, pass)
                                } else {
                                    Toast.makeText(context, "Invalid Password", Toast.LENGTH_SHORT).show()
                                    isFileLoading = false
                                }
                            }
                        }
                    },
                    accentColor = accentColor,
                    isLoading = isFileLoading
                )
            }
        }
    }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var pageNum by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Bookmark", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = pageNum, onValueChange = { if (it.all { c -> c.isDigit() }) pageNum = it }, label = { Text("Page (1-$pageCount)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val p = pageNum.toIntOrNull()?.let { it - 1 }
                    if (title.isNotBlank() && p != null && p in 0 until pageCount) {
                        bookmarks = bookmarks + Bookmark(title, p)
                        showAddDialog = false
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = accentColor)) { Text("ADD") }
            }
        )
    }
}
