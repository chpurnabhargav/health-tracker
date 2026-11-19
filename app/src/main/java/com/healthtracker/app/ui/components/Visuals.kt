package com.healthtracker.app.ui.components

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.healthtracker.app.ui.theme.App
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Phone tilt from the gravity sensor, smoothed, each axis -1..1. Zero when there's no sensor. */
@Composable
fun rememberTilt(): State<Offset> {
    val ctx = LocalContext.current
    val tilt = remember { mutableStateOf(Offset.Zero) }
    DisposableEffect(Unit) {
        val sm = ctx.getSystemService(SensorManager::class.java)
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val x = (-e.values[0] / 9.81f).coerceIn(-1f, 1f)
                val y = ((e.values[1] / 9.81f) - 0.55f).coerceIn(-1f, 1f)
                val o = tilt.value
                tilt.value = Offset(o.x * 0.85f + x * 0.15f, o.y * 0.85f + y * 0.15f)
            }

            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm?.unregisterListener(listener) }
    }
    return tilt
}

/** Tilts the element in 3D toward your finger while you press it. Doesn't block clicks or scrolling. */
fun Modifier.pressTilt(maxDeg: Float = 7f): Modifier = composed {
    var target by remember { mutableStateOf(Offset.Zero) }
    var pressed by remember { mutableStateOf(false) }
    val rx by animateFloatAsState(if (pressed) -target.y * maxDeg else 0f, spring(stiffness = Spring.StiffnessMediumLow), label = "rx")
    val ry by animateFloatAsState(if (pressed) target.x * maxDeg else 0f, spring(stiffness = Spring.StiffnessMediumLow), label = "ry")
    val sc by animateFloatAsState(if (pressed) 0.975f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "sc")
    this
        .pointerInput(Unit) {
            val slop = viewConfiguration.touchSlop
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                fun update(p: Offset) {
                    target = Offset(
                        (p.x / size.width * 2f - 1f).coerceIn(-1f, 1f),
                        (p.y / size.height * 2f - 1f).coerceIn(-1f, 1f),
                    )
                }
                update(down.position)
                pressed = true
                while (true) {
                    val ev = awaitPointerEvent(PointerEventPass.Initial)
                    val ch = ev.changes.firstOrNull() ?: break
                    if (!ch.pressed) break
                    if ((ch.position - down.position).getDistance() > slop * 2) pressed = false
                    if (pressed) update(ch.position)
                }
                pressed = false
            }
        }
        .graphicsLayer {
            rotationX = rx
            rotationY = ry
            scaleX = sc
            scaleY = sc
            cameraDistance = 14f * density
        }
}

/** A soft neon glow behind a rounded shape. */
fun Modifier.glow(color: Color, radius: Dp = 18.dp, corner: Dp = 16.dp, alpha: Float = 0.55f): Modifier = drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val fp = paint.asFrameworkPaint()
        fp.color = android.graphics.Color.TRANSPARENT
        fp.setShadowLayer(radius.toPx(), 0f, 0f, color.copy(alpha = alpha).toArgb())
        canvas.drawRoundRect(0f, 0f, size.width, size.height, corner.toPx(), corner.toPx(), paint)
    }
}

/** Slowly drifting coloured light at the top of a screen. */
@Composable
fun AuroraBackground(colors: List<Color>, modifier: Modifier = Modifier, height: Dp = 420.dp, fadeTo: Color = Color(0xFF07080D)) {
    val t = rememberInfiniteTransition(label = "aurora")
    val a by t.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(16000, easing = LinearEasing)), label = "a")
    Canvas(modifier.fillMaxWidth().height(height).clipToBounds()) {
        val w = size.width
        val h = size.height
        val centers = listOf(
            Offset(w * (0.18f + 0.10f * sin(a)), h * (0.22f + 0.08f * cos(a * 1.3f))),
            Offset(w * (0.85f + 0.08f * cos(a)), h * (0.18f + 0.10f * sin(a * 0.8f))),
            Offset(w * (0.55f + 0.12f * sin(a * 0.7f)), h * (0.55f + 0.08f * cos(a))),
        )
        val radii = listOf(w * 0.62f, w * 0.55f, w * 0.7f)
        centers.forEachIndexed { i, c ->
            val col = colors[i % colors.size]
            drawCircle(
                Brush.radialGradient(listOf(col.copy(alpha = 0.32f), col.copy(alpha = 0.10f), Color.Transparent), center = c, radius = radii[i]),
                radius = radii[i], center = c,
            )
        }
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, fadeTo), startY = h * 0.5f, endY = h))
    }
}


private fun wavePath(left: Float, right: Float, level: Float, bottom: Float, amp: Float, wavelength: Float, phase: Float): Path {
    val p = Path()
    p.moveTo(left, bottom)
    p.lineTo(left, level)
    var x = left
    val step = 6f
    while (x <= right) {
        p.lineTo(x, level + amp * sin((x / wavelength) * 2f * PI.toFloat() + phase))
        x += step
    }
    p.lineTo(right, bottom)
    p.close()
    return p
}

