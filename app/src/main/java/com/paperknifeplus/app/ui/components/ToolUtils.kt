package com.paperknifeplus.app.ui.components

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.LruCache
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.PDResources
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.rendering.PDFRenderer
import com.tom_roush.pdfbox.rendering.ImageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class UriDetails(
    val name: String,
    val size: String,
    val sizeBytes: Long = 0
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

// Simple Memory Cache for Bitmaps to prevent OOM
object BitmapCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 4 // Use 1/4th of available memory
    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun getBitmap(key: String): Bitmap? = memoryCache.get(key)
    fun putBitmap(key: String, bitmap: Bitmap) {
        if (getBitmap(key) == null) memoryCache.put(key, bitmap)
    }
    fun clear() = memoryCache.evictAll()
}

@SuppressLint("Range")
fun getUriDetails(context: Context, uri: Uri): UriDetails {
    var name = "Document.pdf"
    var size = "Unknown size"
    var bytes: Long = 0
    
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
                    bytes = cursor.getLong(sizeIndex)
                    size = formatSize(bytes)
                }
            }
        }
    } catch (e: Exception) {
        uri.lastPathSegment?.let { name = it }
    }
    
    if (!name.contains(".")) name += ".pdf"
    return UriDetails(name, size, bytes)
}

fun formatSize(bytes: Long): String {
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
    renderPageToBitmap(context, uri, 0, password, 1.0f) // High quality cover
}

suspend fun renderPageToBitmap(context: Context, uri: Uri, pageIndex: Int, password: String?, scale: Float = 1f): Bitmap? = withContext(Dispatchers.IO) {
    val key = "$uri-$pageIndex-$scale"
    BitmapCache.getBitmap(key)?.let { return@withContext it }

    // Try Native Renderer first (Fastest, Least Memory)
    if (password == null) {
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val renderer = android.graphics.pdf.PdfRenderer(pfd)
                if (pageIndex < renderer.pageCount) {
                    val page = renderer.openPage(pageIndex)
                    val width = (page.width * scale).toInt().coerceAtLeast(1)
                    val height = (page.height * scale).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    renderer.close()
                    BitmapCache.putBitmap(key, bitmap)
                    return@withContext bitmap
                }
                renderer.close()
            }
        } catch (e: Exception) { }
    }

    // Fallback to PDFBox
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val document = if (password != null) PDDocument.load(inputStream, password) else PDDocument.load(inputStream)
            val renderer = PDFRenderer(document)
            val bitmap = renderer.renderImage(pageIndex, scale, ImageType.RGB)
            document.close()
            BitmapCache.putBitmap(key, bitmap)
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

suspend fun performGrayscaleRewrite(context: Context, inputUri: Uri, outputUri: Uri, password: String?, onProgress: (Int, Int) -> Unit) = withContext(Dispatchers.IO) {
    context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
        val sourceDoc = if (password != null) PDDocument.load(inputStream, password) else PDDocument.load(inputStream)
        val targetDoc = PDDocument()
        val renderer = PDFRenderer(sourceDoc)
        val total = sourceDoc.numberOfPages
        
        for (i in 0 until total) {
            onProgress(i + 1, total)
            // Render to bitmap to ensure 100% grayscale (Nuclear Option)
            val rgbBitmap = renderer.renderImage(i, 1.5f, ImageType.RGB)
            val grayBitmap = toGrayscaleBitmap(rgbBitmap)
            rgbBitmap.recycle()
            
            val pdImage = JPEGFactory.createFromImage(targetDoc, grayBitmap, 0.75f)
            val page = PDPage(PDRectangle(pdImage.width.toFloat(), pdImage.height.toFloat()))
            targetDoc.addPage(page)
            PDPageContentStream(targetDoc, page).use { it.drawImage(pdImage, 0f, 0f) }
            grayBitmap.recycle()
        }
        saveAndFlush(context, targetDoc, outputUri)
        sourceDoc.close()
    }
}

suspend fun compressPdf(context: Context, inputUri: Uri, outputUri: Uri, password: String?, level: String, onProgress: (Int, Int) -> Unit) = withContext(Dispatchers.IO) {
    context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
        val sourceDoc = if (password != null) PDDocument.load(inputStream, password) else PDDocument.load(inputStream)
        val targetDoc = PDDocument()
        val renderer = PDFRenderer(sourceDoc)
        val total = sourceDoc.numberOfPages
        
        // Tuned Quality Settings
        val quality = when(level) { "Extreme" -> 0.4f; "Recommended" -> 0.6f; else -> 0.8f }
        val scale = when(level) { "Extreme" -> 0.5f; "Recommended" -> 0.75f; else -> 1.0f }

        for (i in 0 until total) {
            onProgress(i + 1, total)
            // Render page to flattened bitmap to ensure max compression
            val bitmap = renderer.renderImage(i, scale, ImageType.RGB)
            val pdImage = JPEGFactory.createFromImage(targetDoc, bitmap, quality)
            val page = PDPage(PDRectangle(pdImage.width.toFloat(), pdImage.height.toFloat()))
            targetDoc.addPage(page)
            PDPageContentStream(targetDoc, page).use { it.drawImage(pdImage, 0f, 0f) }
            bitmap.recycle()
        }
        
        saveAndFlush(context, targetDoc, outputUri)
        sourceDoc.close()
    }
}

