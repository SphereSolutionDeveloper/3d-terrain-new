// ==============================================================================
// Khronos glTF 2.0 / Sascha Willems Vulkan PBR Lighting Common Functions (GLSL)
// Path: native-assets/shaders/glsl/include/pbr_common.glsl
// Reference: SaschaWillems/Vulkan (pbrbasic/pbrtexture) & KhronosGroup/Vulkan-Samples
// ==============================================================================

const float PI_PBR = 3.14159265359;

// Normal Distribution Function: Trowbridge-Reitz GGX
float D_GGX(float dotNH, float roughness) {
    float alpha = roughness * roughness;
    float alpha2 = alpha * alpha;
    float denom = dotNH * dotNH * (alpha2 - 1.0) + 1.0;
    return alpha2 / max(PI_PBR * denom * denom, 0.00001);
}

// Geometric Shadowing Function: Schlick-GGX (Direct Lighting k = (roughness+1)^2 / 8)
float G_SchlickGGX(float NdotV, float roughness) {
    float r = roughness + 1.0;
    float k = (r * r) / 8.0;
    return NdotV / max(NdotV * (1.0 - k) + k, 0.00001);
}

float G_Smith(float NdotV, float NdotL, float roughness) {
    return G_SchlickGGX(NdotV, roughness) * G_SchlickGGX(NdotL, roughness);
}

// Fresnel Equation: Schlick Approximation
vec3 F_Schlick(float cosTheta, vec3 F0) {
    return F0 + (1.0 - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
}

// Direct Cook-Torrance Specular + Lambertian Diffuse BRDF
vec3 EvaluatePBRDirectLight(
    vec3 N, vec3 V, vec3 L,
    vec3 albedo, float roughness, float metallic,
    vec3 lightColor
) {
    vec3 H = normalize(V + L);
    float NdotL = max(dot(N, L), 0.0);
    float NdotV = max(dot(N, V), 0.001);
    float NdotH = max(dot(N, H), 0.0);
    float HdotV = max(dot(H, V), 0.0);

    vec3 F0 = mix(vec3(0.04), albedo, metallic);

    float D = D_GGX(NdotH, clamp(roughness, 0.04, 1.0));
    float G = G_Smith(NdotV, NdotL, clamp(roughness, 0.04, 1.0));
    vec3  F = F_Schlick(HdotV, F0);

    vec3 specular = (D * G * F) / max(4.0 * NdotV * NdotL, 0.001);
    vec3 kD = (vec3(1.0) - F) * (1.0 - metallic);
    vec3 diffuse = kD * albedo / PI_PBR;

    return (diffuse + specular) * lightColor * NdotL;
}

// ACES Filmic Tone Mapping (Khronos PBR Neutral / Narkowicz ACES approximation)
vec3 TonemapACES(vec3 x) {
    const float a = 2.51;
    const float b = 0.03;
    const float c = 2.43;
    const float d = 0.59;
    const float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}
