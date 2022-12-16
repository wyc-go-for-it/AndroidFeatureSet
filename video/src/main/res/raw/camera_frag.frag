#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require //YUV数据扩展
precision  mediump float;

out vec4 fraColor;

uniform samplerExternalOES sTexture;//YUV数据扩展
in vec2 outTexturePos;

void main() {
    fraColor = texture(sTexture,outTexturePos);
}