/**
 * The goal orb: a glass sphere that fills with glowing liquid as you get closer to your goal.
 * The liquid stays level when you tilt the phone, and a moon orbits it.
 */
@Composable
fun GoalOrb(progress: Float, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit = {}) {
    val c = App.colors
    val tilt by rememberTilt()
    val level by animateFloatAsState(progress.coerceIn(0.04f, 1f), tween(1400, easing = FastOutSlowInEasing), label = "level")
    val inf = rememberInfiniteTransition(label = "orb")
    val phase by inf.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(3600, easing = LinearEasing)), label = "phase")
    val orbit by inf.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "orbit")

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val r = min(size.width, size.height) * 0.34f
            val orbitTilt = -16f + tilt.x * 8f
            val rx = r * 1.42f
            val ry = r * 0.36f

            // Outer glow
            drawCircle(
                Brush.radialGradient(listOf(c.data.copy(alpha = 0.45f), c.water.copy(alpha = 0.12f), Color.Transparent), center, r * 1.7f),
                r * 1.7f, center,
            )
            // Orbit ring (back) and moon when behind
            rotate(orbitTilt, center) {
                drawOval(Color.White.copy(alpha = 0.12f), Offset(center.x - rx, center.y - ry), Size(rx * 2, ry * 2), style = Stroke(1.5.dp.toPx()))
            }
            val moon = moonPosition(center, rx, ry, orbit, orbitTilt)
            if (sin(orbit) < 0) drawMoon(moon, r * 0.07f, c.primary, 0.55f)

            // Sphere body with off-centre light for depth
            val light = Offset(center.x - r * 0.35f + tilt.x * r * 0.2f, center.y - r * 0.45f + tilt.y * r * 0.2f)
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFF6C5CFF), Color(0xFF2B2080), Color(0xFF0D0A2A)), light, r * 1.65f),
                r, center,
            )

            // Liquid, kept level against the phone's tilt
            val sphere = Path().apply { addOval(androidx.compose.ui.geometry.Rect(center, r)) }
            clipPath(sphere) {
                val levelY = center.y + r - 2f * r * level
                rotate(-tilt.x * 24f, center) {
                    drawPath(
                        wavePath(center.x - r * 1.8f, center.x + r * 1.8f, levelY, center.y + r * 1.8f, r * 0.05f, r * 1.1f, phase + PI.toFloat()),
                        Brush.verticalGradient(listOf(c.water.copy(alpha = 0.55f), Color(0xFF1B3F9E).copy(alpha = 0.6f)), levelY, center.y + r),
                    )
                    drawPath(
                        wavePath(center.x - r * 1.8f, center.x + r * 1.8f, levelY + r * 0.03f, center.y + r * 1.8f, r * 0.06f, r * 1.3f, phase),
                        Brush.verticalGradient(listOf(c.primary, Color(0xFF5FF0C8), Color(0xFF1F7FB0)), levelY, center.y + r),
                    )
                }
                drawCircle(
                    Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent), Offset(center.x, center.y + r), r * 1.1f),
                    r * 1.2f, Offset(center.x, center.y + r),
                )
            }

            // Rim light and specular highlight
            drawCircle(Color.White.copy(alpha = 0.16f), r, center, style = Stroke(1.5.dp.toPx()))
            drawArc(
                Color.White.copy(alpha = 0.35f), 195f, 70f, false,
                Offset(center.x - r * 0.9f, center.y - r * 0.9f), Size(r * 1.8f, r * 1.8f),
                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
            )
            val spec = Offset(center.x - r * 0.42f + tilt.x * r * 0.12f, center.y - r * 0.55f + tilt.y * r * 0.12f)
            drawOval(
                Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent), spec, r * 0.32f),
                Offset(spec.x - r * 0.26f, spec.y - r * 0.15f), Size(r * 0.52f, r * 0.3f),
            )

            // Orbit ring (front) and moon when in front
            rotate(orbitTilt, center) {
                drawArc(
                    Color.White.copy(alpha = 0.32f), 0f, 180f, false,
                    Offset(center.x - rx, center.y - ry), Size(rx * 2, ry * 2), style = Stroke(1.5.dp.toPx()),
                )
            }
            if (sin(orbit) >= 0) drawMoon(moon, r * 0.08f, c.primary, 1f)
        }
        content()
    }
}

private fun moonPosition(center: Offset, rx: Float, ry: Float, angle: Float, tiltDeg: Float): Offset {
    val x = rx * cos(angle)
    val y = ry * sin(angle)
    val t = Math.toRadians(tiltDeg.toDouble())
    return Offset(center.x + (x * cos(t) - y * sin(t)).toFloat(), center.y + (x * sin(t) + y * cos(t)).toFloat())
}

private fun DrawScope.drawMoon(p: Offset, r: Float, color: Color, alpha: Float) {
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.6f * alpha), Color.Transparent), p, r * 3.5f), r * 3.5f, p)
    drawCircle(color.copy(alpha = alpha), r, p)
}

