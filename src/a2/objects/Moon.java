package a2.objects;

import a3.shapes.Sphere;
import com.jogamp.opengl.GLAutoDrawable;
import graphicslib3D.Matrix3D;
import graphicslib3D.MatrixStack;

import java.util.Vector;

/**
 * Created by Connor on 10/23/2015.
 */
public class Moon extends Orbitable {
    private Matrix3D myTranslation;
    private int orbitDistance;
    private float orbitRate;
    private boolean xyRotate;

    private String moonTexture = "a3/moon.jpg";

    public Moon(int p) {
        myShape = new Sphere(48);
        setTextureURL(moonTexture);
        orbitDistance = p;
        orbitRate = 0;
        orbitList = new Vector<>();
        myTranslation = new Matrix3D();
        setRotationRate(1, 1, 1);
        Matrix3D init = new Matrix3D();
        init.translate(0, 0, 0);
        setTranslation(init);
        init.setToIdentity();
        init.scale(.3, .3, .3);
        setScale(init);

        SpaceRock rock = new SpaceRock(1f, 1f);
        rock.setRotationRate(1,1,1);
        rock.scale(.5,.5,.5);
        addToOrbit(rock);
    }

    public void setOrbitRate(float o) {
        orbitRate = o;
    }

    public void setXyRotate(boolean b) {
        xyRotate = b;
    }

    @Override
    public void draw(GLAutoDrawable glAutoDrawable, MatrixStack mvStack, Matrix3D pMat) {
        double amt = (double) (System.currentTimeMillis() % 360000) / 1000.0;
        myTranslation.setToIdentity();
        if (xyRotate) {
            myTranslation.translate(0.0f, Math.sin(amt * orbitRate) * orbitDistance, Math.cos(amt * orbitRate) * orbitDistance);
        } else {
            myTranslation.translate(Math.sin(amt * orbitRate) * orbitDistance, 0.0f, Math.cos(amt * orbitRate) * orbitDistance);
        }
        setTranslation(myTranslation);
        super.draw(glAutoDrawable, mvStack, pMat);
    }
}
