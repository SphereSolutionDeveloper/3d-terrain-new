// ==============================================================================
// Road Network System - Vulkan 1.3 Vertex Shader
// Path: src/road/shaders/road_network.vert
// ==============================================================================
#version 460
#extension GL_ARB_separate_shader_objects : enable

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec3 inNormal;
layout(location = 2) in vec2 inUV;
layout(location = 3) in vec4 inTangent;
layout(location = 4) in vec4 inColor;
layout(location = 5) in vec4 inPbrParams;   // x=roughness, y=metallic, z=ao, w=emissive
layout(location = 6) in uint inMaterialId;

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

layout(push_constant) uniform RoadPushConstants {
    mat4 transformMatrix;
    float bendCurvature;
    float superelevationBankRad;
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
    vec3 localPos = inPosition * max(pc.componentScale, 0.01);
    vec3 localNorm = inNormal;

    // Optional real-time GPU horizontal arc bend & banking warp along Z axis
    if (abs(pc.bendCurvature) > 0.0001) {
        float theta = localPos.z * pc.bendCurvature;
        float s = sin(theta);
        float c = cos(theta);
        float radial = localPos.x;
        localPos.x = radial * c + (1.0 - c) / pc.bendCurvature;
        localPos.z = sin(theta) / pc.bendCurvature - radial * s;
        localNorm = vec3(localNorm.x * c - localNorm.z * s, localNorm.y, localNorm.x * s + localNorm.z * c);
    }

    if (abs(pc.superelevationBankRad) > 0.0001) {
        float cb = cos(pc.superelevationBankRad);
        float sb = sin(pc.superelevationBankRad);
        localPos.y += localPos.x * sb;
        localNorm = normalize(vec3(localNorm.x * cb - localNorm.y * sb, localNorm.x * sb + localNorm.y * cb, localNorm.z));
    }

    mat4 worldMat = ubo.model * pc.transformMatrix;
    vec4 worldPos = worldMat * vec4(localPos, 1.0);
    mat3 normalMat = transpose(inverse(mat3(worldMat)));

    outWorldPos = worldPos.xyz;
    outNormal = normalize(normalMat * localNorm);
    outUV = inUV;
    outTangent = vec4(normalize(normalMat * inTangent.xyz), inTangent.w);
    outColor = inColor;
    outPbrParams = inPbrParams;
    outMaterialId = inMaterialId;

    gl_Position = ubo.projection * ubo.view * worldPos;
}
