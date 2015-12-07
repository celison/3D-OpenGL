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
        double[] normal = calculateNormals(tetraPositions);
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

    private double[] calculateNormals(float[] verts) {
        double normal[] = new double[verts.length];

        Vector3D vectorA;
        Vector3D vectorB;
        Vector3D vectorC;

        for (int i = 0; i < verts.length / 9; i++) {
            // load points in a plane
            float x1 = verts[(i * 9)];
            float y1 = verts[(i * 9) + 1];
            float z1 = verts[(i * 9) + 2];

            float x2 = verts[(i * 9) + 3];
            float y2 = verts[(i * 9) + 4];
            float z2 = verts[(i * 9) + 5];

            float x3 = verts[(i * 9) + 6];
            float y3 = verts[(i * 9) + 7];
            float z3 = verts[(i * 9) + 8];

            // calculate vectors that make up the plane
            vectorA = new Vector3D(x1 - x2, y1 - y2, z1 - z2);
            vectorB = new Vector3D(x2 - x3, y2 - y3, z2 - z3);

            // cross multiply to find normal
            vectorC = vectorA.cross(vectorB);

            // insert normals into float array;
            for (int j = 0; j < 3; j++) {
                normal[i * 9 + (j * 3)] = vectorC.getX();
                normal[(i * 9) + ((j * 3) + 1)] = vectorC.getY();
                normal[(i * 9) + ((j * 3) + 2)] = vectorC.getZ();
            }
            System.out.println("vectorA = " + vectorA.toString() + "vectorB = " + vectorB.toString() + "vectorC = " + vectorC.toString());
        }
        return normal;
    }
}
