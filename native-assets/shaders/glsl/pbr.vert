// ==============================================================================
// Procedural Vulkan Asset Suite - Road & Terrain PBR Vertex Shader (Vulkan 1.3 GLSL)
// Path: native-assets/shaders/glsl/pbr.vert
// Architecture: Sascha Willems (SaschaWillems/Vulkan) & Khronos Vulkan Samples
// ==============================================================================
#version 460

layout(location = 0) in vec3  inPosition;
layout(location = 1) in vec3  inNormal;
layout(location = 2) in vec2  inUV;
layout(location = 3) in vec4  inTangent;
layout(location = 4) in vec4  inColor;
layout(location = 5) in vec4  inPbrParams;   // x=roughness, y=metallic, z=ao, w=emissive
layout(location = 6) in uint  inMaterialId;

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

layout(push_constant) uniform RoadPushConstants {
    mat4 transformMatrix;
    float bendCurvature;
    float superelevationBank;
    float componentScale;
    uint  componentType;
    vec4  pbrOverride;
    float customParams[8];
} pc;

layout(location = 0) out vec3 outWorldPos;
layout(location = 1) out vec3 outNormal;
layout(location = 2) out vec2 outUV;
layout(location = 3) out vec4 outTangent;
layout(location = 4) out vec4 outColor;
layout(location = 5) out vec4 outPbrParams;
layout(location = 6) flat out uint outMaterialId;

void main() {
    mat4 worldMat = ubo.model * pc.transformMatrix;
    vec4 worldPos = worldMat * vec4(inPosition * max(pc.componentScale, 0.001), 1.0);

    mat3 normalMat = transpose(inverse(mat3(worldMat)));
    outWorldPos = worldPos.xyz;
    outNormal = normalize(normalMat * inNormal);
    outUV = inUV;
    outTangent = vec4(normalize(normalMat * inTangent.xyz), inTangent.w);
    outColor = inColor;
    outPbrParams = inPbrParams;
    outMaterialId = inMaterialId;

    gl_Position = ubo.projection * ubo.view * worldPos;
}
