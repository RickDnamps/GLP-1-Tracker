package com.rickdnamps.glp1tracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.domain.InjectionSite
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

/*
 * Interactive injection-site body map: an anatomical front-view illustration (man or woman,
 * res/drawable-nodpi/body_*.webp, generated for the app by the owner) with six tappable zones
 * coloured by how long each site has rested.
 *
 * Geometry is in the images' pixels (FIG_W x FIG_H, both figures share pose and framing) scaled
 * uniformly to fit, centred, in the canvas. FRONT VIEW: the person's LEFT side is on the
 * viewer's RIGHT, so every *_LEFT site sits at x > FIG_W / 2.
 */

private const val FIG_W = 440f
private const val FIG_H = 967f
private const val ZONE_R = 27f
private const val SELECTED_GROW = 0.16f
private val FigureHeight = 340.dp

/**
 * Body map of the six injection sites.
 *
 * @param selected highlighted site (filled + check mark); null or [InjectionSite.NONE] = none.
 * @param suggested site that gets the pulsing "next site" halo; null = no hint.
 * @param lastUse epoch millis of the last injection per site (missing = never used).
 * @param onSelect called when a zone is tapped; null makes the map read-only.
 * @param female draws the woman's figure (from the profile's sex) instead of the man's.
 */
@Composable
fun BodyMap(
    selected: InjectionSite?,
    suggested: InjectionSite?,
    lastUse: Map<InjectionSite, Long>,
    onSelect: ((InjectionSite) -> Unit)?,
    modifier: Modifier = Modifier,
    female: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    val figure = ImageBitmap.imageResource(if (female) R.drawable.body_female else R.drawable.body_male)
    val sites = InjectionSite.injectable
    val sel = selected?.takeIf { it != InjectionSite.NONE }
    val sug = suggested?.takeIf { it != InjectionSite.NONE }

    // Whole calendar days since last use (yesterday 8 pm seen at 9 am = 1 day, not 0).
    val days: Map<InjectionSite, Int> = remember(lastUse) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        lastUse.filterKeys { it != InjectionSite.NONE }.mapValues { (_, ms) ->
            val day = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
            ChronoUnit.DAYS.between(day, today).toInt().coerceAtLeast(0)
        }
    }

    // --- Per-zone animated state (read inside the draw lambda only, so animations just redraw) ---
    val zones = sites.map { site ->
        val tint = recencyColor(days[site], cs.primary, cs.tertiary, cs.outline)
        val isSel = site == sel
        BodyZoneAnim(
            site = site,
            ring = animateColorAsState(
                targetValue = if (isSel) cs.primary else tint,
                animationSpec = tween(durationMillis = 350),
                label = "zoneRing",
            ),
            fill = animateColorAsState(
                targetValue = if (isSel) cs.primary else tint.copy(alpha = 0.35f),
                animationSpec = tween(durationMillis = 350),
                label = "zoneFill",
            ),
            selection = animateFloatAsState(
                targetValue = if (isSel) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow),
                label = "zoneSelection",
            ),
        )
    }

    val pulseTransition = rememberInfiniteTransition(label = "bodyMapPulse")
    val pulse = pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1600, easing = LinearEasing), RepeatMode.Restart),
        label = "halo",
    )
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing)) }

    // --- Texts (resolved in composition, drawn on the canvas) ---
    val noData = stringResource(R.string.bodymap_no_data)
    val todayLabel = stringResource(R.string.bodymap_today)
    val labels = sites.map { s ->
        val d = days[s]
        when {
            d == null -> noData
            d == 0 -> todayLabel
            else -> stringResource(R.string.bodymap_days_short, d)
        }
    }
    val siteStatus = sites.map { s ->
        val name = stringResource(s.nameRes)
        val d = days[s]
        when {
            d == null -> stringResource(R.string.bodymap_cd_site_never, name)
            d == 0 -> stringResource(R.string.bodymap_cd_site_today, name)
            else -> stringResource(R.string.bodymap_cd_site_days, name, d)
        }
    }
    val titleCd = stringResource(R.string.bodymap_cd_title)
    val selectedCd = if (sel != null) {
        stringResource(R.string.bodymap_selected, stringResource(sel.nameRes))
    } else {
        stringResource(R.string.bodymap_cd_no_selection)
    }
    val suggestedCd = if (sug != null) stringResource(R.string.bodymap_cd_suggested, stringResource(sug.nameRes)) else null
    val description = (listOfNotNull(titleCd, selectedCd, suggestedCd) + siteStatus).joinToString(". ")

    val currentOnSelect by rememberUpdatedState(onSelect)
    val haptics = LocalHapticFeedback.current
    val actionLabels = sites.map { stringResource(R.string.bodymap_action_select, stringResource(it.nameRes)) }
    val a11yActions = if (onSelect == null) emptyList() else sites.mapIndexed { i, s ->
        CustomAccessibilityAction(actionLabels[i]) {
            currentOnSelect?.invoke(s)
            currentOnSelect != null
        }
    }

    val tapModifier = if (onSelect == null) Modifier else Modifier.pointerInput(Unit) {
        detectTapGestures { offset ->
            val frame = BodyFrame(size.width.toFloat(), size.height.toFloat())
            val touchRadius = max(frame.len(ZONE_R) * 1.8f, 24.dp.toPx())
            val hit = InjectionSite.injectable
                .map { it to (frame.pt(zoneCenter(it)) - offset).getDistance() }
                .filter { it.second <= touchRadius }
                .minByOrNull { it.second }
                ?.first
            if (hit != null) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                currentOnSelect?.invoke(hit)
            }
        }
    }

    // --- Theme colours (no hard-coded colours: works in light and dark) ---
    val shadowColor = cs.onSurface.copy(alpha = 0.07f)
    val backdropGlow = cs.primaryContainer
    val haloColor = cs.tertiary
    val checkColor = cs.onPrimary
    val selectedGlow = cs.primary
    val pillColor = cs.surface.copy(alpha = 0.92f)
    val pillSelected = cs.primaryContainer
    val labelColor = cs.onSurface
    val labelSelected = cs.onPrimaryContainer
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = labelColor)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(FigureHeight)
                .semantics {
                    contentDescription = description
                    if (a11yActions.isNotEmpty()) customActions = a11yActions
                }
                .then(tapModifier)
        ) {
            val f = BodyFrame(size.width, size.height)

            // Soft backdrop glow behind the figure.
            val glowCenter = f.pt(FIG_W / 2f, 430f)
            val glowR = f.len(420f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(backdropGlow.copy(alpha = 0.35f), backdropGlow.copy(alpha = 0f)),
                    center = glowCenter,
                    radius = glowR,
                ),
                radius = glowR,
                center = glowCenter,
            )
            // Ground shadow, then the illustration (transparent background).
            drawOval(color = shadowColor, topLeft = f.pt(130f, 950f), size = Size(f.len(180f), f.len(16f)))
            drawImage(
                figure,
                dstOffset = IntOffset(f.originX.roundToInt(), f.originY.roundToInt()),
                dstSize = IntSize(f.len(FIG_W).roundToInt(), f.len(FIG_H).roundToInt()),
                filterQuality = FilterQuality.High,
            )

            val enter = appear.value
            val baseR = f.len(ZONE_R)

            // Suggested site: two expanding, fading ripples + a slowly turning dashed ring.
            if (sug != null) {
                val c = f.pt(zoneCenter(sug))
                val sugSel = zones.first { it.site == sug }.selection.value
                val r = baseR * (1f + SELECTED_GROW * sugSel) * backOut(stagger(enter, sites.indexOf(sug)))
                if (r > 0f) {
                    for (k in 0 until 2) {
                        val t = (pulse.value + k * 0.5f) % 1f
                        val rr = r * (1.05f + 0.95f * t)
                        val a = 1f - t
                        drawCircle(color = haloColor.copy(alpha = 0.22f * a), radius = rr, center = c)
                        drawCircle(color = haloColor.copy(alpha = 0.6f * a), radius = rr, center = c, style = Stroke(width = 1.5.dp.toPx()))
                    }
                    val ringR = r + 6.dp.toPx()
                    val segment = (2f * PI.toFloat() * ringR) / 12f
                    drawCircle(
                        color = haloColor,
                        radius = ringR,
                        center = c,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(segment * 0.5f, segment * 0.5f), pulse.value * segment),
                        ),
                    )
                }
            }

            // Zones.
            val ringWidth = 2.dp.toPx()
            zones.forEachIndexed { i, z ->
                val pop = backOut(stagger(enter, i))
                if (pop <= 0f) return@forEachIndexed
                val c = f.pt(zoneCenter(z.site))
                val s = z.selection.value
                val p = s.coerceIn(0f, 1f)
                val r = baseR * (1f + SELECTED_GROW * s) * pop
                if (p > 0.01f) drawCircle(color = selectedGlow.copy(alpha = 0.2f * p), radius = r + 4.dp.toPx(), center = c)
                drawCircle(color = Color.White.copy(alpha = 0.55f), radius = r, center = c)
                drawCircle(color = z.fill.value, radius = r, center = c)
                drawCircle(color = Color.White, radius = r, center = c, style = Stroke(width = ringWidth + 2.dp.toPx()))
                drawCircle(color = z.ring.value, radius = r, center = c, style = Stroke(width = ringWidth))
                val ring = z.ring.value
                if (p < 0.99f) drawCircle(color = ring.copy(alpha = ring.alpha * (1f - p)), radius = r * 0.28f, center = c)
                if (p > 0.01f) drawCheck(c, r, checkColor, p)
            }

            // Day labels under each zone, kept on their own side of the body's midline so the
            // left/right pills never overlap.
            val midX = f.pt(FIG_W / 2f, 0f).x
            val gap = 1.5.dp.toPx()
            val padH = 5.dp.toPx()
            val padV = 1.5.dp.toPx()
            zones.forEachIndexed { i, z ->
                val alpha = stagger(enter, i).coerceIn(0f, 1f)
                if (alpha <= 0f) return@forEachIndexed
                val p = z.selection.value.coerceIn(0f, 1f)
                val design = zoneCenter(z.site)
                val c = f.pt(design)
                val layout = measurer.measure(labels[i], labelStyle)
                val w = layout.size.width + 2 * padH
                val h = layout.size.height + 2 * padV
                var left = c.x - w / 2f
                left = if (design.x < FIG_W / 2f) min(left, midX - gap - w) else max(left, midX + gap)
                val top = c.y + baseR * (1f + SELECTED_GROW) + 2.dp.toPx()
                drawRoundRect(
                    color = lerp(pillColor, pillSelected, p).copy(alpha = pillColor.alpha * alpha),
                    topLeft = Offset(left, top),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(h / 2f, h / 2f),
                )
                drawText(
                    layout,
                    color = lerp(labelColor, labelSelected, p).copy(alpha = alpha),
                    topLeft = Offset(left + padH, top + padV),
                )
            }
        }

        // "Your right" / "Your left" under the matching side of the figure (absolute, not RTL-mirrored,
        // because the canvas is not mirrored either).
        Row(
            Modifier.widthIn(max = FigureHeight * 0.8f).fillMaxWidth(),
            horizontalArrangement = Arrangement.Absolute.SpaceBetween,
        ) {
            SideLabel(stringResource(R.string.bodymap_your_right))
            SideLabel(stringResource(R.string.bodymap_your_left))
        }
        ColorKey(showSuggested = sug != null)
    }
}

