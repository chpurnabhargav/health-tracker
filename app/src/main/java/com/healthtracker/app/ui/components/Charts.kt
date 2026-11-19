package com.healthtracker.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthtracker.app.domain.Fmt
import com.healthtracker.app.domain.TrendPoint
import com.healthtracker.app.ui.theme.App
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Daily readings as dots with the 7-day trend as a line. Tap or drag to inspect a day. */
@Composable
fun WeightChart(points: List<TrendPoint>, modifier: Modifier = Modifier) {
    val c = App.colors
    val tm = rememberTextMeasurer()
    var selected by remember(points) { mutableStateOf<Int?>(null) }

    if (points.isEmpty()) {
        Box(modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
            Text("Your chart starts with your first weigh-in.", style = MaterialTheme.typography.bodyMedium, color = c.ink2)
        }
        return
    }

    val lo0 = points.minOf { min(it.kg, it.trend) }
    val hi0 = points.maxOf { max(it.kg, it.trend) }
    var lo = floor(lo0 - 0.3)
    var hi = ceil(hi0 + 0.3)
    if (hi - lo < 2) { lo -= 1; hi += 1 }
    val range = hi - lo
    val step = when { range <= 5 -> 1.0; range <= 12 -> 2.0; range <= 30 -> 5.0; else -> 10.0 }
    val firstDay = points.first().day
    val lastDay = points.last().day
    val spanDays = max(1L, lastDay - firstDay)

    val labelStyle = TextStyle(fontSize = 11.sp, color = c.ink2)
    val tipStyle = TextStyle(fontSize = 12.sp, color = c.ink, fontWeight = FontWeight.SemiBold)
    val endStyle = TextStyle(fontSize = 12.sp, color = c.ink, fontWeight = FontWeight.Bold)

    fun xFor(day: Long, width: Float, left: Float, right: Float): Float =
        if (points.size == 1) (left + width - right) / 2f
        else left + (width - left - right) * ((day - firstDay).toFloat() / spanDays)

    val desc = "Weight chart: ${points.size} readings from ${Fmt.kg(points.first().kg)} kg to ${Fmt.kg(points.last().kg)} kg. Trend now ${Fmt.kg(points.last().trend)} kg."

    Canvas(
        modifier
            .fillMaxWidth()
            .height(210.dp)
            .semantics { contentDescription = desc }
            .pointerInput(points) {
                val left = 36.dp.toPx(); val right = 44.dp.toPx()
                detectTapGestures { o ->
                    val w = size.width.toFloat()
                    selected = points.indices.minByOrNull { abs(xFor(points[it].day, w, left, right) - o.x) }
                }
            }
            .pointerInput(points) {
                val left = 36.dp.toPx(); val right = 44.dp.toPx()
                detectHorizontalDragGestures { change, _ ->
                    val w = size.width.toFloat()
                    selected = points.indices.minByOrNull { abs(xFor(points[it].day, w, left, right) - change.position.x) }
                }
            }
    ) {
        val left = 36.dp.toPx()
        val right = 44.dp.toPx()
        val top = 28.dp.toPx()
        val bottom = 24.dp.toPx()
        val w = size.width
        val h = size.height
        fun y(v: Double): Float = (top + (h - top - bottom) * ((hi - v) / range)).toFloat()

        // Grid and y labels
        var t = ceil(lo / step) * step
        while (t <= hi + 1e-6) {
            val yy = y(t)
            drawLine(c.grid, Offset(left, yy), Offset(w - right + 8.dp.toPx(), yy), 1.dp.toPx())
            val lbl = tm.measure(if (step >= 1) t.toInt().toString() else Fmt.one(t), labelStyle)
            drawText(lbl, topLeft = Offset(left - 6.dp.toPx() - lbl.size.width, yy - lbl.size.height / 2f))
            t += step
        }
        drawLine(c.axis, Offset(left, h - bottom), Offset(w - right + 8.dp.toPx(), h - bottom), 1.dp.toPx())

        // X labels: first, middle, last
        val xDays = if (points.size >= 3) listOf(firstDay, firstDay + spanDays / 2, lastDay) else listOf(firstDay, lastDay).distinct()
        xDays.forEachIndexed { i, d ->
            val lbl = tm.measure(Fmt.dayMonth(LocalDate.ofEpochDay(d)), labelStyle)
            val cx = xFor(d, w, left, right)
            val x = when (i) {
                0 -> max(0f, cx - lbl.size.width / 2f)
                xDays.lastIndex -> min(w - lbl.size.width, cx - lbl.size.width / 2f)
                else -> cx - lbl.size.width / 2f
            }
            drawText(lbl, topLeft = Offset(x, h - bottom + 6.dp.toPx()))
        }

        // Daily dots with a surface ring
        points.forEach { p ->
            val cx = xFor(p.day, w, left, right)
            drawCircle(c.card, radius = 6.dp.toPx(), center = Offset(cx, y(p.kg)))
            drawCircle(c.dot, radius = 4.dp.toPx(), center = Offset(cx, y(p.kg)))
        }

        // Trend line
        val path = Path()
        points.forEachIndexed { i, p ->
            val pt = Offset(xFor(p.day, w, left, right), y(p.trend))
            if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
        }
        drawPath(path, c.data, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        val last = points.last()
        val end = Offset(xFor(last.day, w, left, right), y(last.trend))
        drawCircle(c.card, radius = 7.dp.toPx(), center = end)
        drawCircle(c.data, radius = 5.dp.toPx(), center = end)
        val endLbl = tm.measure(Fmt.kg(last.trend), endStyle)
        drawText(endLbl, topLeft = Offset(min(w - endLbl.size.width, end.x + 9.dp.toPx()), end.y - endLbl.size.height / 2f))

        // Selection crosshair and tooltip
        selected?.let { i ->
            val p = points[i]
            val cx = xFor(p.day, w, left, right)
            drawLine(c.axis, Offset(cx, top - 4.dp.toPx()), Offset(cx, h - bottom), 1.dp.toPx())
            drawCircle(c.card, radius = 7.dp.toPx(), center = Offset(cx, y(p.kg)))
            drawCircle(c.ink, radius = 5.dp.toPx(), center = Offset(cx, y(p.kg)))
            val text = "${Fmt.dayMonth(LocalDate.ofEpochDay(p.day))} · ${Fmt.kg(p.kg)} kg · trend ${Fmt.kg(p.trend)}"
            val lbl = tm.measure(text, tipStyle)
            val pad = 6.dp.toPx()
            val bw = lbl.size.width + pad * 2
            val bx = (cx - bw / 2).coerceIn(0f, w - bw)
            drawRoundRect(c.soft, topLeft = Offset(bx, 0f), size = Size(bw, lbl.size.height + pad), cornerRadius = CornerRadius(8.dp.toPx()))
            drawText(lbl, topLeft = Offset(bx + pad, pad / 2))
        }
    }
}

data class BarDatum(val label: String, val value: Float, val color: Color, val valueLabel: String, val description: String)

/** Vertical bars you can tap, with an optional target line. */
@Composable
fun DayBarChart(
    bars: List<BarDatum>,
    maxValue: Float,
    target: Float?,
    selected: Int?,
    onSelect: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
    showValues: (Int) -> Boolean = { it == selected },
) {
    val c = App.colors
    val plotH = 140.dp
    val labelH = 24.dp
    Box(modifier.fillMaxWidth().height(plotH + labelH + 20.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val base = size.height - labelH.toPx()
            drawLine(c.axis, Offset(0f, base), Offset(size.width, base), 1.dp.toPx())
            if (target != null && maxValue > 0) {
                val ty = base - plotH.toPx() * (target / maxValue).coerceIn(0f, 1f)
                drawLine(c.ink2, Offset(0f, ty), Offset(size.width, ty), 1.5.dp.toPx())
            }
        }
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween) {
            bars.forEachIndexed { i, b ->
                val isSel = i == selected
                val barH = if (maxValue > 0) plotH * (b.value / maxValue).coerceIn(0f, 1f) else 0.dp
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSel) c.soft else Color.Transparent)
                        .then(
                            if (onSelect != null) Modifier.clickable(role = Role.Button) { onSelect(i) } else Modifier
                        )
                        .semantics { contentDescription = b.description; this.selected = isSel },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    if (showValues(i) && b.value > 0) {
                        Text(b.valueLabel, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = c.ink)
                        Spacer(Modifier.height(4.dp))
                    }
                    if (barH > 0.dp) {
                        Box(
                            Modifier
                                .width(24.dp)
                                .height(barH)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(b.color)
                        )
                    }
                    Box(Modifier.height(labelH), contentAlignment = Alignment.BottomCenter) {
                        Text(
                            b.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium),
                            color = c.ink2,
                        )
                    }
                }
            }
        }
    }
}

data class Segment(val fraction: Float, val color: Color, val label: String)

/** One whole split into parts, labelled inside when there's room. */
@Composable
fun SegmentedBar(parts: List<Segment>, description: String) {
    val shown = parts.filter { it.fraction > 0f }
    Row(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        shown.forEach { p ->
            Box(
                Modifier
                    .weight(p.fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(p.color),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (p.fraction >= 0.12f) {
                    Text(
                        p.label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}
