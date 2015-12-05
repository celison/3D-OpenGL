package shadow;

import graphicslib3D.*;
import graphicslib3D.light.*;
import graphicslib3D.GLSLUtils.*;
import java.nio.*;
import javax.swing.*;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import graphicslib3D.shape.*;
import com.jogamp.opengl.util.*;

public class Code extends JFrame implements GLEventListener
{
	graphicslib3D.Material thisMaterial;
	private String[] vBlinn1ShaderSource, vBlinn2ShaderSource, fBlinn2ShaderSource;
	private GLCanvas myCanvas;
	private int rendering_program1, rendering_program2;
	private int vaoID[] = new int[1];
	private int [] bufferIDs = new int [6];
	private int mv_location, proj_location, vertexLoc, n_location;
	float aspect;
	private GLSLUtils util = new GLSLUtils();
	
	// location of torus and camera
	graphicslib3D.Point3D torusLoc = new graphicslib3D.Point3D(1.6, 0.0, -0.3);
	graphicslib3D.Point3D pyrLoc = new graphicslib3D.Point3D(-1.0, 0.1, 0.3);
	graphicslib3D.Point3D cameraLoc = new graphicslib3D.Point3D(0.0, 0.2, 6.0);
	graphicslib3D.Point3D lightLoc = new graphicslib3D.Point3D(-3.8f, 2.2f, 1.1f);
	
	Matrix3D m_matrix = new Matrix3D();
	Matrix3D v_matrix = new Matrix3D();
	Matrix3D mv_matrix = new Matrix3D();
	Matrix3D proj_matrix = new Matrix3D();
	
	// light stuff
	float [] globalAmbient = new float[] { 0.7f, 0.7f, 0.7f, 1.0f };
	PositionalLight currentLight = new PositionalLight();
	
	// shadow stuff
	int scSizeX, scSizeY;
	int [] shadow_tex = new int [1];
	int [] shadow_buffer = new int [1];
	Matrix3D lightV_matrix = new Matrix3D();
	Matrix3D lightP_matrix = new Matrix3D();
	Matrix3D shadowMVP1 = new Matrix3D();
	Matrix3D shadowMVP2 = new Matrix3D();
	Matrix3D b = new Matrix3D();

	// model stuff
	int numPyramidVertices, numPyramidIndices;
	ImportedModel pyramid = new ImportedModel("src/shadow/pyr.obj");
	Torus myTorus = new Torus(0.6f, 0.4f, 48);
	int numTorusVertices;
	
	public Code()
	{	setTitle("Chapter8 - program 1");
		setSize(600, 600);
		myCanvas = new GLCanvas();
		myCanvas.addGLEventListener(this);
		getContentPane().add(myCanvas);
		setVisible(true);
		FPSAnimator animator = new FPSAnimator(myCanvas, 30);
		animator.start();
	}

	public void display(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) drawable.getGL();

		currentLight.setPosition(lightLoc);
		aspect = myCanvas.getWidth() / myCanvas.getHeight();
		proj_matrix = perspective(50.0f, aspect, 0.1f, 1000.0f);
		
		FloatBuffer bg = FloatBuffer.allocate(4);
		bg.put(0, 0.0f); bg.put(1, 0.0f); bg.put(2, 0.2f); bg.put(3, 1.0f);
		gl.glClearBufferfv(GL_COLOR,0,bg);
		
		float depthClearVal[] = new float[1]; depthClearVal[0] = 1.0f;
		gl.glClearBufferfv(GL_DEPTH,0,depthClearVal,0);
	
