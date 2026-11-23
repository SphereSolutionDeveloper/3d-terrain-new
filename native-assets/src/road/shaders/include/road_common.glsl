// ==============================================================================
// Shared Road Network Vulkan 1.3 GLSL Common Functions & PBR Lighting
// Path: src/road/shaders/include/road_common.glsl
// Standards: Khronos glTF 2.0 PBR / Sascha Willems Vulkan Framework
// ==============================================================================
#ifndef APEX_ROAD_COMMON_GLSL
#define APEX_ROAD_COMMON_GLSL

const float ROAD_PI = 3.14159265359;

// Material IDs emitted by C++ Road Generators (RoadMeshPrimitives.h)
const uint MAT_ROAD_ASPHALT   = 0u;
const uint MAT_ROAD_CONCRETE  = 1u;
const uint MAT_ROAD_STEEL     = 2u;
const uint MAT_ROAD_MARKINGS  = 3u;
const uint MAT_ROAD_EMISSIVE  = 4u;

float roadHash21(vec2 p) {
    p = fract(p * vec2(234.34, 435.345));
    p += dot(p, p + 34.23);
    return fract(p.x * p.y);
}

float roadValueNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = roadHash21(i + vec2(0.0, 0.0));
    float b = roadHash21(i + vec2(1.0, 0.0));
    float c = roadHash21(i + vec2(0.0, 1.0));
    float d = roadHash21(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float roadFbm(vec2 p) {
    float v = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 4; ++i) {
        v += amp * roadValueNoise(p);
        p *= 2.04;
        amp *= 0.5;
    }
    return v;
}

float roadVoronoiEdgeDist(vec2 uv, out vec2 cellId) {
    vec2 i = floor(uv);
    vec2 f = fract(uv);
    float md1 = 8.0;
    float md2 = 8.0;
    vec2 bestCell = vec2(0.0);
    for (int y = -1; y <= 1; ++y) {
        for (int x = -1; x <= 1; ++x) {
            vec2 g = vec2(float(x), float(y));
            vec2 o = vec2(roadHash21(i + g), roadHash21(i + g + vec2(17.3, 41.9)));
            vec2 r = g + o - f;
            float d = dot(r, r);
            if (d < md1) {
                md2 = md1;
                md1 = d;
                bestCell = i + g;
            } else if (d < md2) {
                md2 = d;
            }
        }
    }
    cellId = bestCell;
    return sqrt(md2) - sqrt(md1);
}

// Evaluates SDF thermoplastic road paint markings along normalized UV (u=across, v=along)
float evaluateDashedLineMask(vec2 uv, float centerU, float halfW, float dashPeriod, float duty) {
    float distU = abs(uv.x - centerU);
    float lineMask = 1.0 - smoothstep(halfW - 0.005, halfW + 0.005, distU);
    float phase = fract(uv.y * dashPeriod);
    float dashMask = step(phase, duty);
    return lineMask * dashMask;
}

// Cook-Torrance GGX PBR BRDF tailored for wet/dry asphalt & galvanized infrastructure
float roadD_GGX(float NdotH, float roughness) {
    float a = roughness * roughness;
    float a2 = a * a;
    float denom = NdotH * NdotH * (a2 - 1.0) + 1.0;
    return a2 / max(ROAD_PI * denom * denom, 0.00001);
}

float roadG_Smith(float NdotV, float NdotL, float roughness) {
    float r = roughness + 1.0;
    float k = (r * r) / 8.0;
    float gv = NdotV / max(NdotV * (1.0 - k) + k, 0.00001);
    float gl = NdotL / max(NdotL * (1.0 - k) + k, 0.00001);
    return gv * gl;
}

vec3 roadF_Schlick(float cosTheta, vec3 F0) {
    return F0 + (1.0 - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
}

vec3 evaluateRoadPBR(
    vec3 N, vec3 V, vec3 L,
    vec3 albedo, float roughness, float metallic, vec3 lightRadiance
) {
    vec3 H = normalize(V + L);
    float NdotL = max(dot(N, L), 0.0);
    float NdotV = max(dot(N, V), 0.001);
    float NdotH = max(dot(N, H), 0.0);
    float HdotV = max(dot(H, V), 0.0);

    vec3 F0 = mix(vec3(0.04), albedo, metallic);
    float D = roadD_GGX(NdotH, clamp(roughness, 0.05, 1.0));
    float G = roadG_Smith(NdotV, NdotL, clamp(roughness, 0.05, 1.0));
    vec3  F = roadF_Schlick(HdotV, F0);

    vec3 specular = (D * G * F) / max(4.0 * NdotV * NdotL, 0.001);
    vec3 kD = (vec3(1.0) - F) * (1.0 - metallic);
    return (kD * albedo / ROAD_PI + specular) * lightRadiance * NdotL;
}

#endif // APEX_ROAD_COMMON_GLSL
