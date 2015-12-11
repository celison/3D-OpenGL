#version 430

in vec2 tc;
in vec3 vNormal, vLightDir, vVertPos, vHalfVec, vTangent;
in vec4 shadow_coord;

out vec4 fragColor;
 
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
uniform float alpha;

layout (binding=0) uniform sampler2DShadow shadowTex;
layout (binding=1) uniform sampler2D tex_normal;
layout (binding=2) uniform sampler2D tex_texture;

vec3 CalcBumpedNormal();

vec3 CalcBumpedNormal()
{
	vec3 normal = normalize(vNormal);
	vec3 tan = normalize(vTangent);
	tan = normalize(tan - dot(tan, normal) * normal);
	vec3 bitangent = cross(tan, normal);
	vec3 bumpNormal = texture2D(tex_normal,tc).xyz;
	bumpNormal = bumpNormal * 2.0 - 1.0;
	mat3 TBN = mat3(tan, bitangent, normal);
	vec3 newNormal = TBN * bumpNormal;
	newNormal = normalize(newNormal);
	return newNormal;
}

void main(void)
{	vec3 L = normalize(vLightDir);
	vec3 N;
	if (heightMap == 1.0)
		N = CalcBumpedNormal();
	else
		N = normalize(vNormal);
	vec3 V = normalize(-vVertPos);
	vec3 H = normalize(vHalfVec);

	vec4 texColor = texture2D(tex_texture,tc);

	float inShadow = textureProj(shadowTex, shadow_coord);
	
	fragColor = globalAmbient * material.ambient
				+ light.ambient * material.ambient;
	
	if (inShadow != 0.0)
	{	fragColor += light.diffuse * material.diffuse * max(dot(L,N),0.0)
				+ light.specular * material.specular
				* pow(max(dot(H,N),0.0),material.shininess*3.0);
	}

	fragColor = fragColor * 0.5 + texColor * 0.5;

	if (texColor.a != 1.0) {
		fragColor.a = texColor.a *= alpha;
	}
}
