#version 430

layout (location=0) in vec3 vertPos;
layout (location=1) in vec3 vertNormal;
layout (location=2) in vec3 vertTangent;
layout (location=3) in vec2 texCord;

out vec2 tc;
out vec3 vNormal, vLightDir, vVertPos, vHalfVec, vTangent;
out vec4 shadow_coord;

struct PositionalLight
{	vec4 ambient, diffuse, specular;
	vec3 position;
};
struct Material
{	vec4 ambient, diffuse, specular;   
	float shininess;
};

uniform vec4 globalAmbient;
uniform PositionalLight light;
uniform Material material;
uniform mat4 mv_matrix;
uniform mat4 proj_matrix;
uniform mat4 normalMat;
uniform mat4 shadowMVP;
uniform float heightMap;

layout (binding=0) uniform sampler2DShadow shadowTex;
layout (binding=1) uniform sampler2D tex_normal;
layout (binding=2) uniform sampler2D tex_texture;

void main(void)
{	//output the vertex position to the rasterizer for interpolation
	vVertPos = (mv_matrix * vec4(vertPos,1.0)).xyz;
        
	//get a vector from the vertex to the light and output it to the rasterizer for interpolation
	vLightDir = light.position - vVertPos;

	//get a vertex normal vector in eye space and output it to the rasterizer for interpolation
	vNormal = (normalMat * vec4(vertNormal,1.0)).xyz;
	
	// calculate the half vector (L+V)
	vHalfVec = (vLightDir-vVertPos).xyz;
	
	shadow_coord = shadowMVP * vec4(vertPos,1.0);

	vTangent = vertTangent;

	tc = texCord;

	gl_Position = proj_matrix * mv_matrix * vec4(vertPos,1.0);
}
