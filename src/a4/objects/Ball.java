package a4.objects;

import a4.shapes.Sphere;
import graphicslib3D.Material;
import graphicslib3D.Matrix3D;

/**
 * Created by Connor on 12/6/2015.
 */
public class Ball extends WorldObject {
    private final String texturePath = "src/a3/earth.jpg";

    public Ball() {
        myShape = new Sphere(48);
        // blueish material to look earth-ish
        myMaterial = new Material(
                new float[]{0.3F, 0.3F, 0.4F, 1.0F}, // ambient
                new float[]{0.6F, 0.6F, 0.9F, 1.0F}, // diffuse
                new float[]{0.1F, 0.1F, 0.2F, 1.0F}, // specular
                new float[]{0.0F, 0.0F, 0.0F, 1.0F}, // emission
                12.8F); // shininess
        setTextureURL(texturePath);
        normalURL = "src/a4/normal.jpg";
        setRotationRate(0, 1, 0);
        Matrix3D scale = new Matrix3D();
        scale.scale(.25, .25, .25);
        setScale(scale);
        Matrix3D startPos = new Matrix3D();
        startPos.translate(0, 0, 0);
        setTranslation(startPos);
    }
}
