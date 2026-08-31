package com.sunjk.sunjktool.feature.learninglog.edit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.roundToInt

private data class CropRatio(val label: String, val w: Float, val h: Float)

private val cropRatios = listOf(
    CropRatio("自由", 0f, 0f),
    CropRatio("1:1", 1f, 1f),
    CropRatio("4:3", 4f, 3f),
    CropRatio("3:4", 3f, 4f),
    CropRatio("16:9", 16f, 9f),
    CropRatio("9:16", 9f, 16f),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropScreen(
    filePath: String,
    onDismiss: () -> Unit,
    onCropped: (String) -> Unit
) {
    val bitmap = remember(filePath) {
        BitmapFactory.decodeFile(filePath)?.asImageBitmap()
    } ?: run { onDismiss(); return }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var selectedRatioIdx by remember { mutableIntStateOf(0) }

    // Crop frame: left, top, width, height in canvas pixels
    var cropLeft by remember { mutableFloatStateOf(0f) }
    var cropTop by remember { mutableFloatStateOf(0f) }
    var cropW by remember { mutableFloatStateOf(0f) }
    var cropH by remember { mutableFloatStateOf(0f) }

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val barH = remember(density) { with(density) { 72.dp.roundToPx() } }
    val marginPx = remember(density) { with(density) { 24.dp.toPx() } }
    val minCropPx = remember(density) { with(density) { 100.dp.toPx() } }
    val handleSizePx = remember(density) { with(density) { 24.dp.toPx() } }
    val handleHitPx = remember(density) { with(density) { 44.dp.toPx() } }

    // Initialize / recalculate crop frame when canvas or ratio changes (only in free mode)
    LaunchedEffect(canvasSize, selectedRatioIdx) {
        if (canvasSize == IntSize.Zero) return@LaunchedEffect
        val maxW = canvasSize.width.toFloat() - marginPx * 2f
        val maxH = (canvasSize.height - barH).toFloat() - marginPx * 2f
        val ratio = cropRatios[selectedRatioIdx]
        val cw: Float; val ch: Float
        if (ratio.w == 0f) {
            // Free mode: fit within max area maintaining image aspect ratio
            val imgRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            if (maxW / imgRatio <= maxH) { cw = maxW; ch = maxW / imgRatio }
            else { cw = maxH * imgRatio; ch = maxH }
        } else {
            val hFromW = maxW / ratio.w * ratio.h
            if (hFromW <= maxH) { cw = maxW; ch = hFromW }
            else { cw = maxH / ratio.h * ratio.w; ch = maxH }
        }
        cropW = cw
        cropH = ch
        cropLeft = (canvasSize.width - cw) / 2f
        cropTop = ((canvasSize.height - barH) - ch) / 2f
        scale = minOf(cw / bitmap.width, ch / bitmap.height)
        offsetX = 0f
        offsetY = 0f
    }

    /** Snap crop frame for fixed-ratio mode: maintain aspect ratio from the fixed corner. */
    fun snapRatio(fixedLeft: Float, fixedTop: Float, fixedRight: Float, fixedBottom: Float) {
        val ratio = cropRatios[selectedRatioIdx]
        if (ratio.w == 0f) return
        val targetRatio = ratio.w / ratio.h
        val newW: Float; val newH: Float
        val rawW = abs(fixedRight - fixedLeft)
        val rawH = abs(fixedBottom - fixedTop)
        // Determine which dimension to constrain by
        if (rawW / rawH > targetRatio) {
            // Too wide — constrain width by height
            newH = rawH
            newW = newH * targetRatio
        } else {
            // Too tall — constrain height by width
            newW = rawW
            newH = newW / targetRatio
        }
        // Adjust from the fixed corner
        cropLeft = if (fixedLeft < (cropLeft + cropW / 2f)) fixedLeft else fixedRight - newW
        cropTop = if (fixedTop < (cropTop + cropH / 2f)) fixedTop else fixedBottom - newH
        cropW = newW.coerceAtLeast(minCropPx)
        cropH = newH.coerceAtLeast(minCropPx)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            containerColor = Color.Black,
            topBar = {
                TopAppBar(
                    title = { Text("裁剪图片", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "取消", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            scope.launch {
                                val cropped = cropBitmap(
                                    srcPath = filePath,
                                    bitmap = bitmap,
                                    canvasSize = canvasSize,
                                    availableH = canvasSize.height - barH,
                                    cropLeft = cropLeft,
                                    cropTop = cropTop,
                                    cropW = cropW,
                                    cropH = cropH,
                                    scale = scale,
                                    offsetX = offsetX,
                                    offsetY = offsetY
                                )
                                if (cropped != null) onCropped(cropped)
                                else onDismiss()
                            }
                        }) {
                            Icon(Icons.Default.Check, "确认", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
                )
            },
            bottomBar = {
                Surface(color = Color.Black.copy(alpha = 0.9f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        cropRatios.forEachIndexed { idx, r ->
                            val selected = idx == selectedRatioIdx
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f),
                                modifier = Modifier.clickable {
                                    selectedRatioIdx = idx
                                }
                            ) {
                                Text(
                                    r.label,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color.Black)
                    .onSizeChanged { canvasSize = it }
            ) {
                if (canvasSize != IntSize.Zero && cropW > 0f && cropH > 0f) {
                    val imgDisplayW = bitmap.width * scale
                    val imgDisplayH = bitmap.height * scale

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(
                                        minOf(cropW / bitmap.width, cropH / bitmap.height),
                                        5f
                                    )
                                    offsetX += pan.x
                                    offsetY += pan.y
                                }
                            }
                    ) {
                        // Darken outside crop area
                        drawRect(Color.Black.copy(alpha = 0.55f))

                        // Draw image clipped to crop rect
                        clipRect(
                            left = cropLeft, top = cropTop,
                            right = cropLeft + cropW,
                            bottom = cropTop + cropH
                        ) {
                            drawImage(
                                image = bitmap,
                                dstOffset = IntOffset(
                                    (cropLeft + imgDisplayW / 2 + offsetX - bitmap.width * scale / 2).roundToInt(),
                                    (cropTop + imgDisplayH / 2 + offsetY - bitmap.height * scale / 2).roundToInt()
                                ),
                                dstSize = IntSize(imgDisplayW.roundToInt(), imgDisplayH.roundToInt())
                            )
                        }

                        // Crop border
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(cropLeft, cropTop),
                            size = Size(cropW, cropH),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Corner handles
                        val handleR = handleSizePx / 2f
                        val handleColor = Color.White
                        listOf(
                            Offset(cropLeft, cropTop),
                            Offset(cropLeft + cropW, cropTop),
                            Offset(cropLeft, cropTop + cropH),
                            Offset(cropLeft + cropW, cropTop + cropH)
                        ).forEach { corner ->
                            drawCircle(Color.White.copy(alpha = 0.3f), handleHitPx / 2f, corner)
                            drawCircle(handleColor, handleR, corner, style = Stroke(2.dp.toPx()))
                        }

                        // Grid lines (rule of thirds)
                        val gridColor = Color.White.copy(alpha = 0.25f)
                        for (i in 1..2) {
                            val gx = cropLeft + cropW * i / 3f
                            val gy = cropTop + cropH * i / 3f
                            drawLine(gridColor, Offset(gx, cropTop), Offset(gx, cropTop + cropH), 1.dp.toPx())
                            drawLine(gridColor, Offset(cropLeft, gy), Offset(cropLeft + cropW, gy), 1.dp.toPx())
                        }
                    }

                    // ── Corner drag handles (overlaid as Box composables) ──

                    @Composable
                    fun CornerHandle(
                        alignX: Alignment.Horizontal,
                        alignY: Alignment.Vertical,
                        onDrag: (dx: Float, dy: Float) -> Unit
                    ) {
                        val den = LocalDensity.current
                        Box(
                            modifier = Modifier
                                .size(with(den) { handleSizePx.toDp() })
                                .align(Alignment.TopStart)
                                .offset(
                                    x = with(den) {
                                        when (alignX) {
                                            Alignment.Start -> (cropLeft - handleSizePx / 2f).toDp()
                                            else -> (cropLeft + cropW - handleSizePx / 2f).toDp()
                                        }
                                    },
                                    y = with(den) {
                                        when (alignY) {
                                            Alignment.Top -> (cropTop - handleSizePx / 2f).toDp()
                                            else -> (cropTop + cropH - handleSizePx / 2f).toDp()
                                        }
                                    }
                                )
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        onDrag(dragAmount.x, dragAmount.y)
                                    }
                                }
                        )
                    }

                    // Top-left
                    CornerHandle(Alignment.Start, Alignment.Top) { dx, dy ->
                        val newLeft = (cropLeft + dx).coerceIn(marginPx, cropLeft + cropW - minCropPx)
                        val newTop = (cropTop + dy).coerceIn(marginPx, cropTop + cropH - minCropPx)
                        val newW = (cropLeft + cropW - newLeft).coerceAtLeast(minCropPx)
                        val newH = (cropTop + cropH - newTop).coerceAtLeast(minCropPx)
                        cropLeft = newLeft
                        cropTop = newTop
                        cropW = newW
                        cropH = newH
                        if (selectedRatioIdx != 0) snapRatio(cropLeft, cropTop, cropLeft + cropW, cropTop + cropH)
                    }

                    // Top-right
                    CornerHandle(Alignment.End, Alignment.Top) { dx, dy ->
                        val newW = (cropW + dx).coerceAtLeast(minCropPx).coerceAtMost(canvasSize.width - cropLeft - marginPx)
                        val newTop = (cropTop + dy).coerceIn(marginPx, cropTop + cropH - minCropPx)
                        val newH = (cropTop + cropH - newTop).coerceAtLeast(minCropPx)
                        cropW = newW
                        cropTop = newTop
                        cropH = newH
                        if (selectedRatioIdx != 0) snapRatio(cropLeft, cropTop, cropLeft + cropW, cropTop + cropH)
                    }

                    // Bottom-left
                    CornerHandle(Alignment.Start, Alignment.Bottom) { dx, dy ->
                        val newLeft = (cropLeft + dx).coerceIn(marginPx, cropLeft + cropW - minCropPx)
                        val newH = (cropH + dy).coerceAtLeast(minCropPx).coerceAtMost(canvasSize.height - barH - cropTop - marginPx)
                        val newW = (cropLeft + cropW - newLeft).coerceAtLeast(minCropPx)
                        cropLeft = newLeft
                        cropW = newW
                        cropH = newH
                        if (selectedRatioIdx != 0) snapRatio(cropLeft, cropTop, cropLeft + cropW, cropTop + cropH)
                    }

                    // Bottom-right
                    CornerHandle(Alignment.End, Alignment.Bottom) { dx, dy ->
                        val newW = (cropW + dx).coerceAtLeast(minCropPx).coerceAtMost(canvasSize.width - cropLeft - marginPx)
                        val newH = (cropH + dy).coerceAtLeast(minCropPx).coerceAtMost(canvasSize.height - barH - cropTop - marginPx)
                        cropW = newW
                        cropH = newH
                        if (selectedRatioIdx != 0) snapRatio(cropLeft, cropTop, cropLeft + cropW, cropTop + cropH)
                    }
                }
            }
        }
    }
}

