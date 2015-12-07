package a3.objects;

import a3.shapes.Tetrahedron;
import graphicslib3D.Material;
import graphicslib3D.Matrix3D;

/**
 * Created by Connor on 11/12/2015.
 */
public class Debris extends WorldObject {
    private final String texturePath = "src/a3/spaceRock.jpg";

    public Debris() {
        myShape = new Tetrahedron();
        myMaterial = Material.GOLD;
        setTextureURL(texturePath);
        setTranslationRate(0, 0, 0);
        setRotationRate((int) (Math.random()*5), (int) (Math.random()*5), (int) (Math.random()*5));
        Matrix3D scale = new Matrix3D();
        scale.scale(Math.random(), Math.random(), Math.random());
        setScale(scale);
        Matrix3D startPos = new Matrix3D();
        startPos.translate((Math.random() - .5) * 7, (Math.random() - .5) * 7, -9 + (Math.random() - .5) * 5);
        setTranslation(startPos);
    }

}
