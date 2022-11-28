#version 300 es
precision  mediump float;
in vec4 outColor;
out vec4 fraColor;
void main() {
    if(outColor == vec4(0.0,1.0,1.0,0.0)){
        fraColor = vec4(1.0,1.0,1.0,0);
    }else
        fraColor = vec4(1.0,0.0,0.3,0);
}