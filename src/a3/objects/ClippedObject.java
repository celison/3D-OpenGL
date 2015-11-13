package a3.objects;

import a3.IdentityLocs;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import graphicslib3D.Matrix3D;
import graphicslib3D.MatrixStack;

import java.nio.FloatBuffer;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL.GL_TRIANGLES;

/**
 * Created by Connor on 11/11/2015.
 */
public class ClippedObject extends WorldObject {
    protected float[] clipPlane = {0.0f, 1.0f, 0.0f, 0.2f};
    private final float[] noClip = {0.0f, 0.0f, 0.0f, 0.0f};
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
        gl.glProgramUniform4fv(IdentityLocs.getRendProg(), IdentityLocs.getClip_plane(), 1, clipPlane, 0);

        gl.glEnable(GL2GL3.GL_CLIP_DISTANCE0);

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

        gl.glUniform1i(IdentityLocs.getFlip_location(), 0);
        gl.glEnable(GL_CULL_FACE);
        gl.glFrontFace(GL_CCW);
        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);
        // draw arrays
        gl.glDrawArrays(GL_TRIANGLES, 0, myShape.getIndices().length);

        gl.glUniform1i(IdentityLocs.getFlip_location(), 1);
        gl.glEnable(GL_CULL_FACE);
        gl.glFrontFace(GL_CW);
        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);
        // draw arrays
        gl.glDrawArrays(GL_TRIANGLES, 0, myShape.getIndices().length);
        mvStack.popMatrix(); // pop rotate
        mvStack.popMatrix(); // pop translate

        gl.glUniform1i(IdentityLocs.getFlip_location(), 0);
        gl.glProgramUniform4fv(IdentityLocs.getRendProg(), IdentityLocs.getClip_plane(), 1, noClip, 0);
    }
}
