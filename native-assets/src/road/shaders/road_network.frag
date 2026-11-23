// ==============================================================================
// Road Network System - Vulkan 1.3 Fragment Shader (GGX PBR + Wet Puddle + LED)
// Path: src/road/shaders/road_network.frag
// ==============================================================================
#version 460
#extension GL_ARB_separate_shader_objects : enable

#include "include/road_common.glsl"

layout(location = 0) in vec3 inWorldPos;
layout(location = 1) in vec3 inNormal;
layout(location = 2) in vec2 inUV;
layout(location = 3) in vec4 inTangent;
layout(location = 4) in vec4 inColor;
layout(location = 5) in vec4 inPbrParams;   // x=roughness, y=metallic, z=ao, w=emissive
layout(location = 6) flat in uint inMaterialId;

layout(set = 0, binding = 0) uniform RoadSceneUBO {
    mat4 projection;
    mat4 view;
    mat4 model;
    vec4 cameraPos;
    vec4 lightDirection;
    vec4 lightColor;
    float timeSeconds;
    float wetnessFactor;
    float tunnelLuminanceBoost;
    float sunElevation;
} ubo;

layout(set = 1, binding = 0) uniform sampler2D roadAlbedoMap;
layout(set = 1, binding = 1) uniform sampler2D roadNormalMap;
layout(set = 1, binding = 2) uniform sampler2D roadOrmMap;

layout(location = 0) out vec4 outFragColor;

void main() {
    vec3 N = normalize(inNormal);
    vec3 T = normalize(inTangent.xyz);
    vec3 B = cross(N, T) * inTangent.w;

    // Sample procedural compute textures scaled in world meters
    vec2 worldUv = inWorldPos.xz * 0.25;
    vec3 texNormal = texture(roadNormalMap, worldUv).xyz * 2.0 - 1.0;
    vec3 ormSample = texture(roadOrmMap, worldUv).rgb;
    vec3 detailTint = texture(roadAlbedoMap, worldUv).rgb;

    vec3 worldN = normalize(T * texNormal.x + B * texNormal.y + N * max(texNormal.z, 0.35));
    vec3 V = normalize(ubo.cameraPos.xyz - inWorldPos);
    vec3 L = normalize(-ubo.lightDirection.xyz);

    vec3 baseAlbedo = pow(max(inColor.rgb, vec3(0.001)), vec3(2.2));
    float roughness = clamp(mix(inPbrParams.x, ormSample.g, 0.45), 0.06, 0.98);
    float metallic  = clamp(mix(inPbrParams.y, ormSample.b, 0.35), 0.0, 1.0);
    float ao        = clamp(inPbrParams.z * ormSample.r, 0.15, 1.0);
    float emissive  = max(inPbrParams.w, 0.0);

    if (inMaterialId == MAT_ROAD_ASPHALT) {
        baseAlbedo *= mix(vec3(1.0), detailTint * 1.45, 0.38);
        // Puddle specular smoothout driven by wetnessFactor & FBM micro-basins
        float puddleNoise = roadFbm(inWorldPos.xz * 0.14);
        float puddleMask = smoothstep(0.55 - ubo.wetnessFactor * 0.35, 0.72, puddleNoise) * ubo.wetnessFactor;
        roughness = mix(roughness, 0.09, puddleMask);
        baseAlbedo *= mix(1.0, 0.62, puddleMask);
    } else if (inMaterialId == MAT_ROAD_MARKINGS) {
        // Glass bead retroreflectivity boosts return toward headlights/camera
        float retro = pow(max(dot(V, L), 0.0), 4.0) * 0.65;
        emissive += retro;
    } else if (inMaterialId == MAT_ROAD_STEEL) {
        metallic = max(metallic, 0.80);
        roughness = min(roughness, 0.36);
    }

    vec3 lightRadiance = ubo.lightColor.rgb * max(ubo.lightColor.w, 1.0);
    vec3 direct = evaluateRoadPBR(worldN, V, L, baseAlbedo, roughness, metallic, lightRadiance);

    // Split sky/ground IBL ambient
    float hemi = worldN.y * 0.5 + 0.5;
    vec3 skyCol = vec3(0.46, 0.60, 0.82);
    vec3 groundCol = vec3(0.14, 0.13, 0.12);
    vec3 ambient = mix(groundCol, skyCol, hemi) * baseAlbedo * (1.0 - metallic * 0.5) * 0.38 * ao;

    // Animated traffic beacons & VMS glow
    float pulse = 0.85 + 0.15 * sin(ubo.timeSeconds * 4.5);
    vec3 emissiveColor = baseAlbedo * emissive * pulse * (1.0 + ubo.tunnelLuminanceBoost * 0.25);

    vec3 linearOut = direct + ambient + emissiveColor;
    // Narkowicz ACES tonemap
    vec3 mapped = clamp((linearOut * (2.51 * linearOut + 0.03)) / (linearOut * (2.43 * linearOut + 0.59) + 0.14), 0.0, 1.0);
    outFragColor = vec4(pow(mapped, vec3(1.0 / 2.2)), inColor.a);
}
