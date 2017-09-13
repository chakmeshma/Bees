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

    public static int calculateMatrixHash(float[] matrix) {
        float calculateSum = 0;

        if (matrix == null)
            return 0;

        for (int i = 0; i < matrix.length; i++) {
            calculateSum += (matrix[i] * 1000.0f) * (i + 1);
        }

        return Math.round(calculateSum);
    }
}
