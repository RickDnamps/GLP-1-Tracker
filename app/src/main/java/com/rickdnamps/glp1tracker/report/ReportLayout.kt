package com.rickdnamps.glp1tracker.report

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import java.io.OutputStream
import kotlin.math.ceil
import kotlin.math.floor

/** Colours and text styles of the report. Paints are shared, so they are never changed after creation. */
internal object ReportStyle {
    val TEAL = 0xFF00695C.toInt()
    val TEAL_DARK = 0xFF004D40.toInt()
    val TEAL_LIGHT = 0xFFE0F2F1.toInt()
    val TEAL_LINE = 0xFFB2DFDB.toInt()
    val TEXT = 0xFF212121.toInt()
    val TEXT_SECONDARY = 0xFF5F6368.toInt()
    val DIVIDER = 0xFFE0E0E0.toInt()
    val ZEBRA = 0xFFF6F8F8.toInt()
    val CARD = 0xFFF0F5F4.toInt()
    val NOTE = 0xFFF5F5F5.toInt()
    val GOAL = 0xFFE65100.toInt()

    private val regular: Typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    private val bold: Typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

    fun text(size: Float, argb: Int, isBold: Boolean = false, spacing: Float = 0f): TextPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            color = argb
            typeface = if (isBold) bold else regular
            letterSpacing = spacing
        }

    fun fill(argb: Int): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = argb
    }

    fun stroke(argb: Int, width: Float): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = argb
        strokeWidth = width
    }

    val brand = text(9f, TEAL, isBold = true, spacing = 0.12f)
    val title = text(22f, TEXT, isBold = true)
    val subtitle = text(10f, TEXT_SECONDARY)
    val section = text(13f, TEAL, isBold = true)
    val subsection = text(10f, TEXT, isBold = true)
    val body = text(9.5f, TEXT)
    val bodyBold = text(9.5f, TEXT, isBold = true)
    val muted = text(8.5f, TEXT_SECONDARY)
    val label = text(9.5f, TEXT_SECONDARY)
    val cardLabel = text(7.5f, TEXT_SECONDARY, spacing = 0.03f)
    val cardValue = text(13f, TEXT, isBold = true)
    val cardCaption = text(7.5f, TEXT_SECONDARY)
    val tableHeader = text(8f, TEAL_DARK, isBold = true)
    val tableBody = text(8.5f, TEXT)
    val small = text(7.5f, TEXT_SECONDARY)
    val noteTitle = text(8.5f, TEXT, isBold = true)

    val accentFill = fill(TEAL)
    val headerFill = fill(TEAL_LIGHT)
    val zebraFill = fill(ZEBRA)
    val cardFill = fill(CARD)
    val noteFill = fill(NOTE)
    val sectionLine = fill(TEAL_LINE)
    val divider = stroke(DIVIDER, 0.5f)
    val rule = stroke(0xFFBDBDBD.toInt(), 0.6f)
}

/**
 * Flowing layout on A4 pages (595 × 842 pt) with a cursor [y] that starts a new page when
 * the next block does not fit.
 *
 * Drawing is recorded per page and replayed by [writeTo], once the page count is known,
 * so [decorate] can print "Page x / y" on every page. Recorded operations must only capture
 * immutable values: copy [y] into a local `val` before using it in [draw].
 */
