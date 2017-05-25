package net.chakmeshma.brutengine.utilities;

/**
 * Created by chakmeshma on 24.05.2017.
 */

public class MathUtilities {
    public static final float[] identityMatrix = new float[]{
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    };

    public static boolean isNumberSquare(int number) {
        double sqrt = Math.sqrt(number);
        int x = (int) sqrt;
        return Math.pow(sqrt, 2) == Math.pow(x, 2);
    }

    public static int intSquare(int number) {
        return (int) Math.round(Math.sqrt(number));
    }
}
