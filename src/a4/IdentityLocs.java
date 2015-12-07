package a4;

import com.jogamp.opengl.GL4;

import java.util.HashMap;

/**
 * Created by Connor on 10/14/2015.
 */
public class IdentityLocs {
    private static HashMap<String, Integer> hashMap;

    public static final String RENDERING_PROGRAM1 = "rendering_program1",
            RENDERING_PROGRAM2 = "rendering_program2";

    public static int get(String key) {
        if (hashMap == null) {
            hashMap = new HashMap<>();
        }
        return hashMap.get(key);
    }

    public static void put(String key, int value) {
        if (hashMap == null) {
            hashMap = new HashMap<>();
        }
        hashMap.put(key, value);
    }
}