package com.sunjk.sunjktool.util.ocr

import android.content.Context

/**
 * OCR facade — uses the on-device PP-OCRv6 Small engine.
 * The engine is a process-wide singleton so ONNX sessions are loaded once.
 */
object OcrManager {

    private val ppOcr = PpOcrEngine()

    suspend fun recognize(context: Context, imagePaths: List<String>): String {
        if (imagePaths.isEmpty()) return ""
        ppOcr.initOnce(context)
        return ppOcr.recognize(imagePaths)
    }

    /** OCR with per-image progress updates via [onProgress]. */
    suspend fun recognizeWithProgress(
        context: Context,
        imagePaths: List<String>,
        onProgress: suspend (current: Int, total: Int) -> Unit
    ): String {
        if (imagePaths.isEmpty()) return ""
        ppOcr.initOnce(context)
        val results = mutableListOf<String>()
        for ((i, path) in imagePaths.withIndex()) {
            onProgress(i + 1, imagePaths.size)
            val text = ppOcr.recognizeSingle(path)
            if (text.isNotBlank()) results.add(text)
        }
        return results.joinToString("\n")
    }
}