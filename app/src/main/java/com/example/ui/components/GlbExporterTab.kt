package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CppAssetType
import com.example.ui.theme.*

@Composable
fun GlbExporterTab(assetType: CppAssetType) {
    val script = when (assetType) {
        CppAssetType.GARAGE_001 -> """
# dev scripts/export_garage_glb.py
# Exports Garage 001 architectural model (.glb)
def export_garage_001():
    boxes = [
        ((-7.0, 0.0, -11.0), (7.0, 6.0, 11.0)),   # Main Depot Warehouse
        ((-7.5, 6.0, -11.5), (7.5, 6.4, 11.5)),   # Signature Red Roof Trim
        ((-7.1, 0.0, -6.5), (-6.9, 3.6, -1.5)),   # Service Bay 1 Rollup Door
        ((-7.1, 0.0,  1.5), (-6.9, 3.6,  6.5)),   # Service Bay 2 Rollup Door
        ((-7.15, 4.3, -5.0), (-6.95, 5.3, 5.0)),  # Neon Workshop Signboard
        ((-35.0, -0.05, -12.0), (-7.0, 0.05, 12.0)) # Paved Concrete Apron
    ]
    return create_glb(boxes, color_rgb=(0.28, 0.32, 0.38), roughness=0.55, metallic=0.25)
"""
        CppAssetType.TEST_VEHICLE_LORRY -> """
# dev scripts/generate_vehicle_glbs.py
# Generates the Heavy Box Lorry test vehicle
def generate_lorry(name, color, is_flatbed=False):
    boxes = []
    boxes.append(((-0.85, 0.25, -3.2), (0.85, 0.55, 3.0)))
    boxes.extend(get_wheels(1.9, 5.8, y=0.42, r=0.40))
    boxes.append(((-0.95, 0.45, 1.5), (0.95, 2.2, 3.1)))
    boxes.append(((-1.02, 0.55, -3.2), (1.02, 2.45, 1.35)))
    return create_glb(boxes, color_rgb=color)
"""
        CppAssetType.MODULAR_POWER_POST -> """
# dev scripts/export_modular_post_glb.py
# Exports modular utility post with pier, mast, crossarm, transformer and guy wire
def export_modular_post():
    boxes = [
        ((-0.32, 0.0, -0.32), (0.32, 0.45, 0.32)), # Foundation Pier
        ((-0.18, 0.2, -0.18), (0.18, 4.2, 0.18)),   # Timber Mast Base
        ((-0.15, 4.0, -0.15), (0.15, 7.5, 0.15)),   # Mast Mid
        ((-0.12, 7.5, -0.12), (0.12, 10.5, 0.12)),  # Mast Top
        ((-1.2, 9.74, -0.06), (1.2, 9.86, 0.06)),   # HV Crossarm
        ((-0.32, 6.02, 0.2), (0.32, 7.18, 0.85)),   # 50kVA Transformer
        ((-2.26, 0.05, -0.06), (-2.14, 1.85, 0.06)) # Guy Wire Guard Sleeve
    ]
    return create_glb(boxes, color_rgb=(0.42, 0.35, 0.28), roughness=0.75, metallic=0.15)
"""
        CppAssetType.POWER_GRID_NETWORK -> """
# dev scripts/export_power_grid_glb.py
# Exports power grid network with corner turn and catenary cable ribbons
def export_power_grid_network():
    return create_grid_glb(num_posts=4, span=16.0, corner_angle_deg=85.0, sag=0.55)
"""
        else -> """
# dev scripts/export_road_component_glb.py
# Exports Vulkan 1.3 modular road component: ${assetType.title}
def export_road_component():
    return create_road_component_glb(component_id="${assetType.id}")
"""
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "GLB 2.0 EXPORTER SCRIPT",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = CyberCyan
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF090D14), RoundedCornerShape(6.dp))
                .border(1.dp, DarkSurfaceHighlight, RoundedCornerShape(6.dp))
                .padding(8.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            Text(
                text = script.trimIndent(),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFA0E0A0),
                lineHeight = 14.sp
            )
        }
    }
}
