package com.paperknifeplus.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.paperknifeplus.app.ui.theme.PaperPink

/**
 * Single source of truth for tool metadata.
 * Every page (Home, Tools, picker, history, tool headers) reads from here,
 * so changing an icon or color updates the whole app.
 */
object ToolCatalog {
    private val editColor = Color(0xFFF43F5E)
    private val editBg = Color(0xFFFFF1F2)
    private val optimizeColor = Color(0xFFF59E0B)
    private val optimizeBg = Color(0xFFFFFBEB)
    private val secureColor = Color(0xFF6366F1)
    private val secureBg = Color(0xFFEEF2FF)
    private val convertColor = Color(0xFF14B8A6)
    private val convertBg = Color(0xFFF0FDFA)

    val all: List<Tool> = listOf(
        // EDIT - Rose
        Tool("merge", "Merge", "Combine PDFs", "Join Files", Icons.Outlined.CallMerge, "Edit", editColor, editBg, headerSubtitle = "JOIN MULTIPLE DOCUMENTS"),
        Tool("split", "Split", "Extract pages", "Extract Pages", Icons.Outlined.CallSplit, "Edit", editColor, editBg, headerSubtitle = "EXTRACT PAGES FROM PDF"),
        Tool("delete", "Delete", "Remove pages", "Wipe Pages", Icons.Outlined.DeleteOutline, "Edit", editColor, editBg, headerSubtitle = "REMOVE PAGES FROM PDF"),
        Tool("rearrange", "Rearrange", "Sort pages", "Sort Order", Icons.Outlined.Reorder, "Edit", editColor, editBg, headerSubtitle = "MOVE PAGES TO NEW SLOTS"),
        Tool("rotate", "Rotate", "Fix orientation", "Fix Angle", Icons.Outlined.Rotate90DegreesCcw, "Edit", editColor, editBg, headerSubtitle = "FIX DOCUMENT ORIENTATION"),
        Tool("sign", "Sign PDF", "Add signature", "Add Signature", Icons.Outlined.Draw, "Edit", editColor, editBg, headerSubtitle = "SECURE SIGNATURE"),
        Tool("watermark", "Watermark", "Add overlay", "Custom Overlay", Icons.Outlined.BrandingWatermark, "Edit", editColor, editBg, headerSubtitle = "CUSTOM OVERLAY"),
        Tool("page-numbers", "Page Numbers", "Add pagination", "Add Numbers", Icons.Outlined.Numbers, "Edit", editColor, editBg, headerSubtitle = "CUSTOM PAGINATION"),
        Tool("bookmarks", "Bookmarks", "Edit bookmarks", "Nav Anchors", Icons.Outlined.Bookmarks, "Edit", editColor, editBg, headerSubtitle = "EDIT DOCUMENT NAVIGATION"),

        // OPTIMIZE - Amber
        Tool("compress", "Compress", "Small size", "Reduce Size", Icons.Outlined.Compress, "Optimize", optimizeColor, optimizeBg, headerSubtitle = "OPTIMIZE PDF SIZE"),
        Tool("grayscale", "Grayscale", "Gray tones", "Black & White", Icons.Outlined.FilterBAndW, "Optimize", optimizeColor, optimizeBg, headerSubtitle = "CONVERT PDF TO B&W"),
        Tool("repair", "Repair", "Fix corruption", "Fix Defects", Icons.Outlined.Handyman, "Optimize", optimizeColor, optimizeBg, headerSubtitle = "RECOVER CORRUPT DOCUMENTS"),
        Tool("compare", "Compare", "Visual diff", "Side-by-Side", Icons.Outlined.Difference, "Optimize", optimizeColor, optimizeBg, headerSubtitle = "VISUAL DIFFERENCE TOOL"),

        // SECURE - Indigo
        Tool("protect", "Lock", "Password", "Set Pass", Icons.Outlined.Lock, "Secure", secureColor, secureBg, headerSubtitle = "ENCRYPT YOUR DOCUMENT"),
        Tool("unlock", "Unlock", "Remove pass", "Remove Pass", Icons.Outlined.LockOpen, "Secure", secureColor, secureBg, headerSubtitle = "REMOVE PDF RESTRICTIONS"),
        Tool("metadata", "Metadata", "Edit props", "Edit Info", Icons.Outlined.Fingerprint, "Secure", secureColor, secureBg, headerSubtitle = "EDIT DOCUMENT PROPERTIES"),

        // CONVERT - Teal
        Tool("pdf2img", "PDF to Image", "High-res export", "Export Frames", Icons.Outlined.Image, "Convert", convertColor, convertBg, headerSubtitle = "NATIVE EXPORT ENGINE"),
        Tool("img2pdf", "Image to PDF", "Build from photos", "Build PDF", Icons.Outlined.PictureAsPdf, "Convert", convertColor, convertBg, headerSubtitle = "PHOTO TO DOCUMENT CONVERTER"),
        Tool("pdf2zip", "PDF to ZIP", "Archive docs", "Zip Pages", Icons.Outlined.FolderZip, "Convert", convertColor, convertBg, headerSubtitle = "PAGES TO ARCHIVE"),
        Tool("extract-images", "Extract Images", "Save assets", "Strip Raw", Icons.Outlined.PhotoLibrary, "Convert", convertColor, convertBg, headerSubtitle = "EXTRACT ASSETS FROM PDFS"),
        Tool("pdf2text", "PDF to Text", "Extract text", "Plain Text", Icons.Outlined.TextSnippet, "Convert", convertColor, convertBg, headerSubtitle = "EXTRACT RAW CONTENT")
    )

    val byId: Map<String, Tool> = all.associateBy { it.id }

    val categories: List<String> = listOf("Edit", "Optimize", "Secure", "Convert")

    val categoryDescriptions: Map<String, String> = mapOf(
        "Edit" to "Organize & Modify",
        "Optimize" to "Shrink & Repair",
        "Secure" to "Protect & Unlock",
        "Convert" to "Export Assets"
    )

    val essentials: List<Tool> = listOf(
        "merge", "split", "compress", "sign", "protect", "pdf2img"
    ).mapNotNull { byId[it] }

    val core: List<Tool> = listOf(
        "merge", "compress", "protect", "pdf2text"
    ).mapNotNull { byId[it] }

    fun icon(id: String): ImageVector = byId[id]?.icon ?: Icons.Outlined.Build

    fun colorFor(toolName: String): Color = when {
        toolName.contains("Merge", true) ||
            toolName.contains("Split", true) ||
            toolName.contains("Delete", true) ||
            toolName.contains("Rearrange", true) ||
            toolName.contains("Rotate", true) ||
            toolName.contains("Sign", true) ||
            toolName.contains("Watermark", true) ||
            toolName.contains("Number", true) ||
            toolName.contains("Bookmark", true) -> editColor

        toolName.contains("Compress", true) ||
            toolName.contains("Grayscale", true) ||
            toolName.contains("Repair", true) ||
            toolName.contains("Compare", true) -> optimizeColor

        toolName.contains("Protect", true) ||
            toolName.contains("Lock", true) ||
            toolName.contains("Unlock", true) ||
            toolName.contains("Metadata", true) -> secureColor

        toolName.contains("PDF to", true) ||
            toolName.contains("Image to", true) ||
            toolName.contains("To Text", true) ||
            toolName.contains("Extract", true) -> convertColor

        else -> PaperPink
    }
}
