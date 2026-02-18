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
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.ImageType
import com.tom_roush.pdfbox.rendering.PDFRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class PdfPageRequest(
    val uri: Uri,
    val pageIndex: Int,
    val password: String? = null
)

class PdfPageFetcher(
    private val context: Context,
    private val data: PdfPageRequest
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        // 1. Try Native Renderer (Fastest)
        if (data.password == null) {
            try {
                context.contentResolver.openFileDescriptor(data.uri, "r")?.use { pfd ->
                    val renderer = PdfRenderer(pfd)
                    if (data.pageIndex < renderer.pageCount) {
                        val page = renderer.openPage(data.pageIndex)
                        
                        // Calculate optimal scale for thumbnails vs full screen
                        // Standard thumbnail size is usually around 300px width
                        val width = page.width
                        val height = page.height
                        
                        // Use RGB_565 for thumbnails to save 50% RAM
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        renderer.close()
                        
                        return@withContext DrawableResult(
                            drawable = android.graphics.drawable.BitmapDrawable(context.resources, bitmap),
                            isSampled = true,
                            dataSource = DataSource.DISK
                        )
                    }
                    renderer.close()
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
                
                val renderer = com.tom_roush.pdfbox.rendering.PDFRenderer(document)
                // 0.5f scale is usually plenty for thumbnails
                val bitmap = renderer.renderImage(data.pageIndex, 0.5f, ImageType.RGB)
                document.close()
                
                return@withContext DrawableResult(
                    drawable = android.graphics.drawable.BitmapDrawable(context.resources, bitmap),
                    isSampled = true,
                    dataSource = DataSource.DISK
                )
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