suspend fun convertImagesToPdf(context: Context, imageUris: List<Uri>, outputUri: Uri, pageSize: String, onProgress: (Int, Int) -> Unit) = withContext(Dispatchers.IO) {
    val document = PDDocument()
    imageUris.forEachIndexed { index, uri ->
        onProgress(index + 1, imageUris.size)
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val bitmap = BitmapFactory.decodeStream(inputStream) ?: return@use
            val pdImage = JPEGFactory.createFromImage(document, bitmap, 0.8f)
            val rect = if (pageSize == "A4") PDRectangle.A4 else PDRectangle(pdImage.width.toFloat(), pdImage.height.toFloat())
            val page = PDPage(rect)
            document.addPage(page)
            PDPageContentStream(document, page).use { cs ->
                if (pageSize == "A4") {
                    val sc = Math.min(rect.width / pdImage.width, rect.height / pdImage.height)
                    val x = (rect.width - pdImage.width * sc) / 2
                    val y = (rect.height - pdImage.height * sc) / 2
                    cs.drawImage(pdImage, x, y, pdImage.width * sc, pdImage.height * sc)
                } else cs.drawImage(pdImage, 0f, 0f)
            }
            bitmap.recycle()
        }
    }
    saveAndFlush(context, document, outputUri)
}

suspend fun convertPdfToImages(context: Context, pdfUri: Uri, outputUri: Uri, password: String?, selectedPages: List<Int>, format: String, quality: String, onProgress: (Int, Int) -> Unit) = withContext(Dispatchers.IO) {
    context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
        ZipOutputStream(outputStream).use { zipOut ->
            context.contentResolver.openInputStream(pdfUri)?.use { inputStream ->
                val document = if (password != null) PDDocument.load(inputStream, password) else PDDocument.load(inputStream)
                val renderer = PDFRenderer(document)
                val scale = when(quality) { "HD" -> 2.0f; "Standard" -> 1.5f; else -> 1.0f }
                val cf = if (format == "PNG") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                val ext = if (format == "PNG") "png" else "jpg"
                
                selectedPages.forEachIndexed { index, pageIdx ->
                    onProgress(index + 1, selectedPages.size)
                    // Use ImageType.RGB to ensure color is preserved
                    val bitmap = renderer.renderImage(pageIdx, scale, ImageType.RGB)
                    zipOut.putNextEntry(ZipEntry("page_${pageIdx + 1}.$ext"))
                    bitmap.compress(cf, 90, zipOut)
                    zipOut.closeEntry()
                    bitmap.recycle()
                }
                document.close()
            }
            zipOut.flush()
        }
    }
}

fun saveAndFlush(context: Context, document: PDDocument, outputUri: Uri) {
    val info = document.documentInformation
    info.creator = "PaperKnife+"
    info.producer = "PaperKnife+ Native Engine"
    val autoAuthor = PreferencesManager.getDefaultAuthor(context)
    if (autoAuthor.isNotEmpty()) info.author = autoAuthor
    
    // Write content
    context.contentResolver.openOutputStream(outputUri, "rwt")?.use { os ->
        document.save(os)
        os.flush()
        if (os is FileOutputStream) { try { os.fd.sync() } catch (e: Exception) { } }
    }
    document.close()
    
    // Explicitly update MediaStore to refresh file size
    try {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
            // Trigger a scan/update
            val file = File(outputUri.path ?: "")
            if (file.exists()) put(MediaStore.MediaColumns.SIZE, file.length())
        }
        context.contentResolver.update(outputUri, values, null, null)
    } catch (e: Exception) { }
}

suspend fun repairPdf(context: Context, inputUri: Uri, outputUri: Uri, password: String?) = withContext(Dispatchers.IO) {
    context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
        val document = if (password != null) PDDocument.load(inputStream, password) else PDDocument.load(inputStream)
        document.isAllSecurityToBeRemoved = true 
        saveAndFlush(context, document, outputUri)
    }
}
