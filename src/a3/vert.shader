#version 430

layout (location = 0) in vec3 vertPos;
layout (location = 1) in vec3 vertNormal;
layout (location = 2) in vec2 texPos;

out vec3 varyingNormal;
out vec3 varyingLightDir;
out vec3 varyingVertPos;
out vec3 varyingHalfVector;
out vec2 tc;

struct PositionalLight
{   vec4 ambient;
    vec4 diffuse;
    vec4 specular;
    vec3 position;
};

struct Material
{   vec4 ambient;
    vec4 diffuse;
    vec4 specular;
    float shininess;
};
uniform vec4 clip_plane;
uniform int flipNormal;

uniform vec4 globalAmbient;
uniform PositionalLight light;
uniform Material material;
uniform mat4 mv_matrix;
uniform mat4 proj_matrix;
uniform mat4 normal_matrix;

void main(void)
{
    float x = gl_InstanceID % 3 * 3;
    float y = (gl_InstanceID / 3) % 3 * 3; //cos(0.8 * gl_InstanceID) * 1.2;
    float z = gl_InstanceID / 9 * 3; //sin(0.7 * gl_InstanceID) * 1.2;

    vec3 loc = vertPos + vec3(x,y,z);

    gl_ClipDistance[0] = dot(vec4(loc, 1.0), clip_plane);
    varyingVertPos=(mv_matrix * vec4(loc, 1.0)).xyz;
    varyingLightDir = light.position - varyingVertPos;
    varyingNormal = (normal_matrix*vec4(vertNormal, 1.0)).xyz;
    if (flipNormal==1) varyingNormal = -varyingNormal;
    varyingHalfVector = normalize(normalize(varyingLightDir) + normalize(-varyingVertPos)).xyz;
    gl_Position = proj_matrix * mv_matrix * vec4(loc,1.0);
    tc = texPos;
}