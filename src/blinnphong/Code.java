package blinnphong;

import graphicslib3D.*;
import graphicslib3D.GLSLUtils.*;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.nio.*;
import javax.swing.*;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;
import graphicslib3D.shape.*;

import graphicslib3D.light.*;

public class Code extends JFrame implements GLEventListener, MouseMotionListener, MouseListener
{	private GLCanvas myCanvas;
	private int rendering_program;
	private int vao[] = new int[1];
	private int vbo[] = new int[2];
	private Point3D torusLoc = new Point3D(0,0,-1);
	private Point3D cameraLoc = new Point3D(0,0,1);
	private float torLocX, torLocY, torLocZ;
	private GLSLUtils util = new GLSLUtils();
	
	private Torus myTorus = new Torus(0.5f, 0.2f, 48);
	private int numTorusVertices;
	private Matrix3D m_matrix = new Matrix3D();
	private Matrix3D v_matrix = new Matrix3D();
	private Matrix3D mv_matrix = new Matrix3D();
	private Matrix3D proj_matrix = new Matrix3D();

	float startX, startY;

	Material thisMaterial = Material.GOLD;
	private PositionalLight currentLight = new PositionalLight();
	private Point3D lightLoc = new Point3D(5.0f, 2.0f, 2.0f);
	float [] globalAmbient = new float[] { 0.7f, 0.7f, 0.7f, 1.0f };

	public Code()
	{	setTitle("Chapter7 - program3");
		setSize(600, 600);
		myCanvas = new GLCanvas();
		myCanvas.addGLEventListener(this);

		myCanvas.addMouseListener(this);
		myCanvas.addMouseMotionListener(this);

		getContentPane().add(myCanvas);
		FPSAnimator animator = new FPSAnimator(myCanvas, 30);
        animator.start();
		setVisible(true);
	}

	public void display(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) drawable.getGL();

		gl.glUseProgram(rendering_program);

		int mv_location = gl.glGetUniformLocation(rendering_program, "mv_matrix");
		int proj_location = gl.glGetUniformLocation(rendering_program, "proj_matrix");
		int n_location = gl.glGetUniformLocation(rendering_program, "normalMat");

		float aspect = myCanvas.getWidth() / myCanvas.getHeight();
		proj_matrix = perspective(50.0f, aspect, 0.1f, 1000.0f);
		
		m_matrix.setToIdentity();
		m_matrix.translate(torusLoc.getX(), torusLoc.getY(), torusLoc.getZ());
		m_matrix.rotateX(35.0f);

		v_matrix.setToIdentity();
		v_matrix.translate(-cameraLoc.getX(),-cameraLoc.getY(),-cameraLoc.getZ());
		currentLight.setPosition(lightLoc);
		installLights(v_matrix, drawable);

		mv_matrix.setToIdentity();
		mv_matrix.concatenate(v_matrix);
		mv_matrix.concatenate(m_matrix);

		gl.glUniformMatrix4fv(mv_location, 1, false, mv_matrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(proj_location, 1, false, proj_matrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(n_location, 1, false, (mv_matrix.inverse()).transpose().getFloatValues(),0);

		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(1, 3, GL.GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, numTorusVertices);
	}

	public void init(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) drawable.getGL();
		rendering_program = createShaderPrograms(drawable);
		setupVertices(gl);
	}
	
	private void installLights(Matrix3D v_matrix, GLAutoDrawable drawable)
	{	GL4 gl = (GL4) drawable.getGL();

		Material currentMaterial = thisMaterial;
		
		Point3D lightP = currentLight.getPosition();
		Point3D lightPv = lightP.mult(v_matrix);
		float [] currLightPos = new float[] { (float) lightPv.getX(), (float) lightPv.getY(), (float) lightPv.getZ() };

		// set the current globalAmbient settings
		int globalAmbLoc = gl.glGetUniformLocation(rendering_program, "globalAmbient");
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
	
		//  set the uniform light and material values in the shader
		gl.glProgramUniform4fv(rendering_program, ambLoc, 1, currentLight.getAmbient(), 0);
		gl.glProgramUniform4fv(rendering_program, diffLoc, 1, currentLight.getDiffuse(), 0);
		gl.glProgramUniform4fv(rendering_program, specLoc, 1, currentLight.getSpecular(), 0);
		gl.glProgramUniform3fv(rendering_program, posLoc, 1, currLightPos, 0);
		gl.glProgramUniform4fv(rendering_program, MambLoc, 1, currentMaterial.getAmbient(), 0);
		gl.glProgramUniform4fv(rendering_program, MdiffLoc, 1, currentMaterial.getDiffuse(), 0);
		gl.glProgramUniform4fv(rendering_program, MspecLoc, 1, currentMaterial.getSpecular(), 0);
		gl.glProgramUniform1f(rendering_program, MshiLoc, currentMaterial.getShininess());
	}

