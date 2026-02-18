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

data class PdfPageRequest(
    val uri: Uri,
    val pageIndex: Int,
    val password: String? = null,
    val scale: Float = 1.0f
)

// Global mutex to serialize access to the non-thread-safe native PdfRenderer
private val rendererMutex = Mutex()

class PdfPageFetcher(
    private val context: Context,
    private val data: PdfPageRequest
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        // 1. Try Native Renderer (Ultra Fast, but needs Serial Access)
        if (data.password == null) {
            try {
                context.contentResolver.openFileDescriptor(data.uri, "r")?.use { pfd ->
                    // Mutex lock ensures only one page renders at a time, preventing native engine choke
                    rendererMutex.withLock {
                        val renderer = PdfRenderer(pfd)
                        try {
                            if (data.pageIndex < renderer.pageCount) {
                                val page = renderer.openPage(data.pageIndex)
                                
                                val width = (page.width * data.scale).toInt().coerceAtLeast(1)
                                val height = (page.height * data.scale).toInt().coerceAtLeast(1)
                                
                                // REUSE BITMAP FROM POOL
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
                        } finally {
                            renderer.close()
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback to PDFBox
            }
        }

        // 2. Fallback to PDFBox (Slower, but supports passwords)
        try {
            context.contentResolver.openInputStream(data.uri)?.use { inputStream ->
                val document = if (data.password != null) {
                    PDDocument.load(inputStream, data.password)
                } else {
                    PDDocument.load(inputStream)
                }
                
                try {
                    val renderer = com.tom_roush.pdfbox.rendering.PDFRenderer(document)
                    // PDFBox rendering is also CPU intensive, serialize it too if needed, 
                    // but usually used for single page previews anyway.
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
