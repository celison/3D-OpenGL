#version 430
layout (location = 1) in vec4 offset;
layout (location = 2) in vec4 size;
layout (location = 3) in vec4 color_toggle;

out vec4 vs_color;

void main(void)
{
	const vec4 vertices[3] = vec4[3] (
	 	vec4( 0.25, -0.25, 0.5, 1.0),
		vec4(-0.25, -0.25, 0.5, 1.0),
		vec4( 0.25, 0.25, 0.5, 1.0));

	const vec4 colors[] = vec4[3](
		vec4( 1.0, 0.0, 0.0, 1.0),
		vec4( 0.0, 1.0, 0.0, 1.0),
		vec4( 0.0, 0.0, 1.0, 1.0));
	
	// Add "offset" to our hard-coded vertex position
	gl_Position = (vertices[gl_VertexID] * size) + offset;
	// Output a fixed value for vs_color
	if (color_toggle.x == 0.0) {
		vs_color = colors[gl_VertexID];
	} else {
		vs_color = vec4( 1.0, 0.0, 0.0, 1.0);
	}
}