		gl.glBindFramebuffer(GL_FRAMEBUFFER, shadow_buffer[0]);
		gl.glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, shadow_tex[0], 0);
		
		gl.glDrawBuffer(GL.GL_NONE);
		gl.glEnable(GL_DEPTH_TEST);
		
		gl.glEnable(GL_POLYGON_OFFSET_FILL);	// for reducing
		gl.glPolygonOffset(2.0f, 4.0f);			//  shadow artifacts

		firstPass(drawable);
		
		gl.glDisable(GL_POLYGON_OFFSET_FILL);	// artifact reduction, continued
		
		gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
		gl.glActiveTexture(gl.GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, shadow_tex[0]);
	
		gl.glDrawBuffer(GL.GL_FRONT);
		
		secondPass(drawable);
	}
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	public void firstPass(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) drawable.getGL();

		gl.glUseProgram(rendering_program1);
		
		Point3D origin = new Point3D(0.0, 0.0, 0.0);
		Vector3D up = new Vector3D(0.0, 1.0, 0.0);
		lightV_matrix.setToIdentity();
		lightP_matrix.setToIdentity();
	
		lightV_matrix = lookAt(currentLight.getPosition(), origin, up);	// vector from light to origin
		lightP_matrix = perspective(50.0f, aspect, 0.1f, 1000.0f);

		// draw the torus
		
		m_matrix.setToIdentity();
		m_matrix.translate(torusLoc.getX(),torusLoc.getY(),torusLoc.getZ());
		m_matrix.rotateX(25.0);
		
		shadowMVP1.setToIdentity();
		shadowMVP1.concatenate(lightP_matrix);
		shadowMVP1.concatenate(lightV_matrix);
		shadowMVP1.concatenate(m_matrix);
		int shadow_location = gl.glGetUniformLocation(rendering_program1, "shadowMVP");
		gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP1.getFloatValues(), 0);
		
		// set up torus vertices buffer
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferIDs[0]);
		gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);	
	
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, numTorusVertices);

		// ---- draw the pyramid
		
		gl.glUseProgram(rendering_program1);
		mv_location = gl.glGetUniformLocation(rendering_program1, "mv_matrix");
		proj_location = gl.glGetUniformLocation(rendering_program1, "proj_matrix");
		
		//  build the MODEL matrix
		m_matrix.setToIdentity();
		m_matrix.translate(pyrLoc.getX(),pyrLoc.getY(),pyrLoc.getZ());
		m_matrix.rotateX(30.0);
		m_matrix.rotateY(40.0);

		shadowMVP1.setToIdentity();
		shadowMVP1.concatenate(lightP_matrix);
		shadowMVP1.concatenate(lightV_matrix);
		shadowMVP1.concatenate(m_matrix);

		gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP1.getFloatValues(), 0);
		
		// set up vertices buffer
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferIDs[1]);
		gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
	
		gl.glDrawArrays(GL_TRIANGLES, 0, pyramid.getNumVertices());
	}
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	public void secondPass(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) drawable.getGL();

		gl.glUseProgram(rendering_program2);

		// draw the torus
		
		thisMaterial = graphicslib3D.Material.BRONZE;		
		installLights(rendering_program2, v_matrix, drawable);
		
		mv_location = gl.glGetUniformLocation(rendering_program2, "mv_matrix");
		proj_location = gl.glGetUniformLocation(rendering_program2, "proj_matrix");
		n_location = gl.glGetUniformLocation(rendering_program2, "normalMat");
		int shadow_location = gl.glGetUniformLocation(rendering_program2,  "shadowMVP");
		
		//  build the MODEL matrix
		m_matrix.setToIdentity();
		m_matrix.translate(torusLoc.getX(),torusLoc.getY(),torusLoc.getZ());
		double amt = (double)(System.currentTimeMillis()%36000)/100.0;
		m_matrix.rotateX(25.0);

		//  build the VIEW matrix
		v_matrix.setToIdentity();
		v_matrix.translate(-cameraLoc.getX(),-cameraLoc.getY(),-cameraLoc.getZ());
		
		//  build the MODEL-VIEW matrix
		mv_matrix.setToIdentity();
		mv_matrix.concatenate(v_matrix);
		mv_matrix.concatenate(m_matrix);
		
		shadowMVP2.setToIdentity();
		shadowMVP2.concatenate(b);
		shadowMVP2.concatenate(lightP_matrix);
		shadowMVP2.concatenate(lightV_matrix);
		shadowMVP2.concatenate(m_matrix);
		
		//  put the MV and PROJ matrices into the corresponding uniforms
		gl.glUniformMatrix4fv(mv_location, 1, false, mv_matrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(proj_location, 1, false, proj_matrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(n_location, 1, false, (mv_matrix.inverse()).transpose().getFloatValues(), 0);
		gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP2.getFloatValues(), 0);
		
		// set up torus vertices buffer
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferIDs[0]);
		gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		// set up torus normals buffer
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferIDs[2]);
		gl.glVertexAttribPointer(1, 3, GL.GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);	
	
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
	
		gl.glDrawArrays(GL_TRIANGLES, 0, numTorusVertices);

		// draw the pyramid
		
		thisMaterial = graphicslib3D.Material.GOLD;		
		installLights(rendering_program2, v_matrix, drawable);
		
		gl.glUseProgram(rendering_program2);
		mv_location = gl.glGetUniformLocation(rendering_program2, "mv_matrix");
		proj_location = gl.glGetUniformLocation(rendering_program2, "proj_matrix");
		n_location = gl.glGetUniformLocation(rendering_program2, "normalMat");
		
		//  build the MODEL matrix
		m_matrix.setToIdentity();
		m_matrix.translate(pyrLoc.getX(),pyrLoc.getY(),pyrLoc.getZ());
		m_matrix.rotateX(30.0);
		m_matrix.rotateY(40.0);

		//  build the MODEL-VIEW matrix
		mv_matrix.setToIdentity();
		mv_matrix.concatenate(v_matrix);
		mv_matrix.concatenate(m_matrix);
		
		shadowMVP2.setToIdentity();
		shadowMVP2.concatenate(b);
		shadowMVP2.concatenate(lightP_matrix);
		shadowMVP2.concatenate(lightV_matrix);
		shadowMVP2.concatenate(m_matrix);
		gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP2.getFloatValues(), 0);

		//  put the MV and PROJ matrices into the corresponding uniforms
		gl.glUniformMatrix4fv(mv_location, 1, false, mv_matrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(proj_location, 1, false, proj_matrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(n_location, 1, false, (mv_matrix.inverse()).transpose().getFloatValues(), 0);
		
		// set up vertices buffer
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferIDs[1]);
		gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		// set up normals buffer
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferIDs[3]);
		gl.glVertexAttribPointer(1, 3, GL.GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, pyramid.getNumVertices());
	}

	public void init(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) drawable.getGL();
		createShaderPrograms(drawable);
		setupVertices(gl, drawable);
		setupShadowBuffers(drawable);
				
		b.setElementAt(0,0,0.5);b.setElementAt(0,1,0.0);b.setElementAt(0,2,0.0);b.setElementAt(0,3,0.5f);
		b.setElementAt(1,0,0.0);b.setElementAt(1,1,0.5);b.setElementAt(1,2,0.0);b.setElementAt(1,3,0.5f);
		b.setElementAt(2,0,0.0);b.setElementAt(2,1,0.0);b.setElementAt(2,2,0.5);b.setElementAt(2,3,0.5f);
		b.setElementAt(3,0,0.0);b.setElementAt(3,1,0.0);b.setElementAt(3,2,0.0);b.setElementAt(3,3,1.0f);
		
		// may reduce shadow border artifacts
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
	}
	
	public void setupShadowBuffers(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) drawable.getGL();
	
		scSizeX = myCanvas.getWidth();
		scSizeY = myCanvas.getHeight();
	
		gl.glGenFramebuffers(1, shadow_buffer, 0);
		gl.glBindFramebuffer(GL_FRAMEBUFFER, shadow_buffer[0]);
	
		gl.glGenTextures(1, shadow_tex, 0);
		gl.glBindTexture(GL_TEXTURE_2D, shadow_tex[0]);
		gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32,
						scSizeX, scSizeY, 0, GL_DEPTH_COMPONENT, GL_FLOAT, null);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
	}

