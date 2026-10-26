package com.rickdnamps.glp1tracker.report

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path

/** Weight line chart for the PDF canvas: grid, axes, labels and an optional dashed goal line. */
internal object WeightChart {

    class Data(
        /** (epoch millis, weight in the display unit), sorted by time. */
        val points: List<Pair<Long, Double>>,
        /** Goal in the display unit. */
        val goal: Double?,
        val goalLabel: String?,
        val yLabel: (Double) -> String,
        val xLabel: (Long) -> String,
    )

    private val labelPaint = ReportStyle.text(7f, ReportStyle.TEXT_SECONDARY)
    private val goalTextPaint = ReportStyle.text(7f, ReportStyle.GOAL, isBold = true)
    private val gridPaint = ReportStyle.stroke(ReportStyle.DIVIDER, 0.5f)
    private val axisPaint = ReportStyle.stroke(0xFF9E9E9E.toInt(), 0.8f)
    private val linePaint = ReportStyle.stroke(ReportStyle.TEAL, 1.6f).apply {
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val areaPaint = ReportStyle.fill(0x1A00695C)
    private val dotPaint = ReportStyle.fill(ReportStyle.TEAL)
    private val goalPaint = ReportStyle.stroke(ReportStyle.GOAL, 1f).apply {
        pathEffect = DashPathEffect(floatArrayOf(4f, 3f), 0f)
    }

    /** Draws the chart inside the given box (PDF points). Needs at least two points. */
    fun draw(canvas: Canvas, left: Float, top: Float, width: Float, height: Float, data: Data) {
        val points = data.points
        if (points.size < 2) return

        val values = points.map { it.second } + listOfNotNull(data.goal)
        val ticks = ReportMath.niceTicks(values.min(), values.max(), 5)
        val minY = ticks.first()
        val maxY = ticks.last()
        val minX = points.first().first
        val maxX = maxOf(points.last().first, minX + 1)

        val yLabelWidth = ticks.maxOf { labelPaint.measureText(data.yLabel(it)) }
        val plotLeft = left + yLabelWidth + 6f
        val plotRight = left + width - 4f
        val plotTop = top + 6f
        val plotBottom = top + height - 16f

        fun x(t: Long): Float = plotLeft + (t - minX).toFloat() / (maxX - minX).toFloat() * (plotRight - plotLeft)
        fun y(v: Double): Float = plotBottom - ((v - minY) / (maxY - minY)).toFloat() * (plotBottom - plotTop)

        val metrics = labelPaint.fontMetrics
        val centerOffset = (metrics.ascent + metrics.descent) / 2

        // Horizontal grid lines and Y labels.
        ticks.forEach { value ->
            val lineY = y(value)
            canvas.drawLine(plotLeft, lineY, plotRight, lineY, gridPaint)
            val text = data.yLabel(value)
            canvas.drawText(text, plotLeft - 4f - labelPaint.measureText(text), lineY - centerOffset, labelPaint)
        }

        // X ticks and date labels.
        val days = ((maxX - minX) / ReportMath.DAY_MS).toInt()
        ReportMath.timeTicks(minX, maxX, (days + 1).coerceIn(2, 6)).forEach { t ->
            val tickX = x(t)
            canvas.drawLine(tickX, plotBottom, tickX, plotBottom + 3f, axisPaint)
            val text = data.xLabel(t)
            val textWidth = labelPaint.measureText(text)
            val textX = (tickX - textWidth / 2).coerceIn(left, left + width - textWidth)
            canvas.drawText(text, textX, plotBottom + 4f - metrics.ascent, labelPaint)
        }

        // Axes.
        canvas.drawLine(plotLeft, plotTop, plotLeft, plotBottom, axisPaint)
        canvas.drawLine(plotLeft, plotBottom, plotRight, plotBottom, axisPaint)

        // Weight line, with a light fill under it.
        val line = Path()
        points.forEachIndexed { i, (t, v) ->
            if (i == 0) line.moveTo(x(t), y(v)) else line.lineTo(x(t), y(v))
        }
        val area = Path(line).apply {
            lineTo(x(points.last().first), plotBottom)
            lineTo(x(points.first().first), plotBottom)
            close()
        }
        canvas.drawPath(area, areaPaint)
        canvas.drawPath(line, linePaint)
        if (points.size <= 60) {
            points.forEach { (t, v) -> canvas.drawCircle(x(t), y(v), 1.8f, dotPaint) }
        }

        // Goal.
        val goal = data.goal ?: return
        val goalY = y(goal)
        val dashed = Path().apply {
            moveTo(plotLeft, goalY)
            lineTo(plotRight, goalY)
        }
        canvas.drawPath(dashed, goalPaint)
        data.goalLabel?.let { label ->
            val baseline = if (goalY - plotTop > 12f) goalY - 3f else goalY + 9f
            canvas.drawText(label, plotRight - goalTextPaint.measureText(label), baseline, goalTextPaint)
        }
    }
}
