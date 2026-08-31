package com.sunjk.sunjktool.util.ocr

import android.content.Context

/**
 * Abstraction over OCR backends. Thread-safe: a single engine instance may be
 * shared across callers; implementations must guard their own mutable state.
 */
interface OcrEngine {
    /** One-shot context injection before first call to [recognize]. */
    fun initOnce(context: Context)

    /**
     * Extract text from every image at [imagePaths]. The default implementation
     * iterates sequentially; engines may override (e.g. ML Kit benefits from
     * internal batching).
     */
    suspend fun recognize(imagePaths: List<String>): String {
        val results = mutableListOf<String>()
        for (path in imagePaths) {
            val text = recognizeSingle(path)
            if (text.isNotBlank()) results.add(text)
        }
        return results.joinToString("\n")
    }

    /** Engine-specific single-image entry point. */
    suspend fun recognizeSingle(imagePath: String): String

    /** Release native resources (ONNX sessions, etc.). No-op by default. */
    fun close() {}
}
