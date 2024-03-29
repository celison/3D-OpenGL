package a4.objects;

import a4.TextureReader;
import a4.IdentityLocs;
import a4.shapes.IShape;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import graphicslib3D.*;

import java.nio.FloatBuffer;
import java.util.Vector;

import static com.jogamp.opengl.GL.*;

/**
 * Created by Connor on 11/10/2015.
 */
public abstract class WorldObject extends Shape3D implements IGLDrawable {
    protected IShape myShape;
    protected Material myMaterial;
    protected int[] vbo;
    protected int index;
    protected int dxRotate, dyRotate, dzRotate;
    protected double dxTranslate, dyTranslate, dzTranslate;
    protected int mv_location, proj_location, n_location;
    private TextureReader tr = new TextureReader();
    protected int texture, normal;
    protected Matrix3D shadowMVP1, shadowMVP2, m_matrix, mv_matrix;
    protected String textureURL, normalURL;
    protected boolean heightMap = false;
    protected boolean transparent = false;

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
        rv += 3;
        return rv;
    }

    protected void setTranslationRate(double dx, double dy, double dz) {
        dxTranslate = dx;
        dyTranslate = dy;
        dzTranslate = dz;
    }

    public IShape getShape() {
        return myShape;
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        shadowMVP1 = new Matrix3D();
        shadowMVP2 = new Matrix3D();
        m_matrix = new Matrix3D();
        mv_matrix = new Matrix3D();
        texture = tr.loadTexture(drawable, textureURL);
        normal = tr.loadTexture(drawable, normalURL);
    }

