package net.chakmeshma.brutengine.mathematics;

import android.opengl.Matrix;

//TODO add caching
public class Camera {
    private static float[] identityMatrix = new float[]{
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    };
    private float[] projectionMatrix;
    private float[] viewMatrix;
    private float[] viewTranslationMatrix;
    private float[] viewRotationMatrix;
    private float focusPointX;
    private float focusPointY;
    private float focusPointZ;
    private float eulerRotationX;
    private float eulerRotationY;
    private float eulerRotationZ;
    private float distance;
    private float near;
    private float far;
    private float fovy;
    private int viewPortWidth;
    private int viewPortHeight;

    public Camera(
            float focusPointX,
            float focusPointY,
            float focusPointZ,
            float eulerRotationX,
            float eulerRotationY,
            float eulerRotationZ,
            float distance,
            float near,
            float far,
            float fovy,
            int viewPortWidth,
            int viewPortHeight) {
        this.focusPointX = focusPointX;
        this.focusPointY = focusPointY;
        this.focusPointZ = focusPointZ;
        this.eulerRotationX = eulerRotationX;
        this.eulerRotationY = eulerRotationY;
        this.eulerRotationZ = eulerRotationZ;
        this.distance = distance;
        this.near = near;
        this.far = far;
        this.fovy = fovy;
        this.viewPortWidth = viewPortWidth;
        this.viewPortHeight = viewPortHeight;

        initMatrices();

        computeProjectionMatrix();
        computeViewTranslationMatrix();
        computeViewRotationMatrix();
        computeViewMatrix();
    }

    private synchronized void initMatrices() {
        viewMatrix = new float[16];
        Matrix.setIdentityM(viewMatrix, 0);

        projectionMatrix = new float[16];
        Matrix.setIdentityM(projectionMatrix, 0);

        viewTranslationMatrix = new float[16];
        Matrix.setIdentityM(viewTranslationMatrix, 0);

        viewRotationMatrix = new float[16];
        Matrix.setIdentityM(viewRotationMatrix, 0);
    }

    public synchronized float[] getViewMatrix() {
        return this.viewMatrix;
    }

    public synchronized float[] getProjectionMatrix() {
        return this.projectionMatrix;
    }

    public void setViewport(int width, int height) {
        this.viewPortWidth = width;
        this.viewPortHeight = height;

        computeProjectionMatrix();
    }

    //region getRatio*
    private synchronized float getWHRatio() {
        return (((float) viewPortWidth) / ((float) viewPortHeight));
    }

    private synchronized float getHWRatio() {
        return (((float) viewPortHeight) / ((float) viewPortWidth));
    }
    //endregion

    private synchronized void computeProjectionMatrix() {
        Matrix.perspectiveM(projectionMatrix, 0, this.fovy, getWHRatio(), this.near, this.far);
    }

    private synchronized void computeViewMatrix() {
        Matrix.multiplyMM(viewMatrix, 0, viewRotationMatrix, 0, viewTranslationMatrix, 0);
    }

    public void rotateCamera(float dEulerRotationX, float dEulerRotationY, float dEulerRotationZ) {
        this.eulerRotationX += dEulerRotationX;
        this.eulerRotationY += dEulerRotationY;
        this.eulerRotationZ += dEulerRotationZ;

        computeViewRotationMatrix();

        computeViewMatrix();
    }

    public void zoomCamera(float dDistance) {
        this.distance += dDistance;

        computeViewTranslationMatrix();

        computeViewMatrix();
    }

    private synchronized void computeViewRotationMatrix() {
        Matrix.setRotateEulerM(viewRotationMatrix, 0, this.eulerRotationX, this.eulerRotationY, this.eulerRotationZ);
    }

    public synchronized float[] getRotationMatrix() {
        return this.viewRotationMatrix;
    }

    private synchronized void computeViewTranslationMatrix() {
        Matrix.translateM(viewTranslationMatrix, 0, identityMatrix, 0, 0.0f, 0.0f, -this.distance);
    }
}
