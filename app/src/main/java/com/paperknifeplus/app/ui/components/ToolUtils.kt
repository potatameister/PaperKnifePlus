package com.paperknifeplus.app.ui.components

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
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
    renderPageToBitmap(context, uri, 0, password, 0.4f)
}

suspend fun renderPageToBitmap(context: Context, uri: Uri, pageIndex: Int, password: String?, scale: Float = 1f): Bitmap? = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val document = if (password != null) PDDocument.load(inputStream, password) else PDDocument.load(inputStream)
            val renderer = PDFRenderer(document)
            // Fix: Use ImageType.RGB instead of Bitmap.Config
            val bitmap = renderer.renderImage(pageIndex, scale, ImageType.RGB)
            document.close()
            return@withContext bitmap
        }
    } catch (e: Exception) { }
    null
}

fun toGrayscaleBitmap(src: Bitmap): Bitmap {
    val bmpGrayscale = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.RGB_565)
    val canvas = Canvas(bmpGrayscale)
    val paint = Paint()
    val cm = ColorMatrix().apply { setSaturation(0f) }
    paint.colorFilter = ColorMatrixColorFilter(cm)
    canvas.drawBitmap(src, 0f, 0f, paint)
    return bmpGrayscale
}

suspend fun repairPdf(context: Context, inputUri: Uri, outputUri: Uri, password: String?) = withContext(Dispatchers.IO) {
    context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
        val document = if (password != null) PDDocument.load(inputStream, password) else PDDocument.load(inputStream)
        document.isAllSecurityToBeRemoved = true 
        saveAndFlush(context, document, outputUri)
    }
}

suspend fun performGrayscaleRewrite(context: Context, inputUri: Uri, outputUri: Uri, password: String?) = withContext(Dispatchers.IO) {
    context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
        val sourceDoc = if (password != null) PDDocument.load(inputStream, password) else PDDocument.load(inputStream)
        val targetDoc = PDDocument()
        val renderer = PDFRenderer(sourceDoc)
        
        for (i in 0 until sourceDoc.numberOfPages) {
            val rgbBitmap = renderer.renderImage(i, 1.5f, ImageType.RGB)
            val grayBitmap = toGrayscaleBitmap(rgbBitmap)
            rgbBitmap.recycle()
            
            val pdImage = JPEGFactory.createFromImage(targetDoc, grayBitmap, 0.75f)
            val page = PDPage(PDRectangle(pdImage.width.toFloat(), pdImage.height.toFloat()))
            targetDoc.addPage(page)
            
            PDPageContentStream(targetDoc, page).use { contentStream ->
                contentStream.drawImage(pdImage, 0f, 0f)
            }
            grayBitmap.recycle()
        }
        
        saveAndFlush(context, targetDoc, outputUri)
        sourceDoc.close()
    }
}

suspend fun compressPdf(context: Context, inputUri: Uri, outputUri: Uri, password: String?, level: String) = withContext(Dispatchers.IO) {
    context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
        val document = if (password != null) PDDocument.load(inputStream, password) else PDDocument.load(inputStream)
        
        val quality = if (level == "Extreme") 0.3f else 0.7f
        val scale = if (level == "Extreme") 0.5f else 0.8f

        for (page in document.pages) {
            val resources = page.resources
            for (name in resources.xObjectNames) {
                try {
                    val xobject = resources.getXObject(name)
                    if (xobject is com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject) {
                        val bitmap = xobject.image
                        val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
                        val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
                        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
                        
                        val compressedImage = JPEGFactory.createFromImage(document, scaledBitmap, quality)
                        resources.put(name, compressedImage)
                        
                        bitmap.recycle()
                        scaledBitmap.recycle()
                    }
                } catch (e: Exception) { }
            }
        }
        
        saveAndFlush(context, document, outputUri)
    }
}

fun saveAndFlush(context: Context, document: PDDocument, outputUri: Uri) {
    val info = document.documentInformation
    info.creator = "PaperKnife+"
    info.producer = "PaperKnife+ Native Engine"
    
    val autoAuthor = PreferencesManager.getDefaultAuthor(context)
    if (autoAuthor.isNotEmpty()) {
        info.author = autoAuthor
    }

    context.contentResolver.openOutputStream(outputUri, "rwt")?.use { outputStream ->
        document.save(outputStream)
        outputStream.flush()
        // Fix: Cast to FileOutputStream to access FD
        if (outputStream is FileOutputStream) {
            try {
                outputStream.fd.sync()
            } catch (e: Exception) { }
        }
    }
    document.close()
    
    try {
        val values = ContentValues()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        context.contentResolver.update(outputUri, values, null, null)
    } catch (e: Exception) { }
}
