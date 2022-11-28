#version 300 es
layout (location = 0) in vec3 vPos;
layout (location = 1) in vec4 aColor;
uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;
out vec4 outColor;
void main() {
    gl_Position = projection * view * model * vec4(vPos,1.0);
    gl_PointSize = 1.0;
    outColor = aColor;
}