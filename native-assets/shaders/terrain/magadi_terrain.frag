// ─── native-assets/shaders/terrain/magadi_terrain.frag ────────────────────────────
// Vulkan 1.3 GLSL Fragment Shader - Magadi Road Contour Terrain
// Topographic Contour Isolines, Hypsometric Tinting, and PBR Road Lighting
// Reference: Sascha Willems Vulkan Infinite Procedural Terrain (SaschaWillems/Vulkan)
#version 450

layout (location = 0) in vec3 inWorldPos;
layout (location = 1) in vec3 inNormal;
layout (location = 2) in vec2 inUV;
layout (location = 3) in float inElevation;
layout (location = 4) in float inContourBand;
layout (location = 5) flat in uint inSurfaceType;

layout (push_constant) uniform PushConstants {
    mat4 mvpMatrix;
    vec4 lightDir;
    vec4 cameraPos;
    float contourInterval;
    float indexContourInterval;
    float stepTerracingFactor;
    uint showContourIsolines;
    uint polygonMode;
    uint activeChunkCol;
    uint activeChunkRow;
} pushConstants;

layout (location = 0) out vec4 outFragColor;

// Hypsometric elevation color mapping for the Great Rift Valley corridor
vec3 getHypsometricColor(float elev) {
    float t = clamp((elev - 590.0) / 1390.0, 0.0, 1.0);
    if (t < 0.12) {
        // Lake Magadi Trona soda flats: alkaline white & pale turquoise
        return mix(vec3(0.92, 0.96, 0.98), vec3(0.55, 0.85, 0.88), t / 0.12);
    } else if (t < 0.35) {
        // Semi-arid southern savannah: golden sand & acacia scrub
        return mix(vec3(0.85, 0.78, 0.52), vec3(0.72, 0.65, 0.38), (t - 0.12) / 0.23);
    } else if (t < 0.65) {
        // Olorgesailie & Tinga: volcanic terracotta & red silt
        return mix(vec3(0.72, 0.45, 0.32), vec3(0.55, 0.62, 0.35), (t - 0.35) / 0.30);
    } else if (t < 0.88) {
        // Corner Baridi foothills: highland green shrubs & dry forest
        return mix(vec3(0.35, 0.52, 0.28), vec3(0.25, 0.42, 0.22), (t - 0.65) / 0.23);
    } else {
        // Ngong Escarpment summit (1980m): craggy basaltic scarp rock
        return mix(vec3(0.42, 0.38, 0.36), vec3(0.28, 0.25, 0.24), (t - 0.88) / 0.12);
    }
}

void main() {
    vec3 N = normalize(inNormal);
    vec3 L = normalize(pushConstants.lightDir.xyz);
    float NdotL = max(dot(N, L), 0.15);
    float diffuse = 0.35 + 0.65 * NdotL;

    // 1. Base terrain material color
    vec3 baseColor = getHypsometricColor(inElevation);

    // Override for C58 Road Surface
    if (inSurfaceType == 1) {
        // C58 Asphalt bitumen
        baseColor = vec3(0.18, 0.19, 0.21);
    } else if (inSurfaceType == 2) {
        // Gravel shoulder
        baseColor = vec3(0.55, 0.52, 0.45);
    }

    vec3 finalColor = baseColor * diffuse;

    // 2. Anti-aliased 3D Topographic Contour Isolines
    // In Sascha Willems' terrain rendering, contours define the exact shape of elevation!
    if (pushConstants.showContourIsolines == 1 && inSurfaceType != 1) {
        float cInterval = max(pushConstants.contourInterval, 5.0);
        float idxInterval = max(pushConstants.indexContourInterval, cInterval);

        // Screen-space derivatives of elevation to keep contour lines constant pixel width
        float dElev = max(fwidth(inElevation), 0.001);

        // Intermediate contour lines (e.g. every 25m)
        float cCoord = inElevation / cInterval;
        float cDist = abs(fract(cCoord - 0.5) - 0.5) * cInterval;
        float cLine = clamp(1.0 - cDist / (dElev * 1.2), 0.0, 1.0);

        // Major Index contour lines (e.g. every 100m) - thicker & bolder
        float idxCoord = inElevation / idxInterval;
        float idxDist = abs(fract(idxCoord - 0.5) - 0.5) * idxInterval;
        float idxLine = clamp(1.0 - idxDist / (dElev * 2.2), 0.0, 1.0);

        // Blend intermediate contours (fine cyan/white)
        vec3 contourColor = vec3(0.95, 0.95, 0.98);
        finalColor = mix(finalColor, contourColor, cLine * 0.55);

        // Blend index contours (bold golden amber)
        vec3 indexContourColor = vec3(1.0, 0.78, 0.22);
        finalColor = mix(finalColor, indexContourColor, idxLine * 0.85);
    }

    outFragColor = vec4(finalColor, 1.0);
}