// ---------------------------------------------------------------------------------------------
// Legend
// ---------------------------------------------------------------------------------------------

@Composable
private fun SideLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorKey(showSuggested: Boolean) {
    val cs = MaterialTheme.colorScheme
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    ) {
        KeyItem(cs.primary, stringResource(R.string.bodymap_legend_rested))
        KeyItem(cs.tertiary, stringResource(R.string.bodymap_legend_mid))
        KeyItem(cs.outline, stringResource(R.string.bodymap_legend_recent))
        if (showSuggested) KeyItem(cs.tertiary, stringResource(R.string.bodymap_legend_suggested), dashed = true)
    }
}

@Composable
private fun KeyItem(color: Color, label: String, dashed: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(12.dp)) {
            val r = size.minDimension / 2f - 1.dp.toPx()
            if (dashed) {
                val segment = (2f * PI.toFloat() * r) / 6f
                drawCircle(
                    color = color,
                    radius = r,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(segment * 0.55f, segment * 0.45f)),
                    ),
                )
            } else {
                drawCircle(color = color.copy(alpha = 0.22f), radius = r)
                drawCircle(color = color, radius = r, style = Stroke(width = 1.5.dp.toPx()))
                drawCircle(color = color, radius = r * 0.3f)
            }
        }
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

// ---------------------------------------------------------------------------------------------
// Geometry & drawing
// ---------------------------------------------------------------------------------------------

