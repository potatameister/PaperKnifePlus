package com.paperknifeplus.app.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceGray
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.rendering.PDFRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class UriDetails(
    val name: String,
    val size: String
)

@SuppressLint("Range")
fun getUriDetails(context: Context, uri: Uri): UriDetails {
    var name = "Document.pdf"
    var size = "Unknown size"
    
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                
                if (nameIndex != -1) {
                    val rawName = cursor.getString(nameIndex)
                    if (rawName != null) name = rawName
                }
                if (sizeIndex != -1) {
                    val sizeBytes = cursor.getLong(sizeIndex)
                    size = formatSize(sizeBytes)
                }
            }
        }
    } catch (e: Exception) {
        uri.lastPathSegment?.let { name = it }
    }
    
    if (!name.endsWith(".pdf", true) && !name.contains(".")) name += ".pdf"
    return UriDetails(name, size)
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

suspend fun checkIsEncryptedLocal(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val doc = PDDocument.load(inputStream)
            val isEnc = doc.isEncrypted
            doc.close()
            isEnc
        } ?: false
    } catch (e: com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException) {
        true
    } catch (e: Exception) {
        if (e.message?.contains("encrypted", ignoreCase = true) == true) true else false
    }
}

suspend fun verifyPasswordLocal(context: Context, uri: Uri, password: String): Boolean = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            PDDocument.load(inputStream, password).use { doc -> !doc.isEncrypted || true }
        } ?: false
    } catch (e: Exception) {
        false
    }
}

suspend fun loadPreview(context: Context, uri: Uri, password: String?): Bitmap? = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            val renderer = android.graphics.pdf.PdfRenderer(pfd)
            val page = renderer.openPage(0)
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            bitmap
        }
    } catch (e: Exception) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val document = if (password != null) PDDocument.load(inputStream, password) else PDDocument.load(inputStream)
                val renderer = PDFRenderer(document)
                val bitmap = renderer.renderImage(0, 0.5f)
                document.close()
                bitmap
            }
        } catch (e2: Exception) {
            null
        }
    }
}

suspend fun renderPageToBitmap(context: Context, uri: Uri, pageIndex: Int, password: String?, scale: Float = 1f): Bitmap? = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            val renderer = android.graphics.pdf.PdfRenderer(pfd)
            if (pageIndex < renderer.pageCount) {
                val page = renderer.openPage(pageIndex)
                val bitmap = Bitmap.createBitmap((page.width * scale).toInt(), (page.height * scale).toInt(), Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                return@withContext bitmap
            }
            renderer.close()
        }
    } catch (e: Exception) {
        // Fallback to PDFBox for encrypted or complex files
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val document = if (password != null) PDDocument.load(inputStream, password) else PDDocument.load(inputStream)
                val renderer = PDFRenderer(document)
                val bitmap = renderer.renderImage(pageIndex, scale)
                document.close()
                return@withContext bitmap
            }
        } catch (e2: Exception) { }
    }
    null
}

fun toGrayscaleBitmap(src: Bitmap): Bitmap {
    val height = src.height
    val width = src.width
    val bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmpGrayscale)
    val paint = Paint()
    val cm = ColorMatrix()
    cm.setSaturation(0f)
    val f = ColorMatrixColorFilter(cm)
    paint.colorFilter = f
    c.drawBitmap(src, 0f, 0f, paint)
    return bmpGrayscale
}

suspend fun repairPdf(context: Context, inputUri: Uri, outputUri: Uri, password: String?) = withContext(Dispatchers.IO) {
    context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
        val document = if (password != null) PDDocument.load(inputStream, password) else PDDocument.load(inputStream)
        // Enabling this removes corruption in structure
        document.isAllSecurityToBeRemoved = true 
        context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
            document.save(outputStream)
            outputStream.flush()
        }
        document.close()
    }
}
