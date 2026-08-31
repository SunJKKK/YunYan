package com.sunjk.sunjktool.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore

/**
 * 平滑区域折线图（基于 Vico 3 的 LineCartesianLayer 实现）。
 *
 * 特性：
 *  - 三次贝塞尔曲线平滑连接数据点（PointConnector.cubic()）。
 *  - 曲线下方绘制从主题色渐变到透明的"阴影"填充区（AreaFill + vertical gradient）。
 *  - 数据变化时通过 Vico 的 diff 动画平滑过渡（runTransaction + lineSeries）。
 *
 * @param values 归一化后的 0..1 数据点（y 值），x 在 0..(size-1) 上等间距分布。
 * @param lineColor 曲线与渐变主色。
 * @param lineWidth 曲线线宽。
 * @param modifier 应用于图表容器的修饰符。
 * @param showEndDot 是否在最后一个数据点处绘制圆点（仅末端一个点）。
 */
@Composable
fun VicoAreaLineChart(
    values: List<Float>,
    lineColor: Color,
    lineWidth: Dp = 2.dp,
    modifier: Modifier = Modifier,
    showEndDot: Boolean = false
) {
    if (values.size < 2) return

    // 终点圆点（仅最后一个点，与旧 SmoothAreaLineChart 行为一致）。
    val endDotPoint = remember(lineColor, lineWidth, showEndDot, values.size) {
        if (showEndDot) {
            val diameter = (lineWidth.value + 2f) * 2f
            LineCartesianLayer.Point(
                component = ShapeComponent(fill = Fill(lineColor), shape = CircleShape),
                size = diameter.dp
            )
        } else {
            null
        }
    }
    val pointProvider: LineCartesianLayer.PointProvider? = remember(endDotPoint, values.size) {
        val dot = endDotPoint
        if (dot != null) {
            val lastX = (values.size - 1).toDouble()
            object : LineCartesianLayer.PointProvider {
                override fun getPoint(
                    entry: LineCartesianLayerModel.Entry,
                    seriesIndex: Int,
                    extraStore: ExtraStore,
                ): LineCartesianLayer.Point? = if (entry.x == lastX) dot else null

                override fun getLargestPoint(extraStore: ExtraStore): LineCartesianLayer.Point? =
                    dot
            }
        } else {
            null
        }
    }

    val line = remember(lineColor, lineWidth, pointProvider) {
        LineCartesianLayer.Line(
            fill = LineCartesianLayer.LineFill.single(Fill(lineColor)),
            stroke = LineCartesianLayer.LineStroke.Continuous(lineWidth, cap = StrokeCap.Round),
            areaFill = LineCartesianLayer.AreaFill.single(
                Fill(
                    Brush.verticalGradient(
                        listOf(lineColor.copy(alpha = 0.35f), lineColor.copy(alpha = 0f))
                    )
                )
            ),
            pointConnector = LineCartesianLayer.PointConnector.cubic(),
            pointProvider = pointProvider
        )
    }

    val layer = rememberLineCartesianLayer(
        lineProvider = LineCartesianLayer.LineProvider.series(line)
    )
    val chart = rememberCartesianChart(layer)
    val modelProducer = remember { CartesianChartModelProducer() }

    // 数据变化时通过 Vico 的 diff 机制做平滑动画。
    LaunchedEffect(values) {
        modelProducer.runTransaction {
            lineSeries {
                series(x = values.indices.toList(), y = values)
            }
        }
    }

    CartesianChartHost(
        chart = chart,
        modelProducer = modelProducer,
        modifier = modifier.fillMaxWidth()
    )
}
