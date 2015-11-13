package a2.objects;

import a3.shapes.Sphere;
import graphicslib3D.Matrix3D;

import java.util.Vector;

/**
 * Created by cjeli_000 on 10/14/2015.
 */

public class Sun extends Orbitable {
    private final String sunTexture = "a2/sun.jpg";

    public Sun(int p) {
        setTextureURL(sunTexture);
        myShape = new Sphere(p);
        orbitList = new Vector<>();
        Matrix3D initPos = new Matrix3D();
        initPos.translate(0, 0, 0);
        setTranslation(initPos);
        setRotationRate(1,1,1);

        Earth test = new Earth(4);
        test.setRotationRate(1,0,0);
        test.setOrbitRate(.5f);
        addToOrbit(test);

        Io io = new Io(2);
        io.setRotationRate(0,1,0);
        io.setOrbitRate(.8f);
        addToOrbit(io);

        SpikeBall sb = new SpikeBall(3, 1);
        sb.setRotationRate(0,1,0);
        sb.setOrbitRate(.8f);
        addToOrbit(sb);
    }
}
