package tessellator;

import graphicslib3D.*;
import graphicslib3D.GLSLUtils.*;
import java.nio.*;
import javax.swing.*;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import graphicslib3D.shape.*;
import graphicslib3D.light.*;
import com.jogamp.opengl.util.*;

public class Code extends JFrame implements GLEventListener
{	private GLCanvas myCanvas;
	private GLSLUtils util = new GLSLUtils();
	private int rendering_program;
	private int vao[] = new int[1];
	
	Point3D terLoc = new Point3D(0,0,0);
	Point3D cameraLoc = new Point3D(-2.0, 3.0, 6.0);
	
	Matrix3D proj_matrix = new Matrix3D();
	Matrix3D m_matrix = new Matrix3D();
	Matrix3D v_matrix = new Matrix3D();
	Matrix3D mvp_matrix = new Matrix3D();
	Matrix3D mv_matrix = new Matrix3D();
	
	float tessInner = 30.0f;
	float tessOuter = 20.0f;
	
	private TextureReader tr = new TextureReader();
	private int textureID0, textureID1, textureID2;
	
	private PositionalLight currentLight = new PositionalLight();
	private Point3D lightLoc = new Point3D(-2.0f, 1.5f, 10.0f);
	float [] globalAmbient = new float[] { 0.7f, 0.7f, 0.7f, 1.0f };
	Material thisMaterial = Material.SILVER;
	
	float lightMovement = 1.0f;

	public Code()
	{	setTitle("Chapter 12 - program 4b");
		setSize(600, 600);
		myCanvas = new GLCanvas();
		myCanvas.addGLEventListener(this);
		getContentPane().add(myCanvas);
		this.setVisible(true);
		FPSAnimator animator = new FPSAnimator(myCanvas, 30);
		animator.start();
	}

	public void display(GLAutoDrawable drawable)
	{
		GL4 gl = (GL4) drawable.getGL();
		gl.glUseProgram(rendering_program);
		
		lightLoc.setX(lightLoc.getX() + lightMovement * 0.2f);
		if (lightLoc.getX() > 20.0) lightMovement = -1.0f;
		if (lightLoc.getX() < -20.0) lightMovement = 1.0f;
		
		currentLight.setPosition(lightLoc);
		
		int mvp_location = gl.glGetUniformLocation(rendering_program, "mvp");
		int mv_location = gl.glGetUniformLocation(rendering_program, "mv_matrix");
		int proj_location = gl.glGetUniformLocation(rendering_program, "proj_matrix");
		int n_location = gl.glGetUniformLocation(rendering_program, "normalMat");
		
		float aspect = myCanvas.getWidth() / myCanvas.getHeight();
		proj_matrix = perspective(50.0f, aspect, 0.1f, 1000.0f);
	
		m_matrix.setToIdentity();
		m_matrix.translate(terLoc.getX(),terLoc.getY(),terLoc.getZ());
		m_matrix.rotateX(20.0f);
		
		v_matrix.setToIdentity();
		v_matrix.translate(-cameraLoc.getX(),-cameraLoc.getY(),-cameraLoc.getZ());

		mv_matrix.setToIdentity();
		mv_matrix.concatenate(v_matrix);
		mv_matrix.concatenate(m_matrix);
		
		mvp_matrix.setToIdentity();
		mvp_matrix.concatenate(proj_matrix);
		mvp_matrix.concatenate(v_matrix);
		mvp_matrix.concatenate(m_matrix);
		
		installLights(v_matrix, drawable);
		
		gl.glUniformMatrix4fv(mvp_location, 1, false, mvp_matrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(mv_location, 1, false, mv_matrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(proj_location, 1, false, proj_matrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(n_location, 1, false, (mv_matrix.inverse()).transpose().getFloatValues(), 0);
		
		gl.glActiveTexture(gl.GL_TEXTURE0);
		gl.glBindTexture(gl.GL_TEXTURE_2D, textureID0);
		gl.glActiveTexture(gl.GL_TEXTURE1);
		gl.glBindTexture(gl.GL_TEXTURE_2D, textureID1);
		gl.glActiveTexture(gl.GL_TEXTURE2);
		gl.glBindTexture(gl.GL_TEXTURE_2D, textureID2);
		
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glFrontFace(GL_CCW);

		gl.glPatchParameteri(GL_PATCH_VERTICES, 4);
		gl.glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
		gl.glDrawArraysInstanced(GL_PATCHES, 0, 4, 64*64);
	}

	public void init(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) drawable.getGL();
		rendering_program = createShaderPrograms(drawable);

		textureID0 = tr.loadTexture(drawable, "src/tessellator/moon.jpg");
		textureID1 = tr.loadTexture(drawable, "src/tessellator/height.jpg");
		textureID2 = tr.loadTexture(drawable, "src/tessellator/normal.jpg");

		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
	}

//--------------------------------------------------------------------------------------------
	
	public static void main(String[] args) { new Code(); }
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
	public void dispose(GLAutoDrawable drawable) {}
	
//-----------------

	private int createShaderPrograms(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) drawable.getGL();

		String vshaderSource[]  = util.readShaderSource("src/tessellator/vert.shader");
		String tcshaderSource[] = util.readShaderSource("src/tessellator/tessC.shader");
		String teshaderSource[] = util.readShaderSource("src/tessellator/tessE.shader");
		String fshaderSource[]  = util.readShaderSource("src/tessellator/frag.shader");
		int lengths[];

		int vShader  = gl.glCreateShader(GL4.GL_VERTEX_SHADER);
		int tcShader = gl.glCreateShader(GL4.GL_TESS_CONTROL_SHADER);
		int teShader = gl.glCreateShader(GL4.GL_TESS_EVALUATION_SHADER);
		int fShader  = gl.glCreateShader(GL4.GL_FRAGMENT_SHADER);

		lengths = new int[vshaderSource.length];
		for (int i = 0; i < lengths.length; i++)
		{	lengths[i] = vshaderSource[i].length();
		}
		gl.glShaderSource(vShader, vshaderSource.length, vshaderSource, lengths, 0);
		
		lengths = new int[tcshaderSource.length];
		for (int i = 0; i < lengths.length; i++)
		{	lengths[i] = tcshaderSource[i].length();
		}
		gl.glShaderSource(tcShader, tcshaderSource.length, tcshaderSource, lengths, 0);
		
		lengths = new int[teshaderSource.length];
		for (int i = 0; i < lengths.length; i++)
		{	lengths[i] = teshaderSource[i].length();
		}
		gl.glShaderSource(teShader, teshaderSource.length, teshaderSource, lengths, 0);

		lengths = new int[fshaderSource.length];
		for (int i = 0; i < lengths.length; i++)
		{	lengths[i] = fshaderSource[i].length();
		}
		gl.glShaderSource(fShader, fshaderSource.length, fshaderSource, lengths, 0);

		gl.glCompileShader(vShader);
		gl.glCompileShader(tcShader);
		gl.glCompileShader(teShader);
		gl.glCompileShader(fShader);

		int vtfprogram = gl.glCreateProgram();
		gl.glAttachShader(vtfprogram, vShader);
		gl.glAttachShader(vtfprogram, tcShader);
		gl.glAttachShader(vtfprogram, teShader);
		gl.glAttachShader(vtfprogram, fShader);
		gl.glLinkProgram(vtfprogram);
		return vtfprogram;
	}

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
	
	private void installLights(Matrix3D v_matrix, GLAutoDrawable drawable)
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
}