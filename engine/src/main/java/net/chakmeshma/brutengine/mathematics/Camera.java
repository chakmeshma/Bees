package net.chakmeshma.brutengine.mathematics;

//TODO implement
public class Camera {
    private float[] projectionMatrix;
    private float[] viewMatrix;

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
            float fov,
            int viewPortWidth,
            int viewPortHeight) {
    }


    public synchronized float[] getProjectionMatrix() {
        return projectionMatrix;
    }

    public synchronized float[] getViewMatrix() {
        return viewMatrix;
    }

    public void setViewport(int width, int height) {

    }
}
