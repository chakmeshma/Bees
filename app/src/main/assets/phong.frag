#version 100

precision mediump float;

varying vec3 normalInterp;
varying vec3 vertPos;
varying vec2 vertUV;

//const int mode = 1;

const vec3 lightPos = vec3(0.0,0.0,0.0);
const vec3 ambientColor = vec3(0.3, 0.0, 0.0);
const vec3 diffuseColor = vec3(0.5, 0.0, 0.0);
const vec3 specColor = vec3(1.0, 1.0, 1.0);

uniform sampler2D diffuseTexture;
uniform sampler2D normalTexture;

uniform mat3 rotationMatrix;

void main() {
    vec3 normal = normalize(normalInterp);
    vec3 lightDir = normalize(lightPos - vertPos);
    vec3 reflectDir = reflect(-lightDir, normal);
    vec3 viewDir = normalize(-vertPos);

    vec3 normalColor = texture2D(normalTexture, vertUV).rgb;

    float lambertian = max(dot(lightDir, normalColor), 0.1);
    float specular = 0.0;

    if(lambertian > 0.0) {
       float specAngle = max(dot(reflectDir, viewDir), 0.0);
       specular = pow(specAngle, 2.0);
    }

    gl_FragColor = vec4(texture2D(diffuseTexture, vertUV).rgb * lambertian, 1.0) + specular * 0.3 * vec4(specColor, 1.0);

//    // only ambient
//    if(mode == 2) gl_FragColor = vec4(ambientColor, 1.0);
//    // only diffuse
//    if(mode == 3) gl_FragColor = vec4(lambertian*diffuseColor, 1.0);
//    // only specular
//    if(mode == 4) gl_FragColor = vec4(specular*specColor, 1.0);

}
