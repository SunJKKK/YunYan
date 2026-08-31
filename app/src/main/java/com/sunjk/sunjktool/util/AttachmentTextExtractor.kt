package com.sunjk.sunjktool.util

import com.tom_roush.pdfbox.pdmodel.PDDocument
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.util.zip.ZipInputStream

object AttachmentTextExtractor {

    suspend fun extract(context: Context, path: String): String {
        val file = File(path)
        if (!file.exists()) return ""
        return when (file.extension.lowercase()) {
            "docx" -> extractDocx(file)
            "pdf" -> extractPdf(context, file)
            else -> ""
        }
    }

    private fun extractDocx(file: File): String {
        return try {
            ZipInputStream(file.inputStream().buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.name == "word/document.xml") {
                        val xml = zip.readBytes().toString(Charsets.UTF_8)
                        return parseDocxXml(xml)
                    }
                    zip.closeEntry()
                }
                ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun parseDocxXml(xml: String): String {
        return try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(xml.reader())
            val sb = StringBuilder()
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.TEXT) {
                    sb.append(parser.text)
                } else if (event == XmlPullParser.END_TAG && parser.name == "p") {
                    sb.append("\n")
                }
                event = parser.next()
            }
            sb.toString().trim()
        } catch (_: Exception) {
            ""
        }
    }

    private suspend fun extractPdf(context: Context, file: File): String {
        val text = try {
            PDDocument.load(file).use { doc ->
                PDFTextStripper().getText(doc)
            }.trim()
        } catch (_: Exception) {
            ""
        }
        if (text.isNotBlank()) return text
        return try {
            val cacheDir = File(context.cacheDir, "pdf_ocr").apply { mkdirs() }
            val pageFiles = mutableListOf<String>()
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    for (i in 0 until renderer.pageCount) {
                        renderer.openPage(i).use { page ->
                            val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(Color.WHITE)
                            val canvas = Canvas(bitmap)
                            canvas.scale(2f, 2f)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            val out = File(cacheDir, "page_${System.currentTimeMillis()}_$i.png")
                            out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                            bitmap.recycle()
                            pageFiles.add(out.absolutePath)
                        }
                    }
                }
            }
            if (pageFiles.isEmpty()) "" else com.sunjk.sunjktool.util.ocr.OcrManager.recognize(context, pageFiles)
        } catch (_: Exception) {
            ""
        }
    }
}