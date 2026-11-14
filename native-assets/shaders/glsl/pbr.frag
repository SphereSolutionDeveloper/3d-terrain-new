// ==============================================================================
// Procedural Vulkan Asset Suite - Road & Infrastructure PBR Fragment Shader
// Path: native-assets/shaders/glsl/pbr.frag
// Architecture: Sascha Willems (SaschaWillems/Vulkan) & Khronos glTF 2.0 PBR
// ==============================================================================
#version 460

#include "include/pbr_common.glsl"

layout(location = 0) in vec3 inWorldPos;
layout(location = 1) in vec3 inNormal;
layout(location = 2) in vec2 inUV;
layout(location = 3) in vec4 inTangent;
layout(location = 4) in vec4 inColor;
layout(location = 5) in vec4 inPbrParams;   // x=roughness, y=metallic, z=ao, w=emissive
layout(location = 6) flat in uint inMaterialId;

layout(set = 0, binding = 0) uniform SceneUBO {
    mat4 projection;
    mat4 view;
    mat4 model;
    vec4 cameraPos;
    vec4 lightDirection;
    vec4 lightColor;
    float timeSeconds;
    float wetnessFrictionFactor;
    float tunnelLuminanceBoost;
    float padding;
} ubo;

layout(location = 0) out vec4 outFragColor;

void main() {
    vec3 N = normalize(inNormal);
    vec3 V = normalize(ubo.cameraPos.xyz - inWorldPos);
    vec3 L = normalize(-ubo.lightDirection.xyz);

    vec3 albedo = pow(max(inColor.rgb, vec3(0.0)), vec3(2.2));
    float roughness = clamp(inPbrParams.x, 0.05, 1.0);
    float metallic  = clamp(inPbrParams.y, 0.0, 1.0);
    float ao        = clamp(inPbrParams.z, 0.1, 1.0);
    float emissive  = max(inPbrParams.w, 0.0);

    // Wet asphalt specular modulation from environmental friction factor
    if (inMaterialId == 0u) {
        roughness = mix(0.22, roughness, clamp(ubo.wetnessFrictionFactor, 0.0, 1.0));
    }

    vec3 direct = EvaluatePBRDirectLight(N, V, L, albedo, roughness, metallic, ubo.lightColor.rgb * ubo.lightColor.w);

    // Hemisphere IBL ambient sky/ground irradiance approximation (Sascha Willems PBR ambient)
    float hemi = N.y * 0.5 + 0.5;
    vec3 skyColor = vec3(0.45, 0.58, 0.78);
    vec3 groundColor = vec3(0.16, 0.15, 0.14);
    vec3 ambient = mix(groundColor, skyColor, hemi) * albedo * (1.0 - metallic * 0.55) * 0.35 * ao;

    // Emissive contribution (warning beacons, VMS gantries, tunnel LED guides)
    vec3 emissiveTerm = albedo * emissive * (1.0 + ubo.tunnelLuminanceBoost * 0.15);

    vec3 colorLinear = direct + ambient + emissiveTerm;
    vec3 colorMapped = TonemapACES(colorLinear);
    vec3 srgb = pow(colorMapped, vec3(1.0 / 2.2));

    outFragColor = vec4(srgb, inColor.a);
}
