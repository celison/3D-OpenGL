#version 430
in vec4 vs_color;
out vec4 color;
uniform mat4 mv_matrix;
uniform mat4 proj_matrix;
void main(void)
{
	color = vs_color;
}