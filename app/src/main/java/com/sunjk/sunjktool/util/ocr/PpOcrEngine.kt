package com.sunjk.sunjktool.util.ocr

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * PP-OCRv6 Small ONNX engine (det + rec, no cls).
 *
 * Pipeline: DB text detection → per-box crop → CTC recognition.
 *
 * Preprocessing verified against PaddleX inference.yml + RapidOCR source:
 * - det: BGR channel order, `(pixel/255 - [0.485,0.456,0.406]) / [0.229,0.224,0.225]`
 * - rec: BGR channel order, `(pixel/255 - 0.5) / 0.5` → [-1, 1]
 *
 * CTC classes = 18710: blank(0) + 18708 dict chars + space(18709).
 *
 * Simplifications vs PaddleOCR reference implementation:
 * - Axis-aligned boxes from connected components instead of rotated
 *   min-area rects + pyclipper unclip (fine for horizontal text; rotated
 *   text is out of scope — cls model intentionally omitted).
 */
class PpOcrEngine : OcrEngine {

    private companion object {
        // det params (from PP-OCRv6_small_det inference.yml)
        const val DET_LIMIT_SIDE = 960
        const val DET_THRESH = 0.2f
        const val DET_BOX_THRESH = 0.45f
        const val DET_UNCLIP = 1.4f
        const val DET_MIN_SIZE = 3
        val DET_MEAN = floatArrayOf(0.485f, 0.456f, 0.406f) // applied to B, G, R planes
        val DET_STD = floatArrayOf(0.229f, 0.224f, 0.225f)

        // rec params (from PP-OCRv6_small_rec inference.yml)
        const val REC_HEIGHT = 48
        const val REC_MAX_WIDTH = 3200
        const val CTC_NUM_CLASSES = 18710

        // source bitmap decode cap (avoid OOM on huge photos)
        const val MAX_DECODE_SIDE = 4096
    }

    private lateinit var appContext: Context
    private var env: OrtEnvironment? = null
    private var detSession: OrtSession? = null
    private var recSession: OrtSession? = null

    /** characters[0] = blank, [1..18708] = dict, [18709] = space */
    private var characters: List<String> = emptyList()

    /** Serialises inference — ORT sessions are used one call at a time. */
    private val inferenceMutex = Mutex()

    @Volatile
    private var initialized = false

    // ── OcrEngine ─────────────────────────────────────────────────────

    override fun initOnce(context: Context) {
        this.appContext = context.applicationContext
    }

    override suspend fun recognizeSingle(imagePath: String): String =
        withContext(Dispatchers.Default) {
            ensureLoaded()
            val bitmap = loadBitmap(imagePath) ?: return@withContext ""
            try {
                inferenceMutex.withLock { recognizeBitmap(bitmap) }.joinToString("\n")
            } finally {
                bitmap.recycle()
            }
        }

    override fun close() {
        synchronized(this) {
            detSession?.close(); detSession = null
            recSession?.close(); recSession = null
            initialized = false
            // OrtEnvironment is a process-wide singleton — do not close it.
            env = null
        }
    }

    // ── Model loading ─────────────────────────────────────────────────

    private fun ensureLoaded() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            check(::appContext.isInitialized) { "PpOcrEngine.initOnce() 未调用" }

            val dictRaw = appContext.assets.open("ppocr/ppocrv6_dict.txt")
                .bufferedReader().use { it.readLines() }
            characters = buildList(CTC_NUM_CLASSES) {
                add("")           // 0: blank
                addAll(dictRaw)   // 1..18708
                add(" ")          // 18709: space
            }
            require(characters.size == CTC_NUM_CLASSES) {
                "字典不匹配: 期望 $CTC_NUM_CLASSES 类, 实际 ${characters.size}"
            }

