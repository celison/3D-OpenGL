package a3.shapes;

import graphicslib3D.Shape3D;
import graphicslib3D.Vector3D;
import graphicslib3D.Vertex3D;

/**
 * Created by Connor on 10/23/2015.
 */
public class Tetrahedron extends Shape3D implements IShape {
    private int[] indicies;
    private Vertex3D[] verticies;

    public Tetrahedron() {
        init();
    }

    private void init() {
        float[] tetraPositions = {0f, -.5f, .5f, 0f, .5f, 0f, -.5f, -.5f, -.5f,
                0f, -.5f, .5f, .5f, -.5f, -.5f, 0f, .5f, 0f,
                0f, -.5f, .5f, -.5f, -.5f, -.5f, .5f, -.5f, -.5f,
                .5f, -.5f, -.5f, -.5f, -.5f, -.5f, 0f, .5f, 0f};
        float[] normal = {};
        verticies = new Vertex3D[tetraPositions.length / 3];
        indicies = new int[tetraPositions.length / 3];
        for (int i = 0; i < verticies.length; i++) {
            verticies[i] = new Vertex3D();
            verticies[i].setLocation(tetraPositions[i * 3], tetraPositions[(i * 3) + 1], tetraPositions[(i * 3) + 2]);
            switch (i % 3) {
                case 0:
                    verticies[i].setS(0);
                    verticies[i].setT(0);
                    break;
                case 1:
                    verticies[i].setS(.5);
                    verticies[i].setT(1);
                    break;
                case 2:
                    verticies[i].setS(1);
                    verticies[i].setT(0);
                    break;
            }
            verticies[i].setNormal(normal[i * 3], normal[(i * 3) + 1], normal[(i * 3) + 2]);
        }
        for (int i = 0; i < verticies.length; i++) {
            Vector3D thisPt = new Vector3D(verticies[i]);
            Vector3D nextPt;
            if (i == verticies.length - 1) {
                nextPt = new Vector3D(verticies[0]);
            } else {
                nextPt = new Vector3D(verticies[i + 1]);
            }
            Vector3D tangent = nextPt.minus(thisPt);
            verticies[i].setTangent(tangent);
            indicies[i] = i;
        }
    }

    @Override
    public int[] getIndices() {
        return indicies;
    }

    @Override
    public Vertex3D[] getVertices() {
        return verticies;
    }
}
