// ==============================================================================
// MUTCD ASTM D4956 Type XI Microprismatic Retroreflective Sign Shader (.frag)
// Path: src/road/signage/shaders/retroreflective_sign.frag
// ==============================================================================
#version 460
#extension GL_ARB_separate_shader_objects : enable

#include "../../shaders/include/road_common.glsl"

layout(location = 0) in vec3 inWorldPos;
layout(location = 1) in vec3 inWorldNormal;
layout(location = 2) in vec2 inUV;
layout(location = 3) in vec4 inColor;

layout(set = 0, binding = 0) uniform SignageUBO {
    vec4 cameraPos;
    vec4 headlightOrigin;
    vec4 headlightDirCosHalfCone;
    vec4 sunDirIntensity;
} ubo;

layout(set = 0, binding = 1) uniform sampler2D texSignAtlas;

layout(location = 0) out vec4 outFragColor;

void main() {
    vec3 N = normalize(inWorldNormal);
    vec3 V = normalize(ubo.cameraPos.xyz - inWorldPos);
    vec3 Lsun = normalize(ubo.sunDirIntensity.xyz);

    vec4 signTex = texture(texSignAtlas, inUV);
    vec3 albedo = mix(inColor.rgb, signTex.rgb * inColor.rgb * 1.1, signTex.a);

    // Direct solar lighting
    vec3 lit = evaluateRoadPBR(N, V, Lsun, albedo, 0.28, 0.08, vec3(ubo.sunDirIntensity.w));

    // ASTM Type XI microprismatic retroreflection: peaks sharply when observation vector V ~ illumination vector Lhead
    vec3 toLight = ubo.headlightOrigin.xyz - inWorldPos;
    float dist = max(length(toLight), 1.0);
    vec3 Lhead = toLight / dist;
    float spotAlign = dot(-Lhead, normalize(ubo.headlightDirCosHalfCone.xyz));
    float beamMask = smoothstep(ubo.headlightDirCosHalfCone.w - 0.08, ubo.headlightDirCosHalfCone.w + 0.04, spotAlign);

    float obsAngleCos = max(dot(V, Lhead), 0.0);
    float prismCells = 0.85 + 0.15 * sin(inUV.x * 320.0) * sin(inUV.y * 320.0);
    float retroGain = pow(obsAngleCos, 24.0) * max(dot(N, Lhead), 0.0) * beamMask * (48.0 / (dist * dist + 12.0));

    vec3 hdr = lit + albedo * (0.22 + retroGain * prismCells * 6.5);
    vec3 ldr = hdr / (hdr + vec3(1.0));
    outFragColor = vec4(pow(ldr, vec3(1.0 / 2.2)), inColor.a);
}
