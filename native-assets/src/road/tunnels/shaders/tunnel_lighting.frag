// ==============================================================================
// Subterranean Tunnel Portal Transition & Sodium-Vapor / LED Fixture Shader (.frag)
// Path: src/road/tunnels/shaders/tunnel_lighting.frag
// ==============================================================================
#version 460
#extension GL_ARB_separate_shader_objects : enable

#include "../../shaders/include/road_common.glsl"

layout(location = 0) in vec3 inWorldPos;
layout(location = 1) in vec3 inWorldNormal;
layout(location = 2) in vec2 inUV;
layout(location = 3) in vec4 inColor;

layout(set = 0, binding = 0) uniform TunnelUBO {
    vec4 cameraPos;
    float portalZ;
    float fixtureSpacingM;
    float transitionBoost;
    float timeSec;
} ubo;

layout(set = 0, binding = 1) uniform sampler2D texTunnelLining;

layout(location = 0) out vec4 outFragColor;

void main() {
    vec3 N = normalize(inWorldNormal);
    vec3 V = normalize(ubo.cameraPos.xyz - inWorldPos);
    vec3 liningTex = texture(texTunnelLining, inUV * vec2(2.0, 6.0)).rgb;

    // Periodic ceiling luminaire banks along bore axis (Z)
    float spacing = max(ubo.fixtureSpacingM, 6.0);
    float zCell = abs(fract(inWorldPos.z / spacing) - 0.5) * spacing;
    float isLuminaire = step(inColor.r + inColor.g, 1.98) < 0.5 ? 1.0 : 0.0;
    float fixtureFalloff = exp(-zCell * zCell * 0.12);

    // CIE 88 portal threshold luminance zone boost near entry portal
    float distIntoPortal = abs(inWorldPos.z - ubo.portalZ);
    float portalBlend = exp(-distIntoPortal * 0.045) * max(ubo.transitionBoost, 1.0);

    vec3 lightPos = vec3(0.0, max(inWorldPos.y + 1.5, 5.5), round(inWorldPos.z / spacing) * spacing);
    vec3 L = normalize(lightPos - inWorldPos);
    vec3 fixtureColor = mix(vec3(1.0, 0.88, 0.72), vec3(0.95, 0.98, 1.0), clamp(portalBlend, 0.0, 1.0));

    vec3 albedo = inColor.rgb * liningTex;
    vec3 direct = evaluateRoadPBR(N, V, L, albedo, 0.74, 0.05, fixtureColor * (2.4 + 2.0 * fixtureFalloff + portalBlend));
    vec3 emissive = (inColor.r > 0.92 && inColor.g > 0.72) ? inColor.rgb * (2.5 + 0.35 * sin(ubo.timeSec * 3.0)) : vec3(0.0);

    vec3 hdr = direct + albedo * 0.14 + emissive + isLuminaire * 0.25;
    vec3 ldr = hdr / (hdr + vec3(1.0));
    outFragColor = vec4(pow(ldr, vec3(1.0 / 2.2)), inColor.a);
}
