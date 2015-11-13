package a2;

import com.jogamp.opengl.GL4;

/**
 * Created by cjeli_000 on 10/14/2015.
 */
public class IdentityLocs {
    private static IdentityLocs instance;

    private static int projLoc;
    private static int mvLoc;

    private IdentityLocs(int renderingProgram, GL4 gl) {
        gl.glUseProgram(renderingProgram);

        mvLoc = gl.glGetUniformLocation(renderingProgram, "mv_matrix");
        projLoc = gl.glGetUniformLocation(renderingProgram, "proj_matrix");
    }

    public static void init(int renderingProgram, GL4 gl) {
        instance = new IdentityLocs(renderingProgram, gl);
    }


    public static int getProjLoc() {
        return projLoc;
    }

    public static int getMvLoc() {
        return mvLoc;
    }
}
