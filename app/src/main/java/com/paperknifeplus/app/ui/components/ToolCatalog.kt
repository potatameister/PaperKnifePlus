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
        Tool("merge", "Merge", "Combine PDFs", "Join Files", Icons.Outlined.CallMerge, "Edit", editColor, editBg),
        Tool("split", "Split", "Extract pages", "Extract Pages", Icons.Outlined.CallSplit, "Edit", editColor, editBg),
        Tool("delete", "Delete", "Remove pages", "Wipe Pages", Icons.Outlined.DeleteOutline, "Edit", editColor, editBg),
        Tool("rearrange", "Rearrange", "Sort pages", "Sort Order", Icons.Outlined.Reorder, "Edit", editColor, editBg),
        Tool("rotate", "Rotate", "Fix orientation", "Fix Angle", Icons.Outlined.Rotate90DegreesCcw, "Edit", editColor, editBg),
        Tool("sign", "Sign", "Add signature", "Add Signature", Icons.Outlined.Draw, "Edit", editColor, editBg),
        Tool("watermark", "Watermark", "Add overlay", "Custom Overlay", Icons.Outlined.BrandingWatermark, "Edit", editColor, editBg),
        Tool("page-numbers", "Numbers", "Add pagination", "Add Numbers", Icons.Outlined.Numbers, "Edit", editColor, editBg),
        Tool("bookmarks", "Bookmarks", "Edit bookmarks", "Nav Anchors", Icons.Outlined.Bookmarks, "Edit", editColor, editBg),

        // OPTIMIZE - Amber
        Tool("compress", "Compress", "Small size", "Reduce Size", Icons.Outlined.Compress, "Optimize", optimizeColor, optimizeBg),
        Tool("grayscale", "Grayscale", "Gray tones", "Black & White", Icons.Outlined.FilterBAndW, "Optimize", optimizeColor, optimizeBg),
        Tool("repair", "Repair", "Fix corruption", "Fix Defects", Icons.Outlined.Handyman, "Optimize", optimizeColor, optimizeBg),
        Tool("compare", "Compare", "Visual diff", "Side-by-Side", Icons.Outlined.Difference, "Optimize", optimizeColor, optimizeBg),

        // SECURE - Indigo
        Tool("protect", "Lock", "Password", "Set Pass", Icons.Outlined.Lock, "Secure", secureColor, secureBg),
        Tool("unlock", "Unlock", "Remove pass", "Remove Pass", Icons.Outlined.LockOpen, "Secure", secureColor, secureBg),
        Tool("metadata", "Metadata", "Edit props", "Edit Info", Icons.Outlined.Fingerprint, "Secure", secureColor, secureBg),

        // CONVERT - Teal
        Tool("pdf2img", "PDF to Img", "High-res export", "Export Frames", Icons.Outlined.Image, "Convert", convertColor, convertBg),
        Tool("img2pdf", "Img to PDF", "Build from photos", "Build PDF", Icons.Outlined.PictureAsPdf, "Convert", convertColor, convertBg),
        Tool("pdf2zip", "PDF to ZIP", "Archive docs", "Zip Pages", Icons.Outlined.FolderZip, "Convert", convertColor, convertBg),
        Tool("extract-images", "Extract", "Save assets", "Strip Raw", Icons.Outlined.PhotoLibrary, "Convert", convertColor, convertBg),
        Tool("pdf2text", "PDF to Text", "Extract text", "Plain Text", Icons.Outlined.TextSnippet, "Convert", convertColor, convertBg)
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
        "merge", "compress", "unlock", "pdf2text"
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
