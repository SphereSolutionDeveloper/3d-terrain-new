// Procedural Vulkan Asset Suite (Vulkan 1.3 GLSL)
// Path: shaders/glsl/debug/debug_line.frag

#version 460

layout(location = 0) in vec3 inPosition;
layout(location = 0) out vec4 outColor;

void main() {
    gl_Position = vec4(inPosition, 1.0);
    outColor = vec4(1.0);
}
