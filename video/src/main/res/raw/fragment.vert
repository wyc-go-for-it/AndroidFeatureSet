#version 300 es
precision  mediump float;
in vec4 outColor;
out vec4 fraColor;
void main() {
    fraColor = outColor;
}