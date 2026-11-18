package com.example.ui.components

import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.example.ui.theme.*
import kotlin.math.*

class GisSymbologyPaints {
    val labelHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#CC070B12")
        style = Paint.Style.STROKE
        strokeWidth = 5.5f
        strokeJoin = Paint.Join.ROUND
        textSize = 21f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
    val labelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#F3F4F6")
        style = Paint.Style.FILL
        textSize = 21f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
    val shieldTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#FFD54F")
        style = Paint.Style.FILL
        textSize = 19f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
}

fun DrawScope.drawScaleAdaptiveBuilding(
    screenPoly: List<Offset>,
    centerScreen: Offset,
    fillColor: Color,
    strokeColor: Color,
    isSelected: Boolean,
    viewWidthM: Float,
    pathBuffer: Path
) {
    if (screenPoly.size < 3) return
    var minX = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE
    for (pt in screenPoly) {
        if (pt.x < minX) minX = pt.x
        if (pt.x > maxX) maxX = pt.x
        if (pt.y < minY) minY = pt.y
        if (pt.y > maxY) maxY = pt.y
    }
    val spanX = maxX - minX
    val spanY = maxY - minY

    // Ensure buildings NEVER vanish at overview scales (3km..15km)
    if (spanX < 5.5f && spanY < 5.5f) {
        val boxHalf = if (isSelected) 5.5f else if (viewWidthM > 14000f) 3.2f else 3.8f
        val tl = Offset(centerScreen.x - boxHalf, centerScreen.y - boxHalf)
        val sz = Size(boxHalf * 2f, boxHalf * 2f)
        drawRect(Color.Black.copy(alpha = 0.55f), topLeft = tl + Offset(1.5f, 1.5f), size = sz)
        drawRect(fillColor.copy(alpha = 0.92f), topLeft = tl, size = sz)
        drawRect(strokeColor, topLeft = tl, size = sz, style = Stroke(width = if (isSelected) 2.2f else 1.1f))
        return
    }

    // Drop shadow when buildings are clearly resolved
    if (spanX >= 8f || spanY >= 8f) {
        pathBuffer.reset()
        pathBuffer.moveTo(screenPoly[0].x + 2.4f, screenPoly[0].y + 2.4f)
        for (k in 1 until screenPoly.size) {
            pathBuffer.lineTo(screenPoly[k].x + 2.4f, screenPoly[k].y + 2.4f)
        }
        pathBuffer.close()
        drawPath(pathBuffer, Color.Black.copy(alpha = 0.42f))
    }

    // Main footprint polygon
    pathBuffer.reset()
    pathBuffer.moveTo(screenPoly[0].x, screenPoly[0].y)
    for (k in 1 until screenPoly.size) {
        pathBuffer.lineTo(screenPoly[k].x, screenPoly[k].y)
    }
    pathBuffer.close()
    drawPath(pathBuffer, fillColor)
    drawPath(pathBuffer, strokeColor, style = Stroke(width = if (isSelected) 3.0f else 1.3f, join = StrokeJoin.Round))

    // Architectural roof ridge centerline when zoomed into town/house scale
    if (viewWidthM <= 450f && spanX >= 14f && screenPoly.size >= 4) {
        val p0 = screenPoly[0]
        val p1 = screenPoly[1]
        val p2 = screenPoly[2]
        val d01 = hypot(p1.x - p0.x, p1.y - p0.y)
        val d12 = hypot(p2.x - p1.x, p2.y - p1.y)
        val (a, b) = if (d01 >= d12) {
            val midA = Offset((p0.x + screenPoly.last().x) * 0.5f, (p0.y + screenPoly.last().y) * 0.5f)
            val midB = Offset((p1.x + p2.x) * 0.5f, (p1.y + p2.y) * 0.5f)
            midA to midB
        } else {
            val midA = Offset((p0.x + p1.x) * 0.5f, (p0.y + p1.y) * 0.5f)
            val midB = Offset((p2.x + screenPoly[3].x) * 0.5f, (p2.y + screenPoly[3].y) * 0.5f)
            midA to midB
        }
        drawLine(
            color = strokeColor.copy(alpha = 0.55f),
            start = a,
            end = b,
            strokeWidth = 1.2f
        )
    }
}