private class BodyZoneAnim(
    val site: InjectionSite,
    val ring: State<Color>,
    val fill: State<Color>,
    val selection: State<Float>,
)

/** Never used or rested >= 14 days: primary; 7-13 days: tertiary; < 7 days: outline (grey). */
private fun recencyColor(days: Int?, fresh: Color, mid: Color, recent: Color): Color = when {
    days == null || days >= 14 -> fresh
    days >= 7 -> mid
    else -> recent
}

/** Maps figure units to canvas pixels: uniform scale, centred. */
private class BodyFrame(width: Float, height: Float) {
    val scale = min(width / FIG_W, height / FIG_H)
    val originX = (width - FIG_W * scale) / 2f
    val originY = (height - FIG_H * scale) / 2f
    fun pt(x: Float, y: Float) = Offset(originX + x * scale, originY + y * scale)
    fun pt(o: Offset) = pt(o.x, o.y)
    fun len(v: Float) = v * scale
}

/** Zone centres in image pixels (same for both figures). The person's LEFT is on the viewer's RIGHT. */
private fun zoneCenter(site: InjectionSite): Offset {
    fun mirror(o: Offset) = Offset(FIG_W - o.x, o.y)
    val abdomen = Offset(186f, 372f) // beside the navel
    val thigh = Offset(172f, 600f) // front of the thigh, mid-way
    val arm = Offset(108f, 285f) // back of the upper arm, shown on its outer side
    return when (site) {
        InjectionSite.ABDOMEN_RIGHT -> abdomen
        InjectionSite.ABDOMEN_LEFT -> mirror(abdomen)
        InjectionSite.THIGH_RIGHT -> thigh
        InjectionSite.THIGH_LEFT -> mirror(thigh)
        InjectionSite.ARM_RIGHT -> arm
        InjectionSite.ARM_LEFT -> mirror(arm)
        InjectionSite.NONE -> Offset(FIG_W / 2f, 372f) // never drawn
    }
}

