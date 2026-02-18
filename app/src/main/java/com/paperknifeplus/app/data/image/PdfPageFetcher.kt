package com.paperknifeplus.app.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import com.paperknifeplus.app.ui.components.BitmapPool
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.ImageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PdfPageRequest(
    val uri: Uri,
    val pageIndex: Int,
    val password: String? = null,
    val scale: Float = 1.0f,
    val prefetch: Boolean = true
)

/**
 * Robust, Multi-Document Cache for the Native Engine.
 * Replicates the "Blitz" speed of original PaperKnife by keeping the C++ engine open.
 */
object NativeRendererCache {
    private val mutex = Mutex()
    
    private class CacheEntry(val renderer: PdfRenderer, val pfd: ParcelFileDescriptor)
    
    private val cache = object : LruCache<Uri, CacheEntry>(3) {
        override fun entryRemoved(evicted: Boolean, key: Uri?, oldValue: CacheEntry?, newValue: CacheEntry?) {
            try {
                oldValue?.renderer?.close()
                oldValue?.pfd?.close()
            } catch (e: Exception) {}
        }
    }

    suspend fun getRenderer(context: Context, uri: Uri): PdfRenderer? {
        mutex.withLock {
            cache.get(uri)?.let { return it.renderer }
            
            return try {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                if (pfd != null) {
                    val renderer = PdfRenderer(pfd)
                    cache.put(uri, CacheEntry(renderer, pfd))
                    renderer
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
}

// Global Mutex for serialized rendering - Native PdfRenderer is NOT thread-safe
private val drawMutex = Mutex()

class PdfPageFetcher(
    private val context: Context,
    private val data: PdfPageRequest
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Try Native Renderer (Ultra Fast)
            if (data.password == null) {
                val renderer = NativeRendererCache.getRenderer(context, data.uri)
                if (renderer != null && isActive) {
                    
                    drawMutex.withLock {
                        if (!isActive) return@withContext null
                        
                        if (data.pageIndex < renderer.pageCount) {
                            val page = renderer.openPage(data.pageIndex)
                            try {
                                val width = (page.width * data.scale).toInt().coerceAtLeast(1)
                                val height = (page.height * data.scale).toInt().coerceAtLeast(1)
                                
                                // BITMAP POOLING: Prevents GC stutter
                                val config = if (data.scale <= 0.6f) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
                                val bitmap = BitmapPool.get(width, height, config)
                                
                                // ROBUST RENDER: Fill with White before drawing
                                // This fixes "missing images" and transparency glitches
                                val canvas = Canvas(bitmap)
                                canvas.drawColor(Color.WHITE)
                                
                                // Hardware-accelerated native render
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                
                                return@withContext DrawableResult(
                                    drawable = android.graphics.drawable.BitmapDrawable(context.resources, bitmap),
                                    isSampled = data.scale < 1.0f,
                                    dataSource = DataSource.DISK
                                )
                            } finally {
                                try { page.close() } catch (e: Exception) {}
                            }
                        }
                    }
                }
            }

            // 2. Fallback to PDFBox (For Passwords)
            if (!isActive) return@withContext null
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
        }.getOrNull()
    }

    class Factory(private val context: Context) : Fetcher.Factory<PdfPageRequest> {
        override fun create(data: PdfPageRequest, options: Options, imageLoader: ImageLoader): Fetcher {
            return PdfPageFetcher(context, data)
        }
    }
}
