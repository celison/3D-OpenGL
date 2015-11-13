package a1;

import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL.GL_VERSION;
import static com.jogamp.opengl.GL2ES3.GL_COLOR;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.nio.FloatBuffer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

import a1.command.MoveDownCommand;
import a1.command.MoveUpCommand;
import a1.command.ToggleColorCommand;
import a1.command.ZoomCommand;
import graphicslib3D.GLSLUtils;

public class TriangleFrame extends JFrame implements GLEventListener, ITriangleFrame {
	private GLCanvas myCanvas;
	private boolean isSolidColor = false;
	private int renderingProgram;
	private float triangleR = 1.0F;
	private float triangleG = 1.0F;
	private float triangleB = 1.0F;
	private float vertOffset = 0;
	private float zoom = 1;
	private int VAO[] = new int[1];
	private GLSLUtils util = new GLSLUtils();

	public TriangleFrame() {
		setTitle("Elison - Program 1");
		setSize(400, 200);
		myCanvas = new GLCanvas();
		myCanvas.addGLEventListener(this);
		myCanvas.addMouseWheelListener(ZoomCommand.getInstance());
		getContentPane().add(myCanvas);
		FPSAnimator animator = new FPSAnimator(myCanvas, 30);
		animator.start();
		add(createCommandPanel(), BorderLayout.SOUTH);
		setVisible(true);
	}

	private JPanel createCommandPanel() {
		initCommands();
		JPanel myCommandPanel = new JPanel();
		myCommandPanel.setLayout(new GridLayout(1,3));
		myCommandPanel.add(new JButton(MoveUpCommand.getInstance()));
		myCommandPanel.add(new JButton(MoveDownCommand.getInstance()));
		myCommandPanel.add(new JButton(ToggleColorCommand.getInstance()));
		return myCommandPanel;
	}
	
	private void initCommands() {
		ZoomCommand.setTarget(this);
		MoveUpCommand.setTarget(this);
		MoveDownCommand.setTarget(this);
		ToggleColorCommand.setTarget(this);
	}
	
	public void display(GLAutoDrawable drawable) {
		GL4 gl = (GL4) drawable.getGL();
		
		FloatBuffer color = FloatBuffer.allocate(4);
		color.put(0, 0.0f);
		color.put(1, 0.0f);
		color.put(2, 0.0f);
		color.put(3, 1.0f);
		
		gl.glClearBufferfv(GL_COLOR, 0, color);
		gl.glUseProgram(renderingProgram);
		
		FloatBuffer attrib = FloatBuffer.allocate(4);
		attrib.put(0, (float) (Math.sin(System.currentTimeMillis() / 600.0) * 1.0f));
		attrib.put(1, vertOffset);
		attrib.put(2, 0.0f);
		attrib.put(3, 0.0f);
		
		FloatBuffer size = FloatBuffer.allocate(4);
		size.put(0, zoom);
		size.put(1, zoom);
		size.put(2, 1.0f);
		size.put(3, 1.0f);
		
		FloatBuffer triangleColor = FloatBuffer.allocate(4);
		triangleColor.put(0, (isSolidColor) ? 1.0f : 0.0f);
		triangleColor.put(1, 0.0f);
		triangleColor.put(2, 0.0f);
		triangleColor.put(3, 1.0f);
		
		gl.glVertexAttrib4fv(1, attrib);
		gl.glVertexAttrib4fv(2, size);		
		gl.glVertexAttrib4fv(3, triangleColor);
		
		gl.glDrawArrays(GL_TRIANGLES, 0, 3);
	}

	public void init(GLAutoDrawable drawable) {
		GL4 gl = (GL4) drawable.getGL();
		System.out.println("JOGL VERSION: " + JoglVersion.getInstance().getImplementationVersion());
		System.out.println("OPEN GL VERSION: " + gl.glGetString(GL_VERSION));
		renderingProgram = createShaderPrograms(drawable);
		gl.glGenVertexArrays(VAO.length, VAO, 0);
		gl.glBindVertexArray(VAO[0]);
	}

	private int createShaderPrograms(GLAutoDrawable drawable) {
		GL4 gl = (GL4) drawable.getGL();
		int[] vertCompiled = new int[1];
		int[] fragCompiled = new int[1];
		int[] linked = new int[1];

		String vshaderSource[] = util.readShaderSource("src/a1/vert.shader");
		String fshaderSource[] = util.readShaderSource("src/a1/frag.shader");
		int lengths[];

		int vShader = gl.glCreateShader(GL4.GL_VERTEX_SHADER);
		int fShader = gl.glCreateShader(GL4.GL_FRAGMENT_SHADER);

		lengths = new int[vshaderSource.length];
		for (int i = 0; i < lengths.length; i++) {
			lengths[i] = vshaderSource[i].length();
		}
		gl.glShaderSource(vShader, vshaderSource.length, vshaderSource, lengths, 0);

		lengths = new int[fshaderSource.length];
		for (int i = 0; i < lengths.length; i++) {
			lengths[i] = fshaderSource[i].length();
		}
		gl.glShaderSource(fShader, fshaderSource.length, fshaderSource, lengths, 0);

		gl.glCompileShader(vShader);
		util.printOpenGLError(drawable);
		gl.glGetShaderiv(vShader, GL4.GL_COMPILE_STATUS, vertCompiled, 0);
		if (vertCompiled[0] == 1) {
			System.out.println("Vertex compilation success.");
		} else {
			System.out.println("Vertex compilation failed.");
		}
		
		gl.glCompileShader(fShader);
		util.printOpenGLError(drawable);
		gl.glGetShaderiv(fShader, GL4.GL_COMPILE_STATUS, fragCompiled, 0);
		if (fragCompiled[0] == 1) {
			System.out.println("Fragment compilation success.");
		} else {
			System.out.println("Fragment compilation failed.");
		}
		
		if ((vertCompiled[0] != 1) || (fragCompiled[0] != 1)){
			System.out.println("Compilation error; return-flags:");
			System.out.println(" vertCompiled = " + vertCompiled[0]
					+ " ; fragCompiled = " + fragCompiled[0]);
		} else { 
			System.out.println("Successful compilation.");
		}

		int vfprogram = gl.glCreateProgram();
		gl.glAttachShader(vfprogram, vShader);
		gl.glAttachShader(vfprogram, fShader);
		gl.glLinkProgram(vfprogram);
		util.printOpenGLError(drawable);
		gl.glGetProgramiv(vfprogram, GL4.GL_LINK_STATUS, linked,0);
		if (linked[0] == 1) {
			System.out.println("Linking suceeded.");
		} else {
			System.out.println("Linking failed.");
			util.printProgramInfoLog(drawable, vfprogram);
		}
		return vfprogram;
	}

	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
	}

	public void dispose(GLAutoDrawable drawable) {
	}
	
	public void toggleColors() {
		isSolidColor = !isSolidColor;
	}

	@Override
	public void moveUp() {
		vertOffset += 0.1F;
		
	}

	@Override
	public void moveDown() {
		vertOffset -= 0.1F;
		
	}

	@Override
	public void zoom(float amt) {
		zoom *= amt;
		
	}
}