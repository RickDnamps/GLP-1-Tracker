package com.rickdnamps.glp1tracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

private const val PARTICLE_COUNT = 120
private const val DURATION_S = 2.5f
private const val FADE_S = 0.5f
/** Burst origin, as a fraction of the height (top-centre). */
private const val ORIGIN_Y = 0.12f
/** Physics in dp and seconds. */
private const val GRAVITY = 700f
private const val DRAG = 2.2f
private val Gold = Color(0xFFFFC107)
private val LightGold = Color(0xFFFFE082)

/** Mutable on purpose: the 120 particles are allocated once and re-seeded for every burst. */
private class Particle {
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    var rotation = 0f
    var spin = 0f
    var flip = 0f
    var flipSpeed = 0f
    var width = 0f
    var height = 0f
    var life = 0f
    var circle = false
    var color = Color.Unspecified
}

/** [initialTrigger] is the value seen at first composition: it is not played (no replay on rotation). */
private class ConfettiHolder(val initialTrigger: Int) {
    val particles = Array(PARTICLE_COUNT) { Particle() }
    var initialConsumed = false

    fun seed(palette: List<Color>) {
        val random = Random(System.nanoTime())
        for (p in particles) {
            val angle = -PI.toFloat() / 2 + (random.nextFloat() * 2 - 1) * PI.toFloat() * 0.7f
            val speed = 250f + random.nextFloat() * 800f
            p.x = (random.nextFloat() * 2 - 1) * 12f
            p.y = (random.nextFloat() * 2 - 1) * 6f
            p.vx = cos(angle) * speed
            p.vy = sin(angle) * speed
            p.rotation = random.nextFloat() * 360f
            p.spin = (random.nextFloat() * 2 - 1) * 720f
            p.flip = random.nextFloat() * 2 * PI.toFloat()
            p.flipSpeed = 4f + random.nextFloat() * 8f
            p.circle = random.nextFloat() < 0.35f
            p.width = if (p.circle) 5f + random.nextFloat() * 4f else 6f + random.nextFloat() * 5f
            p.height = if (p.circle) p.width else 10f + random.nextFloat() * 6f
            p.life = DURATION_S * (0.65f + random.nextFloat() * 0.35f)
            p.color = palette[random.nextInt(palette.size)]
        }
    }

    fun step(dt: Float) {
        val drag = exp(-DRAG * dt)
        for (p in particles) {
            p.vx *= drag
            p.vy = p.vy * drag + GRAVITY * dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.rotation += p.spin * dt
            p.flip += p.flipSpeed * dt
        }
    }
}

/**
 * Full-screen confetti explosion from the top-centre, about 2.5 s long. Plays each time [trigger]
 * changes to a value > 0 after the first composition, so it does not replay on rotation or when
 * the screen comes back. Keep it permanently in composition, above the content (e.g. last child
 * of a Box around the Scaffold): it draws nothing when idle and never consumes touches.
 */
@Composable
fun ConfettiBurst(trigger: Int, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val palette = remember(scheme.primary, scheme.secondary, scheme.tertiary, scheme.error) {
        listOf(scheme.primary, scheme.secondary, scheme.tertiary, scheme.error, Gold, LightGold)
    }
    val holder = remember { ConfettiHolder(trigger) }
    // Read during composition: flips only at the start and end of a burst.
    var active by remember { mutableStateOf(false) }
    // Read only while drawing: every frame invalidates the draw phase, not the composition.
    var elapsed by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(trigger) {
        val isInitial = !holder.initialConsumed && trigger == holder.initialTrigger
        holder.initialConsumed = true
        if (isInitial || trigger <= 0) {
            active = false
            return@LaunchedEffect
        }
        holder.seed(palette)
        elapsed = 0f
        active = true
        var start = -1L
        var last = 0L
        while (true) {
            val now = withFrameNanos { it }
            if (start < 0) {
                start = now
                last = now
            }
            val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
            last = now
            val t = (now - start) / 1_000_000_000f
            if (t >= DURATION_S) break
            holder.step(dt)
            elapsed = t
        }
        active = false
    }

    if (active) {
        Canvas(modifier.fillMaxSize()) {
            val t = elapsed
            val originX = size.width / 2f
            val originY = size.height * ORIGIN_Y
            val d = density
            for (p in holder.particles) {
                val alpha = ((p.life - t) / FADE_S).coerceIn(0f, 1f)
                if (alpha <= 0f) continue
                val cx = originX + p.x * d
                val cy = originY + p.y * d
                if (cy > size.height + 40f * d) continue
                if (p.circle) {
                    drawCircle(color = p.color, radius = p.width * d / 2f, center = Offset(cx, cy), alpha = alpha)
                } else {
                    // Squashing the width with the flip angle fakes a paper strip tumbling in 3D.
                    val w = p.width * d * (0.15f + 0.85f * abs(cos(p.flip)))
                    val h = p.height * d
                    rotate(degrees = p.rotation, pivot = Offset(cx, cy)) {
                        drawRect(
                            color = p.color,
                            topLeft = Offset(cx - w / 2f, cy - h / 2f),
                            size = Size(w, h),
                            alpha = alpha,
                        )
                    }
                }
            }
        }
    }
}