fun DrawScope.drawCartographicRoadWithSymbols(
    pts: List<Offset>,
    name: String,
    ref: String,
    highwayType: String,
    lanes: Int,
    oneWay: Boolean,
    isBridge: Boolean,
    isRoundabout: Boolean,
    isSelected: Boolean,
    strokePx: Float,
    viewWidthM: Float,
    showSymbols: Boolean,
    paints: GisSymbologyPaints,
    placedBoxes: MutableList<RectF>,
    pathBuffer: Path,
    showTextLabels: Boolean = true
) {
    if (pts.size < 2) return

    val isTrunkPrimary = highwayType in setOf("trunk", "trunk_link", "primary", "primary_link", "motorway", "motorway_link")
    val isSecondaryTertiary = highwayType in setOf("secondary", "secondary_link", "tertiary", "tertiary_link")
    val isUnpavedOrPath = highwayType in setOf("track", "path", "footway", "bridleway", "steps")

    val roadColor = when {
        isSelected -> CyberCyan
        isTrunkPrimary -> LaserOrange
        isSecondaryTertiary -> Color(0xFFFFD54F)
        isUnpavedOrPath -> Color(0xFFA1887F).copy(alpha = 0.90f)
        else -> Color(0xFFE5E7EB).copy(alpha = 0.88f)
    }

    pathBuffer.reset()
    pathBuffer.moveTo(pts[0].x, pts[0].y)
    var totalScreenLen = 0f
    for (k in 1 until pts.size) {
        pathBuffer.lineTo(pts[k].x, pts[k].y)
        totalScreenLen += hypot(pts[k].x - pts[k - 1].x, pts[k].y - pts[k - 1].y)
    }

    // 1. Dark Casing / Bridge Concrete Deck Casing
    val casingColor = if (isBridge) Color(0xFF00E5FF) else DarkBackground.copy(alpha = 0.84f)
    val casingWidth = if (isBridge) strokePx + 5.2f else strokePx + 2.6f
    drawPath(
        path = pathBuffer,
        color = casingColor,
        style = Stroke(width = casingWidth, cap = if (isBridge) StrokeCap.Butt else StrokeCap.Round, join = StrokeJoin.Round)
    )

    // 2. Main Road Body
    val dashEffect = if (isUnpavedOrPath) PathEffect.dashPathEffect(floatArrayOf(9f, 6f), 0f) else null
    drawPath(
        path = pathBuffer,
        color = roadColor,
        style = Stroke(
            width = strokePx,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = dashEffect
        )
    )

    if (!showSymbols) return

    // 3. Dashed Centerline for Multi-Lane / Arterial Roads when zoomed in enough
    if (!isUnpavedOrPath && (isTrunkPrimary || isSecondaryTertiary || lanes >= 2) && strokePx >= 3.2f) {
        drawPath(
            path = pathBuffer,
            color = if (isTrunkPrimary) Color(0xFF1B1F26) else Color(0xFF232730).copy(alpha = 0.72f),
            style = Stroke(
                width = (strokePx * 0.22f).coerceAtLeast(1.1f),
                cap = StrokeCap.Butt,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f), 0f)
            )
        )
    }

    // 4. Bridge Abutment Wing-Walls (`] [`) at entry and exit vertices
    if (isBridge && totalScreenLen >= 8f) {
        drawBridgeAbutmentTicks(pts.first(), pts[1], strokePx)
        drawBridgeAbutmentTicks(pts.last(), pts[pts.size - 2], strokePx)
    }

    // 5. Roundabout Junction Symbol (`◎`)
    if (isRoundabout) {
        val avgX = pts.map { it.x }.average().toFloat()
        val avgY = pts.map { it.y }.average().toFloat()
        val rRadius = (strokePx * 1.4f).coerceIn(6f, 22f)
        drawCircle(DarkBackground, radius = rRadius + 2f, center = Offset(avgX, avgY))
        drawCircle(Color(0xFFFFD54F), radius = rRadius, center = Offset(avgX, avgY), style = Stroke(width = 2.2f))
        drawCircle(CyberCyan, radius = (rRadius * 0.38f).coerceAtLeast(2.2f), center = Offset(avgX, avgY))
    }

    // 6. One-Way Directional Arrows (`▸`)
    if ((oneWay || isTrunkPrimary) && totalScreenLen >= 38f) {
        val midIdx = pts.size / 2
        val a = pts[(midIdx - 1).coerceAtLeast(0)]
        val b = pts[midIdx.coerceAtLeast(1)]
        val dx = b.x - a.x
        val dy = b.y - a.y
        val dLen = hypot(dx, dy)
        if (dLen >= 8f) {
            val ux = dx / dLen
            val uy = dy / dLen
            val nx = -uy
            val ny = ux
            val cx = (a.x + b.x) * 0.5f
            val cy = (a.y + b.y) * 0.5f
            val arrowLen = (strokePx * 0.95f).coerceIn(5.5f, 11f)
            val arrowW = arrowLen * 0.65f
            val tip = Offset(cx + ux * arrowLen, cy + uy * arrowLen)
            val left = Offset(cx - ux * arrowLen * 0.5f + nx * arrowW, cy - uy * arrowLen * 0.5f + ny * arrowW)
            val right = Offset(cx - ux * arrowLen * 0.5f - nx * arrowW, cy - uy * arrowLen * 0.5f - ny * arrowW)
            val arrowPath = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(left.x, left.y)
                lineTo(right.x, right.y)
                close()
            }
            drawPath(arrowPath, if (oneWay) Color.White else Color(0xFFFFF59D))
        }
    }

    // 7. Route Highway Shield (`[C58]`, `[B19]`) & Road Name Labels (Collision-free)
    if (!showTextLabels) return
    if (totalScreenLen < 42f && ref.isBlank()) return
    val anchor = pts[pts.size / 2]
    val cw = size.width
    val ch = size.height
    if (anchor.x !in 32f..(cw - 32f) || anchor.y !in 32f..(ch - 32f)) return

    val shieldLabel = ref.trim().ifBlank {
        if (name.contains("Magadi Road", ignoreCase = true) || name.equals("C58", ignoreCase = true)) "C58" else ""
    }

    if (shieldLabel.isNotBlank() && (isTrunkPrimary || isSecondaryTertiary || totalScreenLen >= 55f)) {
        val badgeText = shieldLabel.take(6)
        val bw = (badgeText.length * 11.5f + 16f).coerceIn(38f, 84f)
        val bh = 22f
        val rect = RectF(anchor.x - bw * 0.5f, anchor.y - bh * 0.5f, anchor.x + bw * 0.5f, anchor.y + bh * 0.5f)
        if (placedBoxes.none { RectF.intersects(it, rect) }) {
            placedBoxes.add(RectF(rect.left - 12f, rect.top - 10f, rect.right + 12f, rect.bottom + 10f))
            drawRoundRect(
                color = Color(0xFF0B1320),
                topLeft = Offset(rect.left, rect.top),
                size = Size(bw, bh),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = if (isTrunkPrimary) LaserOrange else Color(0xFFFFD54F),
                topLeft = Offset(rect.left, rect.top),
                size = Size(bw, bh),
                cornerRadius = CornerRadius(6f, 6f),
                style = Stroke(width = 1.8f)
            )
            paints.shieldTextPaint.color = if (isTrunkPrimary) android.graphics.Color.parseColor("#FFAB40") else android.graphics.Color.parseColor("#FFD54F")
            drawContext.canvas.nativeCanvas.drawText(badgeText, anchor.x, anchor.y + 6.5f, paints.shieldTextPaint)
        }
    } else if (
        viewWidthM <= 3600f &&
        name.isNotBlank() &&
        !name.startsWith("Unnamed ") &&
        !name.startsWith("Magadi Residential") &&
        !name.startsWith("C58 Track") &&
        !(name.startsWith("C58 ") && name.contains('#')) &&
        totalScreenLen >= 68f
    ) {
        val cleanName = name.take(20)
        val estW = cleanName.length * 10.5f
        val rect = RectF(anchor.x - estW * 0.5f, anchor.y - 20f, anchor.x + estW * 0.5f, anchor.y + 4f)
        if (placedBoxes.none { RectF.intersects(it, rect) }) {
            placedBoxes.add(RectF(rect.left - 8f, rect.top - 6f, rect.right + 8f, rect.bottom + 6f))
            paints.labelTextPaint.textSize = if (viewWidthM <= 450f) 21f else 18f
            paints.labelHaloPaint.textSize = paints.labelTextPaint.textSize
            drawContext.canvas.nativeCanvas.drawText(cleanName, rect.left, anchor.y - 4f, paints.labelHaloPaint)
            drawContext.canvas.nativeCanvas.drawText(cleanName, rect.left, anchor.y - 4f, paints.labelTextPaint)
        }
    }
}

