package a3.objects;

import a3.IdentityLocs;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import graphicslib3D.Matrix3D;
import graphicslib3D.MatrixStack;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL.GL_TRIANGLES;

/**
 * Created by Connor on 11/11/2015.
 */
public class InstancedObject extends WorldObject {
    private int numInstances;

    public InstancedObject(int numInstances) {
        super();
        this.numInstances = numInstances;
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
        gl.glUniformMatrix4fv(IdentityLocs.getnlocation(), 1, false, (mvStack.peek().inverse().transpose().getFloatValues()), 0);

        // bind vertex values
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[index]);
        gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);

        // bind normal values
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[index + 2]);
        gl.glVertexAttribPointer(1, 3, GL.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(1);

        // bind texture values
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[index + 1]);
        gl.glVertexAttribPointer(2, 2, GL.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(2);

        gl.glActiveTexture(gl.GL_TEXTURE0);
        gl.glBindTexture(gl.GL_TEXTURE_2D, texture);

        gl.glEnable(GL_CULL_FACE);
        gl.glFrontFace(GL_CCW);
        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);
        // draw arrays
        gl.glDrawArraysInstanced(GL_TRIANGLES, 0, myShape.getIndices().length, numInstances);
        mvStack.popMatrix(); // pop rotate
        mvStack.popMatrix(); // pop translate
    }
}