/** A glass of water that fills up, with waves, bubbles, and a surface that stays level when tilted. */
@Composable
fun LiquidGlass(fraction: Float, modifier: Modifier = Modifier) {
    val c = App.colors
    val tilt by rememberTilt()
    val level by animateFloatAsState(fraction.coerceIn(0f, 1f), spring(dampingRatio = 0.6f, stiffness = 60f), label = "water")
    val inf = rememberInfiniteTransition(label = "glass")
    val phase by inf.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "phase")
    val rise by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "rise")
    val bubbles = remember { List(8) { Triple(Random.nextFloat() * 0.7f + 0.15f, Random.nextFloat() * 0.6f + 0.6f, Random.nextFloat()) } }

    Canvas(modifier) {
        val corner = 26.dp.toPx()
        val inset = 6.dp.toPx()
        val inner = RoundRect(inset, inset, size.width - inset, size.height - inset, CornerRadius(corner - inset))
        val glass = Path().apply { addRoundRect(inner) }
        val innerH = size.height - inset * 2
        val levelY = inset + innerH * (1f - level)

        drawRoundRect(
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0.02f))),
            cornerRadius = CornerRadius(corner),
        )
        clipPath(glass) {
            if (level > 0.001f) {
                rotate(-tilt.x * 18f, Offset(size.width / 2, levelY)) {
                    drawPath(
                        wavePath(-size.width, size.width * 2, levelY, size.height * 2, 4.dp.toPx(), size.width * 0.9f, phase + 2f),
                        Color(0xFF1E7BD8).copy(alpha = 0.6f),
                    )
                    drawPath(
                        wavePath(-size.width, size.width * 2, levelY + 3.dp.toPx(), size.height * 2, 5.dp.toPx(), size.width * 1.1f, phase),
                        Brush.verticalGradient(listOf(Color(0xFF8AF4FF), c.water, Color(0xFF1B5FD6)), levelY, size.height),
                    )
                }
                bubbles.forEach { (bx, speed, off) ->
                    val p = (rise * speed + off) % 1f
                    val y = size.height - inset - p * (size.height - levelY)
                    if (y > levelY + 8.dp.toPx()) {
                        drawCircle(Color.White.copy(alpha = 0.35f * (1f - p)), (2.dp + 3.dp * off).toPx(), Offset(size.width * bx, y), style = Stroke(1.2.dp.toPx()))
                    }
                }
            }
            drawRoundRect(
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)),
                topLeft = Offset(size.width * 0.12f, inset + 10.dp.toPx()), size = Size(size.width * 0.08f, innerH * 0.7f),
                cornerRadius = CornerRadius(8.dp.toPx()),
            )
        }
        for (i in 1..3) {
            val y = inset + innerH * (i / 4f)
            drawLine(Color.White.copy(alpha = 0.22f), Offset(size.width - inset - 14.dp.toPx(), y), Offset(size.width - inset - 4.dp.toPx(), y), 1.5.dp.toPx())
        }
        drawRoundRect(Color.White.copy(alpha = 0.22f), cornerRadius = CornerRadius(corner), style = Stroke(2.dp.toPx()))
    }
}

/** A number that counts up to its value. */
@Composable
fun AnimatedCount(value: Int, style: TextStyle, color: Color, modifier: Modifier = Modifier, format: (Int) -> String = { it.toString() }) {
    val v by animateIntAsState(value, tween(900, easing = FastOutSlowInEasing), label = "count")
    Text(format(v), style = style, color = color, modifier = modifier)
}

private data class Particle(val angle: Float, val speed: Float, val color: Color, val size: Float, val spin: Float)

/** A burst of confetti each time [trigger] changes (and is above zero). */
@Composable
fun ConfettiBurst(trigger: Int, modifier: Modifier = Modifier) {
    if (trigger <= 0) return
    val c = App.colors
    val anim = remember { Animatable(1f) }
    val particles = remember(trigger) {
        val palette = listOf(c.primary, c.water, c.data, c.amber, c.coral, Color.White)
        List(70) {
            Particle(
                angle = (-PI / 2 + (Random.nextFloat() - 0.5f) * PI * 0.9).toFloat(),
                speed = Random.nextFloat() * 0.9f + 0.5f,
                color = palette[it % palette.size],
                size = Random.nextFloat() * 6f + 5f,
                spin = Random.nextFloat() * 720f - 360f,
            )
        }
    }
    LaunchedEffect(trigger) {
        anim.snapTo(0f)
        anim.animateTo(1f, tween(1800, easing = LinearEasing))
    }
    Canvas(modifier.fillMaxSize()) {
        val t = anim.value
        if (t >= 1f) return@Canvas
        val origin = Offset(size.width / 2, size.height * 0.35f)
        val reach = size.height * 0.9f
        particles.forEach { p ->
            val x = origin.x + cos(p.angle) * p.speed * reach * t
            val y = origin.y + sin(p.angle) * p.speed * reach * t + reach * 1.1f * t * t
            rotate(p.spin * t, Offset(x, y)) {
                drawRect(p.color.copy(alpha = (1f - t).coerceIn(0f, 1f)), Offset(x, y), Size(p.size.dp.toPx() * 0.6f, p.size.dp.toPx()))
            }
        }
    }
}
