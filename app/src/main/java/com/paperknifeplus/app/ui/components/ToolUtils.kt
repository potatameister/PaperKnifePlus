package com.paperknifeplus.app.ui.components

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDResources
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.rendering.PDFRenderer
import com.tom_roush.pdfbox.rendering.ImageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class UriDetails(
    val name: String,
    val size: String
)

object PreferencesManager {
    fun setDefaultAuthor(context: Context, name: String) {
        val prefs = context.getSharedPreferences("pk_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("default_author", name).apply()
    }
    fun getDefaultAuthor(context: Context): String {
        val prefs = context.getSharedPreferences("pk_prefs", Context.MODE_PRIVATE)
        return prefs.getString("default_author", "") ?: ""
    }
}

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
    } catch (e: Exception) { true }
}

suspend fun verifyPasswordLocal(context: Context, uri: Uri, password: String): Boolean = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            PDDocument.load(inputStream, password).use { doc -> !doc.isEncrypted || true }
        } ?: false
    } catch (e: Exception) { false }
}

suspend fun loadPreview(context: Context, uri: Uri, password: String?): Bitmap? = withContext(Dispatchers.IO) {
    // Native PdfRenderer is MUCH more efficient for previews than PDFBox
    try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            val renderer = android.graphics.pdf.PdfRenderer(pfd)
            val page = renderer.openPage(0)
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            return@withContext bitmap
        }
    } catch (e: Exception) {
        // Fallback to PDFBox if locked
        renderPageToBitmap(context, uri, 0, password, 0.5f)
    }
    null
}

suspend fun renderPageToBitmap(context: Context, uri: Uri, pageIndex: Int, password: String?, scale: Float = 1f): Bitmap? = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val document = if (password != null) PDDocument.load(inputStream, password) else PDDocument.load(inputStream)
            val renderer = PDFRenderer(document)
            val bitmap = renderer.renderImage(pageIndex, scale, ImageType.RGB)
            document.close()
            return@withContext bitmap
        }
    } catch (e: Exception) { }
    null
}

fun toGrayscaleBitmap(src: Bitmap): Bitmap {
    val bmpGrayscale = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmpGrayscale)
    val paint = Paint()
    val cm = ColorMatrix().apply { setSaturation(0f) }
    paint.colorFilter = ColorMatrixColorFilter(cm)
    canvas.drawBitmap(src, 0f, 0f, paint)
    return bmpGrayscale
}

suspend fun performGrayscaleRewrite(context: Context, inputUri: Uri, outputUri: Uri, password: String?, onProgress: (Int, Int) -> Unit) = withContext(Dispatchers.IO) {
    context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
        val document = if (password != null) PDDocument.load(inputStream, password) else PDDocument.load(inputStream)
        val total = document.numberOfPages
        
        for (i in 0 until total) {
            onProgress(i + 1, total)
            val page = document.getPage(i)
            processResourcesForGrayscale(document, page.resources)
        }
        
        saveAndFlush(context, document, outputUri)
    }
}

private fun processResourcesForGrayscale(document: PDDocument, resources: PDResources) {
    for (name in resources.xObjectNames) {
        try {
            val xobject = resources.getXObject(name)
            if (xobject is PDImageXObject) {
                val bitmap = xobject.image
                val grayBitmap = toGrayscaleBitmap(bitmap)
                val grayImage = JPEGFactory.createFromImage(document, grayBitmap, 0.8f)
                resources.put(name, grayImage)
                bitmap.recycle()
                grayBitmap.recycle()
            }
        } catch (e: Exception) { }
    }
}

suspend fun compressPdf(context: Context, inputUri: Uri, outputUri: Uri, password: String?, level: String, onProgress: (Int, Int) -> Unit) = withContext(Dispatchers.IO) {
    context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
        val document = if (password != null) PDDocument.load(inputStream, password) else PDDocument.load(inputStream)
        
        val quality = when(level) {
            "Extreme" -> 0.3f
            "Recommended" -> 0.6f
            else -> 0.85f
        }
        val scale = when(level) {
            "Extreme" -> 0.5f
            "Recommended" -> 0.75f
            else -> 1.0f
        }

        val total = document.numberOfPages
        for (i in 0 until total) {
            onProgress(i + 1, total)
            val page = document.getPage(i)
            processResourcesForCompression(document, page.resources, scale, quality)
        }
        
        saveAndFlush(context, document, outputUri)
    }
}

private fun processResourcesForCompression(document: PDDocument, resources: PDResources, scale: Float, quality: Float) {
    for (name in resources.xObjectNames) {
        try {
            val xobject = resources.getXObject(name)
            if (xobject is PDImageXObject) {
                val bitmap = xobject.image
                val newWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
                val newHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
                
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                val compressedImage = JPEGFactory.createFromImage(document, scaledBitmap, quality)
                
                resources.put(name, compressedImage)
                bitmap.recycle()
                scaledBitmap.recycle()
            }
        } catch (e: Exception) { }
    }
}

fun saveAndFlush(context: Context, document: PDDocument, outputUri: Uri) {
    val info = document.documentInformation
    info.creator = "PaperKnife+"
    info.producer = "PaperKnife+ Native Engine"
    
    val autoAuthor = PreferencesManager.getDefaultAuthor(context)
    if (autoAuthor.isNotEmpty()) info.author = autoAuthor

    context.contentResolver.openOutputStream(outputUri, "rwt")?.use { outputStream ->
        document.save(outputStream)
        outputStream.flush()
        if (outputStream is FileOutputStream) {
            try { outputStream.fd.sync() } catch (e: Exception) { }
        }
    }
    document.close()
    
    try {
        val values = ContentValues()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        context.contentResolver.update(outputUri, values, null, null)
    } catch (e: Exception) { }
}