private fun DrawScope.drawBridgeAbutmentTicks(endPt: Offset, prevPt: Offset, strokePx: Float) {
    val dx = endPt.x - prevPt.x
    val dy = endPt.y - prevPt.y
    val dLen = hypot(dx, dy).coerceAtLeast(1f)
    val ux = dx / dLen
    val uy = dy / dLen
    val nx = -uy
    val ny = ux
    val halfW = strokePx * 0.72f + 2.5f
    val wing = 5.5f
    drawLine(
        color = Color(0xFF00E5FF),
        start = Offset(endPt.x + nx * halfW, endPt.y + ny * halfW),
        end = Offset(endPt.x + nx * (halfW + wing) + ux * wing, endPt.y + ny * (halfW + wing) + uy * wing),
        strokeWidth = 2.2f
    )
    drawLine(
        color = Color(0xFF00E5FF),
        start = Offset(endPt.x - nx * halfW, endPt.y - ny * halfW),
        end = Offset(endPt.x - nx * (halfW + wing) + ux * wing, endPt.y - ny * (halfW + wing) + uy * wing),
        strokeWidth = 2.2f
    )
}

fun DrawScope.drawRoadInfraNodeSymbol(
    center: Offset,
    category: String,
    paints: GisSymbologyPaints
): Boolean {
    return when (category) {
        "gate", "bollard", "lift_gate", "toll_booth" -> {
            drawCircle(Color(0xFF111827), radius = 6.5f, center = center)
            drawLine(Color(0xFFFF5252), center + Offset(-5.5f, 0f), center + Offset(5.5f, 0f), strokeWidth = 2.8f)
            drawCircle(Color(0xFFFFD54F), radius = 2.0f, center = center)
            true
        }
        "crossing" -> {
            drawCircle(Color(0xFF111827), radius = 6.0f, center = center)
            drawLine(Color.White, center + Offset(-3.5f, -2.5f), center + Offset(3.5f, -2.5f), strokeWidth = 1.6f)
            drawLine(Color.White, center + Offset(-3.5f, 0f), center + Offset(3.5f, 0f), strokeWidth = 1.6f)
            drawLine(Color.White, center + Offset(-3.5f, 2.5f), center + Offset(3.5f, 2.5f), strokeWidth = 1.6f)
            true
        }
        "bus_stop" -> {
            drawRoundRect(Color(0xFF0288D1), topLeft = center - Offset(6f, 6f), size = Size(12f, 12f), cornerRadius = CornerRadius(3f, 3f))
            drawRoundRect(Color.White, topLeft = center - Offset(6f, 6f), size = Size(12f, 12f), cornerRadius = CornerRadius(3f, 3f), style = Stroke(1.3f))
            paints.shieldTextPaint.color = android.graphics.Color.WHITE
            paints.shieldTextPaint.textSize = 11f
            drawContext.canvas.nativeCanvas.drawText("B", center.x, center.y + 4f, paints.shieldTextPaint)
            paints.shieldTextPaint.textSize = 19f
            true
        }
        "traffic_signals" -> {
            drawRoundRect(Color(0xFF111827), topLeft = center - Offset(4f, 7f), size = Size(8f, 14f), cornerRadius = CornerRadius(2f, 2f))
            drawCircle(Color(0xFFFF5252), radius = 1.6f, center = center + Offset(0f, -3.5f))
            drawCircle(Color(0xFFFFD54F), radius = 1.6f, center = center)
            drawCircle(Color(0xFF69F0AE), radius = 1.6f, center = center + Offset(0f, 3.5f))
            true
        }
        "turning_circle", "mini_roundabout" -> {
            drawCircle(Color(0xFFFFD54F), radius = 6.0f, center = center, style = Stroke(2.0f))
            drawCircle(CyberCyan, radius = 2.2f, center = center)
            true
        }
        else -> false
    }
}
