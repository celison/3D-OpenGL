package a3.objects;

import a3.ImportedModel;
import a3.shapes.Sphere;
import com.jogamp.opengl.GLAutoDrawable;
import graphicslib3D.Material;
import graphicslib3D.Matrix3D;
import graphicslib3D.MatrixStack;

/**
 * Created by Connor on 11/11/2015.
 */
public class SpaceShip extends InstancedObject {
    public SpaceShip() {
        super(27);
        //float[] cp = {1.0f, 0.0f, 0.0f, 0.0f};
        //clipPlane = cp;
        myShape = new ImportedModel("src/a3/shuttle.obj");
        //myShape = new Sphere(48);
        myMaterial = Material.SILVER;
        setTextureURL("src/a3/spaceRock.jpg");
        setTranslationRate(0,.0,0);
        setRotationRate(0,0,0);
        Matrix3D scale = new Matrix3D();
        scale.scale(1,1,1);
        setScale(scale);
        Matrix3D startPos = new Matrix3D();
        startPos.translate(-3,-3,-3);
        setTranslation(startPos);
    }
    @Override
    public void draw(GLAutoDrawable glAutoDrawable, MatrixStack mvStack, Matrix3D pMat) {
        super.draw(glAutoDrawable, mvStack, pMat);
        //setTranslationRate(Math.random()-.5, Math.random()-.5, Math.random()-.5);
        //setRotationRate((int) (Math.random()*10-5),(int) (Math.random()*10-5), (int) (Math.random()*10-5));
    }
}
