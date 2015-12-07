package a4;

import a3.shapes.Sphere;
import a4.objects.Ball;
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

import static com.jogamp.opengl.GL.GL_CLAMP_TO_EDGE;
import static com.jogamp.opengl.GL.GL_CULL_FACE;
import static com.jogamp.opengl.GL.GL_CW;
import static com.jogamp.opengl.GL.GL_DEPTH_ATTACHMENT;
import static com.jogamp.opengl.GL.GL_DEPTH_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_DEPTH_COMPONENT32;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_FRAMEBUFFER;
import static com.jogamp.opengl.GL.GL_LEQUAL;
import static com.jogamp.opengl.GL.GL_LINEAR;
import static com.jogamp.opengl.GL.GL_LINES;
import static com.jogamp.opengl.GL.GL_POLYGON_OFFSET_FILL;
import static com.jogamp.opengl.GL.GL_TEXTURE_2D;
import static com.jogamp.opengl.GL.GL_TEXTURE_MAG_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_MIN_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_S;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_T;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL.GL_VERSION;
import static com.jogamp.opengl.GL2ES2.*;
import static com.jogamp.opengl.GL2ES3.GL_COLOR;

/**
 * Created by Connor on 12/4/2015.
 */
public class ShadowFrame extends JFrame
        implements GLEventListener, KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {
    private GLCanvas myCanvas;
    private GLSLUtils util;

    private int[] vao = new int[1];
    private int[] vbo;

    private int renderingProgram1;
    private int renderingProgram2;
    private int lightPointRenderingProgram;
    private int axisRenderingProgram;

    private int startX, startY;

    private PositionalLight pl = new PositionalLight();

    private Matrix3D cameraTranslation;
    private Matrix3D cameraRotation;
    private Matrix3D v_mat = new Matrix3D();

    private Point3D plocation = new Point3D();

    private boolean showAxis = true;
    private boolean showPosLight = true;

    private Material currentMaterial;
    private MatrixStack mvStack;
    private float[] globalAmbient = new float[]{0.3f, 0.3f, 0.3f, 1.0f};

    //shadow stuff
    private int scSizeX, scSizeY;
    private int[] shadow_tex = new int[1];
    private int[] shadow_buffer = new int[1];
    private Matrix3D b = new Matrix3D();

    private Sphere mySphere;


    private static final String FIRST_FRAG_SOURCE = "src/a4/blinnFrag1.shader";
    private static final String FIRST_VERT_SOURCE = "src/a4/blinnVert1.shader";

    private static final String SECOND_FRAG_SOURCE = "src/a4/blinnFrag2.shader";
    private static final String SECOND_VERT_SOURCE = "src/a4/blinnVert2.shader";

    private static final String AXIS_FRAG_SOURCE = "src/a4/fragAxis.shader";
    private static final String AXIS_VERT_SOURCE = "src/a4/vertAxis.shader";

    private static final String LIGHT_FRAG_SOURCE = "src/a4/fragPoint.shader";
    private static final String LIGHT_VERT_SOURCE = "src/a4/vertPoint.shader";

    private Vector<WorldObject> worldObjectList;

    // Constructor
    public ShadowFrame() {
        super();
        setTitle("Elison - Program 4: Shadows");
        setSize(750, 750);
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
        for (int i = 0; i < 27; i++) {
            Ball ball = new Ball();
            int x = i % 3;
            int y = (i / 3) % 3;
            int z = (i / 9) % 3;
            System.out.println(i + ": " + x + ", " + y + ", " + z);
            ball.translate(x, y, z);
            worldObjectList.add(ball);
        }

        FPSAnimator animator = new FPSAnimator(myCanvas, 30);
        animator.start();

        setVisible(true);
    }

    private void togglePosLighting() {
        if (showPosLight) { // lights are on so turn them off
            float[] amb = {0.0f, 0.0f, 0.0f, 1.0f};
            pl.setAmbient(amb);
            float[] diff = {0.0f, 0.0f, 0.0f, 1.0f};
            pl.setDiffuse(diff);
            float[] spec = {0.0f, 0.0f, 0.0f, 1.0f};
            pl.setSpecular(spec);
        } else { // lights are off so turn them on
            float[] amb = {0.0f, 0.0f, 0.0f, 1.0f};
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
        // get sphere vertex, texture, and normal values
        mySphere = new Sphere(48);
        Vertex3D[] vertices = mySphere.getVertices();
        int[] indices = mySphere.getIndices();

        float[] fvalues = new float[indices.length * 3];
        float[] tvalues = new float[indices.length * 2];
        float[] nvalues = new float[indices.length * 3];

        for (int i = 0; i < indices.length; i++) {
            fvalues[i * 3] = (float) (vertices[indices[i]]).getX();
            fvalues[i * 3 + 1] = (float) (vertices[indices[i]]).getY();
            fvalues[i * 3 + 2] = (float) (vertices[indices[i]]).getZ();
            tvalues[i * 2] = (float) (vertices[indices[i]]).getS();
            tvalues[i * 2 + 1] = (float) (vertices[indices[i]]).getT();
            nvalues[i * 3] = (float) (vertices[indices[i]]).getNormalX();
            nvalues[i * 3 + 1] = (float) (vertices[indices[i]]).getNormalY();
            nvalues[i * 3 + 2] = (float) (vertices[indices[i]]).getNormalZ();
        }

        gl.glGenVertexArrays(vao.length, vao, 0);
        gl.glBindVertexArray(vao[0]);

        gl.glGenBuffers(vbo.length, vbo, 0);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[index++]);
        FloatBuffer vertBuf = FloatBuffer.wrap(fvalues);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, vertBuf.limit() * 4, vertBuf, GL.GL_STATIC_DRAW);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[index++]);
        FloatBuffer texBuf = FloatBuffer.wrap(tvalues);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, texBuf.limit() * 4, texBuf, GL.GL_STATIC_DRAW);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[index++]);
        FloatBuffer norBuf = FloatBuffer.wrap(nvalues);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, norBuf.limit() * 4, norBuf, GL.GL_STATIC_DRAW);

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

        int globalAmbLoc = gl.glGetUniformLocation(renderingProgram2, "globalAmbient");
        gl.glProgramUniform4fv(renderingProgram2, globalAmbLoc, 1, globalAmbient, 0);

        // get the locations of the light and material fields in the shader
        int ambLoc = gl.glGetUniformLocation(renderingProgram2, "light.ambient");
        int diffLoc = gl.glGetUniformLocation(renderingProgram2, "light.diffuse");
        int specLoc = gl.glGetUniformLocation(renderingProgram2, "light.specular");
        int posLoc = gl.glGetUniformLocation(renderingProgram2, "light.position");
        int MambLoc = gl.glGetUniformLocation(renderingProgram2, "material.ambient");
        int MdiffLoc = gl.glGetUniformLocation(renderingProgram2, "material.diffuse");
        int MspecLoc = gl.glGetUniformLocation(renderingProgram2, "material.specular");
        int MshiLoc = gl.glGetUniformLocation(renderingProgram2, "material.shininess");

        // set the uniform light and material values in the shader
        gl.glProgramUniform4fv(renderingProgram2, ambLoc, 1, pl.getAmbient(), 0);
        gl.glProgramUniform4fv(renderingProgram2, diffLoc, 1, pl.getDiffuse(), 0);
        gl.glProgramUniform4fv(renderingProgram2, specLoc, 1, pl.getSpecular(), 0);
        gl.glProgramUniform3fv(renderingProgram2, posLoc, 1, currLightPos, 0);
        gl.glProgramUniform4fv(renderingProgram2, MambLoc, 1, currentMaterial.getAmbient(), 0);
        gl.glProgramUniform4fv(renderingProgram2, MdiffLoc, 1, currentMaterial.getDiffuse(), 0);
        gl.glProgramUniform4fv(renderingProgram2, MspecLoc, 1, currentMaterial.getSpecular(), 0);
        gl.glProgramUniform1f(renderingProgram2, MshiLoc, currentMaterial.getShininess());
    }

    public void setupShadowBuffers(GLAutoDrawable drawable) {
        GL4 gl = (GL4) drawable.getGL();

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

    //Overloaded Methods

    @Override
    public void init(GLAutoDrawable drawable) {
        GL4 gl = (GL4) drawable.getGL();
        System.out.println("JOGL VERSION: " + JoglVersion.getInstance().getImplementationVersion());
        System.out.println("OPEN GL VERSION: " + gl.glGetString(GL_VERSION));

        vbo = new int[3 + (3 * worldObjectList.size())];

        // create shader programs
        renderingProgram1 = createShaderProgram(drawable, FIRST_VERT_SOURCE, FIRST_FRAG_SOURCE);
        renderingProgram2 = createShaderProgram(drawable, SECOND_VERT_SOURCE, SECOND_FRAG_SOURCE);
        axisRenderingProgram = createShaderProgram(drawable, AXIS_VERT_SOURCE, AXIS_FRAG_SOURCE);
        lightPointRenderingProgram = createShaderProgram(drawable, LIGHT_VERT_SOURCE, LIGHT_FRAG_SOURCE);

        IdentityLocs.put(IdentityLocs.RENDERING_PROGRAM1, renderingProgram1);
        IdentityLocs.put(IdentityLocs.RENDERING_PROGRAM2, renderingProgram2);

        cameraTranslation = new Matrix3D();
        cameraRotation = new Matrix3D();

        setupVerticies(gl);
        setupShadowBuffers(drawable);
        // instantiate positional light and set location (using default amb, diff, spec settings)
        pl = new PositionalLight();
        plocation = new Point3D(0, 0, 5);
        pl.setPosition(plocation);

        mvStack = new MatrixStack(20);

        // init world objects
        for (WorldObject worldObject : worldObjectList) {
            worldObject.init(drawable);
        }

        b.setElementAt(0, 0, 0.5);
        b.setElementAt(0, 1, 0.0);
        b.setElementAt(0, 2, 0.0);
        b.setElementAt(0, 3, 0.5f);
        b.setElementAt(1, 0, 0.0);
        b.setElementAt(1, 1, 0.5);
        b.setElementAt(1, 2, 0.0);
        b.setElementAt(1, 3, 0.5f);
        b.setElementAt(2, 0, 0.0);
        b.setElementAt(2, 1, 0.0);
        b.setElementAt(2, 2, 0.5);
        b.setElementAt(2, 3, 0.5f);
        b.setElementAt(3, 0, 0.0);
        b.setElementAt(3, 1, 0.0);
        b.setElementAt(3, 2, 0.0);
        b.setElementAt(3, 3, 1.0f);

        // set camera position

        float cameraX = 0.0f;
        float cameraY = -0.5f;
        float cameraZ = 8.0f;
        cameraTranslation.translate(cameraX, cameraY, -cameraZ);

        // may reduce shadow border artifacts
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {

    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL4 gl = (GL4) drawable.getGL();
        pl.setPosition(plocation);
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


        gl.glBindFramebuffer(GL_FRAMEBUFFER, shadow_buffer[0]);
        gl.glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, shadow_tex[0], 0);

        gl.glDrawBuffer(GL.GL_NONE);
        gl.glEnable(GL_DEPTH_TEST);

        gl.glEnable(GL_POLYGON_OFFSET_FILL);    // for reducing
        gl.glPolygonOffset(2.0f, 4.0f);            //  shadow artifacts

        Point3D origin = new Point3D(0.0, 0.0, 0.0);
        Vector3D up = new Vector3D(0.0, 1.0, 0.0);

        Matrix3D lightV_matrix = lookAt(pl.getPosition(), origin, up);
        gl.glClear(GL_DEPTH_BUFFER_BIT);
        for (WorldObject worldObject : worldObjectList) {
            worldObject.firstPass(drawable, lightV_matrix, pMat);
        }

        gl.glDisable(GL_POLYGON_OFFSET_FILL);    // artifact reduction, continued

        gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        gl.glActiveTexture(gl.GL_TEXTURE0);
        gl.glBindTexture(GL_TEXTURE_2D, shadow_tex[0]);
        gl.glDrawBuffer(GL.GL_FRONT);

        gl.glClear(GL_DEPTH_BUFFER_BIT);
        for (WorldObject worldObject : worldObjectList) {
            currentMaterial = worldObject.getMaterial();
            installLights(mvStack.peek(), drawable);
            worldObject.secondPass(drawable, mvStack.peek(), pMat, b, lightV_matrix);
        }
        if (showPosLight) {
            gl.glUseProgram(lightPointRenderingProgram);
            mvLoc = gl.glGetUniformLocation(lightPointRenderingProgram, "mv_matrix");
            projLoc = gl.glGetUniformLocation(lightPointRenderingProgram, "proj_matrix");

            // draw small cube to represent positional lighting

            mvStack.pushMatrix();

            mvStack.translate(plocation.getX(), plocation.getY(), plocation.getZ());
            mvStack.scale(.1, .1, .1);

            gl.glUniformMatrix4fv(mvLoc, 1, false, mvStack.peek().getFloatValues(), 0);
            gl.glUniformMatrix4fv(projLoc, 1, false, pMat.getFloatValues(), 0);

            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
            gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);
            gl.glEnableVertexAttribArray(0);

            gl.glEnable(GL_CULL_FACE);
            gl.glFrontFace(GL_CCW);
            gl.glEnable(GL_DEPTH_TEST);
            gl.glDepthFunc(GL_LEQUAL);

            gl.glDrawArrays(GL_TRIANGLES, 0, mySphere.getIndices().length);
            mvStack.popMatrix();
        }
    }

    private Matrix3D lookAt(graphicslib3D.Point3D eyeP, graphicslib3D.Point3D centerP, Vector3D upV) {
        Vector3D eyeV = new Vector3D(eyeP);
        Vector3D cenV = new Vector3D(centerP);
        Vector3D f = (cenV.minus(eyeV)).normalize();
        Vector3D sV = (f.cross(upV)).normalize();
        Vector3D nU = (sV.cross(f)).normalize();

        Matrix3D l = new Matrix3D();
        l.setElementAt(0, 0, sV.getX());
        l.setElementAt(0, 1, nU.getX());
        l.setElementAt(0, 2, -f.getX());
        l.setElementAt(0, 3, 0.0f);
        l.setElementAt(1, 0, sV.getY());
        l.setElementAt(1, 1, nU.getY());
        l.setElementAt(1, 2, -f.getY());
        l.setElementAt(1, 3, 0.0f);
        l.setElementAt(2, 0, sV.getZ());
        l.setElementAt(2, 1, nU.getZ());
        l.setElementAt(2, 2, -f.getZ());
        l.setElementAt(2, 3, 0.0f);
        l.setElementAt(3, 0, sV.dot(eyeV.mult(-1)));
        l.setElementAt(3, 1, nU.dot(eyeV.mult(-1)));
        l.setElementAt(3, 2, (f.mult(-1)).dot(eyeV.mult(-1)));
        l.setElementAt(3, 3, 1.0f);
        return (l.transpose());
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
