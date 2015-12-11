#version 430

in vec2 tes_out;
out vec4 color;
uniform mat4 mvp;

layout (binding = 0) uniform sampler2D tex_color;
layout (binding = 1) uniform sampler2D tex_height;
layout (binding = 2) uniform sampler2D tex_normal;

/* ---- for lighting ---- */
in vec3 varyingVertPos;
in vec3 varyingLightDir; 
struct PositionalLight
{	vec4 ambient; vec4 diffuse; vec4 specular; vec3 position; };
struct Material
{	vec4 ambient; vec4 diffuse; vec4 specular; float shininess; };  
uniform vec4 globalAmbient;
uniform PositionalLight light;
uniform Material material;
uniform mat4 mv_matrix;
uniform mat4 proj_matrix;
uniform mat4 normalMat;
/* ---------------------- */

void main(void)
{	vec3 L = normalize(varyingLightDir);

	// get the normal from the normal map
	vec3 N = texture2D(tex_normal,tes_out).rgb * 2.0 - 1.0;
	
	vec3 V = normalize(-varyingVertPos);
	vec3 R = normalize(reflect(-L, N));
	float cosTheta = dot(L,N);
	float cosPhi = dot(V,R);

	color = 0.5 * 
				( globalAmbient * material.ambient  +  light.ambient * material.ambient
				+ light.diffuse * material.diffuse * max(cosTheta,0.0)
				+ light.specular * material.specular * pow(max(cosPhi,0.0), material.shininess)
				) +
			0.5 *
				( texture2D(tex_color, tes_out)
				);
}