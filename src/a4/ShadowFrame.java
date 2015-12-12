package a4;

import a4.objects.Ball;
import a4.objects.Bee;
import a4.objects.WorldObject;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;
import graphicslib3D.*;
import graphicslib3D.light.PositionalLight;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Random;
import java.util.Vector;

import static com.jogamp.opengl.GL.GL_CCW;
import static com.jogamp.opengl.GL.GL_CLAMP_TO_EDGE;
import static com.jogamp.opengl.GL.GL_CULL_FACE;
import static com.jogamp.opengl.GL.GL_CW;
import static com.jogamp.opengl.GL.GL_DEPTH_ATTACHMENT;
import static com.jogamp.opengl.GL.GL_DEPTH_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_DEPTH_COMPONENT32;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_FRAMEBUFFER;
import static com.jogamp.opengl.GL.GL_FRONT_AND_BACK;
import static com.jogamp.opengl.GL.GL_LEQUAL;
import static com.jogamp.opengl.GL.GL_LINEAR;
import static com.jogamp.opengl.GL.GL_LINES;
import static com.jogamp.opengl.GL.GL_POLYGON_OFFSET_FILL;
import static com.jogamp.opengl.GL.GL_RGBA;
import static com.jogamp.opengl.GL.GL_RGBA8;
import static com.jogamp.opengl.GL.GL_TEXTURE_2D;
import static com.jogamp.opengl.GL.GL_TEXTURE_MAG_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_MIN_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_S;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_T;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL.GL_VERSION;
import static com.jogamp.opengl.GL2ES3.GL_COLOR;
import static com.jogamp.opengl.GL4.*;

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
    private int skydomeRenderingProgram;
    private int tessRenderingProgram;

    private int groundColorTex;
    private int groundHeightTex;
    private int groundNormalTex;

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
    private Matrix3D pMat;
    private Matrix3D b = new Matrix3D();
    private Matrix3D mvp = new Matrix3D();

    private float[] globalAmbient = new float[]{0.3f, 0.3f, 0.3f, 1.0f};

    //shadow stuff
    private int scSizeX, scSizeY;
    private int[] shadow_tex = new int[1];
    private int[] shadow_buffer = new int[1];

    private Ball mySphere;
    private TextureReader tr;

    private int noiseHeight = 200;
    private int noiseWidth = 200;
    private int noiseDepth = 200;
    private double[][][] noise = new double[noiseHeight][noiseWidth][noiseDepth];
    private Random random = new Random();
    private int cloudTextureID;


    private static final String FIRST_FRAG_SOURCE = "src/a4/blinnFrag1.shader";
    private static final String FIRST_VERT_SOURCE = "src/a4/blinnVert1.shader";

    private static final String SECOND_FRAG_SOURCE = "src/a4/blinnFrag2.shader";
    private static final String SECOND_VERT_SOURCE = "src/a4/blinnVert2.shader";

    private static final String TESS_VERT_SOURCE = "src/a4/vert.shader";
    private static final String TES_SOURCE = "src/a4/tessE.shader";
    private static final String TCS_SOURCE = "src/a4/tessC.shader";
    private static final String TESS_FRAG_SOURCE = "src/a4/frag.shader";

    private static final String AXIS_FRAG_SOURCE = "src/a4/fragAxis.shader";
    private static final String AXIS_VERT_SOURCE = "src/a4/vertAxis.shader";

    private static final String LIGHT_FRAG_SOURCE = "src/a4/fragPoint.shader";
    private static final String LIGHT_VERT_SOURCE = "src/a4/vertPoint.shader";

    private static final String SKYDOME_FRAG_SOURCE = "src/a4/skyboxFrag.shader";
    private static final String SKYDOME_VERT_SOURCE = "src/a4/skyboxVert.shader";

    private Vector<WorldObject> worldObjectList;
    private float cloudFrame = 0.0f;

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
            int z = (i / 3) % 3;
            int y = (i / 9) % 3;
            ball.translate(x - 0.5, y, z);
            worldObjectList.add(ball);
        }
        worldObjectList.add(new Bee());


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
        return createShaderProgram(drawable, vertSource, null, null, fragSource);
    }

    private int createShaderProgram(GLAutoDrawable drawable, String vertSource, String tcsSource, String tesSource, String fragSource) {
        GL4 gl = (GL4) drawable.getGL();
        int[] vertCompiled = new int[1];
        int[] fragCompiled = new int[1];
        int[] linked = new int[1];
        boolean doTessellation = tcsSource != null && tesSource != null;

        System.out.print("Reading " + vertSource + " and " + fragSource);
        if (doTessellation) System.out.print(" and " + tcsSource + " and " + tesSource);
        System.out.print("\n");

        String vshaderSource[] = util.readShaderSource(vertSource);
        String fshaderSource[] = util.readShaderSource(fragSource);
        String[] tcshaderSource;
        String[] teshaderSource;
        int lengths[];


        int vShader = gl.glCreateShader(GL4.GL_VERTEX_SHADER);
        int tessCShader = gl.glCreateShader(GL4.GL_TESS_CONTROL_SHADER);
        int tessEShader = gl.glCreateShader(GL4.GL_TESS_EVALUATION_SHADER);
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
        gl.glGetShaderiv(vShader, GL4.GL_COMPILE_STATUS, vertCompiled, 0);
        if (vertCompiled[0] == 1)
            System.out.println("Vertex compilation success.");
        else
            System.out.println("Vertex compilation failed.");

        if (doTessellation) {
            int[] tescCompiled = new int[1];
            int[] teseCompiled = new int[1];

            tcshaderSource = util.readShaderSource(tcsSource);
            teshaderSource = util.readShaderSource(tesSource);

            lengths = new int[tcshaderSource.length];
            for (int i = 0; i < lengths.length; i++) {
                lengths[i] = tcshaderSource[i].length();
            }
            gl.glShaderSource(tessCShader,
                    tcshaderSource.length, tcshaderSource, lengths, 0);

            lengths = new int[teshaderSource.length];
            for (int i = 0; i < lengths.length; i++) {
                lengths[i] = teshaderSource[i].length();
            }
            gl.glShaderSource(tessEShader,
                    teshaderSource.length, teshaderSource, lengths, 0);

            gl.glCompileShader(tessCShader);
            gl.glGetShaderiv(tessCShader, GL4.GL_COMPILE_STATUS, tescCompiled, 0);
            if (tescCompiled[0] == 1)
                System.out.println("Tessellation control shader compilation success.");
            else
                System.out.println("Tessellation control shader compilation failed.");

            gl.glCompileShader(tessEShader);
            gl.glGetShaderiv(tessEShader, GL4.GL_COMPILE_STATUS, teseCompiled, 0);
            if (teseCompiled[0] == 1)
                System.out.println("Tessellation evaluation shader compilation success.");
            else {
                System.out.println("Tessellation evaluation shader compilation failed.");
                util.printShaderInfoLog(drawable, tessEShader);
            }
        }
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

        int rendering_program = gl.glCreateProgram();
        gl.glAttachShader(rendering_program, vShader);
        if (doTessellation) {
            gl.glAttachShader(rendering_program, tessCShader);
            gl.glAttachShader(rendering_program, tessEShader);
        }
        gl.glAttachShader(rendering_program, fShader);
        gl.glLinkProgram(rendering_program);
        util.printOpenGLError(drawable);
        gl.glGetProgramiv(rendering_program, GL4.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 1)
            System.out.println("Linking success.");
        else {
            System.out.println("Linking failed.");
            util.printProgramInfoLog(drawable, rendering_program);
        }
        return rendering_program;
    }

    private void setupVerticies(GL4 gl) {
        int index = 0;
        // get sphere vertex, texture, and normal values
        mySphere = new Ball();

        Vertex3D[] vertices = mySphere.getShape().getVertices();
        int[] indices = mySphere.getShape().getIndices();

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
            index += 4;
        }

    }

    private void installLights(Matrix3D v_matrix, GLAutoDrawable drawable, int rendProg) {
        GL4 gl = (GL4) drawable.getGL();

        // get light position

        Point3D lightPv = plocation.mult(v_matrix);
        float[] currLightPos = new float[]{(float) lightPv.getX(), (float) lightPv.getY(), (float) lightPv.getZ()};

        int globalAmbLoc = gl.glGetUniformLocation(rendProg, "globalAmbient");
        gl.glProgramUniform4fv(rendProg, globalAmbLoc, 1, globalAmbient, 0);

        // get the locations of the light and material fields in the shader
        int ambLoc = gl.glGetUniformLocation(rendProg, "light.ambient");
        int diffLoc = gl.glGetUniformLocation(rendProg, "light.diffuse");
        int specLoc = gl.glGetUniformLocation(rendProg, "light.specular");
        int posLoc = gl.glGetUniformLocation(rendProg, "light.position");
        int MambLoc = gl.glGetUniformLocation(rendProg, "material.ambient");
        int MdiffLoc = gl.glGetUniformLocation(rendProg, "material.diffuse");
        int MspecLoc = gl.glGetUniformLocation(rendProg, "material.specular");
        int MshiLoc = gl.glGetUniformLocation(rendProg, "material.shininess");

        // set the uniform light and material values in the shader
        gl.glProgramUniform4fv(rendProg, ambLoc, 1, pl.getAmbient(), 0);
        gl.glProgramUniform4fv(rendProg, diffLoc, 1, pl.getDiffuse(), 0);
        gl.glProgramUniform4fv(rendProg, specLoc, 1, pl.getSpecular(), 0);
        gl.glProgramUniform3fv(rendProg, posLoc, 1, currLightPos, 0);
        gl.glProgramUniform4fv(rendProg, MambLoc, 1, currentMaterial.getAmbient(), 0);
        gl.glProgramUniform4fv(rendProg, MdiffLoc, 1, currentMaterial.getDiffuse(), 0);
        gl.glProgramUniform4fv(rendProg, MspecLoc, 1, currentMaterial.getSpecular(), 0);
        gl.glProgramUniform1f(rendProg, MshiLoc, currentMaterial.getShininess());
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

    private void fillDataArray(byte data[]) {
        for (int i = 0; i < noiseHeight; i++) {
            for (int j = 0; j < noiseWidth; j++) {
                for (int k = 0; k < noiseDepth; k++) { // clouds (same as above with blue hue)
                    float hue = 240 / 360.0f;
                    float sat = (float) turbulence(i, j, k, 32) / 256.0f;
                    float bri = 100 / 100.0f;
                    int rgb = Color.HSBtoRGB(hue, sat, bri);
                    Color c = new Color(rgb);
                    data[i * (noiseWidth * noiseHeight * 4) + j * (noiseHeight * 4) + k * 4 + 0] = (byte) c.getRed();
                    data[i * (noiseWidth * noiseHeight * 4) + j * (noiseHeight * 4) + k * 4 + 1] = (byte) c.getGreen();
                    data[i * (noiseWidth * noiseHeight * 4) + j * (noiseHeight * 4) + k * 4 + 2] = (byte) c.getBlue();
                    data[i * (noiseWidth * noiseHeight * 4) + j * (noiseHeight * 4) + k * 4 + 3] = (byte) 0;
                }
            }
        }
    }

    private int loadNoiseTexture(GLAutoDrawable drawable) {
        GL4 gl = (GL4) drawable.getGL();

        byte[] data = new byte[noiseHeight * noiseWidth * noiseDepth * 4];

        ByteBuffer bb = ByteBuffer.allocate(noiseHeight * noiseWidth * noiseDepth * 4);

        fillDataArray(data);

        bb = ByteBuffer.wrap(data);

        int[] textureIDs = new int[1];
        gl.glGenTextures(1, textureIDs, 0);
        int textureID = textureIDs[0];

        gl.glBindTexture(gl.GL_TEXTURE_3D, textureID);

        gl.glTexStorage3D(gl.GL_TEXTURE_3D, 1, GL_RGBA8, noiseWidth, noiseHeight, noiseDepth);
        gl.glTexSubImage3D(gl.GL_TEXTURE_3D, 0, 0, 0, 0,
                noiseWidth, noiseHeight, noiseDepth, GL_RGBA, GL_UNSIGNED_INT_8_8_8_8_REV, bb);

        gl.glTexParameteri(gl.GL_TEXTURE_3D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        return textureID;
    }

    void generateNoise() {
        for (int x = 0; x < noiseHeight; x++) {
            for (int y = 0; y < noiseWidth; y++) {
                for (int z = 0; z < noiseDepth; z++) {
                    noise[x][y][z] = random.nextDouble();
                }
            }
        }
    }

    double smoothNoise(double x1, double y1, double z1) {    //get fractional part of x, y, and z
        double fractX = x1 - (int) x1;
        double fractY = y1 - (int) y1;
        double fractZ = z1 - (int) z1;

        //neighbor values
        int x2 = ((int) x1 + noiseWidth - 1) % noiseWidth;
        int y2 = ((int) y1 + noiseHeight - 1) % noiseHeight;
        int z2 = ((int) z1 + noiseDepth - 1) % noiseDepth;

        //smooth the noise by interpolating
        double value = 0.0;
        value += fractX * fractY * fractZ * noise[(int) x1][(int) y1][(int) z1];
        value += fractX * (1 - fractY) * fractZ * noise[(int) x1][(int) y2][(int) z1];
        value += (1 - fractX) * fractY * fractZ * noise[(int) x2][(int) y1][(int) z1];
        value += (1 - fractX) * (1 - fractY) * fractZ * noise[(int) x2][(int) y2][(int) z1];

        value += fractX * fractY * (1 - fractZ) * noise[(int) x1][(int) y1][(int) z2];
        value += fractX * (1 - fractY) * (1 - fractZ) * noise[(int) x1][(int) y2][(int) z2];
        value += (1 - fractX) * fractY * (1 - fractZ) * noise[(int) x2][(int) y1][(int) z2];
        value += (1 - fractX) * (1 - fractY) * (1 - fractZ) * noise[(int) x2][(int) y2][(int) z2];

        return value;
    }

    private double turbulence(double x, double y, double z, double size) {
        double value = 0.0, initialSize = size;
        while (size >= 0.9) {
            value = value + smoothNoise(x / size, y / size, z / size) * size;
            size = size / 2.0;
        }
        value = value / initialSize;
        value = 256.0 * logistic(value * 128.0 - 120.0);
        return value;
    }

    private double logistic(double x) {
        double k = 0.2;
        return (1.0 / (1.0 + Math.pow(2.718, -k * x)));
    }

    //Overloaded Methods

    @Override
    public void init(GLAutoDrawable drawable) {
        GL4 gl = (GL4) drawable.getGL();
        System.out.println("JOGL VERSION: " + JoglVersion.getInstance().getImplementationVersion());
        System.out.println("OPEN GL VERSION: " + gl.glGetString(GL_VERSION));

        vbo = new int[3 + (4 * worldObjectList.size())];
        cameraTranslation = new Matrix3D();
        cameraRotation = new Matrix3D();
        generateNoise();
        cloudTextureID = loadNoiseTexture(drawable);
        mvStack = new MatrixStack(20);
        tr = new TextureReader();

        groundColorTex = tr.loadTexture(drawable, "src/a4/moon.jpg");
        groundHeightTex = tr.loadTexture(drawable, "src/a4/height.jpg");
        groundNormalTex = tr.loadTexture(drawable, "src/a4/normal.jpg");

        // instantiate positional light and set location (using default amb, diff, spec settings)
        pl = new PositionalLight();
        plocation = new Point3D(0, 0, 5);
        pl.setPosition(plocation);

        // create shader programs
        renderingProgram1 = createShaderProgram(drawable, FIRST_VERT_SOURCE, FIRST_FRAG_SOURCE);
        renderingProgram2 = createShaderProgram(drawable, SECOND_VERT_SOURCE, SECOND_FRAG_SOURCE);
        axisRenderingProgram = createShaderProgram(drawable, AXIS_VERT_SOURCE, AXIS_FRAG_SOURCE);
        lightPointRenderingProgram = createShaderProgram(drawable, LIGHT_VERT_SOURCE, LIGHT_FRAG_SOURCE);
        skydomeRenderingProgram = createShaderProgram(drawable, SKYDOME_VERT_SOURCE, SKYDOME_FRAG_SOURCE);
        tessRenderingProgram = createShaderProgram(drawable, TESS_VERT_SOURCE, TCS_SOURCE, TES_SOURCE, TESS_FRAG_SOURCE);

        IdentityLocs.put(IdentityLocs.RENDERING_PROGRAM1, renderingProgram1);
        IdentityLocs.put(IdentityLocs.RENDERING_PROGRAM2, renderingProgram2);

        setupVerticies(gl);
        setupShadowBuffers(drawable);

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
        pMat = perspective(50.0f, aspect, 0.1f, 1000.0f);

        mvStack = new MatrixStack(20);
        // setup view matrix
        mvStack.pushMatrix();
        mvStack.multMatrix(cameraRotation);

        int mvLoc;
        int projLoc;

        // Skydome

        drawSkydome(drawable);
        mvStack.multMatrix(cameraTranslation);

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

        drawTessGrid(drawable);

        for (WorldObject worldObject : worldObjectList) {
            currentMaterial = worldObject.getMaterial();
            installLights(mvStack.peek(), drawable, renderingProgram2);
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

            gl.glDrawArrays(GL_TRIANGLES, 0, mySphere.getShape().getIndices().length);
            mvStack.popMatrix();
        }
        if (showAxis) {
            gl.glUseProgram(axisRenderingProgram);
            mvLoc = gl.glGetUniformLocation(axisRenderingProgram, "mv_matrix");
            projLoc = gl.glGetUniformLocation(axisRenderingProgram, "proj_matrix");

            gl.glUniformMatrix4fv(mvLoc, 1, false, mvStack.peek().getFloatValues(), 0);
            gl.glUniformMatrix4fv(projLoc, 1, false, pMat.getFloatValues(), 0);

            gl.glClear(GL_DEPTH_BUFFER_BIT);

            gl.glDrawArrays(GL_LINES, 0, 6);
        }
    }

    private void drawTessGrid(GLAutoDrawable drawable) {
        GL4 gl = (GL4) drawable.getGL();
        gl.glUseProgram(tessRenderingProgram);

        mvp.setToIdentity();
        mvp.concatenate(pMat);
        mvp.concatenate(mvStack.peek());

        int mvLoc = gl.glGetUniformLocation(tessRenderingProgram, "mv_matrix");
        int projLoc = gl.glGetUniformLocation(tessRenderingProgram, "proj_matrix");
        int nLoc = gl.glGetUniformLocation(tessRenderingProgram, "normalMat");
        int mvpLoc = gl.glGetUniformLocation(tessRenderingProgram, "mvp");

        currentMaterial = Material.SILVER;
        installLights(mvStack.peek(), drawable, tessRenderingProgram);

        gl.glUniformMatrix4fv(mvpLoc, 1, false, mvp.getFloatValues(), 0);
        gl.glUniformMatrix4fv(mvLoc, 1, false, mvStack.peek().getFloatValues(), 0);
        gl.glUniformMatrix4fv(projLoc, 1, false, pMat.getFloatValues(), 0);
        gl.glUniformMatrix4fv(nLoc, 1, false, (mvStack.peek().inverse()).transpose().getFloatValues(), 0);

        gl.glActiveTexture(gl.GL_TEXTURE0);
        gl.glBindTexture(gl.GL_TEXTURE_2D, groundColorTex);
        gl.glActiveTexture(gl.GL_TEXTURE1);
        gl.glBindTexture(gl.GL_TEXTURE_2D, groundHeightTex);
        gl.glActiveTexture(gl.GL_TEXTURE2);
        gl.glBindTexture(gl.GL_TEXTURE_2D, groundNormalTex);

        //gl.glClear(GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL_DEPTH_TEST);
        gl.glFrontFace(GL_CCW);

        gl.glPatchParameteri(GL_PATCH_VERTICES, 4);
        gl.glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        gl.glDrawArraysInstanced(GL_PATCHES, 0, 4, 64 * 64);
    }

    private void drawSkydome(GLAutoDrawable drawable) {
        GL4 gl = (GL4) drawable.getGL();
        gl.glUseProgram(skydomeRenderingProgram);

        int mvLoc = gl.glGetUniformLocation(skydomeRenderingProgram, "mv_matrix");
        int projLoc = gl.glGetUniformLocation(skydomeRenderingProgram, "proj_matrix");
        int frame = gl.glGetUniformLocation(skydomeRenderingProgram, "d");

        mvStack.pushMatrix();
        double amt = (System.currentTimeMillis() % 360000) / 1000.0;
        mvStack.rotate(90, 0, 0 ,1);
        mvStack.rotate(amt, 1, 0, 0);
        cloudFrame = cloudFrame + 0.000025f;
        if (cloudFrame >= 1.0f) cloudFrame = 0.0f;
        gl.glUniformMatrix4fv(mvLoc, 1, false, mvStack.peek().getFloatValues(), 0);
        gl.glUniformMatrix4fv(projLoc, 1, false, pMat.getFloatValues(), 0);
        gl.glUniform1f(frame, cloudFrame);
        mvStack.popMatrix();

        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(gl.GL_TEXTURE_3D, cloudTextureID);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
        gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[1]);
        gl.glVertexAttribPointer(1, 2, GL.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(1);

        gl.glClear(GL_DEPTH_BUFFER_BIT);
        gl.glFrontFace(GL_CW);
        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);

        gl.glDrawArrays(GL_TRIANGLES, 0, mySphere.getShape().getIndices().length);

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
