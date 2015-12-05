package a4;

import a4.objects.WorldObject;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;
import graphicslib3D.*;
import graphicslib3D.light.PositionalLight;

import javax.swing.*;
import java.awt.event.*;
import java.nio.FloatBuffer;
import java.util.Vector;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES3.GL_COLOR;

/**
 * Created by Connor on 12/4/2015.
 */
public class ShadowFrame extends JFrame
        implements GLEventListener, KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {
    private GLCanvas myCanvas;
    private GLSLUtils util;

    private int[] vao;
    private int[] vbo;

    private int renderingProgram;
    private int axisRenderingProgram;

    private int startX, startY;

    private PositionalLight pl = new PositionalLight();

    private Matrix3D cameraTranslation;
    private Matrix3D cameraRotation;

    private Point3D plocation;

    private boolean showAxis = true;
    private boolean showPosLight = true;

    private Material currentMaterial;
    private MatrixStack mvStack;
    private float[] globalAmbient = new float[]{0.3f, 0.3f, 0.3f, 1.0f};

    private static final String FRAG_SOURCE = "src/a3/frag.shader";
    private static final String VERT_SOURCE = "src/a3/vert.shader";

    private static final String AXIS_FRAG_SOURCE = "src/a4/fragAxis.shader";
    private static final String AXIS_VERT_SOURCE = "src/a4/vertAxis.shader";

    private Vector<WorldObject> worldObjectList;

    // Constructor
    public ShadowFrame() {
        super();
        setTitle("Elison - Program 4: Shadows");
        setSize(1280, 980);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        util = new GLSLUtils();

        myCanvas = new GLCanvas();
        myCanvas.addGLEventListener(this);
        getContentPane().add(myCanvas);
        addKeyListener(this);
        myCanvas.addKeyListener(this);
        myCanvas.addMouseMotionListener(this);
        myCanvas.addMouseListener(this);
        myCanvas.addMouseWheelListener(this);

        worldObjectList = new Vector<>();

        FPSAnimator animator = new FPSAnimator(myCanvas, 30);
        animator.start();

        setVisible(true);
    }

    private void togglePosLighting() {
        if (showPosLight) { // lights are on so turn them off
            float[] amb = {0.0f,0.0f,0.0f,1.0f};
            pl.setAmbient(amb);
            float[] diff = {0.0f, 0.0f, 0.0f, 1.0f};
            pl.setDiffuse(diff);
            float[] spec = {0.0f, 0.0f, 0.0f, 1.0f};
            pl.setSpecular(spec);
        } else { // lights are off so turn them on
            float[] amb = {0.0f,0.0f,0.0f,1.0f};
            pl.setAmbient(amb);
            float[] diff = {1.0f, 1.0f, 1.0f, 1.0f};
            pl.setDiffuse(diff);
            float[] spec = {1.0f, 1.0f, 1.0f, 1.0f};
            pl.setSpecular(spec);
        }
        showPosLight = !showPosLight;
    }

    //Private Helper Methods

    private Matrix3D perspective(float fovy,
                                 float aspect, float n, float f) {
        float q = 1.0f / ((float) Math.tan
                (Math.toRadians(0.5f * fovy)));
        float A = q / aspect;
        float B = (n + f) / (n - f);
        float C = (2.0f * n * f) / (n - f);
        Matrix3D r = new Matrix3D();
        Matrix3D rt = new Matrix3D();
        r.setElementAt(0, 0, A);
        r.setElementAt(1, 1, q);
        r.setElementAt(2, 2, B);
        r.setElementAt(2, 3, -1.0f);
        r.setElementAt(3, 2, C);
        rt = r.transpose();
        return rt;
    }

    private int createShaderProgram(GLAutoDrawable drawable, String vertSource, String fragSource) {
        GL4 gl = (GL4) drawable.getGL();
        int[] vertCompiled = new int[1];
        int[] fragCompiled = new int[1];
        int[] linked = new int[1];

        System.out.println("Reading " + vertSource + " and " + fragSource);

        String vshaderSource[] = util.readShaderSource(vertSource);
        String fshaderSource[] = util.readShaderSource(fragSource);
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
        if (vertCompiled[0] == 1)
            System.out.println("Vertex compilation success.");
        else
            System.out.println("Vertex compilation failed.");

        gl.glCompileShader(fShader);
        gl.glGetShaderiv(fShader, GL4.GL_COMPILE_STATUS, fragCompiled, 0);
        if (fragCompiled[0] == 1)
            System.out.println("Fragment compilation success.");
        else
            System.out.println("Fragment compilation failed.");

        if ((vertCompiled[0] != 1) || (fragCompiled[0] != 1)) {
            System.out.println("Compilation error; return flags:");
            System.out.println("vertCompiled = " + vertCompiled[0] + " ; fragCompiled = " + fragCompiled[0]);
        } else
            System.out.println("Successful compilation.");

        int vfprogram = gl.glCreateProgram();
        gl.glAttachShader(vfprogram, vShader);
        gl.glAttachShader(vfprogram, fShader);
        gl.glLinkProgram(vfprogram);
        util.printOpenGLError(drawable);
        gl.glGetProgramiv(vfprogram, GL4.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 1)
            System.out.println("Linking success.");
        else {
            System.out.println("Linking failed.");
            util.printProgramInfoLog(drawable, vfprogram);
        }
        return vfprogram;
    }

    private void setupVerticies(GL4 gl) {
        int index = 0;

        for (WorldObject worldObject : worldObjectList) {
            worldObject.setupBuffers(gl, vbo, index);
            index += 3;
        }
    }

    private void installLights(Matrix3D v_matrix, GLAutoDrawable drawable) {
        GL4 gl = (GL4) drawable.getGL();

        // get light position

        Point3D lightPv = plocation.mult(v_matrix);
        float[] currLightPos = new float[]{(float) lightPv.getX(), (float) lightPv.getY(), (float) lightPv.getZ()};

        int globalAmbLoc = gl.glGetUniformLocation(renderingProgram, "globalAmbient");
        gl.glProgramUniform4fv(renderingProgram, globalAmbLoc, 1, globalAmbient, 0);

        // get the locations of the light and material fields in the shader
        int ambLoc = gl.glGetUniformLocation(renderingProgram, "light.ambient");
        int diffLoc = gl.glGetUniformLocation(renderingProgram, "light.diffuse");
        int specLoc = gl.glGetUniformLocation(renderingProgram, "light.specular");
        int posLoc = gl.glGetUniformLocation(renderingProgram, "light.position");
        int MambLoc = gl.glGetUniformLocation(renderingProgram, "material.ambient");
        int MdiffLoc = gl.glGetUniformLocation(renderingProgram, "material.diffuse");
        int MspecLoc = gl.glGetUniformLocation(renderingProgram, "material.specular");
        int MshiLoc = gl.glGetUniformLocation(renderingProgram, "material.shininess");

        // set the uniform light and material values in the shader
        gl.glProgramUniform4fv(renderingProgram, ambLoc, 1, pl.getAmbient(), 0);
        gl.glProgramUniform4fv(renderingProgram, diffLoc, 1, pl.getDiffuse(), 0);
        gl.glProgramUniform4fv(renderingProgram, specLoc, 1, pl.getSpecular(), 0);
        gl.glProgramUniform3fv(renderingProgram, posLoc, 1, currLightPos, 0);
        gl.glProgramUniform4fv(renderingProgram, MambLoc, 1, currentMaterial.getAmbient(), 0);
        gl.glProgramUniform4fv(renderingProgram, MdiffLoc, 1, currentMaterial.getDiffuse(), 0);
        gl.glProgramUniform4fv(renderingProgram, MspecLoc, 1, currentMaterial.getSpecular(), 0);
        gl.glProgramUniform1f(renderingProgram, MshiLoc, currentMaterial.getShininess());
    }

    //Overloaded Methods

    @Override
    public void init(GLAutoDrawable drawable) {
        GL4 gl = (GL4) drawable.getGL();
        System.out.println("JOGL VERSION: " + JoglVersion.getInstance().getImplementationVersion());
        System.out.println("OPEN GL VERSION: " + gl.glGetString(GL_VERSION));

        vbo = new int[9 + 3 * worldObjectList.size()];

        // create shader programs

        cameraTranslation = new Matrix3D();
        cameraRotation = new Matrix3D();

        renderingProgram = createShaderProgram(drawable, VERT_SOURCE, FRAG_SOURCE);
        axisRenderingProgram = createShaderProgram(drawable, AXIS_VERT_SOURCE, AXIS_FRAG_SOURCE);
        //IdentityLocs.init(renderingProgram, gl);
        setupVerticies(gl);

        // instantiate positional light and set location (using default amb, diff, spec settings)

        pl = new PositionalLight();
        plocation = new Point3D(2, 2, 2);
        pl.setPosition(plocation);
        mvStack = new MatrixStack(20);

        // init world objects

        for (WorldObject worldObject : worldObjectList) {
            worldObject.init(drawable);
        }

        // set camera position

        float cameraX = 0.0f;
        float cameraY = -0.5f;
        float cameraZ = 8.0f;
        cameraTranslation.translate(cameraX, cameraY, -cameraZ);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {

    }

    @Override
    public void display(GLAutoDrawable drawable) {GL4 gl = (GL4) drawable.getGL();

        //Clean Background
        gl.glClear(GL_DEPTH_BUFFER_BIT);
        FloatBuffer background = FloatBuffer.allocate(4);
        gl.glClearBufferfv(GL_COLOR, 0, background);

        // setup projection Matrix
        float aspect = myCanvas.getWidth() / myCanvas.getHeight();
        Matrix3D pMat = perspective(50.0f, aspect, 0.1f, 1000.0f);

        MatrixStack mvStack = new MatrixStack(20);
        // setup view matrix
        mvStack.pushMatrix();
        mvStack.multMatrix(cameraRotation);
        mvStack.multMatrix(cameraTranslation);

        int mvLoc;
        int projLoc;

        if (showAxis) {
            gl.glUseProgram(axisRenderingProgram);
            mvLoc = gl.glGetUniformLocation(axisRenderingProgram, "mv_matrix");
            projLoc = gl.glGetUniformLocation(axisRenderingProgram, "proj_matrix");

            gl.glUniformMatrix4fv(mvLoc, 1, false, mvStack.peek().getFloatValues(), 0);
            gl.glUniformMatrix4fv(projLoc, 1, false, pMat.getFloatValues(), 0);

            gl.glDrawArrays(GL_LINES, 0, 6);
        }


        // Draw Light Source

//        if (showPosLight) {
//            gl.glUseProgram(lightPointRenderingProgram);
//            mvLoc = gl.glGetUniformLocation(lightPointRenderingProgram, "mv_matrix");
//            projLoc = gl.glGetUniformLocation(lightPointRenderingProgram, "proj_matrix");
//
//            pl.setPosition(plocation);
//
//            // draw small cube to represent positional lighting
//
//            mvStack.pushMatrix();
//
//            mvStack.translate(plocation.getX(), plocation.getY(), plocation.getZ());
//            mvStack.scale(.1, .1, .1);
//
//            gl.glUniformMatrix4fv(mvLoc, 1, false, mvStack.peek().getFloatValues(), 0);
//            gl.glUniformMatrix4fv(projLoc, 1, false, pMat.getFloatValues(), 0);
//
//            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
//            gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);
//            gl.glEnableVertexAttribArray(0);
//
//            gl.glEnable(GL_CULL_FACE);
//            gl.glFrontFace(GL_CW);
//            gl.glEnable(GL_DEPTH_TEST);
//            gl.glDepthFunc(GL_LEQUAL);
//
//            gl.glDrawArrays(GL_TRIANGLES, 0, 36);
//            mvStack.popMatrix();
//        }

        for (WorldObject worldObject : worldObjectList) {
            currentMaterial = worldObject.getMaterial();
            installLights(mvStack.peek(), drawable);
            worldObject.draw(drawable, mvStack, pMat);
        }
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int i, int i1, int i2, int i3) {

    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W: // + N
                cameraTranslation.translate(.1 * cameraRotation.elementAt(2, 0), .1 * cameraRotation.elementAt(2, 1), .1 * cameraRotation.elementAt(2, 2));
                break;
            case KeyEvent.VK_S: // - N
                cameraTranslation.translate(-.1 * cameraRotation.elementAt(2, 0), -.1 * cameraRotation.elementAt(2, 1), -.1 * cameraRotation.elementAt(2, 2));
                break;
            case KeyEvent.VK_Q: // + V
                cameraTranslation.translate(.1 * cameraRotation.elementAt(1, 0), .1 * cameraRotation.elementAt(1, 1), .1 * cameraRotation.elementAt(1, 2));
                break;
            case KeyEvent.VK_E: // - V
                cameraTranslation.translate(-.1 * cameraRotation.elementAt(1, 0), -.1 * cameraRotation.elementAt(1, 1), -.1 * cameraRotation.elementAt(1, 2));
                break;
            case KeyEvent.VK_A: // + U
                cameraTranslation.translate(.1 * cameraRotation.elementAt(0, 0), .1 * cameraRotation.elementAt(0, 1), .1 * cameraRotation.elementAt(0, 2));
                break;
            case KeyEvent.VK_D: // - U
                cameraTranslation.translate(-.1 * cameraRotation.elementAt(0, 0), -.1 * cameraRotation.elementAt(0, 1), -.1 * cameraRotation.elementAt(0, 2));
                break;
            case KeyEvent.VK_LEFT: // rotate around V axis
                cameraRotation.rotate(-1, new Vector3D(cameraRotation.elementAt(1, 0), cameraRotation.elementAt(1, 1), cameraRotation.elementAt(1, 2)));
                break;
            case KeyEvent.VK_RIGHT: // rotate around V axis
                cameraRotation.rotate(1, new Vector3D(cameraRotation.elementAt(1, 0), cameraRotation.elementAt(1, 1), cameraRotation.elementAt(1, 2)));
                break;
            case KeyEvent.VK_UP: // rotate around U axis
                cameraRotation.rotate(-1, new Vector3D(cameraRotation.elementAt(0, 0), cameraRotation.elementAt(0, 1), cameraRotation.elementAt(0, 2)));
                break;
            case KeyEvent.VK_DOWN: // rotate around U axis
                cameraRotation.rotate(1, new Vector3D(cameraRotation.elementAt(0, 0), cameraRotation.elementAt(0, 1), cameraRotation.elementAt(0, 2)));
                break;
            case KeyEvent.VK_Z: // rotate around N axis
                cameraRotation.rotate(-1, new Vector3D(cameraRotation.elementAt(2, 0), cameraRotation.elementAt(2, 1), cameraRotation.elementAt(2, 2)));
                break;
            case KeyEvent.VK_C: // rotate around N axis
                cameraRotation.rotate(1, new Vector3D(cameraRotation.elementAt(2, 0), cameraRotation.elementAt(2, 1), cameraRotation.elementAt(2, 2)));
                break;
            case KeyEvent.VK_SPACE:
                showAxis = !(showAxis);
                break;
            case KeyEvent.VK_L:
                togglePosLighting();
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (showPosLight) {
            plocation.setX(plocation.getX() + (-startX + e.getX()) * .01);
            plocation.setY(plocation.getY() + (startY - e.getY()) * .01);
        }
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

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (showPosLight) {
            if (e.getWheelRotation() > 0) {
                plocation.setZ(plocation.getZ() + e.getScrollAmount() * .1);
            } else {
                plocation.setZ(plocation.getZ() - e.getScrollAmount() * .1);
            }
        }
    }
}
