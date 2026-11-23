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
import com.example.model.PhysicalBridgeSpec
import com.example.model.computePhysicalBridgeSpec
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

enum class GisLandmarkKind {
    PLACE_SETTLEMENT,
    FACTORY_INDUSTRIAL,
    POLICE,
    SCHOOL_EDUCATION,
    HOSPITAL_HEALTH,
    TRANSPORT_AIR,
    WORSHIP,
    COMMERCIAL_FUEL,
    GENERIC
}

data class GisLandmarkStyle(
    val kind: GisLandmarkKind,
    val badge: String,
    val accentColor: Color,
    val fillColor: Color,
    val strokeColor: Color,
    val priority: Int
)

fun classifyGisLandmark(
    typeOrCategory: String,
    amenity: String = "",
    name: String = ""
): GisLandmarkStyle {
    val t = typeOrCategory.lowercase()
    val a = amenity.lowercase()
    val n = name.lowercase()
    val combined = "$t $a"

    return when {
        t in setOf("city", "town", "village", "suburb", "quarter", "neighbourhood", "hamlet", "locality") ->
            GisLandmarkStyle(
                kind = GisLandmarkKind.PLACE_SETTLEMENT,
                badge = "",
                accentColor = NeonLime,
                fillColor = NeonLime.copy(alpha = 0.25f),
                strokeColor = NeonLime,
                priority = 340
            )
        combined.contains("factory") || combined.contains("industrial") || combined.contains("works") ||
            combined.contains("mine") || combined.contains("quarry") || combined.contains("sodium_carbonate") ||
            combined.contains("soda") || combined.contains("warehouse") || combined.contains("manufacture") ||
            combined.contains("plant") || combined.contains("craft:") || combined.contains("silo") ||
            combined.contains("storage_tank") || combined.contains("processing_hall") || combined.contains("weighbridge") ||
            combined.contains("boiler") || combined.contains("kiln") || combined.contains("pump_house") ||
            combined.contains("generator") ->
            GisLandmarkStyle(
                kind = GisLandmarkKind.FACTORY_INDUSTRIAL,
                badge = "F",
                accentColor = Color(0xFFFFB300),
                fillColor = Color(0xFFFF9800).copy(alpha = 0.68f),
                strokeColor = Color(0xFFFFE082),
                priority = 290
            )
        combined.contains("police") || combined.contains("prison") || combined.contains("courthouse") ||
            combined.contains("fire_station") || combined.contains("military") || combined.contains("ranger_station") ||
            combined.contains("barracks") || combined.contains("armory") || combined.contains("guard_house") ||
            (n.contains("police") && combined.contains("office")) ->
            GisLandmarkStyle(
                kind = GisLandmarkKind.POLICE,
                badge = "P",
                accentColor = Color(0xFF42A5F5),
                fillColor = Color(0xFF1E88E5).copy(alpha = 0.68f),
                strokeColor = Color(0xFFBBDEFB),
                priority = 285
            )
        combined.contains("school") || combined.contains("college") || combined.contains("university") ||
            combined.contains("kindergarten") || combined.contains("education") || combined.contains("library") ||
            combined.contains("research") || combined.contains("tuition") || combined.contains("dormitory") ||
            combined.contains("laboratory") || combined.contains("assembly_hall") || combined.contains("dining_hall") ->
            GisLandmarkStyle(
                kind = GisLandmarkKind.SCHOOL_EDUCATION,
                badge = "S",
                accentColor = Color(0xFF69F0AE),
                fillColor = Color(0xFF2E7D32).copy(alpha = 0.70f),
                strokeColor = Color(0xFFB9F6CA),
                priority = 280
            )
        combined.contains("hospital") || combined.contains("clinic") || combined.contains("doctors") ||
            combined.contains("pharmacy") || combined.contains("healthcare") || combined.contains("dispensary") ||
            combined.contains("health_post") || combined.contains("counselling") ||
            combined.contains("ward_block") || combined.contains("outpatient") ->
            GisLandmarkStyle(
                kind = GisLandmarkKind.HOSPITAL_HEALTH,
                badge = "+",
                accentColor = Color(0xFFFF5252),
                fillColor = Color(0xFFE53935).copy(alpha = 0.70f),
                strokeColor = Color(0xFFFFCDD2),
                priority = 280
            )
        combined.contains("aerodrome") || combined.contains("helipad") || combined.contains("runway") ||
            combined.contains("hangar") || combined.contains("bus_station") || combined.contains("station") ||
            combined.contains("aeroway") ->
            GisLandmarkStyle(
                kind = GisLandmarkKind.TRANSPORT_AIR,
                badge = "T",
                accentColor = Color(0xFF29B6F6),
                fillColor = Color(0xFF0288D1).copy(alpha = 0.66f),
                strokeColor = Color(0xFFB3E5FC),
                priority = 240
            )
        combined.contains("worship") || combined.contains("church") || combined.contains("mosque") ||
            combined.contains("chapel") || combined.contains("temple") || combined.contains("religious") ||
            combined.contains("shrine") || combined.contains("parish") ->
            GisLandmarkStyle(
                kind = GisLandmarkKind.WORSHIP,
                badge = "W",
                accentColor = Color(0xFFCE93D8),
                fillColor = Color(0xFF8E24AA).copy(alpha = 0.64f),
                strokeColor = Color(0xFFE1BEE7),
                priority = 200
            )
        combined.contains("fuel") || combined.contains("bank") || combined.contains("mall") ||
            combined.contains("supermarket") || combined.contains("marketplace") || combined.contains("commercial") ||
            combined.contains("retail") || combined.contains("hotel") || combined.contains("guest_house") ||
            combined.contains("restaurant") || combined.contains("office") || combined.contains("institutional") ||
            combined.contains("townhall") || combined.contains("post_office") || combined.contains("community_centre") ||
            combined.contains("bar") || combined.contains("cafe") || combined.contains("shop") ||
            combined.contains("tourism") || combined.contains("public") || combined.contains("kiosk") ->
            GisLandmarkStyle(
                kind = GisLandmarkKind.COMMERCIAL_FUEL,
                badge = "M",
                accentColor = Color(0xFF26C6DA),
                fillColor = Color(0xFF00ACC1).copy(alpha = 0.64f),
                strokeColor = Color(0xFF80DEEA),
                priority = 190
            )
        else ->
            GisLandmarkStyle(
                kind = GisLandmarkKind.GENERIC,
                badge = "",
                accentColor = Color(0xFFE5E7EB),
                fillColor = LaserOrange.copy(alpha = 0.62f),
                strokeColor = Color(0xFFFFD180),
                priority = 50
            )
    }
}

