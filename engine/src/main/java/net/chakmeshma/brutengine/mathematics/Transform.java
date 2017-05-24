package net.chakmeshma.brutengine.mathematics;

import android.opengl.Matrix;


//TODO add caching
public class Transform {
    private static final float[] defaultEulerRotation = new float[]{1.0f, 1.0f, 1.0f};
    private static final float[] defaultScale = new float[]{1.0f, 1.0f, 1.0f};
    private float x, y, z;
    private float eulerRotationX, eulerRotationY, eulerRotationZ;
    private float scaleX, scaleY, scaleZ;
    private float pivotX, pivotY, pivotZ;
    private float[] rotationMatrix;
    private float[] translationMatrix;
    private float[] scaleMatrix;
    private float[] modelMatrix;
    private float[] identityMatrix;

    public Transform(float pivotX, float pivotY, float pivotZ, float x, float y, float z) {
        this.pivotX = pivotX;
        this.pivotY = pivotY;
        this.pivotZ = pivotZ;

        initMatrices();

        setTranslation(x, y, z);
        setEulerRotation(defaultEulerRotation[0], defaultEulerRotation[1], defaultEulerRotation[2]);
        setScale(defaultScale[0], defaultScale[1], defaultScale[2]);
    }

    public synchronized void setTranslation(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;

        computeTranslationMatrix();

        computeModelMatrix();
    }

    public synchronized void setEulerRotation(float x, float y, float z) {
        this.eulerRotationX = x;
        this.eulerRotationY = y;
        this.eulerRotationZ = z;

        computeRotationMatrix();

        computeModelMatrix();
    }

    public synchronized void setScale(float x, float y, float z) {
        this.scaleX = x;
        this.scaleY = y;
        this.scaleZ = z;

        computeScaleMatrix();

        computeModelMatrix();
    }

    public synchronized float[] getModelMatrix() {
        return modelMatrix;
    }

    private synchronized void initMatrices() {
        translationMatrix = new float[16];
        Matrix.setIdentityM(translationMatrix, 0);

        rotationMatrix = new float[16];
        Matrix.setIdentityM(rotationMatrix, 0);

        scaleMatrix = new float[16];
        Matrix.setIdentityM(scaleMatrix, 0);

        modelMatrix = new float[16];
        Matrix.setIdentityM(modelMatrix, 0);

        identityMatrix = new float[16];
        Matrix.setIdentityM(identityMatrix, 0);
    }

    private synchronized void computeRotationMatrix() {
        Matrix.setRotateEulerM(this.rotationMatrix, 0, this.eulerRotationX, this.eulerRotationY, this.eulerRotationZ);
    }

    private synchronized void computeModelMatrix() {
        Matrix.translateM(this.modelMatrix, 0, this.identityMatrix, 0, -this.pivotX, -this.pivotY, -this.pivotZ);
        Matrix.multiplyMM(this.modelMatrix, 0, this.scaleMatrix, 0, this.modelMatrix, 0);
        Matrix.multiplyMM(this.modelMatrix, 0, this.rotationMatrix, 0, this.modelMatrix, 0);
        Matrix.translateM(this.modelMatrix, 0, this.modelMatrix, 0, this.pivotX, this.pivotY, this.pivotZ);
        Matrix.multiplyMM(this.modelMatrix, 0, this.translationMatrix, 0, this.modelMatrix, 0);
    }

    public synchronized float[] getRotationMatrix() {
        return this.rotationMatrix;
    }

    private synchronized void computeScaleMatrix() {
        Matrix.scaleM(this.scaleMatrix, 0, this.identityMatrix, 0, this.scaleX, this.scaleY, this.scaleZ);
    }

    private synchronized void computeTranslationMatrix() {
        Matrix.translateM(this.translationMatrix, 0, this.identityMatrix, 0, this.x, this.y, this.z);
    }
}
