#version 300 es
layout (location = 0) in vec3 vPos;
layout (location = 1) in vec2 aTexturePos;
layout (location = 2) in vec3 trivPos;

uniform bool hasTri;

//纹理矩阵
uniform mat4 uTextureMatrix;
out vec2 outTexturePos;
void main() {
    if(hasTri){
        gl_Position = vec4(trivPos,1.0);
    }else{
        outTexturePos = aTexturePos;
        gl_Position = vec4(vPos,1.0);
    }
}