/** Staggered 0..1 entrance progress of zone [index] from the global entrance progress [t]. */
private fun stagger(t: Float, index: Int): Float = ((t - index * 0.07f) / 0.58f).coerceIn(0f, 1f)

/** Ease-out with a small overshoot ("pop"). backOut(0) = 0, backOut(1) = 1. */
private fun backOut(t: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val u = t - 1f
    return 1f + c3 * u * u * u + c1 * u * u
}

/** Check mark inside a zone of radius [r], stroked progressively as [progress] goes 0 -> 1. */
private fun DrawScope.drawCheck(center: Offset, r: Float, color: Color, progress: Float) {
    val a = center + Offset(-0.42f * r, 0.02f * r)
    val b = center + Offset(-0.12f * r, 0.32f * r)
    val c = center + Offset(0.44f * r, -0.30f * r)
    val l1 = (b - a).getDistance()
    val l2 = (c - b).getDistance()
    val drawn = (l1 + l2) * progress
    val path = Path().apply {
        moveTo(a.x, a.y)
        if (drawn <= l1) {
            val e = a + (b - a) * (drawn / l1)
            lineTo(e.x, e.y)
        } else {
            lineTo(b.x, b.y)
            val e = b + (c - b) * ((drawn - l1) / l2)
            lineTo(e.x, e.y)
        }
    }
    drawPath(path = path, color = color, style = Stroke(width = r * 0.24f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
