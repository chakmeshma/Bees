#version 100

attribute vec3 positions;
attribute vec3 normals;
attribute vec2 uvs;

uniform mat4 projectionMatrix, viewMatrix, modelMatrix;
uniform mat3 rotationMatrix;

varying vec3 normalInterp;
varying vec3 vertPos;
varying vec2 vertUV;

void main(){
    mat4 modelview = viewMatrix * modelMatrix;

    gl_Position = projectionMatrix * modelview * vec4(positions, 1.0);

    vec4 vertPos4 = modelview * vec4(positions, 1.0);
    vertPos = vec3(vertPos4) / vertPos4.w;
    normalInterp = rotationMatrix * normals;
    vertUV = uvs;
}
