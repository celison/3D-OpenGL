package a4.objects;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import graphicslib3D.Matrix3D;
import graphicslib3D.MatrixStack;

/**
 * Created by Connor on 10/14/2015.
 */
public interface IGLDrawable {
//    void draw(GLAutoDrawable glAutoDrawable, MatrixStack mv, Matrix3D p);
    void init(GLAutoDrawable glAutoDrawable);
    void setupBuffers(GL4 gl, int[] vbo, int index);
    int getIndexesUsed();
}