//    @Override
//    public void draw(GLAutoDrawable glAutoDrawable, MatrixStack mvStack, Matrix3D pMat) {
//        GL4 gl = (GL4) glAutoDrawable.getGL();
//
//        mvStack.pushMatrix(); // push translate
//        translate(dxTranslate, dyTranslate, dzTranslate);
//        mvStack.multMatrix(getTranslation());
//        mvStack.pushMatrix(); // push rotate
//
//        rotate(dxRotate, dyRotate, dzRotate);
//        mvStack.multMatrix(getRotation());
//        mvStack.multMatrix(getScale());
//
//        gl.glUniformMatrix4fv(IdentityLocs.getMvLoc(), 1, false, mvStack.peek().getFloatValues(), 0);
//        gl.glUniformMatrix4fv(IdentityLocs.getProjLoc(), 1, false, pMat.getFloatValues(), 0);
//        gl.glUniformMatrix4fv(IdentityLocs.getnlocation(), 1, false, (mvStack.peek().inverse().transpose().getFloatValues()), 0);
//
//        // bind vertex values
//        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[index]);
//        gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);
//        gl.glEnableVertexAttribArray(0);
//
//        // bind normal values
//        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[index + 2]);
//        gl.glVertexAttribPointer(1, 3, GL.GL_FLOAT, false, 0, 0);
//        gl.glEnableVertexAttribArray(1);
//
//        // bind texture values
//        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[index + 1]);
//        gl.glVertexAttribPointer(2, 2, GL.GL_FLOAT, false, 0, 0);
//        gl.glEnableVertexAttribArray(2);
//
//        gl.glActiveTexture(gl.GL_TEXTURE0);
//        gl.glBindTexture(gl.GL_TEXTURE_2D, texture);
//
//        gl.glEnable(GL_CULL_FACE);
//        gl.glFrontFace(GL_CCW);
//        gl.glEnable(GL_DEPTH_TEST);
//        gl.glDepthFunc(GL_LEQUAL);
//        // draw arrays
//        gl.glDrawArrays(GL_TRIANGLES, 0, myShape.getIndices().length);
//        mvStack.popMatrix(); // pop rotate
//        mvStack.popMatrix(); // pop translate
//    }

    public void setupBuffers(GL4 gl, int[] vbo, int index) {
        this.vbo = vbo;
        this.index = index;

        Vertex3D[] vertices = myShape.getVertices();
        int[] indices = myShape.getIndices();

        float[] fvalues = new float[indices.length * 3];
        float[] tvalues = new float[indices.length * 2];
        float[] nvalues = new float[indices.length * 3];
        float[] tanvalues = new float[indices.length * 3];
        float lastx = 0, lasty = 0, lastz = 0;

        for (int i = 0; i < indices.length; i++) {
            fvalues[i * 3] = (float) (vertices[indices[i]]).getX();
            fvalues[i * 3 + 1] = (float) (vertices[indices[i]]).getY();
            fvalues[i * 3 + 2] = (float) (vertices[indices[i]]).getZ();
            tvalues[i * 2] = (float) (vertices[indices[i]]).getS();
            tvalues[i * 2 + 1] = (float) (vertices[indices[i]]).getT();
            nvalues[i * 3] = (float) (vertices[indices[i]]).getNormalX();
            nvalues[i * 3 + 1] = (float) (vertices[indices[i]]).getNormalY();
            nvalues[i * 3 + 2] = (float) (vertices[indices[i]]).getNormalZ();
            if (vertices[indices[i]].getTangent() != null) {
                tanvalues[i * 3] = (float) (vertices[indices[i]]).getTangent().getX();
                tanvalues[i * 3 + 1] = (float) (vertices[indices[i]]).getTangent().getY();
                tanvalues[i * 3 + 2] = (float) (vertices[indices[i]]).getTangent().getZ();
            } else {
                tanvalues[i * 3] = lastx - fvalues[i * 3];
                tanvalues[i * 3 + 1] = lasty - fvalues[i * 3 + 1];
                tanvalues[i * 3 + 2] = lastz - fvalues[i * 3 + 2];
                lastx = fvalues[i * 3];
                lasty = fvalues[i * 3 + 1];
                lastz = fvalues[i * 3 + 2];
            }
        }

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[index++]);
        FloatBuffer vertBuf = FloatBuffer.wrap(fvalues);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, vertBuf.limit() * 4, vertBuf, GL.GL_STATIC_DRAW);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[index++]);
        FloatBuffer texBuf = FloatBuffer.wrap(tvalues);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, texBuf.limit() * 4, texBuf, GL.GL_STATIC_DRAW);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[index++]);
        FloatBuffer norBuf = FloatBuffer.wrap(nvalues);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, norBuf.limit() * 4, norBuf, GL.GL_STATIC_DRAW);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[index++]);
        FloatBuffer tanBuf = FloatBuffer.wrap(tanvalues);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, tanBuf.limit() * 4, tanBuf, GL.GL_STATIC_DRAW);

    }

    public Material getMaterial() {
        return myMaterial;
    }

    public void setMaterial(Material m) {
        myMaterial = m;
    }

    public void firstPass(GLAutoDrawable drawable, Matrix3D lightV_matrix, Matrix3D lightP_matrix) {
        GL4 gl = (GL4) drawable.getGL();

        gl.glUseProgram(IdentityLocs.get(IdentityLocs.RENDERING_PROGRAM1));

        // draw the shape
        m_matrix.setToIdentity();
        m_matrix.concatenate(getTranslation());
        m_matrix.concatenate(getRotation());
        m_matrix.concatenate(getScale());

        shadowMVP1.setToIdentity();
        shadowMVP1.concatenate(lightP_matrix);
        shadowMVP1.concatenate(lightV_matrix);
        shadowMVP1.concatenate(m_matrix);
        int shadow_location = gl.glGetUniformLocation(IdentityLocs.get(IdentityLocs.RENDERING_PROGRAM1), "shadowMVP");
        gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP1.getFloatValues(), 0);

        // set up vertices buffer
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[index]);
        gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);

        gl.glEnable(GL_CULL_FACE);
        gl.glFrontFace(GL_CCW);
        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);

        gl.glDrawArrays(GL_TRIANGLES, 0, myShape.getIndices().length);

    }

    public void secondPass(GLAutoDrawable drawable, Matrix3D v_matrix, Matrix3D proj_mat, Matrix3D b, Matrix3D lightV_matrix) {
        GL4 gl = (GL4) drawable.getGL();

        gl.glUseProgram(IdentityLocs.get(IdentityLocs.RENDERING_PROGRAM2));

        rotate(dxRotate, dyRotate, dzRotate);

        // draw the object

        int rendering_program2 = IdentityLocs.get(IdentityLocs.RENDERING_PROGRAM2);
        mv_location = gl.glGetUniformLocation(rendering_program2, "mv_matrix");
        proj_location = gl.glGetUniformLocation(rendering_program2, "proj_matrix");
        n_location = gl.glGetUniformLocation(rendering_program2, "normalMat");
        int shadow_location = gl.glGetUniformLocation(rendering_program2, "shadowMVP");
        int height_location = gl.glGetUniformLocation(rendering_program2, "heightMap");
        //  build the MODEL matrix
        m_matrix.setToIdentity();
        m_matrix.concatenate(getTranslation());
        m_matrix.concatenate(getRotation());
        m_matrix.concatenate(getScale());

        //  build the MODEL-VIEW matrix
        mv_matrix.setToIdentity();
        mv_matrix.concatenate(v_matrix);
        mv_matrix.concatenate(m_matrix);

        shadowMVP2.setToIdentity();
        shadowMVP2.concatenate(b);
        shadowMVP2.concatenate(proj_mat);
        shadowMVP2.concatenate(lightV_matrix);
        shadowMVP2.concatenate(m_matrix);

        //  put the MV and PROJ matrices into the corresponding uniforms
        gl.glUniformMatrix4fv(mv_location, 1, false, mv_matrix.getFloatValues(), 0);
        gl.glUniformMatrix4fv(proj_location, 1, false, proj_mat.getFloatValues(), 0);
        gl.glUniformMatrix4fv(n_location, 1, false, (mv_matrix.inverse()).transpose().getFloatValues(), 0);
        gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP2.getFloatValues(), 0);
        gl.glUniform1f(height_location, (heightMap) ? 1.0f : 0.0f);

        // set up vertices buffer
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[index]);
        gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);

        // set up texture buffer
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[index + 1]);
        gl.glVertexAttribPointer(3, 2, GL.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(3);

        // set up normals buffer
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[index + 2]);
        gl.glVertexAttribPointer(1, 3, GL.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(1);

        // set up tangent buffer
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[index + 3]);
        gl.glVertexAttribPointer(2, 3, GL.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(2);

        gl.glActiveTexture(gl.GL_TEXTURE1);
        gl.glBindTexture(gl.GL_TEXTURE_2D, normal);

        gl.glActiveTexture(gl.GL_TEXTURE2);
        gl.glBindTexture(gl.GL_TEXTURE_2D, texture);
        if (transparent) {
            int a_location = gl.glGetUniformLocation(rendering_program2, "alpha");

            float alpha = 0.4f;
            float factor = 0.75f;

            gl.glEnable(GL_BLEND);
            gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            gl.glBlendEquation(GL_FUNC_ADD);

            gl.glDisable(GL_CULL_FACE);
            gl.glDepthFunc(GL_LESS);
            gl.glProgramUniform1f(rendering_program2, a_location, 0.0f);
            gl.glDrawArrays(GL_TRIANGLES, 0, myShape.getIndices().length);

            gl.glEnable(GL_CULL_FACE);
            gl.glCullFace(GL_FRONT);
            gl.glDepthFunc(GL_ALWAYS);
            gl.glProgramUniform1f(rendering_program2, a_location, factor*alpha);
            gl.glDrawArrays(GL_TRIANGLES, 0, myShape.getIndices().length);

            gl.glEnable(GL_CULL_FACE);
            gl.glCullFace(GL_FRONT);
            gl.glDepthFunc(GL_LEQUAL);
            gl.glProgramUniform1f(rendering_program2, a_location, (alpha-factor*alpha)/(1.0f-factor*alpha));
            gl.glDrawArrays(GL_TRIANGLES, 0, myShape.getIndices().length);

            gl.glEnable(GL_CULL_FACE);
            gl.glCullFace(GL_BACK);
            gl.glDepthFunc(GL_ALWAYS);
            gl.glProgramUniform1f(rendering_program2, a_location, factor*alpha);
            gl.glDrawArrays(GL_TRIANGLES, 0, myShape.getIndices().length);

            //gl.glEnable(GL_CULL_FACE);	//These two, try with and without
            //gl.glCullFace(GL_BACK);
            gl.glDisable(GL_CULL_FACE);
            gl.glDepthFunc(GL_LEQUAL);
            gl.glProgramUniform1f(rendering_program2, a_location, (alpha-factor*alpha)/(1.0f-factor*alpha));
            gl.glDrawArrays(GL_TRIANGLES, 0, myShape.getIndices().length);

            gl.glDisable(GL_BLEND);
        } else {
            gl.glEnable(GL_CULL_FACE);
            gl.glFrontFace(GL_CCW);
            gl.glEnable(GL_DEPTH_TEST);
            gl.glDepthFunc(GL_LEQUAL);

            gl.glDrawArrays(GL_TRIANGLES, 0, myShape.getIndices().length);
        }
    }
}
