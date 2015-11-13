package a3.objects;

import a3.ImportedModel;
import a3.shapes.Sphere;
import graphicslib3D.Material;
import graphicslib3D.Matrix3D;
import graphicslib3D.Vector3D;

/**
 * Created by Connor on 11/11/2015.
 */
public class Dome extends ClippedObject {
    public Dome() {
        float[] cp = {0.0f, 1.0f, 0.0f, 0.8f};
        clipPlane = cp;
        //myShape = new ImportedModel("src/a3/shuttle.obj");
        myShape = new Sphere(48);
        myMaterial = new Material(
                new float[]{0.135F, 0.2225F, 0.1575F, 0.95F}, // ambient
                new float[]{0.54F, 0.89F, 0.63F, 0.95F}, // diffuse
                new float[]{0.3162F, 0.3162F, 0.3162F, 0.95F}, // specular
                new float[]{0.0F, 0.0F, 0.0F, 1.0F}, // emission
                12.8F); // shininess
        setTextureURL("src/a3/moon.jpg");
        setTranslationRate(0,.0,0);
        setRotationRate(0,0,0);
        Matrix3D scale = new Matrix3D();
        scale.scale(10,15,10);
        setScale(scale);
        Matrix3D startPos = new Matrix3D();
        startPos.translate(0,-0,0);
        setTranslation(startPos);
        Matrix3D startRot = new Matrix3D();
        startRot.rotate(90, new Vector3D(1,0,0));
        setRotation(startRot);
    }
}
