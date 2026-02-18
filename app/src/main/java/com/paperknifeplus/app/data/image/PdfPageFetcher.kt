package com.paperknifeplus.app.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.Size
import com.paperknifeplus.app.ui.components.BitmapPool
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.ImageType
import com.tom_roush.pdfbox.rendering.PDFRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlinx.coroutines.isActive

data class PdfPageRequest(
    val uri: Uri,
    val pageIndex: Int,
    val password: String? = null,
    val scale: Float = 1.0f
)

/**
 * High-performance Cache for the Native Engine.
 * This ensures we only parse the PDF structure ONCE per document, 
 * matching the speed of iLovePDF and original PaperKnife.
 */
object NativeRendererCache {
    private val mutex = Mutex()
    private var currentUri: Uri? = null
    private var renderer: PdfRenderer? = null
    private var pfd: ParcelFileDescriptor? = null

    suspend fun getRenderer(context: Context, uri: Uri): PdfRenderer? {
        if (currentUri == uri && renderer != null) return renderer
        
        mutex.withLock {
            // double check after lock
            if (currentUri == uri && renderer != null) return renderer
            
            // Close old
            renderer?.close()
            pfd?.close()
            
            // Open new
            return try {
                val newPfd = context.contentResolver.openFileDescriptor(uri, "r")
                val newRenderer = PdfRenderer(newPfd!!)
                pfd = newPfd
                renderer = newRenderer
                currentUri = uri
                newRenderer
            } catch (e: Exception) {
                null
            }
        }
    }
}

// Separate mutex for the actual drawing to prevent thread conflicts in the native layer
private val drawMutex = Mutex()

class PdfPageFetcher(
    private val context: Context,
    private val data: PdfPageRequest
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        // 1. Try Native Renderer (Turbo Mode)
        if (data.password == null) {
            try {
                val renderer = NativeRendererCache.getRenderer(context, data.uri)
                if (renderer != null && coroutineContext.isActive) {
                    
                    drawMutex.withLock {
                        if (!coroutineContext.isActive) return@withContext null
                        
                        if (data.pageIndex < renderer.pageCount) {
                            val page = renderer.openPage(data.pageIndex)
                            
                            val width = (page.width * data.scale).toInt().coerceAtLeast(1)
                            val height = (page.height * data.scale).toInt().coerceAtLeast(1)
                            
                            // ZERO ALLOCATION: Reuse bitmap from pool
                            val config = if (data.scale <= 0.5f) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
                            val bitmap = BitmapPool.get(width, height, config)
                            
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            page.close()
                            
                            return@withContext DrawableResult(
                                drawable = android.graphics.drawable.BitmapDrawable(context.resources, bitmap),
                                isSampled = data.scale < 1.0f,
                                dataSource = DataSource.DISK
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback to PDFBox if native fails
            }
        }

        // 2. Fallback to PDFBox (For Passwords)
        if (!coroutineContext.isActive) return@withContext null
        try {
            context.contentResolver.openInputStream(data.uri)?.use { inputStream ->
                val document = if (data.password != null) {
                    PDDocument.load(inputStream, data.password)
                } else {
                    PDDocument.load(inputStream)
                }
                
                try {
                    val renderer = com.tom_roush.pdfbox.rendering.PDFRenderer(document)
                    val bitmap = renderer.renderImage(data.pageIndex, data.scale, ImageType.RGB)
                    
                    return@withContext DrawableResult(
                        drawable = android.graphics.drawable.BitmapDrawable(context.resources, bitmap),
                        isSampled = data.scale < 1.0f,
                        dataSource = DataSource.DISK
                    )
                } finally {
                    document.close()
                }
            }
        } catch (e: Exception) {
            return@withContext null
        }
        return@withContext null
    }

    class Factory(private val context: Context) : Fetcher.Factory<PdfPageRequest> {
        override fun create(data: PdfPageRequest, options: Options, imageLoader: ImageLoader): Fetcher {
            return PdfPageFetcher(context, data)
        }
    }
}