// -----------------------------
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
	{setupShadowBuffers(drawable);}

	private void setupVertices(GL4 gl, GLAutoDrawable drawable)
	{
		// pyramid definition
		Vertex3D[] pyramid_vertices = pyramid.getVertices();
		int[] pyramid_indices = pyramid.getIndices();
		numPyramidVertices = pyramid.getNumVertices();
		numPyramidIndices = pyramid.getNumIndices();

		float[] pyramid_vertex_positions = new float[numPyramidIndices*3];
		float[] pyramid_texture_coordinates = new float[numPyramidIndices*2];
		float[] pyramid_normals = new float[numPyramidIndices*3];
		float[] pyramid_cvalues = new float[numPyramidIndices*3];

		for (int i=0; i<numPyramidIndices; i++)
		{	pyramid_vertex_positions[i*3]   = (float) (pyramid_vertices[pyramid_indices[i]]).getX();			
			pyramid_vertex_positions[i*3+1] = (float) (pyramid_vertices[pyramid_indices[i]]).getY();
			pyramid_vertex_positions[i*3+2] = (float) (pyramid_vertices[pyramid_indices[i]]).getZ();
			
			pyramid_texture_coordinates[i*2]   = (float) (pyramid_vertices[pyramid_indices[i]]).getS();
			pyramid_texture_coordinates[i*2+1] = (float) (pyramid_vertices[pyramid_indices[i]]).getT();
			
			pyramid_normals[i*3]   = (float) (pyramid_vertices[pyramid_indices[i]]).getNormalX();
			pyramid_normals[i*3+1] = (float) (pyramid_vertices[pyramid_indices[i]]).getNormalY();
			pyramid_normals[i*3+2] = (float) (pyramid_vertices[pyramid_indices[i]]).getNormalZ();
		}

		for (int i=0; i<numPyramidIndices/2; i++)
		{	pyramid_cvalues[i*3] = 1.0f; pyramid_cvalues[i*3+1] = 0.0f; pyramid_cvalues[i*3+2] = 0.0f;
		}
		for (int i=numPyramidIndices/2; i<numPyramidIndices; i++)
		{	pyramid_cvalues[i*3] = 0.0f; pyramid_cvalues[i*3+1] = 1.0f; pyramid_cvalues[i*3+2] = 0.0f;
		}

		Vertex3D[] torus_vertices = myTorus.getVertices();
		
		int[] torus_indices = myTorus.getIndices();	
		float[] torus_fvalues = new float[torus_indices.length*3];
		float[] torus_tvalues = new float[torus_indices.length*2];
		float[] torus_nvalues = new float[torus_indices.length*3];
		float[] torus_cvalues = new float[torus_indices.length*3];
		
		for (int i=0; i<torus_indices.length; i++)
		{	torus_fvalues[i*3]   = (float) (torus_vertices[torus_indices[i]]).getX();			
			torus_fvalues[i*3+1] = (float) (torus_vertices[torus_indices[i]]).getY();
			torus_fvalues[i*3+2] = (float) (torus_vertices[torus_indices[i]]).getZ();
			
			torus_tvalues[i*2]   = (float) (torus_vertices[torus_indices[i]]).getS();
			torus_tvalues[i*2+1] = (float) (torus_vertices[torus_indices[i]]).getT();
			
			torus_nvalues[i*3]   = (float) (torus_vertices[torus_indices[i]]).getNormalX();
			torus_nvalues[i*3+1] = (float) (torus_vertices[torus_indices[i]]).getNormalY();
			torus_nvalues[i*3+2] = (float) (torus_vertices[torus_indices[i]]).getNormalZ();
		}
		for (int i=0; i<torus_indices.length/2; i++)
		{	torus_cvalues[i*3] = 1.0f; torus_cvalues[i*3+1] = 0.0f; torus_cvalues[i*3+2] = 0.0f;
		}
		for (int i=torus_indices.length/2; i<torus_indices.length; i++)
		{	torus_cvalues[i*3] = 0.0f; torus_cvalues[i*3+1] = 1.0f; torus_cvalues[i*3+2] = 0.0f;
		}
		
		numTorusVertices = torus_indices.length;

		gl.glGenVertexArrays(vaoID.length, vaoID, 0);
		gl.glBindVertexArray(vaoID[0]);

		gl.glGenBuffers(6, bufferIDs, 0);

		//  put the Torus vertices into the first buffer,
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferIDs[0]);
		FloatBuffer vertBuf = FloatBuffer.wrap(torus_fvalues);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL.GL_STATIC_DRAW);
		
		//  load the pyramid vertices into the second buffer
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferIDs[1]);
		FloatBuffer pyrVertBuf = FloatBuffer.wrap(pyramid_vertex_positions);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, pyrVertBuf.limit()*4, pyrVertBuf, GL.GL_STATIC_DRAW);
		
		// load the torus normal coordinates into the third buffer
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferIDs[2]);
		FloatBuffer torusNorBuf = FloatBuffer.wrap(torus_nvalues);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, torusNorBuf.limit()*4, torusNorBuf, GL.GL_STATIC_DRAW);
		
		// load the pyramid normal coordinates into the fourth buffer
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferIDs[3]);
		FloatBuffer pyrNorBuf = FloatBuffer.wrap(pyramid_normals);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, pyrNorBuf.limit()*4, pyrNorBuf, GL.GL_STATIC_DRAW);
	}
	
	private void installLights(int rendering_program, Matrix3D v_matrix, GLAutoDrawable drawable)
	{	GL4 gl = (GL4) drawable.getGL();

		Material currentMaterial = new Material(); currentMaterial = thisMaterial;
		
		Point3D lightP = currentLight.getPosition();
		Point3D lightPv = lightP.mult(v_matrix);
		
		float [] currLightPos = new float[] { (float) lightPv.getX(),
		(float) lightPv.getY(),
		(float) lightPv.getZ() };

		// get the location of the global ambient light field in the shader
		int globalAmbLoc = gl.glGetUniformLocation(rendering_program, "globalAmbient");
	
		// set the current globalAmbient settings
		gl.glProgramUniform4fv(rendering_program, globalAmbLoc, 1, globalAmbient, 0);

		// get the locations of the light and material fields in the shader
		int ambLoc = gl.glGetUniformLocation(rendering_program, "light.ambient");
		int diffLoc = gl.glGetUniformLocation(rendering_program, "light.diffuse");
		int specLoc = gl.glGetUniformLocation(rendering_program, "light.specular");
		int posLoc = gl.glGetUniformLocation(rendering_program, "light.position");

		int MambLoc = gl.glGetUniformLocation(rendering_program, "material.ambient");
		int MdiffLoc = gl.glGetUniformLocation(rendering_program, "material.diffuse");
		int MspecLoc = gl.glGetUniformLocation(rendering_program, "material.specular");
		int MshiLoc = gl.glGetUniformLocation(rendering_program, "material.shininess");

		// set the uniform light and material values in the shader
		gl.glProgramUniform4fv(rendering_program, ambLoc, 1, currentLight.getAmbient(), 0);
		gl.glProgramUniform4fv(rendering_program, diffLoc, 1, currentLight.getDiffuse(), 0);
		gl.glProgramUniform4fv(rendering_program, specLoc, 1, currentLight.getSpecular(), 0);
		gl.glProgramUniform3fv(rendering_program, posLoc, 1, currLightPos, 0);
	
		gl.glProgramUniform4fv(rendering_program, MambLoc, 1, currentMaterial.getAmbient(), 0);
		gl.glProgramUniform4fv(rendering_program, MdiffLoc, 1, currentMaterial.getDiffuse(), 0);
		gl.glProgramUniform4fv(rendering_program, MspecLoc, 1, currentMaterial.getSpecular(), 0);
		gl.glProgramUniform1f(rendering_program, MshiLoc, currentMaterial.getShininess());
	}

	public static void main(String[] args) { new Code(); }

	@Override
	public void dispose(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) drawable.getGL();
		gl.glDeleteVertexArrays(1, vaoID, 0);
	}

