#version 300 es
layout (location = 0) in vec3 vPos;

uniform float timeValue;
uniform vec4 uColor;

out vec4 color;
out vec2 colorPos;

void main() {
    gl_Position = vec4(vPos,1.0);
    gl_PointSize = 1.0;
    float s = sin(timeValue);
    float c = cos(timeValue + 1.5833333);
    if(vPos.y > 0.0){
        color = mix(vec4(1.0 ,sin(timeValue),c,0.0),uColor,0.8);
    }else{
        color = mix(vec4(1.0,c,s,0.0),uColor,0.2);
    }
    colorPos = vec2(vPos.x,vPos.y);
}