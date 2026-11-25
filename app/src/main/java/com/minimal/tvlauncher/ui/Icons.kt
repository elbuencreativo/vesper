package com.minimal.tvlauncher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

enum class QuickAction { Wifi, Bluetooth, AndroidSettings, LauncherSettings, ShowArt }

/** Iconos de línea fina dibujados a mano: sin recursos ni librerías de iconos. */
@Composable
fun QuickIcon(action: QuickAction, tint: Color, size: Dp = 16.dp) {
    Canvas(Modifier.size(size)) {
        val stroke = Stroke(width = this.size.minDimension * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (action) {
            QuickAction.Wifi -> drawWifi(tint, stroke)
            QuickAction.Bluetooth -> drawBluetooth(tint, stroke)
            QuickAction.AndroidSettings -> drawGear(tint, stroke)
            QuickAction.LauncherSettings -> drawHalfCircle(tint, stroke)
            QuickAction.ShowArt -> drawEye(tint, stroke)
        }
    }
}

private fun DrawScope.drawWifi(c: Color, stroke: Stroke) {
    val w = size.width
    val cx = w / 2f
    val baseY = size.height * 0.82f
    listOf(0.22f, 0.40f, 0.58f).forEach { r ->
        val rad = w * r
        drawArc(
            color = c, startAngle = 225f, sweepAngle = 90f, useCenter = false,
            topLeft = Offset(cx - rad, baseY - rad), size = Size(rad * 2, rad * 2), style = stroke,
        )
    }
    drawCircle(c, radius = w * 0.045f, center = Offset(cx, baseY))
}

private fun DrawScope.drawBluetooth(c: Color, stroke: Stroke) {
    val w = size.width
    val h = size.height
    val p = Path().apply {
        moveTo(w * 0.28f, h * 0.30f)
        lineTo(w * 0.72f, h * 0.70f)
        lineTo(w * 0.50f, h * 0.90f)
        lineTo(w * 0.50f, h * 0.10f)
        lineTo(w * 0.72f, h * 0.30f)
        lineTo(w * 0.28f, h * 0.70f)
    }
    drawPath(p, c, style = stroke)
}

private fun DrawScope.drawGear(c: Color, stroke: Stroke) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val r = size.width * 0.22f
    drawCircle(c, radius = r, center = center, style = stroke)
    for (i in 0 until 8) {
        val a = Math.toRadians(i * 45.0)
        val from = Offset(center.x + (cos(a) * size.width * 0.33f).toFloat(), center.y + (sin(a) * size.width * 0.33f).toFloat())
        val to = Offset(center.x + (cos(a) * size.width * 0.45f).toFloat(), center.y + (sin(a) * size.width * 0.45f).toFloat())
        drawLine(c, from, to, strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}

/** Ojo: contorno de almendra con la pupila (ver la obra sin interfaz). */
private fun DrawScope.drawEye(c: Color, stroke: Stroke) {
    val w = size.width
    val h = size.height
    val p = Path().apply {
        moveTo(w * 0.06f, h * 0.5f)
        quadraticBezierTo(w * 0.5f, h * 0.06f, w * 0.94f, h * 0.5f)
        quadraticBezierTo(w * 0.5f, h * 0.94f, w * 0.06f, h * 0.5f)
        close()
    }
    drawPath(p, c, style = stroke)
    drawCircle(c, radius = w * 0.14f, center = Offset(w / 2f, h / 2f))
}

private fun DrawScope.drawHalfCircle(c: Color, stroke: Stroke) {
    val d = size.width * 0.84f
    val tl = Offset((size.width - d) / 2f, (size.height - d) / 2f)
    drawCircle(c, radius = d / 2f, center = Offset(size.width / 2f, size.height / 2f), style = stroke)
    drawArc(c, startAngle = 90f, sweepAngle = 180f, useCenter = true, topLeft = tl, size = Size(d, d))
}
