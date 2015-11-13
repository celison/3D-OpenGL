package a2;

import a2.objects.IGLDrawable;
import a2.objects.Sun;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

import static com.jogamp.opengl.GL4.*;

import graphicslib3D.GLSLUtils;
import graphicslib3D.Matrix3D;
import graphicslib3D.MatrixStack;
import graphicslib3D.Vector3D;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import static com.jogamp.opengl.GL.GL_DEPTH_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_VERSION;

/**
 * Created by cjeli_000 on 10/7/2015.
 */
public class SolarSystemFrame extends JFrame implements GLEventListener, KeyListener {
    private GLCanvas myCanvas;

    private int renderingProgram;
    private int axisRenderingProgram;

    private int[] vao = new int[1];
    private int[] vbo;

    private GLSLUtils util = new GLSLUtils();

    private Matrix3D cameraTranslation;
    private Matrix3D cameraRotation;

    private ArrayList<IGLDrawable> drawObjects = new ArrayList<>();

    private boolean showAxis = true;

    private static final String FRAG_SOURCE = "a2/frag.shader";
    private static final String VERT_SOURCE = "a2/vert.shader";

    private static final String AXIS_FRAG_SOURCE = "a2/fragAxis.shader";
    private static final String AXIS_VERT_SOURCE = "a2/vertAxis.shader";

    public SolarSystemFrame() {
        setTitle("Elison - Program 2");
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cameraTranslation = new Matrix3D();
        cameraRotation = new Matrix3D();

        addKeyListener(this);

        myCanvas = new GLCanvas();
        myCanvas.addGLEventListener(this);
        getContentPane().add(myCanvas);

        FPSAnimator animator = new FPSAnimator(myCanvas, 30);
        animator.start();

        setVisible(true);
    }

