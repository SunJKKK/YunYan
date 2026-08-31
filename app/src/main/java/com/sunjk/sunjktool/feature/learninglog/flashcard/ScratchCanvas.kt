package com.sunjk.sunjktool.feature.learninglog.flashcard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.abs

// ── Data ────────────────────────────────────────────────────────────────

private data class PenStroke(
    val points: List<Offset>,
    val color: Color,
    val width: Float
)

// ── Presets ─────────────────────────────────────────────────────────────

private val PEN_COLORS = listOf(
    "黑色" to Color.Black,
    "红色" to Color(0xFFD32F2F),
    "蓝色" to Color(0xFF1976D2)
)

private val PEN_WIDTHS = listOf(
    "细" to 3f,
    "中" to 6f,
    "粗" to 12f
)

private const val ERASER_HIT_RADIUS = 30f

// ── Entry point ─────────────────────────────────────────────────────────

/**
 * Full-screen transparent drawing overlay.
 *
 * Rendered inside a parent [Box] so that no scrim / dim layer is applied
 * (unlike [androidx.compose.ui.window.Dialog]).
 */
@Composable
fun ScratchCanvas(onClose: () -> Unit) {
    val strokes = remember { mutableStateListOf<PenStroke>() }
    var currentColor by remember { mutableStateOf(PEN_COLORS[0].second) }
    var currentWidth by remember { mutableStateOf(PEN_WIDTHS[1].second) }
    var isEraser by remember { mutableStateOf(false) }
    var currentPoints = remember { mutableStateListOf<Offset>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // ── Drawing canvas ──
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isEraser, currentColor, currentWidth) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            val pos = change.position

                            if (isEraser) {
                                if (change.pressed) {
                                    val hitIdx = strokes.indexOfLast { stroke ->
                                        isNearStroke(pos, stroke, ERASER_HIT_RADIUS + stroke.width)
                                    }
                                    if (hitIdx >= 0) strokes.removeAt(hitIdx)
                                }
                            } else {
                                when {
                                    change.pressed && !change.previousPressed -> {
                                        currentPoints.clear()
                                        currentPoints.add(pos)
                                    }
                                    change.pressed -> currentPoints.add(pos)
                                    !change.pressed && change.previousPressed -> {
                                        if (currentPoints.size >= 2) {
                                            strokes.add(
                                                PenStroke(
                                                    points = currentPoints.toList(),
                                                    color = currentColor,
                                                    width = currentWidth
                                                )
                                            )
                                        }
                                        currentPoints.clear()
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            for (stroke in strokes) {
                val path = pointsToPath(stroke.points)
                drawPath(path, stroke.color, style = Stroke(stroke.width, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            if (currentPoints.size >= 2) {
                val path = pointsToPath(currentPoints.toList())
                drawPath(path, currentColor, style = Stroke(currentWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }

        // ── Bottom control bar ──
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
            tonalElevation = 3.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ── Color pickers ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PEN_COLORS.forEach { (label, color) ->
                        val selected = !isEraser && currentColor == color
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (selected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
                                )
                                .clickable { isEraser = false; currentColor = color },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                // ── Width pickers ──
                PEN_WIDTHS.forEach { (label, width) ->
                    val selected = !isEraser && currentWidth == width
                    FilterChip(
                        selected = selected,
                        onClick = { isEraser = false; currentWidth = width },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.padding(horizontal = 2.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        )
                    )
                }

                Spacer(Modifier.weight(1f))

                // ── Eraser toggle ──
                FilterChip(
                    selected = isEraser,
                    onClick = { isEraser = !isEraser },
                    label = { Text("橡皮", style = MaterialTheme.typography.labelMedium) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.DeleteSweep,
                            null,
                            Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.padding(horizontal = 2.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    )
                )

                // Close via top-bar scratch button toggle; no in-canvas close button
            }
        }
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────

private fun pointsToPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    for (i in 1 until points.size) path.lineTo(points[i].x, points[i].y)
    return path
}

private fun isNearStroke(point: Offset, stroke: PenStroke, threshold: Float): Boolean {
    val pts = stroke.points
    if (pts.isEmpty()) return false
    if (pts.size == 1) return (point - pts[0]).getDistance() < threshold
    for (i in 0 until pts.size - 1) {
        if (pointToSegmentDistance(point, pts[i], pts[i + 1]) < threshold) return true
    }
    return false
}

private fun pointToSegmentDistance(p: Offset, a: Offset, b: Offset): Float {
    val ab = b - a
    val ap = p - a
    val abLenSq = ab.x * ab.x + ab.y * ab.y
    if (abLenSq == 0f) return (p - a).getDistance()
    val t = ((ap.x * ab.x + ap.y * ab.y) / abLenSq).coerceIn(0f, 1f)
    val nearest = Offset(a.x + t * ab.x, a.y + t * ab.y)
    return (p - nearest).getDistance()
}
