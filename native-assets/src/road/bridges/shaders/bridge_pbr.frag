// ==============================================================================
// Modular Bridge GGX PBR Fragment Shader (Weathering Steel / Concrete / Cables)
// Path: src/road/bridges/shaders/bridge_pbr.frag
// ==============================================================================
#version 460
#extension GL_ARB_separate_shader_objects : enable

#include "../../shaders/include/road_common.glsl"

layout(location = 0) in vec3 inWorldPos;
layout(location = 1) in vec3 inWorldNormal;
layout(location = 2) in vec2 inUV;
layout(location = 3) in vec4 inColor;

layout(set = 0, binding = 0) uniform BridgeSceneUBO {
    mat4 viewProj;
    vec4 cameraPos;
    vec4 sunDirIntensity;
} ubo;

layout(set = 0, binding = 1) uniform sampler2D texSteelDeck;
layout(set = 0, binding = 2) uniform sampler2D texConcretePier;

layout(push_constant) uniform BridgePushConstants {
    mat4 model;
    float spanLengthM;
    float midspanDeflectionM;
    float cableVibrationTimeS;
    float weatheringPatina;
} pc;

layout(location = 0) out vec4 outFragColor;

void main() {
    vec3 N = normalize(inWorldNormal);
    vec3 V = normalize(ubo.cameraPos.xyz - inWorldPos);
    vec3 L = normalize(ubo.sunDirIntensity.xyz);

    vec3 steelSample = texture(texSteelDeck, inUV * 4.0).rgb;
    vec3 concSample = texture(texConcretePier, inUV * 3.0).rgb;

    // Classify steel girder/cable vs precast concrete pylon/pier via chroma + height
    float chroma = max(max(inColor.r, inColor.g), inColor.b) - min(min(inColor.r, inColor.g), inColor.b);
    float steelMask = clamp(smoothstep(0.06, 0.25, chroma) + smoothstep(0.65, 0.90, length(inColor.rgb)) * 0.25, 0.0, 1.0);

    vec3 albedo = mix(concSample * inColor.rgb * 1.15, steelSample * inColor.rgb * 1.10, steelMask);
    // Corten/weathering patina tint along undersides & splash zones
    float rustPatina = roadFbm(inWorldPos.xz * 0.35) * clamp(pc.weatheringPatina, 0.0, 1.0) * steelMask;
    albedo = mix(albedo, vec3(0.44, 0.22, 0.12), rustPatina * 0.45);

    float roughness = mix(0.84, 0.32, steelMask);
    float metallic = mix(0.04, 0.85, steelMask);

    vec3 direct = evaluateRoadPBR(N, V, L, albedo, roughness, metallic, vec3(ubo.sunDirIntensity.w));
    vec3 ambient = albedo * mix(vec3(0.14, 0.18, 0.24), vec3(0.30, 0.36, 0.44), N.y * 0.5 + 0.5) * 0.45;
    vec3 hdr = direct + ambient;
    vec3 ldr = hdr / (hdr + vec3(1.0));
    outFragColor = vec4(pow(ldr, vec3(1.0 / 2.2)), inColor.a);
}
