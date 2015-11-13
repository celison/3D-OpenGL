#version 430

in vec2 tc;
out vec4 fragColor;

layout (binding=0) uniform sampler2D s;
uniform mat4 mv_matrix;
uniform mat4 proj_matrix;

void main(void)
{
	fragColor = texture2D(s,tc);
}
