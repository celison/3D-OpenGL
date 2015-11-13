#version 430

uniform mat4 mv_matrix;
uniform mat4 proj_matrix;

out vec4 vs_color;

void main(void)
{
	const vec4 vertices[6] = vec4[6] (
	 	vec4( 0, 0.0, 0.0, 1.0),
		vec4( 10.0, 0.0, 0.0, 1.0),
		vec4( 0.0, 0, 0.0, 1.0),
		vec4( 0.0, 10.0, 0.0, 1.0),
		vec4( 0.0, 0.0, 0, 1.0),
		vec4( 0.0, 0.0, 10.0, 1.0));

	gl_Position = proj_matrix * mv_matrix * vertices[gl_VertexID];

	// Output a fixed value for vs_color
	if (gl_VertexID == 0 || gl_VertexID == 1) {
		vs_color = vec4( 1.0, 0.0, 0.0, 1.0);
	} else if (gl_VertexID == 2 || gl_VertexID == 3) {
		vs_color = vec4( 0.0, 1.0, 0.0, 1.0);
	} else {
        vs_color = vec4( 0.0, 0.0, 1.0, 1.0);
	}
}