//-----------------
	private void createShaderPrograms(GLAutoDrawable drawable)
	{	int[] vertCompiled = new int[1];
		int[] fragCompiled = new int[1];
		int[] linked = new int[1];
		int[] lengths;
		GL4 gl = (GL4) drawable.getGL();

		vBlinn1ShaderSource = util.readShaderSource("src/shadow/blinnVert1.shader");
		vBlinn2ShaderSource = util.readShaderSource("src/shadow/blinnVert2.shader");
		fBlinn2ShaderSource = util.readShaderSource("src/shadow/blinnFrag2.shader");

		int vertexShader1 = gl.glCreateShader(GL4.GL_VERTEX_SHADER);
		int vertexShader2 = gl.glCreateShader(GL4.GL_VERTEX_SHADER);
		int fragmentShader2 = gl.glCreateShader(GL4.GL_FRAGMENT_SHADER);

		System.out.println("\nLoading shader source into shader objects");
		lengths = new int[vBlinn1ShaderSource.length];
		for (int i = 0; i < lengths.length; i++)
		{	lengths[i] = vBlinn1ShaderSource[i].length(); }
		gl.glShaderSource(vertexShader1, vBlinn1ShaderSource.length, vBlinn1ShaderSource, lengths, 0);
		
		lengths = new int[vBlinn2ShaderSource.length];
		for (int i = 0; i < lengths.length; i++)
		{	lengths[i] = vBlinn2ShaderSource[i].length(); }
		gl.glShaderSource(vertexShader2, vBlinn2ShaderSource.length, vBlinn2ShaderSource, lengths, 0);
		
		lengths = new int[fBlinn2ShaderSource.length];
		for (int i = 0; i < lengths.length; i++)
		{	lengths[i] = fBlinn2ShaderSource[i].length(); }
		gl.glShaderSource(fragmentShader2, fBlinn2ShaderSource.length, fBlinn2ShaderSource, lengths, 0);

		gl.glCompileShader(vertexShader1);
		gl.glCompileShader(vertexShader2);
		gl.glCompileShader(fragmentShader2);

		rendering_program1 = gl.glCreateProgram();
		rendering_program2 = gl.glCreateProgram();

		gl.glAttachShader(rendering_program1, vertexShader1);
		gl.glAttachShader(rendering_program2, vertexShader2);
		gl.glAttachShader(rendering_program2, fragmentShader2);

		gl.glLinkProgram(rendering_program1);
		gl.glLinkProgram(rendering_program2);
	}

