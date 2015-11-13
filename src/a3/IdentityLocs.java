package a3;

import com.jogamp.opengl.GL4;

/**
 * Created by cjeli_000 on 10/14/2015.
 */
public class IdentityLocs {
    private static IdentityLocs instance;
    private static int rendProg;
    private static int projLoc;
    private static int mvLoc;
    private static int n_location;
    private static int flip_location;
    private static int clip_plane;

    private IdentityLocs(int renderingProgram, GL4 gl) {
        gl.glUseProgram(renderingProgram);
        rendProg = renderingProgram;
        mvLoc = gl.glGetUniformLocation(renderingProgram, "mv_matrix");
        projLoc = gl.glGetUniformLocation(renderingProgram, "proj_matrix");
        n_location = gl.glGetUniformLocation(renderingProgram, "normal_matrix");
        flip_location = gl.glGetUniformLocation(renderingProgram, "flipNormal");
        clip_plane = gl.glGetUniformLocation(renderingProgram, "clip_plane");
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

    public static int getnlocation() {
        return n_location;
    }

    public static int getFlip_location() {
        return flip_location;
    }

    public static int getClip_plane() {
        return clip_plane;
    }

    public static int getRendProg() { return rendProg; }
}
