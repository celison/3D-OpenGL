package a2.objects;

import a2.IdentityLocs;
import a2.TextureReader;
import a3.shapes.Sphere;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import graphicslib3D.Matrix3D;
import graphicslib3D.MatrixStack;
import graphicslib3D.Shape3D;
import graphicslib3D.Vertex3D;

import java.nio.FloatBuffer;
import java.util.Vector;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL.GL_TRIANGLES;

/**
 * Created by Connor on 10/20/2015.
 */
public abstract class Orbitable extends Shape3D implements IGLDrawable {
    protected Sphere myShape;
    private int[] vbo;
    private int index;
    private int dxRotate, dyRotate, dzRotate;
    private double dxTranslate, dyTranslate, dzTranslate;

    private TextureReader tr = new TextureReader();
    private int texture;

    protected String textureURL;

    protected Vector<IGLDrawable> orbitList;

    protected String getTextureURL() {
        return textureURL;
    }

    protected void setTextureURL(String textureURL) {
        this.textureURL = textureURL;
    }

    protected void setRotationRate(int dx, int dy, int dz) {
        dxRotate = dx;
        dyRotate = dy;
        dzRotate = dz;
    }

    public int getIndexesUsed() {
        int rv = 0;
        for (IGLDrawable o : orbitList) {
            rv += o.getIndexesUsed();
        }
        rv += 3;
        return rv;
    }

    protected void setTranslationRate(double dx, double dy, double dz) {
        dxTranslate = dx;
        dyTranslate = dy;
        dzTranslate = dz;
    }

    @Override
    public  void init(GLAutoDrawable drawable) {
        texture = tr.loadTexture(drawable, textureURL);
        for (IGLDrawable d : orbitList) {
            d.init(drawable);
        }
    }
    @Override
    public void draw(GLAutoDrawable glAutoDrawable, MatrixStack mvStack, Matrix3D pMat) {
        GL4 gl = (GL4) glAutoDrawable.getGL();

        mvStack.pushMatrix(); // push translate
        translate(dxTranslate, dyTranslate, dzTranslate);
        mvStack.multMatrix(getTranslation());
        mvStack.pushMatrix(); // push rotate

        rotate(dxRotate, dyRotate, dzRotate);
        mvStack.multMatrix(getRotation());
        mvStack.multMatrix(getScale());

        gl.glUniformMatrix4fv(IdentityLocs.getMvLoc(), 1, false, mvStack.peek().getFloatValues(), 0);
        gl.glUniformMatrix4fv(IdentityLocs.getProjLoc(), 1, false, pMat.getFloatValues(), 0);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[index]);
        gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[index + 1]);
        gl.glVertexAttribPointer(1, 2, GL.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(1);

        gl.glActiveTexture(gl.GL_TEXTURE0);
        gl.glBindTexture(gl.GL_TEXTURE_2D, texture);

        gl.glEnable(GL_CULL_FACE);
        gl.glFrontFace(GL_CCW);
        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);
        // draw arrays
        gl.glDrawArrays(GL_TRIANGLES, 0, myShape.getIndices().length);
        mvStack.popMatrix(); // pop rotate
        MatrixStack mvCopy = mvStack;
        for (IGLDrawable d : orbitList) {
            d.draw(glAutoDrawable, mvCopy, pMat);
        }
        mvStack.popMatrix();
    }

    public void setupBuffers(GL4 gl, int[] vbo, int index) {
        this.vbo = vbo;
        this.index = index;


        Vertex3D[] vertices = myShape.getVertices();
        int[] indices = myShape.getIndices();

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

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[index++]);
        FloatBuffer vertBuf = FloatBuffer.wrap(fvalues);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL.GL_STATIC_DRAW);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[index++]);
        FloatBuffer texBuf = FloatBuffer.wrap(tvalues);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, texBuf.limit()*4, texBuf, GL.GL_STATIC_DRAW);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[index++]);
        FloatBuffer norBuf = FloatBuffer.wrap(nvalues);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, norBuf.limit()*4,norBuf, GL.GL_STATIC_DRAW);

        for (IGLDrawable d : orbitList) {
            d.setupBuffers(gl, vbo, index);
            index+=d.getIndexesUsed();
        }
    }


    private float[] getTriangleArray() {
        int[] indices = myShape.getIndices();
        Vertex3D[] vertices = myShape.getVertices();
        float[] rv = new float[indices.length * 3];

        for (int i = 0; i < indices.length; i++) {
            rv[i * 3] = (float) (vertices[indices[i]]).getX();
            rv[i * 3 + 1] = (float) (vertices[indices[i]]).getY();
            rv[i * 3 + 2] = (float) (vertices[indices[i]]).getZ();
        }

        return rv;
    }

    public void addToOrbit(IGLDrawable drawable) {
        orbitList.add(drawable);
    }
}
