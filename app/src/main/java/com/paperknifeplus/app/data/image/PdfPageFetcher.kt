package com.paperknifeplus.app.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import com.paperknifeplus.app.ui.components.BitmapPool
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.ImageType
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class PdfPageRequest(
    val uri: Uri,
    val pageIndex: Int,
    val password: String? = null,
    val scale: Float = 1.0f,
    val priority: Int = 0 // 0: Prefetch, 1: High (Current View)
)

/**
 * NITRO ENGINE 4.0: Native Renderer Pool.
 * Manages native PdfRenderer instances for high-speed rendering.
 */
object NativeRendererPool {
    private val mutex = Mutex()
    private val pool = mutableMapOf<Uri, MutableList<Pair<PdfRenderer, ParcelFileDescriptor>>>()
    private val inUse = mutableSetOf<PdfRenderer>()

    suspend fun acquire(context: Context, uri: Uri): PdfRenderer? = mutex.withLock {
        if (pool.size > 10) {
            val oldest = pool.keys.first()
            pool.remove(oldest)?.forEach { (r, p) -> 
                try { r.close(); p.close() } catch (e: Exception) {}
            }
        }

        val list = pool.getOrPut(uri) { mutableListOf() }
        val idle = list.find { it.first !in inUse }
        if (idle != null) {
            inUse.add(idle.first)
            return idle.first
        }

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

/**
 * NITRO ENGINE 4.0: PDFBox Document Pool.
 * Keeps PDDocument instances open to prevent expensive re-parsing on every page fetch.
 */
object PdDocumentPool {
    private val mutex = Mutex()
    private val docPool = mutableMapOf<Uri, PDDocument>()
    private val lastUsed = mutableMapOf<Uri, Long>()

    suspend fun acquire(context: Context, uri: Uri, password: String?): PDDocument? = mutex.withLock {
        docPool[uri]?.let {
            lastUsed[uri] = System.currentTimeMillis()
            return it
        }

        // Limit pool size (max 3 docs) to conserve memory
        if (docPool.size >= 3) {
            val oldest = lastUsed.minByOrNull { it.value }?.key
            if (oldest != null) {
                docPool.remove(oldest)?.close()
                lastUsed.remove(oldest)
            }
        }

        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val mem = MemoryUsageSetting.setupMainMemoryOnly()
                val doc = if (password != null) PDDocument.load(inputStream, password, mem) else PDDocument.load(inputStream, mem)
                docPool[uri] = doc
                lastUsed[uri] = System.currentTimeMillis()
                doc
            }
        } catch (e: Exception) { null }
    }

    suspend fun invalidate(uri: Uri) = mutex.withLock {
        docPool.remove(uri)?.close()
        lastUsed.remove(uri)
    }
}

// Global Concurrency Controller (Nitro Blitz Throttle)
private val renderSemaphore = Semaphore(4)
private val highPriorityDispatcher = Dispatchers.IO
private val lowPriorityDispatcher = Dispatchers.Default

class PdfPageFetcher(
    private val context: Context,
    private val data: PdfPageRequest
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(if (data.priority > 0) highPriorityDispatcher else lowPriorityDispatcher) {
        runCatching {
            // 1. Try Native Renderer (Turbo Path)
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
                                    
                                    val bitmap = BitmapPool.get(width, height, Bitmap.Config.ARGB_8888)
                                    val canvas = Canvas(bitmap)
                                    canvas.drawColor(Color.WHITE)
                                    
                                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                                    
                                    return@withContext DrawableResult(
                                        drawable = android.graphics.drawable.BitmapDrawable(context.resources, bitmap),
                                        isSampled = data.scale < 1.0f,
                                        dataSource = DataSource.MEMORY
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

            // 2. Try Pooled PDFBox (Fallback / Protected)
            if (!isActive) return@withContext null
            val document = PdDocumentPool.acquire(context, data.uri, data.password)
            if (document != null && isActive) {
                renderSemaphore.withPermit {
                    if (!isActive) return@withContext null
                    val renderer = com.tom_roush.pdfbox.rendering.PDFRenderer(document)
                    val bitmap = renderer.renderImage(data.pageIndex, data.scale, ImageType.RGB)
                    
                    return@withContext DrawableResult(
                        drawable = android.graphics.drawable.BitmapDrawable(context.resources, bitmap),
                        isSampled = data.scale < 1.0f,
                        dataSource = DataSource.MEMORY
                    )
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
