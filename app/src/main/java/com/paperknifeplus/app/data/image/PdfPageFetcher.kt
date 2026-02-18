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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class PdfPageRequest(
    val uri: Uri,
    val pageIndex: Int,
    val password: String? = null,
    val scale: Float = 1.0f,
    val prefetch: Boolean = true
)

/**
 * NITRO ENGINE: Renderer Pool.
 * Allows multiple pages of the same document to render in parallel.
 */
object NativeRendererPool {
    private val mutex = Mutex()
    private val pool = mutableMapOf<Uri, MutableList<Pair<PdfRenderer, ParcelFileDescriptor>>>()
    private val inUse = mutableSetOf<PdfRenderer>()

    suspend fun acquire(context: Context, uri: Uri): PdfRenderer? = mutex.withLock {
        val list = pool.getOrPut(uri) { mutableListOf() }
        
        // Find an idle renderer
        val idle = list.find { it.first !in inUse }
        if (idle != null) {
            inUse.add(idle.first)
            return idle.first
        }

        // Create new if pool is small (allow up to 4 per document for "Nitro" speed)
        if (list.size < 4) {
            try {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                if (pfd != null) {
                    val renderer = PdfRenderer(pfd)
                    list.add(renderer to pfd)
                    inUse.add(renderer)
                    return renderer
                }
            } catch (e: Exception) {}
        }
        return null
    }

    suspend fun release(renderer: PdfRenderer) = mutex.withLock {
        inUse.remove(renderer)
    }
}

// Global Semaphore to limit total system-wide PDF pressure (4 threads)
private val renderSemaphore = Semaphore(4)

class PdfPageFetcher(
    private val context: Context,
    private val data: PdfPageRequest
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Try Native Renderer (Ultra Fast Nitro Path)
            if (data.password == null) {
                val renderer = NativeRendererPool.acquire(context, data.uri)
                if (renderer != null && isActive) {
                    try {
                        renderSemaphore.withPermit {
                            if (!isActive) return@withContext null
                            
                            if (data.pageIndex < renderer.pageCount) {
                                val page = renderer.openPage(data.pageIndex)
                                try {
                                    val width = (page.width * data.scale).toInt().coerceAtLeast(1)
                                    val height = (page.height * data.scale).toInt().coerceAtLeast(1)
                                    
                                    // BITMAP POOLING: Enforce ARGB_8888 for native compatibility
                                    val bitmap = BitmapPool.get(width, height, Bitmap.Config.ARGB_8888)
                                    
                                    val canvas = Canvas(bitmap)
                                    canvas.drawColor(Color.WHITE)
                                    
                                    // Use PRINT mode which is often more robust for complex PDFs
                                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                                    
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
                    } finally {
                        NativeRendererPool.release(renderer)
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
