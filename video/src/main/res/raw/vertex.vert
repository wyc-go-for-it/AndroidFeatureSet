#version 300 es
layout (location = 0) in vec3 vPos;
layout (location = 1) in vec4 aColor;
out vec4 outColor;
void main() {
    gl_Position = vec4(vPos.x,vPos.y,vPos.z,1.0);
    gl_PointSize = 10.0;
    outColor = aColor;
}