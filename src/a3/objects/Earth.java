package a3.objects;

import a3.shapes.Sphere;
import a3.shapes.Tetrahedron;
import graphicslib3D.Material;
import graphicslib3D.Matrix3D;

/**
 * Created by Connor on 11/11/2015.
 */
public class Earth extends WorldObject {
    private final String texturePath = "src/a3/earth.jpg";
    public Earth() {
        myShape = new Sphere(48);
       // blueish material to look earth-ish
        myMaterial = new Material(
                new float[]{0.3F, 0.3F, 0.4F, 1.0F}, // ambient
                new float[]{0.6F, 0.6F, 0.9F, 1.0F}, // diffuse
                new float[]{0.1F, 0.1F, 0.2F, 1.0F}, // specular
                new float[]{0.0F, 0.0F, 0.0F, 1.0F}, // emission
                12.8F); // shininess
        setTextureURL(texturePath);
        setRotationRate(0,1,0);
        Matrix3D scale = new Matrix3D();
        scale.scale(2,2,2);
        setScale(scale);
        Matrix3D startPos = new Matrix3D();
        startPos.translate(0,0,-15);
        setTranslation(startPos);
    }
}
