package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CppAssetType
import com.example.ui.theme.*

@Composable
fun CppSourceCodeTab(
    assetType: CppAssetType,
    renderingBackend: String = "Vulkan 1.3"
) {
    var selectedFileIndex by remember { mutableStateOf(0) }

    val fileList = listOf(
        "vulkan_engine.h",
        "vulkan_engine.cpp",
        "vulkan_asset_models.h",
        "vulkan_asset_models.cpp",
        "continuous_cable_cylinder.h",
        "continuous_cable_cylinder.cpp",
        "vulkan_power_grid.cpp",
        "catenary_cable_physics.h",
        "CMakeLists.txt"
    )

    val currentFileName = fileList.getOrElse(selectedFileIndex) { fileList.first() }

    val code = when (currentFileName) {
        "vulkan_engine.h" -> """
// vulkan_engine.h — Core Vulkan 1.3 Engine Pipeline
#pragma once
#include <vector>
#include <cstdint>

namespace custom_vulkan {
    struct EngineVertex {
        float pos[3];       // Position X, Y, Z
        float normal[3];    // Unit Normal Nx, Ny, Nz
        float uv[2];        // UV Coordinates
        float color[4];     // Color RGBA
    };

    struct EnginePushConstants {
        float mvpMatrix[16];
        float lightDir[4];
        float cableSag;
        uint32_t polygonMode;
    };
}
"""
        "vulkan_engine.cpp" -> """
// vulkan_engine.cpp — VK_KHR_dynamic_rendering Command Recording
#include "vulkan_engine.h"

void recordDynamicRenderingCommands(VkCommandBuffer cmd, VkRenderingInfoKHR renderingInfo) {
    vkCmdBeginRendering(cmd, &renderingInfo);
    vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline);
    vkCmdDrawIndexed(cmd, indexCount, 1, 0, 0, 0);
    vkCmdEndRendering(cmd);
}
"""
        "vulkan_asset_models.h" -> """
// vulkan_asset_models.h — Procedural Models Declarations
#pragma once
#include "vulkan_engine.h"

namespace custom_vulkan {
    GPUMeshBuffer buildGarage001Mesh(float bay1OpenPct, float bay2OpenPct, bool neonGlow);
    GPUMeshBuffer buildModularPostMesh(const std::string& type, bool transformer, bool streetlight, bool guyWire);
    GPUMeshBuffer buildPowerGridNetwork(int postCount, float cornerAngleDeg, float sagM, bool energized);
}
"""
        "vulkan_asset_models.cpp" -> """
// vulkan_asset_models.cpp — Procedural Architecture & Vehicle Geometry
#include "vulkan_asset_models.h"

GPUMeshBuffer custom_vulkan::buildGarage001Mesh(float bay1Open, float bay2Open, bool neon) {
    GPUMeshBuffer mesh;
    // Foundation slab floor
    addBox(mesh, {-7.0f, 0.0f, -11.0f}, {7.0f, 0.2f, 11.0f}, {0.22f, 0.26f, 0.29f, 1.0f});
    // Modular rollup door panels & interior workshop fixtures
    addModularRollupDoor(mesh, -7.0f, bay1Open, -4.0f);
    addModularRollupDoor(mesh, -7.0f, bay2Open,  4.0f);
    return mesh;
}
"""
        "continuous_cable_cylinder.h" -> """
// continuous_cable_cylinder.h — Swept 3D Cylinder Mesh Tubes
#pragma once
#include <vector>

namespace custom_vulkan {
    void sweepCylinderAlongCatenary(
        std::vector<EngineVertex>& outVerts,
        std::vector<uint32_t>& outIndices,
        const Vec3& p0, const Vec3& p1,
        float sag, float radius, int sides,
        const Vec4& color
    );
}
"""
        "continuous_cable_cylinder.cpp" -> """
// continuous_cable_cylinder.cpp — Cylindrical Ring Extrusions
#include "continuous_cable_cylinder.h"

void custom_vulkan::sweepCylinderAlongCatenary(...) {
    // Computes Frenet-Serret frame (T, N, B) along hyperbolic catenary trajectory
    // Generates N-sided regular polygonal vertex rings and quad index strips
}
"""
        "vulkan_power_grid.cpp" -> """
// vulkan_power_grid.cpp — Multi-Span Transmission Line Synthesizer
#include "vulkan_asset_models.h"
#include "continuous_cable_cylinder.h"

GPUMeshBuffer custom_vulkan::buildPowerGridNetwork(int postCount, float cornerDeg, float sag, bool energized) {
    // Generates connected distribution posts, turns line at corner post,
    // and extrudes 3-phase overhead ACSR conductors + guy wires
}
"""
        "catenary_cable_physics.h" -> """
// catenary_cable_physics.h — Analytical Hyperbolic Curve Solver
#pragma once

inline float catenarySag(float x, float span, float sag) {
    float a = span * span / (8.0f * (sag > 0.001f ? sag : 0.001f));
    float xRel = x - span * 0.5f;
    return a * (coshf(xRel / a) - 1.0f) - sag;
}
"""
        "CMakeLists.txt" -> """
cmake_minimum_required(VERSION 3.22.1)
project("apex_vulkan_procedural_studio")
set(CMAKE_CXX_STANDARD 20)
find_package(Vulkan REQUIRED)
add_library(procedural_vulkan SHARED
    vulkan_engine.cpp
    vulkan_asset_models.cpp
    continuous_cable_cylinder.cpp
    vulkan_power_grid.cpp
)
target_link_libraries(procedural_vulkan Vulkan::Vulkan log android)
"""
        else -> "// Source file: $currentFileName"
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // File tabs selector row
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(fileList.size) { idx ->
                val fileName = fileList[idx]
                val isSelected = idx == selectedFileIndex
                Surface(
                    color = if (isSelected) CyberCyan.copy(alpha = 0.22f) else DarkSurfaceElevated,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) CyberCyan else DarkSurfaceHighlight),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { selectedFileIndex = idx }
                ) {
                    Text(
                        text = fileName,
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) CyberCyan else TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF090D14), RoundedCornerShape(6.dp))
                .border(1.dp, DarkSurfaceHighlight, RoundedCornerShape(6.dp))
                .padding(8.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            Text(
                text = code.trimIndent(),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFC0D8F0),
                lineHeight = 14.sp
            )
        }
    }
}
