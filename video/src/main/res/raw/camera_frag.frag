#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require //YUV数据扩展
precision  mediump float;

out vec4 fraColor;

uniform samplerExternalOES sESOTexture;//YUV数据扩展
uniform sampler2D sTexture;
in vec2 outTexturePos;

uniform mat4 zoom;

uniform bool hasTri;
void main() {
    if(hasTri){
        vec2 v = ( vec4(outTexturePos.x,outTexturePos.y,1.0,1.0) * zoom).xy;
        fraColor = texture(sESOTexture,v);
/**        float average = 0.2126 * fraColor.r + 0.7152 * fraColor.g + 0.0722 * fraColor.b;
        fraColor = vec4(average, average, average, 1.0);*/
    }else
        fraColor = mix(texture(sTexture, outTexturePos),texture(sESOTexture, outTexturePos),0.5);
}