// ==============================================================================
// Toll Plaza Overhead LED Variable Message Sign (VMS) Matrix Shader (.frag)
// Path: src/road/tolls/shaders/toll_vms_led.frag
// ==============================================================================
#version 460
#extension GL_ARB_separate_shader_objects : enable

#include "../../shaders/include/road_common.glsl"

layout(location = 0) in vec3 inWorldPos;
layout(location = 1) in vec3 inWorldNormal;
layout(location = 2) in vec2 inUV;
layout(location = 3) in vec4 inColor;

layout(set = 0, binding = 0) uniform TollVmsUBO {
    float timeSec;
    uint laneOpenBitmask;
    uint isEtcExpress;
    float ambientLux;
} ubo;

layout(location = 0) out vec4 outFragColor;

void main() {
    // 64x16 RGB LED pixel matrix pitch across canopy VMS display face
    vec2 matrixRes = vec2(64.0, 16.0);
    vec2 cellUv = fract(inUV * matrixRes) - 0.5;
    float diodeMask = 1.0 - smoothstep(0.24, 0.38, length(cellUv));

    // Animated downward lane-open green arrow or red X indicator
    float xCentered = abs(inUV.x - 0.5);
    float scrollY = fract(inUV.y - ubo.timeSec * 0.65);
    float arrowHead = 1.0 - smoothstep(0.02, 0.04, abs(xCentered - (1.0 - scrollY) * 0.28));
    float crossSign = 1.0 - smoothstep(0.04, 0.06, min(abs((inUV.x - 0.5) - (inUV.y - 0.5)), abs((inUV.x - 0.5) + (inUV.y - 0.5))));

    bool laneOpen = (ubo.laneOpenBitmask & 1u) != 0u;
    vec3 ledColor = laneOpen
        ? mix(vec3(0.04, 0.95, 0.42), vec3(0.0, 0.85, 1.0), float(ubo.isEtcExpress))
        : vec3(1.0, 0.14, 0.10);

    float glyph = laneOpen ? arrowHead : crossSign;
    float luminanceBoost = mix(4.2, 2.2, clamp(ubo.ambientLux, 0.0, 1.0));
    vec3 displayColor = mix(vec3(0.03, 0.035, 0.04), ledColor * luminanceBoost, diodeMask * glyph);

    vec3 bezelMix = mix(inColor.rgb * 0.45, displayColor, step(0.06, inUV.x) * step(inUV.x, 0.94) * step(0.10, inUV.y) * step(inUV.y, 0.90));
    outFragColor = vec4(bezelMix / (bezelMix + vec3(1.0)), 1.0);
}