    private int createShaderProgram(GLAutoDrawable drawable, String vertSource, String fragSource) {
        GL4 gl = (GL4) drawable.getGL();
        int[] vertCompiled = new int[1];
        int[] fragCompiled = new int[1];
        int[] linked = new int[1];

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
        float[] cubePositions = {-0.25f, 0.25f, -0.25f, -0.25f, -0.25f, -0.25f, 0.25f, -0.25f, -0.25f, 0.25f, -0.25f, -0.25f, 0.25f, 0.25f, -0.25f, -0.25f, 0.25f, -0.25f,
                0.25f, -0.25f, -0.25f, 0.25f, -0.25f, 0.25f, 0.25f, 0.25f, -0.25f, 0.25f, -0.25f, 0.25f, 0.25f, 0.25f, 0.25f, 0.25f, 0.25f, -0.25f,
                0.25f, -0.25f, 0.25f, -0.25f, -0.25f, 0.25f, 0.25f, 0.25f, 0.25f, -0.25f, -0.25f, 0.25f, -0.25f, 0.25f, 0.25f, 0.25f, 0.25f, 0.25f,
                -0.25f, -0.25f, 0.25f, -0.25f, -0.25f, -0.25f, -0.25f, 0.25f, 0.25f, -0.25f, -0.25f, -0.25f, -0.25f, 0.25f, -0.25f, -0.25f, 0.25f, 0.25f,
                -0.25f, -0.25f, 0.25f, 0.25f, -0.25f, 0.25f, 0.25f, -0.25f, -0.25f, 0.25f, -0.25f, -0.25f, -0.25f, -0.25f, -0.25f, -0.25f, -0.25f, 0.25f,
                -0.25f, 0.25f, -0.25f, 0.25f, 0.25f, -0.25f, 0.25f, 0.25f, 0.25f, 0.25f, 0.25f, 0.25f, -0.25f, 0.25f, 0.25f, -0.25f, 0.25f, -0.25f};

        float[] pyramidPositions =
                {0.25f, -0.25f, -0.25f, -0.25f, -0.25f, -0.25f, 0.0f, 0.25f, 0.0f,
                        -0.25f, -0.25f, -0.25f, -0.25f, -0.25f, 0.25f, 0.0f, 0.25f, 0.0f,
                        -0.25f, -0.25f, 0.25f, 0.25f, -0.25f, 0.25f, 0.0f, 0.25f, 0.0f,
                        0.25f, -0.25f, 0.25f, 0.25f, -0.25f, -0.25f, 0.0f, 0.25f, 0.0f,
                        -0.25f, -0.25f, -0.25f, 0.25f, -0.25f, 0.25f, -0.25f, -0.25f, 0.25f,
                        -0.25f, -0.25f, -0.25f, 0.25f, -0.25f, -0.25f, 0.25f, -0.25f, 0.25f
                };

        float[] tetraPositions = {0f, -.5f, .5f, 0f, .5f, 0f, -.5f, -.5f, -.5f,
                0f, -.5f, .5f, .5f, -.5f, -.5f, 0f, .5f, 0f,
                0f, -.5f, .5f, -.5f, -.5f, -.5f, .5f, -.5f, -.5f,
                .5f, -.5f, -.5f, -.5f, -.5f, -.5f, 0f, .5f, 0f};


        gl.glGenVertexArrays(vao.length, vao, 0);
        gl.glBindVertexArray(vao[0]);
        gl.glGenBuffers(vbo.length, vbo, 0);

        int index = 0;

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[index++]);
        FloatBuffer cubeBuf = FloatBuffer.wrap(cubePositions);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, cubeBuf.limit() * 4, cubeBuf, GL.GL_STATIC_DRAW);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[index++]);
        FloatBuffer pyrBuf = FloatBuffer.wrap(pyramidPositions);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, pyrBuf.limit() * 4, pyrBuf, GL.GL_STATIC_DRAW);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[index++]);
        FloatBuffer tetraBuf = FloatBuffer.wrap(tetraPositions);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, tetraBuf.limit() * 4, tetraBuf, GL.GL_STATIC_DRAW);

        for (IGLDrawable drawable : drawObjects) {
            drawable.setupBuffers(gl, vbo, index);
        }
    }

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

    @Override
    public void init(GLAutoDrawable drawable) {
        GL4 gl = (GL4) drawable.getGL();
        System.out.println("JOGL VERSION: " + JoglVersion.getInstance().getImplementationVersion());
        System.out.println("OPEN GL VERSION: " + gl.glGetString(GL_VERSION));

        drawObjects.add(new Sun(48));
        int i = 0;
        for (IGLDrawable o : drawObjects) {
            o.init(drawable);
            i += o.getIndexesUsed();
        }

        vbo = new int[i + 4];
        renderingProgram = createShaderProgram(drawable, VERT_SOURCE, FRAG_SOURCE);
        axisRenderingProgram = createShaderProgram(drawable, AXIS_VERT_SOURCE, AXIS_FRAG_SOURCE);
        IdentityLocs.init(renderingProgram, gl);
        setupVerticies(gl);

        float cameraX = 0.0f;
        float cameraY = -0.5f;
        float cameraZ = 8.0f;

        cameraTranslation.translate(cameraX, cameraY, -cameraZ);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {

    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL4 gl = (GL4) drawable.getGL();

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

        if ( showAxis ) {
            gl.glUseProgram(axisRenderingProgram);
            mvLoc = gl.glGetUniformLocation(axisRenderingProgram, "mv_matrix");
            projLoc = gl.glGetUniformLocation(axisRenderingProgram, "proj_matrix");

            gl.glUniformMatrix4fv(mvLoc, 1, false, mvStack.peek().getFloatValues(), 0);
            gl.glUniformMatrix4fv(projLoc, 1, false, pMat.getFloatValues(), 0);

            gl.glDrawArrays(GL_LINES, 0, 6);
        }
        double amt = 5;//(double) (System.currentTimeMillis() % 360000) / 1000.0;
        gl.glUseProgram(renderingProgram);
        mvLoc = gl.glGetUniformLocation(renderingProgram, "mv_matrix");
        projLoc = gl.glGetUniformLocation(renderingProgram, "proj_matrix");

//        mvStack.pushMatrix();
//        mvStack.translate(0.0f, 0.0f, 0.0f);
//        mvStack.scale(.1f, 100f, .1f);
//        gl.glUniformMatrix4fv(mvLoc, 1, false, mvStack.peek().getFloatValues(), 0);
//        gl.glUniformMatrix4fv(projLoc, 1, false, pMat.getFloatValues(), 0);
//        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
//        gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);
//        gl.glEnableVertexAttribArray(0);
//        gl.glEnable(GL_CULL_FACE);
//        gl.glFrontFace(GL_CW);
//        gl.glEnable(GL_DEPTH_TEST);
//        gl.glDepthFunc(GL_LEQUAL);
//        gl.glDrawArrays(GL_TRIANGLES, 0, 36);
//        mvStack.popMatrix();
//
//        mvStack.pushMatrix();
//        mvStack.translate(0.0f, 0.0f, 0.0f);
//        mvStack.scale(.1f, .1f, 100f);
//        gl.glUniformMatrix4fv(mvLoc, 1, false, mvStack.peek().getFloatValues(), 0);
//        gl.glUniformMatrix4fv(projLoc, 1, false, pMat.getFloatValues(), 0);
//        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
//        gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);
//        gl.glEnableVertexAttribArray(0);
//        gl.glEnable(GL_CULL_FACE);
//        gl.glFrontFace(GL_CW);
//        gl.glEnable(GL_DEPTH_TEST);
//        gl.glDepthFunc(GL_LEQUAL);
//        gl.glDrawArrays(GL_TRIANGLES, 0, 36);
//        mvStack.popMatrix();
//
//        mvStack.pushMatrix();
//        mvStack.translate(0.0f, 0.0f, 0.0f);
//        mvStack.scale(100f, .1f, .1f);
//        gl.glUniformMatrix4fv(mvLoc, 1, false, mvStack.peek().getFloatValues(), 0);
//        gl.glUniformMatrix4fv(projLoc, 1, false, pMat.getFloatValues(), 0);
//        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
//        gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);
//        gl.glEnableVertexAttribArray(0);
//        gl.glEnable(GL_CULL_FACE);
//        gl.glFrontFace(GL_CW);
//        gl.glEnable(GL_DEPTH_TEST);
//        gl.glDepthFunc(GL_LEQUAL);
//        gl.glDrawArrays(GL_TRIANGLES, 0, 36);
//        mvStack.popMatrix();

        for (IGLDrawable drawObj : drawObjects) {
            drawObj.draw(drawable, mvStack, pMat);
        }
//        // Square = Earth
//        mvStack.pushMatrix();
//        mvStack.translate(Math.sin(amt) * 3, 0.0f, Math.cos(amt) * 3);
//        mvStack.pushMatrix();
//        mvStack.rotate((System.currentTimeMillis() % 3600) / 10.0, 1.0, 0.0, 0.0);
//        gl.glUniformMatrix4fv(mvLoc, 1, false, mvStack.peek().getFloatValues(), 0);
//        gl.glUniformMatrix4fv(projLoc, 1, false, pMat.getFloatValues(), 0);
//        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
//        gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);
//        gl.glEnableVertexAttribArray(0);
//        gl.glEnable(GL_CULL_FACE);
//        gl.glFrontFace(GL_CW);
//        gl.glEnable(GL_DEPTH_TEST);
//        gl.glDepthFunc(GL_LEQUAL);
//        gl.glDrawArrays(GL_TRIANGLES, 0, 36);
//        mvStack.popMatrix();
//        // small Square = moon
//        mvStack.pushMatrix();
//        mvStack.translate(0.0f, Math.sin(amt) / 2.0f, Math.cos(amt) / 2.0f);
//        mvStack.rotate((System.currentTimeMillis() % 3600) / 10.0, 1.0, 0.0, 0.0);
//        mvStack.scale(0.25, 0.25, 0.25);
//        gl.glUniformMatrix4fv(mvLoc, 1, false, mvStack.peek().getFloatValues(), 0);
//        gl.glUniformMatrix4fv(projLoc, 1, false, pMat.getFloatValues(), 0);
//        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[2]);
//        gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);
//        gl.glEnableVertexAttribArray(0);
//        gl.glEnable(GL_CULL_FACE);
//        gl.glFrontFace(GL_CW);
//        gl.glEnable(GL_DEPTH_TEST);
//        gl.glDepthFunc(GL_LEQUAL);
//        gl.glDrawArrays(GL_TRIANGLES, 0, 12);
//        mvStack.popMatrix();
//        mvStack.popMatrix();
//        // second planet
//        mvStack.pushMatrix();
//
//        mvStack.translate(0.0f, Math.sin(amt) * 4, Math.cos(amt) * 4);
//        mvStack.pushMatrix();
//        mvStack.rotate((System.currentTimeMillis() % 3600) / 10.0, 1.0, 0.0, 0.0);
//        gl.glUniformMatrix4fv(mvLoc, 1, false, mvStack.peek().getFloatValues(), 0);
//        gl.glUniformMatrix4fv(projLoc, 1, false, pMat.getFloatValues(), 0);
//        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
//        gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);
//        gl.glEnableVertexAttribArray(0);
//        gl.glEnable(GL_CULL_FACE);
//        gl.glFrontFace(GL_CW);
//        gl.glEnable(GL_DEPTH_TEST);
//        gl.glDepthFunc(GL_LEQUAL);
//        gl.glDrawArrays(GL_TRIANGLES, 0, 36);
//        mvStack.popMatrix();
//        mvStack.popMatrix();
//        mvStack.popMatrix();
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
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}
