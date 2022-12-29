#version 300 es
layout (location = 0) in vec3 vPos;
layout (location = 1) in vec2 aTexturePos;

uniform bool hasTri;

//纹理矩阵
uniform mat4 uTextureMatrix;
out vec2 outTexturePos;
void main() {
    outTexturePos = aTexturePos;
    if(!hasTri){
        float radian = radians(-90.0);
        mat4 rotate = mat4(
            cos(radian),sin(radian),0.0,0.0,
            -sin(radian),cos(radian),0.0,0.0,
            0.0,0.0,1.0,0.0 ,
            0.0,0.0,0.0,1.0
        );
        gl_Position = rotate * vec4(vPos,1.0);
    }else gl_Position = vec4(vPos,1.0);

}