internal class ReportLayout(
    private val decorate: (canvas: Canvas, pageNumber: Int, pageCount: Int) -> Unit,
) {
    class Column(val title: String, val weight: Float, val align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL)

    class Stat(val label: String, val value: String, val caption: String? = null)

    companion object {
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 42f
        const val CONTENT_TOP = 46f
        const val CONTENT_BOTTOM = PAGE_HEIGHT - 56f

        private const val CELL_PAD_X = 4f
        private const val CELL_PAD_Y = 3.5f
        private const val CARD_PAD = 7f
        private const val CARD_GAP = 8f
        private const val LINE_SPACING_MULT = 1.12f
    }

    val left = MARGIN
    val right = PAGE_WIDTH - MARGIN
    val width = right - left

    private val pages = mutableListOf<MutableList<(Canvas) -> Unit>>()

    /** Top of the free space on the current page. */
    var y = CONTENT_TOP
        private set

    val remaining: Float get() = CONTENT_BOTTOM - y

    private val atPageTop: Boolean get() = y <= CONTENT_TOP + 0.5f

    init {
        newPage()
    }

    fun newPage() {
        pages += mutableListOf<(Canvas) -> Unit>()
        y = CONTENT_TOP
    }

    /** Starts a new page unless a block of [height] still fits below the cursor. */
    fun ensureSpace(height: Float) {
        if (y + height > CONTENT_BOTTOM && !atPageTop) newPage()
    }

    /** Vertical space; ignored at the top of a page. */
    fun gap(height: Float) {
        if (!atPageTop) y += height
    }

    /** Records a drawing operation on the current page. */
    fun draw(op: (Canvas) -> Unit) {
        pages.last().add(op)
    }

    fun layout(
        text: CharSequence,
        paint: TextPaint,
        width: Float,
        align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        maxLines: Int = Int.MAX_VALUE,
    ): StaticLayout {
        val builder = StaticLayout.Builder.obtain(text, 0, text.length, paint, floor(width).toInt().coerceAtLeast(1))
            .setAlignment(align)
            .setLineSpacing(0f, LINE_SPACING_MULT)
            .setIncludePad(false)
        if (maxLines != Int.MAX_VALUE) {
            builder.setMaxLines(maxLines).setEllipsize(TextUtils.TruncateAt.END)
        }
        return builder.build()
    }

    fun drawLayout(layout: StaticLayout, x: Float, top: Float) = draw { canvas ->
        canvas.save()
        canvas.translate(x, top)
        layout.draw(canvas)
        canvas.restore()
    }

    /** Wrapped text across the content width (kept on one page). */
    fun paragraph(text: CharSequence, paint: TextPaint, after: Float = 6f, indent: Float = 0f) {
        val layout = layout(text, paint, width - indent)
        ensureSpace(layout.height.toFloat())
        drawLayout(layout, left + indent, y)
        y += layout.height + after
    }

    /** Teal section title with a hairline; moves to a new page if [keepWithNext] does not fit under it. */
    fun sectionTitle(title: String, keepWithNext: Float) {
        gap(16f)
        val layout = layout(title, ReportStyle.section, width)
        ensureSpace(layout.height + 10f + keepWithNext)
        val top = y
        drawLayout(layout, left, top)
        val lineTop = top + layout.height + 3f
        draw { it.drawRect(left, lineTop, right, lineTop + 0.8f, ReportStyle.sectionLine) }
        y = lineTop + 0.8f + 9f
    }

    fun subTitle(title: String, keepWithNext: Float = 40f) {
        gap(6f)
        val layout = layout(title, ReportStyle.subsection, width)
        ensureSpace(layout.height + 4f + keepWithNext)
        drawLayout(layout, left, y)
        y += layout.height + 5f
    }

    /** Label / value rows, e.g. "Current dose   0.5 mg". */
    fun keyValues(rows: List<Pair<String, String>>, labelWidth: Float = 150f) {
        rows.forEach { (key, value) ->
            val keyLayout = layout(key, ReportStyle.label, labelWidth - 8f)
            val valueLayout = layout(value, ReportStyle.bodyBold, width - labelWidth)
            val height = maxOf(keyLayout.height, valueLayout.height).toFloat()
            ensureSpace(height)
            val top = y
            drawLayout(keyLayout, left, top)
            drawLayout(valueLayout, left + labelWidth, top)
            y += height + 4f
        }
    }

    /** Rows of equal-width cards: small label, big value, optional caption. */
    fun statCards(stats: List<Stat>, perRow: Int = 4) {
        val cardWidth = (width - CARD_GAP * (perRow - 1)) / perRow
        val inner = cardWidth - 2 * CARD_PAD
        stats.chunked(perRow).forEach { row ->
            val cards = row.map { stat ->
                Triple(
                    layout(stat.label, ReportStyle.cardLabel, inner, maxLines = 2),
                    layout(stat.value, ReportStyle.cardValue, inner, maxLines = 2),
                    stat.caption?.let { layout(it, ReportStyle.cardCaption, inner, maxLines = 3) },
                )
            }
            val height = cards.maxOf { (label, value, caption) ->
                label.height + 3f + value.height + (caption?.let { 2f + it.height } ?: 0f)
            } + 2 * CARD_PAD
            ensureSpace(height)
            val top = y
            cards.forEachIndexed { index, (label, value, caption) ->
                val x = left + index * (cardWidth + CARD_GAP)
                draw { it.drawRoundRect(x, top, x + cardWidth, top + height, 4f, 4f, ReportStyle.cardFill) }
                draw { it.drawRect(x, top + 4f, x + 2f, top + height - 4f, ReportStyle.accentFill) }
                var textTop = top + CARD_PAD
                drawLayout(label, x + CARD_PAD, textTop)
                textTop += label.height + 3f
                drawLayout(value, x + CARD_PAD, textTop)
                textTop += value.height + 2f
                if (caption != null) drawLayout(caption, x + CARD_PAD, textTop)
            }
            y = top + height + CARD_GAP
        }
    }

    /** Light band of columns (label, value, caption) used for the report header. */
    fun infoBand(items: List<Stat>) {
        if (items.isEmpty()) return
        val columnWidth = width / items.size
        val inner = columnWidth - 2 * CARD_PAD - 4f
        val columns = items.map { item ->
            Triple(
                layout(item.label, ReportStyle.cardLabel, inner),
                layout(item.value, ReportStyle.bodyBold, inner),
                item.caption?.let { layout(it, ReportStyle.cardCaption, inner) },
            )
        }
        val height = columns.maxOf { (label, value, caption) ->
            label.height + 3f + value.height + (caption?.let { 2f + it.height } ?: 0f)
        } + 2 * CARD_PAD + 2f
        ensureSpace(height)
        val top = y
        draw { it.drawRoundRect(left, top, right, top + height, 5f, 5f, ReportStyle.headerFill) }
        draw { it.drawRect(left, top + 5f, left + 3f, top + height - 5f, ReportStyle.accentFill) }
        columns.forEachIndexed { index, (label, value, caption) ->
            val x = left + index * columnWidth + CARD_PAD + 4f
            if (index > 0) {
                val lineX = left + index * columnWidth
                draw { it.drawLine(lineX, top + 8f, lineX, top + height - 8f, ReportStyle.divider) }
            }
            var textTop = top + CARD_PAD + 1f
            drawLayout(label, x, textTop)
            textTop += label.height + 3f
            drawLayout(value, x, textTop)
            textTop += value.height + 2f
            if (caption != null) drawLayout(caption, x, textTop)
        }
        y = top + height
    }

    /**
     * Table with wrapped cells (at most [maxLines] lines each), zebra rows and the header
     * repeated on every page it spans.
     */
    fun table(columns: List<Column>, rows: List<List<String>>, maxLines: Int = 8) {
        if (rows.isEmpty()) return
        val totalWeight = columns.sumOf { it.weight.toDouble() }.toFloat()
        val widths = columns.map { width * it.weight / totalWeight }
        val xs = widths.runningFold(left) { x, w -> x + w }

        fun layoutRow(texts: List<String>, paint: TextPaint): List<StaticLayout> =
            columns.indices.map { i ->
                layout(texts.getOrElse(i) { "" }, paint, widths[i] - 2 * CELL_PAD_X, columns[i].align, maxLines)
            }

        val header = layoutRow(columns.map { it.title }, ReportStyle.tableHeader)
        val headerHeight = header.maxOf { it.height } + 2 * CELL_PAD_Y

        fun drawHeader() {
            val top = y
            draw { it.drawRect(left, top, right, top + headerHeight, ReportStyle.headerFill) }
            header.forEachIndexed { i, cell -> drawLayout(cell, xs[i] + CELL_PAD_X, top + CELL_PAD_Y) }
            y += headerHeight
        }

        rows.forEachIndexed { index, row ->
            val cells = layoutRow(row, ReportStyle.tableBody)
            val height = cells.maxOf { it.height } + 2 * CELL_PAD_Y
            if (index == 0) {
                ensureSpace(headerHeight + height)
                drawHeader()
            } else if (y + height > CONTENT_BOTTOM) {
                newPage()
                drawHeader()
            }
            val top = y
            if (index % 2 == 1) draw { it.drawRect(left, top, right, top + height, ReportStyle.zebraFill) }
            cells.forEachIndexed { i, cell -> drawLayout(cell, xs[i] + CELL_PAD_X, top + CELL_PAD_Y) }
            draw { it.drawLine(left, top + height, right, top + height, ReportStyle.divider) }
            y += height
        }
        y += 4f
    }

    /**
     * Compact list of (label, value) pairs, e.g. dates and weights, printed as [groups]
     * side-by-side column pairs. Each page is filled top to bottom, then left to right.
     */
    fun compactGrid(titles: Pair<String, String>, entries: List<Pair<String, String>>, groups: Int = 3) {
        if (entries.isEmpty()) return
        val headerPaint = ReportStyle.tableHeader
        val bodyPaint = ReportStyle.tableBody
        val rowHeight = ceil(bodyPaint.fontSpacing) + 2 * CELL_PAD_Y
        val gutter = 10f
        val groupWidth = (width - gutter * (groups - 1)) / groups
        val labelWidth = groupWidth * 0.62f - 2 * CELL_PAD_X
        val valueWidth = groupWidth * 0.38f - 2 * CELL_PAD_X

        var index = 0
        while (index < entries.size) {
            if (index == 0) ensureSpace(rowHeight * 3) else newPage()
            val fitRows = floor((CONTENT_BOTTOM - y - rowHeight) / rowHeight).toInt().coerceAtLeast(1)
            val neededRows = ceil((entries.size - index) / groups.toDouble()).toInt()
            val rows = minOf(fitRows, neededRows)
            val chunk = entries.subList(index, minOf(entries.size, index + rows * groups))
            val usedGroups = ceil(chunk.size / rows.toDouble()).toInt()
            val top = y

            for (g in 0 until usedGroups) {
                val x = left + g * (groupWidth + gutter)
                draw { it.drawRect(x, top, x + groupWidth, top + rowHeight, ReportStyle.headerFill) }
                singleLine(titles.first, headerPaint, x + CELL_PAD_X, top, rowHeight, labelWidth, alignRight = false)
                singleLine(titles.second, headerPaint, x + groupWidth - CELL_PAD_X, top, rowHeight, valueWidth, alignRight = true)
            }
            chunk.forEachIndexed { k, (label, value) ->
                val x = left + (k / rows) * (groupWidth + gutter)
                val rowTop = top + rowHeight * (1 + k % rows)
                if (k % rows % 2 == 1) draw { it.drawRect(x, rowTop, x + groupWidth, rowTop + rowHeight, ReportStyle.zebraFill) }
                singleLine(label, bodyPaint, x + CELL_PAD_X, rowTop, rowHeight, labelWidth, alignRight = false)
                singleLine(value, bodyPaint, x + groupWidth - CELL_PAD_X, rowTop, rowHeight, valueWidth, alignRight = true)
            }
            y = top + rowHeight * (rows + 1) + 4f
            index += chunk.size
        }
    }

    /** One line of text vertically centred in a row, ellipsized to [maxWidth]; [x] is the right edge if [alignRight]. */
    private fun singleLine(text: String, paint: TextPaint, x: Float, top: Float, height: Float, maxWidth: Float, alignRight: Boolean) {
        val fitted = TextUtils.ellipsize(text, paint, maxWidth.coerceAtLeast(1f), TextUtils.TruncateAt.END).toString()
        val metrics = paint.fontMetrics
        val baseline = top + (height - (metrics.descent - metrics.ascent)) / 2 - metrics.ascent
        val startX = if (alignRight) x - paint.measureText(fitted) else x
        draw { it.drawText(fitted, startX, baseline, paint) }
    }

    /** Reserves [height] (on a new page if needed) and draws [op] there, with the block's top. */
    fun block(height: Float, op: (canvas: Canvas, top: Float) -> Unit) {
        ensureSpace(height)
        val top = y
        draw { op(it, top) }
        y += height
    }

    /** [count] writing lines, [spacing] apart. */
    fun ruledLines(count: Int, spacing: Float) {
        repeat(count) {
            ensureSpace(spacing)
            y += spacing
            val lineY = y
            draw { it.drawLine(left, lineY, right, lineY, ReportStyle.rule) }
        }
    }

    private fun noteLayouts(title: String, text: String): Pair<StaticLayout, StaticLayout> {
        val inner = width - 2 * CARD_PAD - 4f
        return layout(title, ReportStyle.noteTitle, inner) to layout(text, ReportStyle.small, inner)
    }

    fun noteHeight(title: String, text: String): Float {
        val (titleLayout, textLayout) = noteLayouts(title, text)
        return titleLayout.height + 3f + textLayout.height + 2 * CARD_PAD
    }

    /** Grey note anchored to the bottom of the current page (or of a new one if it does not fit). */
    fun bottomNote(title: String, text: String) {
        val (titleLayout, textLayout) = noteLayouts(title, text)
        val height = titleLayout.height + 3f + textLayout.height + 2 * CARD_PAD
        if (y + height + 8f > CONTENT_BOTTOM && !atPageTop) newPage()
        val top = CONTENT_BOTTOM - height
        draw { it.drawRoundRect(left, top, right, top + height, 4f, 4f, ReportStyle.noteFill) }
        drawLayout(titleLayout, left + CARD_PAD + 2f, top + CARD_PAD)
        drawLayout(textLayout, left + CARD_PAD + 2f, top + CARD_PAD + titleLayout.height + 3f)
        y = CONTENT_BOTTOM
    }

    /** Renders every recorded page into a PDF written to [out] (the stream is not closed). */
    fun writeTo(out: OutputStream) {
        val document = PdfDocument()
        try {
            pages.forEachIndexed { index, ops ->
                val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create()
                val page = document.startPage(info)
                val canvas = page.canvas
                ops.forEach { it(canvas) }
                decorate(canvas, index + 1, pages.size)
                document.finishPage(page)
            }
            document.writeTo(out)
        } finally {
            document.close()
        }
    }
}
