#version 300 es
precision  mediump float;
out vec4 fraColor;

in vec4 color;
in vec2 colorPos;

uniform sampler2D ourTexture;
uniform float timeValue1;

void main() {
    float y = sin(timeValue1);
    float x = colorPos.x;

    fraColor = texture(ourTexture,vec2(x,y));
}