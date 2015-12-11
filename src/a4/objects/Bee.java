package a4.objects;

import a4.shapes.ImportedModel;
import graphicslib3D.Material;
import graphicslib3D.Matrix3D;

/**
 * Created by Connor on 11/11/2015.
 */
public class Bee extends WorldObject {
    private final String modelPath = "src/a4/bee_04.obj";
    private final String texturePath = "src/a4/colour.png";
    public Bee() {
        myShape = new ImportedModel(modelPath);
        myMaterial = Material.SILVER;
        transparent = true;
        normalURL = "src/a4/normal2.jpg";
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
}
