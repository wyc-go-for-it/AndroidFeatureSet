#version 300 es
layout (location = 0) in vec3 vPos;
layout (location = 1) in vec4 aTexturePos;
//纹理矩阵
uniform mat4 uTextureMatrix;
out vec2 outTexturePos;
void main() {
    gl_Position = vec4(vPos,1.0);
    outTexturePos = (uTextureMatrix * aTexturePos).xy;
}