private suspend fun cropBitmap(
    srcPath: String,
    bitmap: ImageBitmap,
    canvasSize: IntSize,
    availableH: Int,
    cropLeft: Float,
    cropTop: Float,
    cropW: Float,
    cropH: Float,
    scale: Float,
    offsetX: Float,
    offsetY: Float
): String? = withContext(Dispatchers.IO) {
    try {
        val src = BitmapFactory.decodeFile(srcPath) ?: return@withContext null
        val imgCenterX = cropLeft + bitmap.width * scale / 2f + offsetX
        val imgCenterY = cropTop + bitmap.height * scale / 2f + offsetY
        val srcLeft = ((cropLeft - imgCenterX + bitmap.width * scale / 2) / scale).roundToInt().coerceIn(0, src.width)
        val srcTop = ((cropTop - imgCenterY + bitmap.height * scale / 2) / scale).roundToInt().coerceIn(0, src.height)
        val srcW = (cropW / scale).roundToInt().coerceAtMost(src.width - srcLeft)
        val srcH = (cropH / scale).roundToInt().coerceAtMost(src.height - srcTop)

        val cropped = Bitmap.createBitmap(src, srcLeft, srcTop, srcW.coerceAtLeast(1), srcH.coerceAtLeast(1))
        src.recycle()

        val outFile = File(srcPath.replace(".jpg", "_crop.jpg").replace(".png", "_crop.png"))
        FileOutputStream(outFile).use { out ->
            cropped.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        cropped.recycle()
        outFile.absolutePath
    } catch (_: Exception) { null }
}

