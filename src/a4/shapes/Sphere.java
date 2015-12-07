package a4.shapes;

import a4.shapes.IShape;
import graphicslib3D.Point3D;
import graphicslib3D.Shape3D;
import graphicslib3D.Vector3D;
import graphicslib3D.Vertex3D;

import static java.lang.Math.*;

public class Sphere extends Shape3D implements IShape {
    private int numVertices, numIndices, prec = 48;
    private int[] indices;
    private Vertex3D[] vertices;

    public Sphere(int p) {
        prec = p;
        InitSphere();
    }

    public void InitSphere() {
        numVertices = (prec + 1) * (prec + 1);
        numIndices = (prec + 1) * (prec) * 6;
        vertices = new Vertex3D[numVertices];
        indices = new int[numIndices];

        for (int i = 0; i < numVertices; i++) {
            vertices[i] = new Vertex3D();
        }

        // calculate triangle vertices
        for (int i = 0; i <= prec; i++) {
            for (int j = 0; j <= prec; j++) {    // calculate vertex location
                float y = (float) cos(toRadians(180 - i * 180 / prec));
                float x = -(float) cos(toRadians(j * 360.0 / prec)) * (float) abs(cos(asin(y)));
                float z = (float) sin(toRadians(j * 360.0f / (float) (prec))) * (float) abs(cos(asin(y)));
                vertices[i * (prec + 1) + j].setLocation(new Point3D(x, y, z));

                // calculate tangent vector
                float nextY = (float) cos(toRadians(180 - (i + 1) * 180 / prec));
                float nextX = -(float) cos(toRadians((j + 1) * 360.0 / prec)) * (float) abs(cos(asin(nextY)));
                float nextZ = (float) sin(toRadians((j + 1) * 360.0f / (float) (prec))) * (float) abs(cos(asin(nextY)));
                Vector3D thisPt = new Vector3D(x, y, z);
                Vector3D nextPt = new Vector3D(nextX, nextY, nextZ);
                Vector3D tangent = nextPt.minus(thisPt);
                vertices[i * (prec + 1) + j].setTangent(tangent);

                // calculate texture coordinates
                vertices[i * (prec + 1) + j].setS((float) j / (float) (prec));
                vertices[i * (prec + 1) + j].setT((float) i / (float) (prec));

                // calculate normal vector
                vertices[i * (prec + 1) + j].setNormal(new Vector3D(vertices[i * (prec + 1) + j].getLocation()));
            }
        }
        // calculate triangle indices
        for (int i = 0; i < prec; i++) {
            for (int j = 0; j < prec; j++) {
                indices[6 * (i * (prec + 1) + j) + 0] = i * (prec + 1) + j;
                indices[6 * (i * (prec + 1) + j) + 1] = i * (prec + 1) + j + 1;
                indices[6 * (i * (prec + 1) + j) + 2] = (i + 1) * (prec + 1) + j;
                indices[6 * (i * (prec + 1) + j) + 3] = i * (prec + 1) + j + 1;
                indices[6 * (i * (prec + 1) + j) + 4] = (i + 1) * (prec + 1) + j + 1;
                indices[6 * (i * (prec + 1) + j) + 5] = (i + 1) * (prec + 1) + j;
            }
        }
    }

    public int[] getIndices() {
        return indices;
    }

    public Vertex3D[] getVertices() {
        return vertices;
    }
}