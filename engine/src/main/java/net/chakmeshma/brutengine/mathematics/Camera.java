package net.chakmeshma.brutengine.mathematics;

import android.opengl.Matrix;

import net.chakmeshma.brutengine.rendering.Renderable;
import net.chakmeshma.brutengine.utilities.MathUtilities;

//TODO add caching
public class Camera {
    public static int lastUploadedProjectionMatrixHash;
    public static int lastUploadedViewMatrixHash;
    private float[] projectionMatrix;
    private float[] viewMatrix;
    private float[] viewTranslationMatrix;
    private float[] viewRotationMatrix;
    private float focusPointX;
    private float focusPointY;
    private float focusPointZ;
    //    private float eulerRotationX;
//    private float eulerRotationY;
//    private float eulerRotationZ;
    private float distance;
    private float near;
    private float far;
    private float fovy;
    private int viewPortWidth;
    private int viewPortHeight;
    private float rotationYaw = 0.0f;
    private float rotationPitch = 0.0f;
    //    private float rotationRoll.
    private int projectionMatrixHash;
    private int viewMatrixHash;

    public Camera(
            float focusPointX,
            float focusPointY,
            float focusPointZ,
            float distance,
            float near,
            float far,
            float fovy,
            float rotationYaw,
            float rotationPitch,
//            float rotationRoll,
            int viewPortWidth,
            int viewPortHeight) {
        this.focusPointX = focusPointX;
        this.focusPointY = focusPointY;
        this.focusPointZ = focusPointZ;
//        this.eulerRotationX = eulerRotationX;
//        this.eulerRotationY = eulerRotationY;
//        this.eulerRotationZ = eulerRotationZ;
        this.distance = distance;
        this.near = near;
        this.far = far;
        this.fovy = fovy;
        this.viewPortWidth = viewPortWidth;
        this.viewPortHeight = viewPortHeight;
        this.rotationYaw = rotationYaw;
        this.rotationPitch = rotationPitch;
//        this.rotationRoll = rotationRoll;

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

    public synchronized void setViewport(int width, int height) {
        this.viewPortWidth = width;
        this.viewPortHeight = height;

        computeProjectionMatrix();
    }
    //endregion

    //region getRatio*
    private synchronized float getWHRatio() {
        return (((float) this.viewPortWidth) / ((float) this.viewPortHeight));
    }

    private synchronized float getHWRatio() {
        return (((float) this.viewPortHeight) / ((float) this.viewPortWidth));
    }

    public synchronized float[] getRotationMatrix() {
        return this.viewRotationMatrix;
    }

    public synchronized void rotateCamera(float dYaw, float dPitch/*, float dRoll*/) {
        this.rotationYaw += dYaw;       //alpha
        this.rotationPitch += dPitch;   //beta
//        this.rotationRoll += dRoll;     //gamma

        if (this.rotationPitch > 89.0f)
            this.rotationPitch = 89.0f;
        else if (this.rotationPitch < -89.0f)
            this.rotationPitch = -89.0f;

        computeViewRotationMatrix();

        computeViewMatrix();
    }

    private synchronized void computeViewRotationMatrix() {
        float A = this.rotationPitch;
        float B = this.rotationYaw;
//        float C = this.rotationRoll;

//        viewRotationMatrix[0] = (float) (Math.cos(C) * Math.cos(B) - Math.sin(C) * Math.sin(A) * Math.sin(B));
//        viewRotationMatrix[1] = (float) (Math.sin(C) * Math.cos(B) + Math.cos(C) * Math.sin(A) * Math.sin(B));
//        viewRotationMatrix[2] = (float) (-Math.cos(A) * Math.sin(B));
//        viewRotationMatrix[3] = 0.0f;
//
//        viewRotationMatrix[4] = (float) (-Math.sin(C) * Math.cos(A));
//        viewRotationMatrix[5] = (float) (Math.cos(C) * Math.cos(A));
//        viewRotationMatrix[6] = (float) Math.sin(A);
//        viewRotationMatrix[7] = 0.0f;
//
//        viewRotationMatrix[8] = (float) (Math.cos(C) * Math.sin(B) + Math.sin(C) * Math.sin(A) * Math.cos(B));
//        viewRotationMatrix[9] = (float) (Math.sin(C) * Math.sin(B) - Math.cos(C) * Math.sin(A) * Math.cos(B));
//        viewRotationMatrix[10] = (float) (Math.cos(A) * Math.cos(B));
//        viewRotationMatrix[11] = 0.0f;
//
//        viewRotationMatrix[12] = 0.0f;
//        viewRotationMatrix[13] = 0.0f;
//        viewRotationMatrix[14] = 0.0f;
//        viewRotationMatrix[15] = 1.0f;

        Matrix.setIdentityM(this.viewRotationMatrix, 0);

        Matrix.translateM(this.viewRotationMatrix, 0, this.focusPointX, this.focusPointY, this.focusPointZ);

        Matrix.rotateM(this.viewRotationMatrix, 0, A, 1.0f, 0.0f, 0.0f);
        Matrix.rotateM(this.viewRotationMatrix, 0, B, 0.0f, 1.0f, 0.0f);

        Matrix.translateM(this.viewRotationMatrix, 0, -this.focusPointX, -this.focusPointY, -this.focusPointZ);

        Renderable.SimpleRenderable.lastUploadedCombinedRotationMatrixHash++;
    }

    public synchronized void zoomCamera(float dDistance) {
        this.distance += dDistance;

        computeViewTranslationMatrix();

        computeViewMatrix();
    }

    private synchronized void computeViewMatrix() {
        Matrix.multiplyMM(this.viewMatrix, 0, this.viewTranslationMatrix, 0, this.viewRotationMatrix, 0);

        calculateViewMatrixHash();
    }

    private synchronized void computeProjectionMatrix() {
        Matrix.perspectiveM(this.projectionMatrix, 0, this.fovy, getWHRatio(), this.near, this.far);

        calculateProjectionMatrixHash();
    }

    private void calculateProjectionMatrixHash() {
        this.projectionMatrixHash = MathUtilities.calculateMatrixHash(projectionMatrix);
    }

    private void calculateViewMatrixHash() {
        this.viewMatrixHash = MathUtilities.calculateMatrixHash(viewMatrix);
    }

    private synchronized void computeViewTranslationMatrix() {
        Matrix.translateM(this.viewTranslationMatrix, 0, MathUtilities.identityMatrix, 0, -this.focusPointX, -this.focusPointY, -this.focusPointZ - this.distance);
    }

    public synchronized int getViewMatrixHash() {
        return this.viewMatrixHash;
    }

    public synchronized int getProjectionMatrixHash() {
        return this.projectionMatrixHash;
    }
}