            val ortEnv = OrtEnvironment.getEnvironment()
            val opts = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(
                    Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
                )
            }
            val detBytes = appContext.assets.open("ppocr/det.onnx").use { it.readBytes() }
            val recBytes = appContext.assets.open("ppocr/rec.onnx").use { it.readBytes() }
            detSession = ortEnv.createSession(detBytes, opts)
            recSession = ortEnv.createSession(recBytes, opts)
            env = ortEnv
            initialized = true
        }
    }

    // ── Bitmap loading (with downsampling) ────────────────────────────

    private fun loadBitmap(path: String): Bitmap? {
        val file = File(path)
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_DECODE_SIDE) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(path, opts)
    }

    // ── Full-image pipeline ───────────────────────────────────────────

    private fun recognizeBitmap(src: Bitmap): List<String> {
        val det = detPreprocess(src)
        val (probMap, probH, probW) = runDet(det.data, det.h, det.w)
        val boxes = extractTextBoxes(
            probMap, probH, probW,
            det.scaleW, det.scaleH, src.width, src.height
        )
        if (boxes.isEmpty()) return emptyList()

        val lines = mutableListOf<String>()
        for (box in boxes) {
            val crop = try {
                Bitmap.createBitmap(src, box.x, box.y, box.w, box.h)
            } catch (_: Exception) {
                continue
            }
            val text = try {
                runRecAndDecode(crop)
            } finally {
                crop.recycle()
            }
            if (text.isNotBlank()) lines.add(text)
        }
        return lines
    }

    // ── Detection ─────────────────────────────────────────────────────

    private class DetInput(val data: FloatArray, val h: Int, val w: Int, val scaleW: Float, val scaleH: Float)

    private class TextBox(val x: Int, val y: Int, val w: Int, val h: Int)

    /**
     * Resize longest side to ≤960 (dims rounded to nearest multiple of 32,
     * matching DetPreProcess.resize), then BGR-CHW + ImageNet normalisation.
     */
    private fun detPreprocess(src: Bitmap): DetInput {
        val origW = src.width
        val origH = src.height
        val ratio = if (max(origW, origH) > DET_LIMIT_SIDE)
            DET_LIMIT_SIDE.toFloat() / max(origW, origH) else 1f
        val resizeW = roundTo32(origW * ratio)
        val resizeH = roundTo32(origH * ratio)

        val scaled = if (resizeW == origW && resizeH == origH) src
        else Bitmap.createScaledBitmap(src, resizeW, resizeH, true)
        val pixels = IntArray(resizeW * resizeH)
        scaled.getPixels(pixels, 0, resizeW, 0, 0, resizeW, resizeH)
        if (scaled !== src) scaled.recycle()

        val plane = resizeW * resizeH
        val chw = FloatArray(3 * plane)
        for (i in pixels.indices) {
            val p = pixels[i]
            val b = (p and 0xFF) / 255f
            val g = ((p shr 8) and 0xFF) / 255f
            val r = ((p shr 16) and 0xFF) / 255f
            chw[i] = (b - DET_MEAN[0]) / DET_STD[0]
            chw[plane + i] = (g - DET_MEAN[1]) / DET_STD[1]
            chw[2 * plane + i] = (r - DET_MEAN[2]) / DET_STD[2]
        }
        return DetInput(
            chw, resizeH, resizeW,
            resizeW.toFloat() / origW, resizeH.toFloat() / origH
        )
    }

    private fun roundTo32(value: Float): Int =
        max(32, (value / 32f).roundToInt() * 32)

    /** Runs det session; returns (probMap, H, W). Closes all native handles. */
    private fun runDet(data: FloatArray, h: Int, w: Int): Triple<FloatArray, Int, Int> {
        val session = detSession!!
        val shape = longArrayOf(1, 3, h.toLong(), w.toLong())
        OnnxTensor.createTensor(env!!, directFloatBuffer(data), shape).use { input ->
            session.run(mapOf(session.inputNames.first() to input)).use { result ->
                val out = result[0] as OnnxTensor
                val outShape = out.info.shape // [1, 1, H, W]
                val oh = outShape[2].toInt()
                val ow = outShape[3].toInt()
                val probMap = FloatArray(oh * ow)
                out.floatBuffer.get(probMap)
                return Triple(probMap, oh, ow)
            }
        }
    }

    /**
     * DB postprocess (simplified): binarise → 4-connected components →
     * score/size filter → unclip-style expansion → map to original coords →
     * sort into reading order (line grouping by vertical overlap).
     */
    private fun extractTextBoxes(
        probMap: FloatArray, h: Int, w: Int,
        scaleW: Float, scaleH: Float,
        origW: Int, origH: Int
    ): List<TextBox> {
        val binary = BooleanArray(h * w) { probMap[it] > DET_THRESH }
        val visited = BooleanArray(h * w)
        val queue = ArrayDeque<Int>()
        val raw = mutableListOf<TextBox>()

        for (start in binary.indices) {
            if (!binary[start] || visited[start]) continue
            var minX = start % w; var maxX = minX
            var minY = start / w; var maxY = minY
            var size = 0
            var scoreSum = 0f

            visited[start] = true
            queue.add(start)
            while (queue.isNotEmpty()) {
                val cur = queue.removeFirst()
                val cx = cur % w
                val cy = cur / w
                if (cx < minX) minX = cx; if (cx > maxX) maxX = cx
                if (cy < minY) minY = cy; if (cy > maxY) maxY = cy
                size++
                scoreSum += probMap[cur]

                // 4-neighbourhood
                if (cx > 0) tryVisit(cur - 1, binary, visited, queue)
                if (cx < w - 1) tryVisit(cur + 1, binary, visited, queue)
                if (cy > 0) tryVisit(cur - w, binary, visited, queue)
                if (cy < h - 1) tryVisit(cur + w, binary, visited, queue)
            }

            val bw = maxX - minX + 1
            val bh = maxY - minY + 1
            if (bw < DET_MIN_SIZE || bh < DET_MIN_SIZE) continue
            if (scoreSum / size < DET_BOX_THRESH) continue

            // Unclip approximation: expand by area * ratio / perimeter
            // (matches pyclipper offset distance for the same polygon)
            val expand = ((bw * bh) * DET_UNCLIP / (2f * (bw + bh))).roundToInt()
            val ex1 = max(0, minX - expand)
            val ey1 = max(0, minY - expand)
            val ex2 = min(w - 1, maxX + expand)
            val ey2 = min(h - 1, maxY + expand)

            // Map back to original image coordinates
            val ox = (ex1 / scaleW).toInt().coerceIn(0, origW - 1)
            val oy = (ey1 / scaleH).toInt().coerceIn(0, origH - 1)
            val owBox = (((ex2 - ex1 + 1) / scaleW).toInt()).coerceIn(1, origW - ox)
            val ohBox = (((ey2 - ey1 + 1) / scaleH).toInt()).coerceIn(1, origH - oy)
            raw.add(TextBox(ox, oy, owBox, ohBox))
        }
        return sortReadingOrder(raw)
    }

    private fun tryVisit(idx: Int, binary: BooleanArray, visited: BooleanArray, queue: ArrayDeque<Int>) {
        if (binary[idx] && !visited[idx]) {
            visited[idx] = true
            queue.add(idx)
        }
    }

    /** Group boxes into lines by vertical-centre overlap, then left→right. */
    private fun sortReadingOrder(boxes: List<TextBox>): List<TextBox> {
        if (boxes.size <= 1) return boxes
        val byTop = boxes.sortedBy { it.y }
        val lines = mutableListOf<MutableList<TextBox>>()
        for (box in byTop) {
            val centerY = box.y + box.h / 2f
            val line = lines.lastOrNull()?.takeIf { ln ->
                val ref = ln.first()
                centerY < ref.y + ref.h // box centre falls inside reference box's vertical span
            }
            if (line != null) line.add(box) else lines.add(mutableListOf(box))
        }
        return lines.flatMap { it.sortedBy { b -> b.x } }
    }

    // ── Recognition ───────────────────────────────────────────────────

    /** Crop → h=48 aspect-kept resize → BGR CHW [-1,1] → rec → CTC decode. */
    private fun runRecAndDecode(crop: Bitmap): String {
        val aspect = crop.width.toFloat() / crop.height
        val resizeW = (REC_HEIGHT * aspect).roundToInt().coerceIn(4, REC_MAX_WIDTH)
        val resized = Bitmap.createScaledBitmap(crop, resizeW, REC_HEIGHT, true)
        val pixels = IntArray(resizeW * REC_HEIGHT)
        resized.getPixels(pixels, 0, resizeW, 0, 0, resizeW, REC_HEIGHT)
        resized.recycle()

        val plane = resizeW * REC_HEIGHT
        val chw = FloatArray(3 * plane)
        for (i in pixels.indices) {
            val p = pixels[i]
            chw[i] = ((p and 0xFF) / 127.5f) - 1f               // B
            chw[plane + i] = (((p shr 8) and 0xFF) / 127.5f) - 1f  // G
            chw[2 * plane + i] = (((p shr 16) and 0xFF) / 127.5f) - 1f // R
        }

        val session = recSession!!
        val shape = longArrayOf(1, 3, REC_HEIGHT.toLong(), resizeW.toLong())
        OnnxTensor.createTensor(env!!, directFloatBuffer(chw), shape).use { input ->
            session.run(mapOf(session.inputNames.first() to input)).use { result ->
                val out = result[0] as OnnxTensor
                val outShape = out.info.shape // [1, T, 18710]
                return ctcDecode(out.floatBuffer, outShape[1].toInt(), outShape[2].toInt())
            }
        }
    }

    /** Argmax per timestep → CTC dedup → drop blank → map to chars. */
    private fun ctcDecode(fb: FloatBuffer, t: Int, c: Int): String {
        val sb = StringBuilder()
        var prev = -1
        for (i in 0 until t) {
            var maxIdx = 0
            var maxVal = fb.get(i * c)
            for (j in 1 until c) {
                val v = fb.get(i * c + j)
                if (v > maxVal) {
                    maxVal = v
                    maxIdx = j
                }
            }
            if (maxIdx != 0 && maxIdx != prev) {
                characters.getOrNull(maxIdx)?.let { sb.append(it) }
            }
            prev = maxIdx
        }
        return sb.toString()
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private fun directFloatBuffer(array: FloatArray): FloatBuffer =
        ByteBuffer.allocateDirect(array.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .also { it.put(array); it.rewind() }
}