//------------------
	private Matrix3D perspective(float fovy, float aspect, float n, float f)
	{	float q = 1.0f / ((float) Math.tan(Math.toRadians(0.5f * fovy)));
		float A = q / aspect;
		float B = (n + f) / (n - f);
		float C = (2.0f * n * f) / (n - f);
		Matrix3D r = new Matrix3D();
		r.setElementAt(0,0,A);
		r.setElementAt(1,1,q);
		r.setElementAt(2,2,B);
		r.setElementAt(3,2,-1.0f);
		r.setElementAt(2,3,C);
		r.setElementAt(3,3,0.0f);
		return r;
	}

	private Matrix3D lookAt(graphicslib3D.Point3D eyeP, graphicslib3D.Point3D centerP, Vector3D upV)
	{	Vector3D eyeV = new Vector3D(eyeP);
		Vector3D cenV = new Vector3D(centerP);
		Vector3D f = (cenV.minus(eyeV)).normalize();
		Vector3D sV = (f.cross(upV)).normalize();
		Vector3D nU = (sV.cross(f)).normalize();

		Matrix3D l = new Matrix3D();
		l.setElementAt(0,0,sV.getX());l.setElementAt(0,1,nU.getX());l.setElementAt(0,2,-f.getX());l.setElementAt(0,3,0.0f);
		l.setElementAt(1,0,sV.getY());l.setElementAt(1,1,nU.getY());l.setElementAt(1,2,-f.getY());l.setElementAt(1,3,0.0f);
		l.setElementAt(2,0,sV.getZ());l.setElementAt(2,1,nU.getZ());l.setElementAt(2,2,-f.getZ());l.setElementAt(2,3,0.0f);
		l.setElementAt(3,0,sV.dot(eyeV.mult(-1)));
		l.setElementAt(3,1,nU.dot(eyeV.mult(-1)));
		l.setElementAt(3,2,(f.mult(-1)).dot(eyeV.mult(-1)));
		l.setElementAt(3,3,1.0f);
		return(l.transpose());
	}
}