fun DrawScope.drawScaleAdaptiveBuilding(
    screenPoly: List<Offset>,
    centerScreen: Offset,
    fillColor: Color,
    strokeColor: Color,
    isSelected: Boolean,
    viewWidthM: Float,
    pathBuffer: Path,
    isCompoundPerimeter: Boolean = false,
    roofType: String = "GABLED",
    levels: Int = 1
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

    // Drop shadow scaled by levels when buildings are resolved (skip for compound ground enclosures)
    if (!isCompoundPerimeter && (spanX >= 8f || spanY >= 8f)) {
        val shOff = (2.0f + (levels - 1).coerceIn(0, 5) * 1.1f)
        pathBuffer.reset()
        pathBuffer.moveTo(screenPoly[0].x + shOff, screenPoly[0].y + shOff)
        for (k in 1 until screenPoly.size) {
            pathBuffer.lineTo(screenPoly[k].x + shOff, screenPoly[k].y + shOff)
        }
        pathBuffer.close()
        drawPath(pathBuffer, Color.Black.copy(alpha = 0.45f))
    }

    // Main footprint polygon
    pathBuffer.reset()
    pathBuffer.moveTo(screenPoly[0].x, screenPoly[0].y)
    for (k in 1 until screenPoly.size) {
        pathBuffer.lineTo(screenPoly[k].x, screenPoly[k].y)
    }
    pathBuffer.close()

    if (isCompoundPerimeter) {
        drawPath(pathBuffer, fillColor.copy(alpha = if (isSelected) 0.26f else 0.13f))
        drawPath(
            pathBuffer,
            strokeColor.copy(alpha = 0.92f),
            style = Stroke(
                width = if (isSelected) 3.2f else 2.0f,
                join = StrokeJoin.Round,
                pathEffect = if (isSelected) null else PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
            )
        )
        return
    }

    drawPath(pathBuffer, fillColor)
    drawPath(
        pathBuffer,
        strokeColor,
        style = Stroke(
            width = if (isSelected) 3.0f else if (levels >= 3) 2.0f else 1.3f,
            join = StrokeJoin.Round
        )
    )

    // Architectural roof geometry plan hints when zoomed into town/house scale
    if (viewWidthM <= 480f && (spanX >= 14f || spanY >= 14f) && screenPoly.size >= 3) {
        val roofHintColor = strokeColor.copy(alpha = 0.60f)
        when (roofType) {
            "CONICAL_DOME" -> {
                val r = (min(spanX, spanY) * 0.26f).coerceIn(2.5f, 18f)
                drawCircle(roofHintColor, radius = r, center = centerScreen, style = Stroke(1.1f))
                drawCircle(roofHintColor, radius = 1.8f, center = centerScreen)
            }
            "SAW_TOOTH_INDUSTRIAL" -> {
                val steps = 3
                for (s in 1 until steps) {
                    val f = s.toFloat() / steps
                    val x = minX + spanX * f
                    drawLine(roofHintColor, Offset(x, minY + spanY * 0.15f), Offset(x, maxY - spanY * 0.15f), strokeWidth = 1.1f)
                }
            }
            "FLAT_PARAPET" -> {
                val inset = 2.6f
                if (spanX > inset * 4f && spanY > inset * 4f) {
                    drawRect(
                        color = roofHintColor,
                        topLeft = Offset(minX + inset, minY + inset),
                        size = Size(spanX - inset * 2f, spanY - inset * 2f),
                        style = Stroke(width = 1.0f)
                    )
                }
            }
            else -> if (screenPoly.size >= 4) {
                // GABLED / HIPPED / ARCHED_TRUSS / MONO_PITCH_SHED ridge line
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
                if (roofType == "HIPPED") {
                    val ra = Offset(a.x + (b.x - a.x) * 0.22f, a.y + (b.y - a.y) * 0.22f)
                    val rb = Offset(a.x + (b.x - a.x) * 0.78f, a.y + (b.y - a.y) * 0.78f)
                    drawLine(roofHintColor, ra, rb, strokeWidth = 1.3f)
                    drawLine(roofHintColor.copy(alpha = 0.40f), p0, ra, strokeWidth = 0.9f)
                    drawLine(roofHintColor.copy(alpha = 0.40f), screenPoly.last(), ra, strokeWidth = 0.9f)
                    drawLine(roofHintColor.copy(alpha = 0.40f), p1, rb, strokeWidth = 0.9f)
                    drawLine(roofHintColor.copy(alpha = 0.40f), p2, rb, strokeWidth = 0.9f)
                } else {
                    drawLine(roofHintColor, a, b, strokeWidth = 1.2f)
                }
            }
        }
    }
}

