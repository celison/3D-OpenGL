#version 430

out vec2 tc;

uniform mat4 mvp;
layout (binding = 0) uniform sampler2D tex_color;
layout (binding = 1) uniform sampler2D tex_height;
layout (binding = 2) uniform sampler2D tex_normal;

/* --- light stuff ----*/
struct PositionalLight
{ vec4 ambient; vec4 diffuse; vec4 specular; vec3 position; };
struct Material
{ vec4 ambient; vec4 diffuse; vec4 specular; float shininess; };
uniform vec4 globalAmbient;
uniform PositionalLight light;
uniform Material material;
uniform mat4 mv_matrix;
uniform mat4 proj_matrix;
uniform mat4 normalMat;
/*-----------------*/

void main(void)
{	const vec4 vertices[] =
		vec4[] (vec4(-0.5, 0.0, -0.5, 1.0),
				vec4( 0.5, 0.0, -0.5, 1.0),
				vec4(-0.5, 0.0,  0.5, 1.0),
				vec4( 0.5, 0.0,  0.5, 1.0));
	
	// compute an offset for coordinates based on distance
	int x = gl_InstanceID & 63;
	int y = gl_InstanceID >> 6;
	vec2 offs = vec2(x,y);
	
	// texture coordinates are distributed across 64 patches
	tc = (vertices[gl_VertexID].xz + offs + vec2(0.5))/64.0;
	
	// vertex locations range from -32 to +32
	gl_Position = vertices[gl_VertexID] + vec4(float(x-32), 0.0, float(y-32), 0.0);
}