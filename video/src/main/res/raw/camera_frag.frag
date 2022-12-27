#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require //YUV数据扩展
precision  mediump float;

out vec4 fraColor;

uniform samplerExternalOES sESOTexture;//YUV数据扩展
uniform sampler2D sTexture;
in vec2 outTexturePos;

uniform bool hasTri;
void main() {
    if(hasTri){
        fraColor = vec4(1.0f,0.0f,0.0f,0.0f);
    }else
        fraColor = mix(texture(sESOTexture, outTexturePos), texture(sTexture, outTexturePos), 0.2);
}