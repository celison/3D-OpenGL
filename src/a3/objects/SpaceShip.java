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
    private final String modelPath = "src/a3/shuttle.obj";
    private final String texturePath = "src/a3/spaceRock.jpg";
    public SpaceShip() {
        super(27);
        myShape = new ImportedModel(modelPath);
        myMaterial = Material.SILVER;
        setTextureURL(texturePath);
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
