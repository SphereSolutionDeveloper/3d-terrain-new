// ─── native-assets/shaders/terrain/magadi_terrain.vert ────────────────────────────
// Vulkan 1.3 GLSL Vertex Shader - Magadi Road Contour Terrain
// Reference: Sascha Willems Vulkan Infinite Procedural Terrain (SaschaWillems/Vulkan)
#version 450

layout (location = 0) in vec3 inPos;
layout (location = 1) in vec3 inNormal;
layout (location = 2) in vec2 inUV;
layout (location = 3) in float inRawElevation;
layout (location = 4) in float inContourBand;
layout (location = 5) in uint inSurfaceType;

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

layout (location = 0) out vec3 outWorldPos;
layout (location = 1) out vec3 outNormal;
layout (location = 2) out vec2 outUV;
layout (location = 3) out float outElevation;
layout (location = 4) out float outContourBand;
layout (location = 5) flat out uint outSurfaceType;

void main() {
    outWorldPos = inPos;
    outNormal = inNormal;
    outUV = inUV;
    outElevation = inRawElevation;
    outContourBand = inContourBand;
    outSurfaceType = inSurfaceType;

    gl_Position = pushConstants.mvpMatrix * vec4(inPos, 1.0);
}
