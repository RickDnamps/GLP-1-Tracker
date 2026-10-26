package com.rickdnamps.glp1tracker.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rickdnamps.glp1tracker.ui.theme.LocalBrand

/** Big gradient card with soft decorative bubbles. */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    brush: Brush = LocalBrand.current.hero,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(brush)
            .drawBehind {
                drawCircle(Color.White.copy(alpha = 0.10f), radius = size.height * 0.55f, center = Offset(size.width * 0.95f, size.height * 0.05f))
                drawCircle(Color.White.copy(alpha = 0.07f), radius = size.height * 0.35f, center = Offset(size.width * 0.78f, size.height * 1.02f))
            }
            .padding(22.dp),
        content = content,
    )
}

/** Translucent tile for use on top of a [HeroCard] or a tinted background. */
@Composable
fun GlassTile(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        content = content,
    )
}

/** Animated circular progress with rounded ends; [content] is centred inside. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    stroke: Dp = 12.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    colors: List<Color> = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
    content: @Composable BoxScope.() -> Unit = {},
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(1_200, easing = FastOutSlowInEasing),
        label = "ring",
    )
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx()
            val inset = w / 2
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - w, this.size.height - w)
            drawArc(trackColor, -90f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(w, cap = StrokeCap.Round))
            if (animated > 0f) {
                // Rotate so the sweep gradient starts at 12 o'clock like the arc.
                rotate(-90f) {
                    drawArc(
                        Brush.sweepGradient(colors + colors.first()),
                        0f, 360f * animated, false, Offset(inset, inset), arcSize,
                        style = Stroke(w, cap = StrokeCap.Round),
                    )
                }
            }
        }
        content()
    }
}

/** Text that slides when its value changes (counters, countdowns). */
@Composable
fun AnimatedValue(text: String, style: TextStyle, color: Color = Color.Unspecified, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
        },
        label = "value",
        modifier = modifier,
    ) { value ->
        Text(value, style = style, color = color, fontWeight = FontWeight.Bold)
    }
}

/** Soft breathing alpha for "due now" hints. */
@Composable
fun pulseAlpha(): Float {
    val t = rememberInfiniteTransition(label = "pulse")
    val a by t.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha",
    )
    return a
}

/** Background glow behind the screens. */
fun Modifier.topGlow(color: Color): Modifier = drawBehind {
    drawRect(
        Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.22f), Color.Transparent),
            center = Offset(size.width * 0.15f, 0f),
            radius = size.width * 1.1f,
        )
    )
    drawRect(
        Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.10f), Color.Transparent),
            center = Offset(size.width * 1.0f, size.height * 0.25f),
            radius = size.width * 0.8f,
        )
    )
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, container: Color = Color.White.copy(alpha = 0.2f), content: Color = Color.White) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = content,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/** Glass of water whose level follows [progress] with a gently moving wave. */
@Composable
fun WaterGlass(progress: Float, modifier: Modifier = Modifier) {
    val level by animateFloatAsState(progress.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing), label = "level")
    val phase by rememberInfiniteTransition(label = "wave").animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2_400, easing = androidx.compose.animation.core.LinearEasing)),
        label = "phase",
    )
    val outline = MaterialTheme.colorScheme.outline
    val water = listOf(Color(0xFF4FC3F7), Color(0xFF0288D1))
    Canvas(modifier.size(width = 64.dp, height = 84.dp)) {
        val w = size.width
        val h = size.height
        val inset = w * 0.12f
        val glass = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w - inset, h)
            lineTo(inset, h)
            close()
        }
        clipPath(glass) {
            val top = h * (1f - level)
            val amp = if (level in 0.02f..0.98f) h * 0.035f else 0f
            val wave = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, h)
                lineTo(0f, top)
                var x = 0f
                while (x <= w) {
                    lineTo(x, top + amp * kotlin.math.sin(phase + x / w * 2f * Math.PI.toFloat()))
                    x += w / 24f
                }
                lineTo(w, h)
                close()
            }
            drawPath(wave, Brush.verticalGradient(water, startY = top, endY = h))
        }
        drawPath(glass, outline, style = Stroke(width = 2.dp.toPx(), join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}
