// ==============================================================================
// Modular Bridge Vertex Shader with Thermal Expansion & Live-Load Deflection
// Path: src/road/bridges/shaders/bridge_pbr.vert
// ==============================================================================
#version 460
#extension GL_ARB_separate_shader_objects : enable

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec3 inNormal;
layout(location = 2) in vec2 inUV;
layout(location = 3) in vec4 inColor;

layout(set = 0, binding = 0) uniform BridgeSceneUBO {
    mat4 viewProj;
    vec4 cameraPos;
    vec4 sunDirIntensity;
} ubo;

layout(push_constant) uniform BridgePushConstants {
    mat4 model;
    float spanLengthM;
    float midspanDeflectionM;
    float cableVibrationTimeS;
    float weatheringPatina;
} pc;

layout(location = 0) out vec3 outWorldPos;
layout(location = 1) out vec3 outWorldNormal;
layout(location = 2) out vec2 outUV;
layout(location = 3) out vec4 outColor;

void main() {
    vec3 localPos = inPosition;

    // Parabolic AASHTO HL-93 live-load elastic sag across main span
    float halfSpan = max(pc.spanLengthM * 0.5, 1.0);
    float normZ = clamp(localPos.z / halfSpan, -1.0, 1.0);
    float sagProfile = max(0.0, 1.0 - normZ * normZ);
    localPos.y -= pc.midspanDeflectionM * sagProfile;

    // Subtle wind-induced vortex harmonic on high tensile stay cables (y > 8m)
    float cableBandMask = smoothstep(8.0, 22.0, localPos.y);
    localPos.x += sin(pc.cableVibrationTimeS * 3.8 + localPos.z * 0.14) * 0.015 * cableBandMask;

    vec4 worldPos = pc.model * vec4(localPos, 1.0);
    mat3 normalMat = transpose(inverse(mat3(pc.model)));

    outWorldPos = worldPos.xyz;
    outWorldNormal = normalize(normalMat * inNormal);
    outUV = inUV;
    outColor = inColor;
    gl_Position = ubo.viewProj * worldPos;
}
