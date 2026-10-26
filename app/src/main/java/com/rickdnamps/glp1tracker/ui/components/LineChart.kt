package com.rickdnamps.glp1tracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

/**
 * Small dependency-free line chart. X values are epoch millis.
 * Points after [dashedAfter] are drawn dashed (used for projections).
 */
@Composable
fun LineChart(
    points: List<Pair<Long, Double>>,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    goal: Double? = null,
    goalColor: Color = MaterialTheme.colorScheme.tertiary,
    marker: Long? = null,
    dashedAfter: Long? = null,
    showDots: Boolean = false,
    yMinZero: Boolean = false,
    /** Vertical event markers (e.g. dose changes) with a short label. */
    events: List<Pair<Long, String>> = emptyList(),
    yLabel: (Double) -> String,
    xLabel: (Long) -> String,
    emptyText: String = "",
) {
    if (points.size < 2) {
        Box(modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
            Text(emptyText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val markerColor = MaterialTheme.colorScheme.error
    val eventColor = MaterialTheme.colorScheme.tertiary
    val eventStyle = TextStyle(fontSize = 9.sp, color = eventColor)
    // Draw-in animation each time the data set changes.
    val reveal = remember(points.size, points.firstOrNull()?.first, points.lastOrNull()?.first) { Animatable(0f) }
    LaunchedEffect(reveal) { reveal.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }

    Canvas(modifier.fillMaxWidth().height(height)) {
        val values = points.map { it.second } + listOfNotNull(goal)
        var minY = if (yMinZero) 0.0 else values.min()
        var maxY = values.max()
        if (maxY - minY < 1e-6) { maxY += 1.0; if (!yMinZero) minY -= 1.0 }
        val pad = (maxY - minY) * 0.08
        maxY += pad
        if (!yMinZero) minY -= pad
        val minX = points.first().first
        val maxX = max(points.last().first, minX + 1)

        val leftPad = 44.dp.toPx()
        val bottomPad = 18.dp.toPx()
        val topPad = 6.dp.toPx()
        val w = size.width - leftPad
        val h = size.height - bottomPad - topPad

        fun x(v: Long) = leftPad + ((v - minX).toFloat() / (maxX - minX).toFloat()) * w
        fun y(v: Double) = topPad + h - ((v - minY) / (maxY - minY)).toFloat() * h

        // Grid + Y labels
        val steps = 4
        for (i in 0..steps) {
            val v = minY + (maxY - minY) * i / steps
            val yy = y(v)
            drawLine(gridColor, Offset(leftPad, yy), Offset(size.width, yy), strokeWidth = 1f)
            val layout = measurer.measure(yLabel(v), labelStyle)
            drawText(layout, topLeft = Offset(leftPad - layout.size.width - 6.dp.toPx(), yy - layout.size.height / 2))
        }
        // X labels: start, middle, end
        listOf(minX, (minX + maxX) / 2, maxX).forEachIndexed { i, v ->
            val layout = measurer.measure(xLabel(v), labelStyle)
            val xx = when (i) {
                0 -> leftPad
                1 -> x(v) - layout.size.width / 2
                else -> size.width - layout.size.width
            }
            drawText(layout, topLeft = Offset(xx, size.height - layout.size.height))
        }

        goal?.let {
            val gy = y(it)
            drawLine(
                goalColor, Offset(leftPad, gy), Offset(size.width, gy), strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
            )
        }

        val revealRight = leftPad + w * reveal.value
        clipRect(right = revealRight) {
            val solid = points.filter { dashedAfter == null || it.first <= dashedAfter }
            val dashed = if (dashedAfter == null) emptyList() else points.filter { it.first >= dashedAfter }

            fun pathOf(list: List<Pair<Long, Double>>) = Path().apply {
                list.forEachIndexed { i, p -> if (i == 0) moveTo(x(p.first), y(p.second)) else lineTo(x(p.first), y(p.second)) }
            }

            if (solid.size >= 2) {
                val fill = pathOf(solid).apply {
                    lineTo(x(solid.last().first), topPad + h)
                    lineTo(x(solid.first().first), topPad + h)
                    close()
                }
                drawPath(fill, Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.30f), lineColor.copy(alpha = 0.02f))))
                drawPath(pathOf(solid), lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
            }
            if (dashed.size >= 2) {
                drawPath(
                    pathOf(dashed), lineColor.copy(alpha = 0.7f),
                    style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
                )
            }
            if (showDots) {
                points.forEach { drawCircle(lineColor, radius = 3.dp.toPx(), center = Offset(x(it.first), y(it.second))) }
            }
        }
        events.filter { it.first in minX..maxX }.forEach { (t, label) ->
            val ex = x(t)
            drawLine(
                eventColor.copy(alpha = 0.7f), Offset(ex, topPad), Offset(ex, topPad + h), strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
            )
            val layout = measurer.measure(label, eventStyle)
            drawText(layout, topLeft = Offset((ex + 2.dp.toPx()).coerceAtMost(size.width - layout.size.width), topPad))
        }
        marker?.takeIf { it in minX..maxX }?.let {
            val mx = x(it)
            drawLine(markerColor, Offset(mx, topPad), Offset(mx, topPad + h), strokeWidth = 1.5.dp.toPx())
        }
    }
}