	private void setupVertices(GL4 gl)
	{	Vertex3D[] vertices = myTorus.getVertices();
		int[] indices = myTorus.getIndices();
		
		float[] fvalues = new float[indices.length*3];
		float[] tvalues = new float[indices.length*2];
		float[] nvalues = new float[indices.length*3];
		
		for (int i=0; i<indices.length; i++)
		{	fvalues[i*3] = (float) (vertices[indices[i]]).getX();
			fvalues[i*3+1] = (float) (vertices[indices[i]]).getY();
			fvalues[i*3+2] = (float) (vertices[indices[i]]).getZ();
			tvalues[i*2] = (float) (vertices[indices[i]]).getS();
			tvalues[i*2+1] = (float) (vertices[indices[i]]).getT();
			nvalues[i*3] = (float) (vertices[indices[i]]).getNormalX();
			nvalues[i*3+1]= (float)(vertices[indices[i]]).getNormalY();
			nvalues[i*3+2]=(float) (vertices[indices[i]]).getNormalZ();
		}
		
		numTorusVertices = indices.length;
		
		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
		gl.glGenBuffers(vbo.length, vbo, 0);
		
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
		FloatBuffer vertBuf = FloatBuffer.wrap(fvalues);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL.GL_STATIC_DRAW);

		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[1]);
		FloatBuffer norBuf = FloatBuffer.wrap(nvalues);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, norBuf.limit()*4,norBuf, GL.GL_STATIC_DRAW);
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

	public static void main(String[] args) { new Code(); }
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
	public void dispose(GLAutoDrawable drawable) {}

	private int createShaderPrograms(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) drawable.getGL();

		String vshaderSource[] = util.readShaderSource("src/blinnphong/vert.shader");
		String fshaderSource[] = util.readShaderSource("src/blinnphong/frag.shader");
		int lengths[];

		int vShader = gl.glCreateShader(GL4.GL_VERTEX_SHADER);
		int fShader = gl.glCreateShader(GL4.GL_FRAGMENT_SHADER);

		lengths = new int[vshaderSource.length];
		for (int i = 0; i < lengths.length; i++)
		{	lengths[i] = vshaderSource[i].length();
		}
		gl.glShaderSource(vShader, vshaderSource.length, vshaderSource, lengths, 0);

		lengths = new int[fshaderSource.length];
		for (int i = 0; i < lengths.length; i++)
		{	lengths[i] = fshaderSource[i].length();
		}
		gl.glShaderSource(fShader, fshaderSource.length, fshaderSource, lengths, 0);

		gl.glCompileShader(vShader);
		gl.glCompileShader(fShader);

		int vfprogram = gl.glCreateProgram();
		gl.glAttachShader(vfprogram, vShader);
		gl.glAttachShader(vfprogram, fShader);
		gl.glLinkProgram(vfprogram);
		return vfprogram;
	}
	@Override
	public void mouseDragged(MouseEvent e) {
		lightLoc.setX(lightLoc.getX() + (-startX + e.getX()) * .01);
		lightLoc.setY(lightLoc.getY() + (startY - e.getY()) * .01);
//        System.out.printf("StartX %d, StartY %d, newX %d, newY %d\n" , startX, startY, e.getX(), e.getY());
		startX = e.getX();
		startY = e.getY();
	}

	@Override
	public void mouseMoved(MouseEvent e) {

	}

	@Override
	public void mouseClicked(MouseEvent e) {

	}

	@Override
	public void mousePressed(MouseEvent e) {
		startX = e.getX();
		startY = e.getY();
	}

	@Override
	public void mouseReleased(MouseEvent e) {

	}

	@Override
	public void mouseEntered(MouseEvent e) {

	}

	@Override
	public void mouseExited(MouseEvent e) {

	}
}