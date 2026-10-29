package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ProceduralModelMesh
import com.example.ui.theme.*
import kotlin.math.*

@Composable
fun CablePhysicsCylinderTab(
    mesh: ProceduralModelMesh,
    cableGeometryMode: String,
    cableCylinderSides: Int,
    cableConductorRadiusMm: Float,
    cableTensionKn: Float,
    cableLinearMassKgM: Float,
    gridWireSagM: Float,
    gridSpanDistanceM: Float = 16.0f,
    onCableGeometryModeChange: (String) -> Unit,
    onCableCylinderSidesChange: (Int) -> Unit,
    onCableConductorRadiusMmChange: (Float) -> Unit,
    onCableTensionKnChange: (Float) -> Unit,
    onCableLinearMassKgMChange: (Float) -> Unit
) {
    val isCylinder = cableGeometryMode == "CYLINDER"

    // --- Exact Physics Equations Evaluation ---
    val g = 9.80665f // Standard gravitational acceleration (m/s^2)
    val w = cableLinearMassKgM // Conductor linear mass density (kg/m)
    val tensionN = cableTensionKn * 1000.0f // Horizontal tension T_H (N)
    val spanL = gridSpanDistanceM.coerceAtLeast(1.0f) // Span distance L (m)

    // Catenary Parameter a = T_H / (w * g) (in meters)
    val catenaryA = tensionN / (w * g)

    // Half-span ratio u_half = L / (2 * a)
    val halfSpanRatio = (spanL / (2.0f * catenaryA)).toDouble()

    // Exact Mid-span Maximum Sag: S = a * [cosh(L / 2a) - 1]
    val exactSagM = (catenaryA * (cosh(halfSpanRatio) - 1.0)).toFloat()

    // Parabolic Approximation Sag: S_approx = (w * g * L^2) / (8 * T_H) = L^2 / (8 * a)
    val parabolicSagM = (spanL * spanL) / (8.0f * catenaryA)

    // Conductor Arc Length: s = 2 * a * sinh(L / 2a)
    val arcLengthM = (2.0f * catenaryA * sinh(halfSpanRatio)).toFloat()

    // Maximum Tangent Angle at Insulator Clamp: theta_max = atan(sinh(L / 2a))
    val maxSlopeRad = atan(sinh(halfSpanRatio)).toFloat()
    val maxSlopeDeg = Math.toDegrees(maxSlopeRad.toDouble()).toFloat()

    // Maximum Conductor Tension at Clamps: T_max = T_H + w * g * S = T_H * cosh(L / 2a)
    val maxTensionN = tensionN * cosh(halfSpanRatio).toFloat()
    val maxTensionKn = maxTensionN / 1000.0f

    // Conductor Geometry & Polygonal Triangles Budget
    val radiusM = cableConductorRadiusMm / 1000.0f
    val crossSectionAreaMm2 = PI * cableConductorRadiusMm * cableConductorRadiusMm
    val segmentsPerSpan = 10
    val totalSpans = if (mesh.wires.isNotEmpty()) mesh.wires.size / 4 else 3
    val totalConductors = mesh.wires.size.coerceAtLeast(12)

    val trianglesPerConductor = if (isCylinder) {
        segmentsPerSpan * cableCylinderSides * 2
    } else {
        segmentsPerSpan * 12
    }
    val verticesPerConductor = if (isCylinder) {
        (segmentsPerSpan + 1) * cableCylinderSides
    } else {
        segmentsPerSpan * 8
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. Header Banner
        Surface(
            color = CyberCyan.copy(alpha = 0.12f),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CATENARY CABLE PHYSICS & 3D CYLINDER SWEPT TUBE",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                    Surface(
                        color = if (isCylinder) NeonLime.copy(alpha = 0.2f) else LaserOrange.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isCylinder) NeonLime else LaserOrange)
                    ) {
                        Text(
                            text = if (isCylinder) "CYLINDER TUBE ACTIVE" else "BOX CHORD ACTIVE",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (isCylinder) NeonLime else LaserOrange,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Engineering analysis and real-time procedural geometry synthesis: replacing coarse 4-sided box chords with true circular 3D cylinder swept tubes curved along the exact hyperbolic cosine equilibrium curve.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )
            }
        }

        // 2. Direct Answers Card (Answering the user's specific questions)
        Surface(
            color = DarkSurfaceElevated,
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "ARCHITECTURAL OVERVIEW & ANSWERS",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                // Answer 1: Can we use cylinder instead of box?
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(NeonLime.copy(alpha = 0.2f), CircleShape)
                            .border(1.dp, NeonLime, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("1", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonLime)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Can we use a cylinder instead of a box for cables?",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonLime
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "YES! A box is an axis-aligned/oriented 6-sided cuboid with sharp 90° edges (12 triangles per segment). In electrical transmission reality, conductors (e.g., ACSR Drake, Cardinal) are circular cylindrical tubes. Using an extruded cylinder with 6, 8, or 12 radial facets creates a continuous round tubular swept mesh, provides realistic Lambertian radial shading highlights, and prevents flat-edge rendering artifacts.",
                            fontSize = 10.5.sp,
                            color = TextSecondary,
                            lineHeight = 14.5.sp
                        )
                    }
                }

                Divider(color = DarkSurfaceHighlight)

                // Answer 2: Where does the cylinder need to curve with physics equations?
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(CyberCyan.copy(alpha = 0.2f), CircleShape)
                            .border(1.dp, CyberCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("2", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Where does the cylinder need to be curved with physics equations?",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            PhysicsLocationBullet(
                                label = "Span Sagitta (Free-Hanging Overhead Spans):",
                                desc = "Between the crossarm insulator suspension clamps across each span (L = 16m). The conductor hangs freely in 3D air under gravitational self-weight w (kg/m) and horizontal tension T_H (kN). Curvature is purely vertical along the hyperbolic cosine catenary curve y(u) = a[cosh(L/2a) - cosh(u/a)]."
                            )
                            PhysicsLocationBullet(
                                label = "Strain Guy-Wires & Stay Ground Anchors:",
                                desc = "Straight tubular tension stays running from mast attachment down to concrete ground anchor rods. Curved tension load vectors are calculated along the corner turn angle bisector to counteract 3-phase line pull."
                            )
                            PhysicsLocationBullet(
                                label = "Slack Jumpers / Bypass Loops:",
                                desc = "At corner and strain posts, slack conductor loops curve in a deep catenary arc between dead-end strain clamps to maintain 3-phase electrical continuity without carrying mechanical line tension."
                            )
                        }
                    }
                }
            }
        }

        // 3. Geometry Mode Selector (Cylinder vs Box)
        Column {
            Text(
                text = "Conductor Geometry Synthesis Mode:",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "CYLINDER" to "3D CYLINDER SWEPT TUBE",
                    "BOX" to "RECTANGULAR BOX CHORD"
                ).forEach { (mode, label) ->
                    val isSel = cableGeometryMode == mode
                    Surface(
                        color = if (isSel) CyberCyan.copy(alpha = 0.22f) else DarkSurfaceElevated,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) CyberCyan else DarkSurfaceHighlight),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onCableGeometryModeChange(mode) }
                            .testTag("btn_cable_geom_${mode.lowercase()}")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) CyberCyan else TextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (mode == "CYLINDER") "Round • $cableCylinderSides facets • PBR tubular" else "4-sided cuboid • Coarse chord",
                                fontSize = 8.5.sp,
                                color = if (isSel) TextPrimary else TextSecondary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // 4. Cylinder Radial Facets (when in Cylinder Mode)
        if (isCylinder) {
            Column {
                Text(
                    text = "Cylinder Radial Facet Count (N-Sides):",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        6 to "6 SIDES (Hex)",
                        8 to "8 SIDES (Octagonal)",
                        12 to "12 SIDES (Smooth)"
                    ).forEach { (sides, label) ->
                        val isSel = cableCylinderSides == sides
                        Surface(
                            color = if (isSel) NeonLime.copy(alpha = 0.22f) else DarkSurfaceElevated,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) NeonLime else DarkSurfaceHighlight),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onCableCylinderSidesChange(sides) }
                                .testTag("btn_sides_$sides")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 7.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 9.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) NeonLime else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Conductor Radius Slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Conductor Outer Radius (R):", fontSize = 11.sp, color = TextSecondary)
                Text(
                    "${cableConductorRadiusMm.toInt()} mm (Ø ${(cableConductorRadiusMm * 2).toInt()} mm)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan
                )
            }
            Slider(
                value = cableConductorRadiusMm,
                onValueChange = onCableConductorRadiusMmChange,
                valueRange = 10.0f..60.0f,
                colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan),
                modifier = Modifier.fillMaxWidth().testTag("slider_cable_radius")
            )
            Text(
                text = "Standard ACSR Drake (795 kcmil) = 28.1mm dia • ACSR Cardinal (954 kcmil) = 30.4mm dia",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary.copy(alpha = 0.8f)
            )
        }

        // 6. Horizontal Mechanical Tension (T_H) Slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Horizontal Conductor Tension (T_H):", fontSize = 11.sp, color = TextSecondary)
                Text("%.1f kN (%.0f N)".format(cableTensionKn, tensionN), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LaserOrange)
            }
            Slider(
                value = cableTensionKn,
                onValueChange = onCableTensionKnChange,
                valueRange = 5.0f..40.0f,
                colors = SliderDefaults.colors(thumbColor = LaserOrange, activeTrackColor = LaserOrange),
                modifier = Modifier.fillMaxWidth().testTag("slider_cable_tension")
            )
            Text(
                text = "Higher tension reduces vertical sag but increases mechanical clamp stress and guy-wire anchoring load.",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary.copy(alpha = 0.8f)
            )
        }

        // 7. Linear Mass Density (w) Slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Conductor Unit Mass Density (w):", fontSize = 11.sp, color = TextSecondary)
                Text("%.2f kg/m".format(cableLinearMassKgM), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFEE58))
            }
            Slider(
                value = cableLinearMassKgM,
                onValueChange = onCableLinearMassKgMChange,
                valueRange = 0.40f..3.50f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFFFFEE58), activeTrackColor = Color(0xFFFFEE58)),
                modifier = Modifier.fillMaxWidth().testTag("slider_cable_mass")
            )
        }

        // 8. Live Physics Metrics Grid
        Surface(
            color = DarkSurfaceElevated,
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "LIVE PHYSICAL EQUILIBRIUM PARAMETERS (L = ${spanL.toInt()}m SPAN)",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(
                        title = "CATENARY CONST a",
                        value = "%.1f m".format(catenaryA),
                        subtitle = "T_H / (w · g)",
                        color = NeonLime,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "MAX MID-SPAN SAG",
                        value = "%.3f m".format(exactSagM),
                        subtitle = "Exact Cosh Sag",
                        color = CyberCyan,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(
                        title = "ARC LENGTH (s)",
                        value = "%.3f m".format(arcLengthM),
                        subtitle = "+%.3fm slack".format(arcLengthM - spanL),
                        color = LaserOrange,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "CLAMP ANGLE θ",
                        value = "%.2f°".format(maxSlopeDeg),
                        subtitle = "atan(sinh(L/2a))",
                        color = Color(0xFFFFEE58),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(
                        title = "MAX TENSION T_max",
                        value = "%.2f kN".format(maxTensionKn),
                        subtitle = "At Insulator Clamps",
                        color = Color(0xFFFF5252),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "PARABOLIC ERROR",
                        value = "%.4f%%".format(abs(exactSagM - parabolicSagM) / exactSagM * 100.0f),
                        subtitle = "|S_exact - S_para|",
                        color = TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(
                        title = "TRIANGLES / SPAN",
                        value = "$trianglesPerConductor TRIS",
                        subtitle = if (isCylinder) "$cableCylinderSides radial facets" else "12 tris/box",
                        color = CyberCyan,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "TOTAL NETWORK WIRES",
                        value = "${trianglesPerConductor * totalConductors} TRIS",
                        subtitle = "$totalConductors conductors total",
                        color = NeonLime,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 9. Exact Physics Equations Formulation Box
        Surface(
            color = Color(0xFF090D14),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "GOVERNING PHYSICS EQUATIONS & MATHEMATICAL PROOFS",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan
                )

                EquationRow(
                    equation = "d²y/dx² = (w · g / T_H) · √(1 + (dy/dx)²)",
                    explanation = "Differential equation of flexible, inelastic conductor suspended under uniform gravity g."
                )
                EquationRow(
                    equation = "y(u) = a · [cosh(L / 2a) - cosh(u / a)]",
                    explanation = "Exact catenary solution along horizontal coordinate u ∈ [-L/2, +L/2] with origin at mid-span."
                )
                EquationRow(
                    equation = "a = T_H / (w · g) = %.1f m".format(catenaryA),
                    explanation = "Catenary scaling constant representing radius of curvature at the lowest mid-span point."
                )
                EquationRow(
                    equation = "S_exact = a · [cosh(L / 2a) - 1] = %.4f m".format(exactSagM),
                    explanation = "Maximum mid-span vertical sagitta drop."
                )
                EquationRow(
                    equation = "S_parabolic ≈ (w · g · L²) / (8 · T_H) = %.4f m".format(parabolicSagM),
                    explanation = "2nd-order Taylor expansion approximation (cosh(u) ≈ 1 + u²/2). Valid when sag << span."
                )
                EquationRow(
                    equation = "s = 2a · sinh(L / 2a) = %.4f m".format(arcLengthM),
                    explanation = "Actual physical conductor arc length between the two support clamps."
                )
                EquationRow(
                    equation = "T_max = T_H · cosh(L / 2a) = T_H + w·g·S = %.3f kN".format(maxTensionKn),
                    explanation = "Maximum tension occurring at suspension insulator clamps, including weight of hanging sag."
                )
                EquationRow(
                    equation = "V_{k,j} = P_k + R · [cos(2πj/M) · R_k + sin(2πj/M) · N_k]",
                    explanation = "Frenet-Serret / Parallel Transport circular tubular cylinder ring vertices at catenary node P_k."
                )
            }
        }
    }
}

@Composable
private fun PhysicsLocationBullet(label: String, desc: String) {
    Column {
        Text(text = "• $label", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(text = desc, fontSize = 10.sp, color = TextSecondary, lineHeight = 14.sp)
    }
}

@Composable
private fun EquationRow(equation: String, explanation: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Text(
                text = equation,
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = NeonLime
            )
        }
        Text(
            text = explanation,
            fontSize = 9.sp,
            color = TextSecondary,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )
    }
}
