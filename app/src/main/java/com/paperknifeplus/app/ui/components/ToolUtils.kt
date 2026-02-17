package com.paperknifeplus.app.ui.components

import android.graphics.Bitmap
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun checkIsEncryptedLocal(context: android.content.Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
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

suspend fun verifyPasswordLocal(context: android.content.Context, uri: Uri, password: String): Boolean = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            PDDocument.load(inputStream, password).use { doc -> !doc.isEncrypted || true }
        } ?: false
    } catch (e: Exception) {
        false
    }
}

suspend fun loadPreview(context: android.content.Context, uri: Uri, password: String?): Bitmap? = withContext(Dispatchers.IO) {
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
        null
    }
}