fun DrawScope.drawPerimeterFence2D(
    fenceScreenPoly: List<Offset>,
    fenceType: String,
    isSelected: Boolean,
    gateScreenPos: Offset?,
    gateWidthPx: Float,
    showPosts: Boolean
) {
    if (fenceScreenPoly.size < 3) return
    val fenceColor = when {
        isSelected -> NeonLime
        fenceType == "MASONRY_BLOCK_WALL" -> Color(0xFFD7CCC8).copy(alpha = 0.88f)
        fenceType == "PALISADE_SECURITY_STEEL" -> Color(0xFF90CAF9).copy(alpha = 0.86f)
        fenceType == "LIVE_THORN_HEDGE" -> Color(0xFF7CB342).copy(alpha = 0.86f)
        fenceType == "TIMBER_POST_RAIL" -> Color(0xFFBCAAA4).copy(alpha = 0.84f)
        else -> Color(0xFF80DEEA).copy(alpha = 0.78f) // CHAIN_LINK_POSTS
    }
    val strokeW = when (fenceType) {
        "MASONRY_BLOCK_WALL", "LIVE_THORN_HEDGE" -> if (isSelected) 3.0f else 2.1f
        else -> if (isSelected) 2.6f else 1.5f
    }
    val gateGapSq = (max(6f, gateWidthPx * 0.58f)).let { it * it }
    val nf = fenceScreenPoly.size
    for (i in 0 until nf) {
        val a = fenceScreenPoly[i]
        val b = fenceScreenPoly[(i + 1) % nf]
        val mx = (a.x + b.x) * 0.5f
        val my = (a.y + b.y) * 0.5f
        if (gateScreenPos != null) {
            val dxG = mx - gateScreenPos.x
            val dyG = my - gateScreenPos.y
            if (dxG * dxG + dyG * dyG < gateGapSq) continue
        }
        drawLine(
            color = fenceColor,
            start = a,
            end = b,
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        if (showPosts) {
            drawCircle(fenceColor, radius = 1.9f, center = a)
        }
    }
}

fun DrawScope.drawPropertyGate2D(
    gateCenter: Offset,
    gateAngleRad: Float,
    gateWidthPx: Float,
    gateType: String,
    isSelected: Boolean
) {
    val halfW = (gateWidthPx * 0.5f).coerceIn(3.5f, 26f)
    // World East (+cos) -> Screen +X; World North (+sin) -> Screen -Y
    val tx = cos(gateAngleRad)
    val ty = -sin(gateAngleRad)
    val nx = -ty
    val ny = tx
    val pA = Offset(gateCenter.x - tx * halfW, gateCenter.y - ty * halfW)
    val pB = Offset(gateCenter.x + tx * halfW, gateCenter.y + ty * halfW)

    val leafColor = when {
        isSelected -> Color.White
        gateType == "SECURITY_BOOM_BARRIER" -> Color(0xFFFF9800)
        gateType == "DOUBLE_SWING_TIMBER" -> Color(0xFFFFCA28)
        gateType == "PEDESTRIAN_WICKET" -> NeonLime
        else -> CyberCyan
    }
    // Backing dark contrast
    drawLine(Color(0xFF0A101A), pA, pB, strokeWidth = 5.0f, cap = StrokeCap.Butt)
    // Main Gate Leaf / Boom Bar
    drawLine(leafColor, pA, pB, strokeWidth = 2.8f, cap = StrokeCap.Butt)
    // Outward road-facing tick
    drawLine(
        color = leafColor,
        start = gateCenter,
        end = Offset(gateCenter.x + nx * 4.5f, gateCenter.y + ny * 4.5f),
        strokeWidth = 1.8f
    )
    // Gate Masonry Pillars
    drawCircle(Color.White, radius = 2.6f, center = pA)
    drawCircle(Color.White, radius = 2.6f, center = pB)
}

fun DrawScope.drawLandmarkPinAndLabel(
    center: Offset,
    label: String,
    style: GisLandmarkStyle,
    paints: GisSymbologyPaints,
    placedBoxes: MutableList<RectF>,
    canvasW: Float,
    canvasH: Float,
    showTextLabels: Boolean,
    textSizePx: Float = 20f,
    drawPinAlways: Boolean = false
): Boolean {
    if (center.x < 8f || center.x > canvasW - 8f || center.y < 14f || center.y > canvasH - 8f) return false

    if (!showTextLabels) {
        if (drawPinAlways || style.kind != GisLandmarkKind.GENERIC) {
            drawCircle(DarkBackground, radius = 5.4f, center = center)
            drawCircle(style.accentColor, radius = 3.6f, center = center)
        }
        return false
    }

    val cleanLabel = label.trim().take(26)
    if (cleanLabel.isEmpty()) {
        if (drawPinAlways) {
            drawCircle(DarkBackground, radius = 4.6f, center = center)
            drawCircle(style.accentColor, radius = 3.0f, center = center)
        }
        return false
    }

    paints.labelTextPaint.textSize = textSizePx
    paints.labelHaloPaint.textSize = textSizePx
    val textW = paints.labelTextPaint.measureText(cleanLabel).coerceAtLeast(24f)
    val badgeOffset = if (style.badge.isNotEmpty()) 13f else 9f

    val candidates = arrayOf(
        // Right of pin
        RectF(center.x + badgeOffset, center.y - textSizePx * 0.65f, center.x + badgeOffset + textW, center.y + textSizePx * 0.35f),
        // Left of pin
        RectF(center.x - badgeOffset - textW, center.y - textSizePx * 0.65f, center.x - badgeOffset, center.y + textSizePx * 0.35f),
        // Above pin
        RectF(center.x - textW * 0.5f, center.y - badgeOffset - textSizePx, center.x + textW * 0.5f, center.y - badgeOffset * 0.6f),
        // Below pin
        RectF(center.x - textW * 0.5f, center.y + badgeOffset * 0.6f, center.x + textW * 0.5f, center.y + badgeOffset + textSizePx * 0.85f)
    )

    var chosen: RectF? = null
    for (cRect in candidates) {
        if (cRect.left < 8f || cRect.right > canvasW - 8f || cRect.top < 14f || cRect.bottom > canvasH - 8f) continue
        val padded = RectF(cRect.left - 6f, cRect.top - 5f, cRect.right + 6f, cRect.bottom + 5f)
        if (placedBoxes.none { RectF.intersects(it, padded) }) {
            chosen = cRect
            placedBoxes.add(padded)
            break
        }
    }

    if (chosen == null) {
        if (drawPinAlways || style.kind != GisLandmarkKind.GENERIC) {
            drawCircle(DarkBackground, radius = 4.5f, center = center)
            drawCircle(style.accentColor.copy(alpha = 0.85f), radius = 2.9f, center = center)
        }
        return false
    }

    // Draw pin or categorized badge icon
    if (style.badge.isNotEmpty()) {
        val sz = 16f
        val half = sz * 0.5f
        val badgeTL = Offset(center.x - half, center.y - half)
        drawRoundRect(
            color = Color(0xFF0A101A),
            topLeft = badgeTL,
            size = Size(sz, sz),
            cornerRadius = CornerRadius(4f, 4f)
        )
        drawRoundRect(
            color = style.accentColor,
            topLeft = badgeTL,
            size = Size(sz, sz),
            cornerRadius = CornerRadius(4f, 4f),
            style = Stroke(width = 1.6f)
        )
        paints.shieldTextPaint.textSize = 12.5f
        paints.shieldTextPaint.color = when (style.kind) {
            GisLandmarkKind.FACTORY_INDUSTRIAL -> android.graphics.Color.parseColor("#FFB300")
            GisLandmarkKind.POLICE -> android.graphics.Color.parseColor("#42A5F5")
            GisLandmarkKind.SCHOOL_EDUCATION -> android.graphics.Color.parseColor("#69F0AE")
            GisLandmarkKind.HOSPITAL_HEALTH -> android.graphics.Color.parseColor("#FF5252")
            GisLandmarkKind.WORSHIP -> android.graphics.Color.parseColor("#CE93D8")
            GisLandmarkKind.TRANSPORT_AIR -> android.graphics.Color.parseColor("#29B6F6")
            else -> android.graphics.Color.parseColor("#26C6DA")
        }
        drawContext.canvas.nativeCanvas.drawText(style.badge, center.x, center.y + 4.2f, paints.shieldTextPaint)
        paints.shieldTextPaint.textSize = 19f
    } else {
        drawCircle(DarkBackground, radius = 5.4f, center = center)
        drawCircle(style.accentColor, radius = 3.8f, center = center)
        drawCircle(DarkBackground, radius = 1.6f, center = center)
    }

    val prevColor = paints.labelTextPaint.color
    paints.labelTextPaint.color = when (style.kind) {
        GisLandmarkKind.PLACE_SETTLEMENT -> android.graphics.Color.parseColor("#C6FF00")
        GisLandmarkKind.FACTORY_INDUSTRIAL -> android.graphics.Color.parseColor("#FFD54F")
        GisLandmarkKind.POLICE -> android.graphics.Color.parseColor("#90CAF9")
        GisLandmarkKind.SCHOOL_EDUCATION -> android.graphics.Color.parseColor("#B9F6CA")
        GisLandmarkKind.HOSPITAL_HEALTH -> android.graphics.Color.parseColor("#FF8A80")
        GisLandmarkKind.TRANSPORT_AIR -> android.graphics.Color.parseColor("#81D4FA")
        GisLandmarkKind.WORSHIP -> android.graphics.Color.parseColor("#E1BEE7")
        GisLandmarkKind.COMMERCIAL_FUEL -> android.graphics.Color.parseColor("#80DEEA")
        GisLandmarkKind.GENERIC -> android.graphics.Color.parseColor("#F3F4F6")
    }
    val baseY = chosen.bottom - textSizePx * 0.22f
    drawContext.canvas.nativeCanvas.drawText(cleanLabel, chosen.left, baseY, paints.labelHaloPaint)
    drawContext.canvas.nativeCanvas.drawText(cleanLabel, chosen.left, baseY, paints.labelTextPaint)
    paints.labelTextPaint.color = prevColor
    return true
}

private fun sampleScreenPolyline(pts: List<Offset>, t: Float): Pair<Offset, Offset> {
    if (pts.size < 2) return (pts.firstOrNull() ?: Offset.Zero) to Offset(1f, 0f)
    val clampedT = t.coerceIn(0f, 1f)
    val segLens = FloatArray(pts.size - 1)
    var total = 0f
    for (i in 0 until pts.size - 1) {
        val d = hypot(pts[i + 1].x - pts[i].x, pts[i + 1].y - pts[i].y)
        segLens[i] = d
        total += d
    }
    if (total < 1e-4f) return pts.first() to Offset(1f, 0f)
    val target = clampedT * total
    var acc = 0f
    for (i in 0 until pts.size - 1) {
        val seg = segLens[i]
        if (acc + seg >= target || i == pts.size - 2) {
            val u = if (seg > 1e-4f) ((target - acc) / seg).coerceIn(0f, 1f) else 0f
            val pos = Offset(
                pts[i].x + (pts[i + 1].x - pts[i].x) * u,
                pts[i].y + (pts[i + 1].y - pts[i].y) * u
            )
            val dx = pts[i + 1].x - pts[i].x
            val dy = pts[i + 1].y - pts[i].y
            val dl = hypot(dx, dy).coerceAtLeast(1e-4f)
            return pos to Offset(dx / dl, dy / dl)
        }
        acc += seg
    }
    return pts.last() to Offset(1f, 0f)
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
    showTextLabels: Boolean = true,
    pxPerMeter: Float = 0f,
    roadWidthM: Float = 0f,
    roadLengthM: Float = 0f
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

    val bridgeSpec = if (isBridge) {
        val estLenM = if (roadLengthM > 1f) roadLengthM else if (pxPerMeter > 0.001f) totalScreenLen / pxPerMeter else 30f
        computePhysicalBridgeSpec(estLenM, roadWidthM, lanes, highwayType)
    } else null

    val effDeckWidthPx = if (bridgeSpec != null && pxPerMeter > 0f) {
        max(strokePx, bridgeSpec.deckWidthM * pxPerMeter)
    } else strokePx

    // 1. Dark Casing / True-Scale Bridge Deck Parapet Casing
    if (bridgeSpec != null) {
        val parapetPx = (bridgeSpec.parapetWidthM * pxPerMeter).coerceIn(1.8f, 10f)
        val deckTotalPx = effDeckWidthPx + parapetPx * 2f
        // Sub-shadow & concrete deck soffit
        drawPath(
            path = pathBuffer,
            color = Color(0xFF0A0F18).copy(alpha = 0.78f),
            style = Stroke(width = deckTotalPx + 3.2f, cap = StrokeCap.Butt, join = StrokeJoin.Round)
        )
        // Brushed-steel / concrete parapet edges
        drawPath(
            path = pathBuffer,
            color = if (isSelected) Color(0xFF80F3FF) else Color(0xFF90A4AE),
            style = Stroke(width = deckTotalPx, cap = StrokeCap.Butt, join = StrokeJoin.Round)
        )
        // Dark steel fascia inner rim
        drawPath(
            path = pathBuffer,
            color = Color(0xFF263238),
            style = Stroke(width = (effDeckWidthPx + 1.2f).coerceAtLeast(2.2f), cap = StrokeCap.Butt, join = StrokeJoin.Round)
        )
    } else {
        drawPath(
            path = pathBuffer,
            color = DarkBackground.copy(alpha = 0.84f),
            style = Stroke(width = strokePx + 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    // 2. Main Road / Bridge Deck Body
    val dashEffect = if (isUnpavedOrPath && !isBridge) PathEffect.dashPathEffect(floatArrayOf(9f, 6f), 0f) else null
    val deckBodyWidth = if (bridgeSpec != null) (effDeckWidthPx * 0.92f).coerceAtLeast(strokePx * 0.9f) else strokePx
    drawPath(
        path = pathBuffer,
        color = if (bridgeSpec != null && !isUnpavedOrPath) roadColor.copy(alpha = 0.95f) else roadColor,
        style = Stroke(
            width = deckBodyWidth,
            cap = if (bridgeSpec != null) StrokeCap.Butt else StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = dashEffect
        )
    )

    // 2b. Physical-World Bridge Structure: Longitudinal Steel Girders, Cross Diaphragms, Support Columns, Wing-Wall Abutments & Expansion Detents
    if (bridgeSpec != null && totalScreenLen >= 6f) {
        drawPhysicalScaleBridgePlan(
            pts = pts,
            totalScreenLen = totalScreenLen,
            spec = bridgeSpec,
            deckWidthPx = effDeckWidthPx,
            pxPerMeter = pxPerMeter,
            isSelected = isSelected
        )
    }

    if (!showSymbols) return

    // 3. Dashed Centerline for Multi-Lane / Arterial Roads when zoomed in enough
    if (!isUnpavedOrPath && (isTrunkPrimary || isSecondaryTertiary || lanes >= 2) && deckBodyWidth >= 3.2f) {
        drawPath(
            path = pathBuffer,
            color = if (isTrunkPrimary) Color(0xFF1B1F26) else Color(0xFF232730).copy(alpha = 0.72f),
            style = Stroke(
                width = (deckBodyWidth * 0.20f).coerceAtLeast(1.1f),
                cap = StrokeCap.Butt,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f), 0f)
            )
        )
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

    // 7. Route Highway Shield (`[C58]`, `[B19]`), Bridge Physical Dimensions, & Road Name Labels (Collision-free)
    if (!showTextLabels) return
    val cw = size.width
    val ch = size.height
    val anchor = pts[pts.size / 2]

    if (bridgeSpec != null && totalScreenLen >= 18f && (pxPerMeter >= 0.40f || isSelected)) {
        if (anchor.x in 24f..(cw - 24f) && anchor.y in 20f..(ch - 20f)) {
            val dimLabel = "BRG ${bridgeSpec.lengthM.roundToInt()}×${String.format(java.util.Locale.US, "%.1f", bridgeSpec.deckWidthM)}m • ${bridgeSpec.spanCount}SP"
            paints.labelTextPaint.textSize = 17.5f
            paints.labelHaloPaint.textSize = 17.5f
            val estW = paints.labelTextPaint.measureText(dimLabel)
            val lx = (anchor.x - estW * 0.5f).coerceIn(8f, (cw - estW - 8f).coerceAtLeast(8f))
            val ly = (anchor.y - effDeckWidthPx * 0.5f - 12f).coerceIn(22f, ch - 10f)
            val rect = RectF(lx - 4f, ly - 18f, lx + estW + 4f, ly + 4f)
            if (placedBoxes.none { RectF.intersects(it, rect) }) {
                placedBoxes.add(RectF(rect.left - 4f, rect.top - 4f, rect.right + 4f, rect.bottom + 4f))
                val prevC = paints.labelTextPaint.color
                paints.labelTextPaint.color = android.graphics.Color.parseColor("#80F8FF")
                drawContext.canvas.nativeCanvas.drawText(dimLabel, lx, ly, paints.labelHaloPaint)
                drawContext.canvas.nativeCanvas.drawText(dimLabel, lx, ly, paints.labelTextPaint)
                paints.labelTextPaint.color = prevC
            }
        }
    }

    if (totalScreenLen < 42f && ref.isBlank()) return
    if (anchor.x !in 32f..(cw - 32f) || anchor.y !in 32f..(ch - 32f)) return

    val shieldLabel = ref.trim()

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
        !name.contains('#') &&
        totalScreenLen >= 68f
    ) {
        val cleanName = name.take(20)
        paints.labelTextPaint.textSize = if (viewWidthM <= 450f) 21f else 18f
        paints.labelHaloPaint.textSize = paints.labelTextPaint.textSize
        val estW = paints.labelTextPaint.measureText(cleanName)
        val rect = RectF(anchor.x - estW * 0.5f, anchor.y - 20f, anchor.x + estW * 0.5f, anchor.y + 4f)
        if (placedBoxes.none { RectF.intersects(it, rect) }) {
            placedBoxes.add(RectF(rect.left - 8f, rect.top - 6f, rect.right + 8f, rect.bottom + 6f))
            drawContext.canvas.nativeCanvas.drawText(cleanName, rect.left, anchor.y - 4f, paints.labelHaloPaint)
            drawContext.canvas.nativeCanvas.drawText(cleanName, rect.left, anchor.y - 4f, paints.labelTextPaint)
        }
    }
}

private fun DrawScope.drawPhysicalScaleBridgePlan(
    pts: List<Offset>,
    totalScreenLen: Float,
    spec: PhysicalBridgeSpec,
    deckWidthPx: Float,
    pxPerMeter: Float,
    isSelected: Boolean
) {
    val halfW = deckWidthPx * 0.5f
    val parapetExtraPx = if (pxPerMeter > 0f) (spec.parapetWidthM * pxPerMeter).coerceIn(1.5f, 8f) else 2.2f
    val capHalfW = halfW + parapetExtraPx + if (pxPerMeter > 0f) (0.45f * pxPerMeter).coerceIn(1.5f, 6f) else 2.5f

    // 1. Longitudinal Steel I-Girder Flanges & Transverse Steel Diaphragm Bracing (when resolved in meters)
    if (halfW >= 4.0f) {
        val girderStroke = if (pxPerMeter > 0f) (0.35f * pxPerMeter).coerceIn(1.0f, 3.2f) else 1.2f
        val steelColor = if (isSelected) Color(0xFFB3E5FC) else Color(0xFF90A4AE).copy(alpha = 0.72f)
        for (g in 1..spec.girderCount) {
            val frac = (g.toFloat() / (spec.girderCount + 1)) * 2f - 1f // [-0.66..+0.66]
            val latPx = halfW * 0.84f * frac
            for (i in 0 until pts.size - 1) {
                val a = pts[i]
                val b = pts[i + 1]
                val dx = b.x - a.x
                val dy = b.y - a.y
                val dl = hypot(dx, dy)
                if (dl < 1f) continue
                val nx = -dy / dl
                val ny = dx / dl
                drawLine(
                    color = steelColor,
                    start = Offset(a.x + nx * latPx, a.y + ny * latPx),
                    end = Offset(b.x + nx * latPx, b.y + ny * latPx),
                    strokeWidth = girderStroke
                )
            }
        }

        // Transverse steel cross-diaphragm bracing spaced every ~7.5m along the span
        val diaphragmCount = (spec.lengthM / 7.5f).toInt().coerceIn(0, 48)
        if (diaphragmCount > 0 && totalScreenLen >= 28f) {
            val diagStroke = (girderStroke * 0.85f).coerceIn(0.8f, 2.2f)
            for (d in 1 until diaphragmCount) {
                val t = d.toFloat() / diaphragmCount
                val (pos, dir) = sampleScreenPolyline(pts, t)
                val nx = -dir.y
                val ny = dir.x
                drawLine(
                    color = Color(0xFF78909C).copy(alpha = 0.65f),
                    start = Offset(pos.x - nx * (halfW * 0.82f), pos.y - ny * (halfW * 0.82f)),
                    end = Offset(pos.x + nx * (halfW * 0.82f), pos.y + ny * (halfW * 0.82f)),
                    strokeWidth = diagStroke
                )
            }
        }
    }

    // 2. Intermediate Concrete Pier Caps, Columns, Bearings & Expansion Joint Detents
    val expJointStroke = if (pxPerMeter > 0f) (spec.expansionJointWidthM * pxPerMeter).coerceIn(1.4f, 4.5f) else 1.8f
    val colRadiusPx = if (pxPerMeter > 0f) (spec.columnDiameterM * 0.5f * pxPerMeter).coerceIn(2.0f, 9.0f) else 2.8f
    val pierCapStroke = if (pxPerMeter > 0f) (1.15f * pxPerMeter).coerceIn(2.4f, 10.0f) else 3.2f

    for (p in 1 until spec.spanCount) {
        val t = p.toFloat() / spec.spanCount
        val (pos, dir) = sampleScreenPolyline(pts, t)
        val nx = -dir.y
        val ny = dir.x

        // Transverse concrete pier cap beam extending past deck soffit
        drawLine(
            color = Color(0xFFCFD8DC).copy(alpha = 0.92f),
            start = Offset(pos.x - nx * capHalfW, pos.y - ny * capHalfW),
            end = Offset(pos.x + nx * capHalfW, pos.y + ny * capHalfW),
            strokeWidth = pierCapStroke,
            cap = StrokeCap.Butt
        )

        // Structural Columns along pier bent + Steel Pot Bearing seats
        for (c in 0 until spec.columnCountPerBent) {
            val cFrac = if (spec.columnCountPerBent == 1) 0f else (c.toFloat() / (spec.columnCountPerBent - 1)) * 2f - 1f
            val cx = pos.x + nx * (halfW * 0.56f * cFrac)
            val cy = pos.y + ny * (halfW * 0.56f * cFrac)
            drawCircle(Color(0xFF102027), radius = colRadiusPx + 1.2f, center = Offset(cx, cy))
            drawCircle(Color(0xFF00E5FF), radius = colRadiusPx, center = Offset(cx, cy))
        }

        // Steel finger-plate expansion joint detent across roadway deck
        drawLine(
            color = Color(0xFF1A232C),
            start = Offset(pos.x - nx * halfW, pos.y - ny * halfW),
            end = Offset(pos.x + nx * halfW, pos.y + ny * halfW),
            strokeWidth = expJointStroke
        )
    }

    // 3. End Concrete Abutment Seat Walls, Wing-Walls (35° Splay), and End Expansion Joint Detents
    val wingPx = if (pxPerMeter > 0f) (spec.wingWallLenM * pxPerMeter).coerceIn(5.0f, 36.0f) else (halfW * 0.85f + 4.5f)
    val abutStroke = if (pxPerMeter > 0f) (0.75f * pxPerMeter).coerceIn(2.0f, 7.0f) else 2.6f

    // Start abutment (outward dir = -tangent)
    val (pStart, dirStart) = sampleScreenPolyline(pts, 0f)
    drawScaledAbutmentAndWings(pStart, Offset(-dirStart.x, -dirStart.y), capHalfW, halfW, wingPx, abutStroke, expJointStroke)

    // End abutment (outward dir = +tangent)
    val (pEnd, dirEnd) = sampleScreenPolyline(pts, 1f)
    drawScaledAbutmentAndWings(pEnd, dirEnd, capHalfW, halfW, wingPx, abutStroke, expJointStroke)
}

private fun DrawScope.drawScaledAbutmentAndWings(
    endPt: Offset,
    outDir: Offset,
    capHalfW: Float,
    deckHalfW: Float,
    wingLenPx: Float,
    abutStroke: Float,
    expJointStroke: Float
) {
    val nx = -outDir.y
    val ny = outDir.x
    // Concrete abutment seat wall & backwall
    val leftWall = Offset(endPt.x + nx * capHalfW, endPt.y + ny * capHalfW)
    val rightWall = Offset(endPt.x - nx * capHalfW, endPt.y - ny * capHalfW)
    drawLine(Color(0xFF00E5FF), leftWall, rightWall, strokeWidth = abutStroke, cap = StrokeCap.Butt)

    // Splayed 35-degree concrete wing walls anchoring bridge into approach embankment
    val wingCos = 0.819f
    val wingSin = 0.574f
    val leftWingEnd = Offset(
        leftWall.x + (outDir.x * wingCos + nx * wingSin) * wingLenPx,
        leftWall.y + (outDir.y * wingCos + ny * wingSin) * wingLenPx
    )
    val rightWingEnd = Offset(
        rightWall.x + (outDir.x * wingCos - nx * wingSin) * wingLenPx,
        rightWall.y + (outDir.y * wingCos - ny * wingSin) * wingLenPx
    )
    drawLine(Color(0xFF00E5FF), leftWall, leftWingEnd, strokeWidth = (abutStroke * 0.88f).coerceAtLeast(1.8f), cap = StrokeCap.Round)
    drawLine(Color(0xFF00E5FF), rightWall, rightWingEnd, strokeWidth = (abutStroke * 0.88f).coerceAtLeast(1.8f), cap = StrokeCap.Round)

    // Steel modular expansion joint detent flush across carriageway entrance
    drawLine(
        color = Color(0xFF141C24),
        start = Offset(endPt.x + nx * deckHalfW, endPt.y + ny * deckHalfW),
        end = Offset(endPt.x - nx * deckHalfW, endPt.y - ny * deckHalfW),
        strokeWidth = expJointStroke
    )
}

fun DrawScope.drawRoadInfraNodeSymbol(
    center: Offset,
    category: String,
    paints: GisSymbologyPaints,
    showTextLabels: Boolean = true
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
            if (showTextLabels) {
                paints.shieldTextPaint.color = android.graphics.Color.WHITE
                paints.shieldTextPaint.textSize = 11f
                drawContext.canvas.nativeCanvas.drawText("B", center.x, center.y + 4f, paints.shieldTextPaint)
                paints.shieldTextPaint.textSize = 19f
            } else {
                drawCircle(Color.White, radius = 2.2f, center = center)
            }
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
