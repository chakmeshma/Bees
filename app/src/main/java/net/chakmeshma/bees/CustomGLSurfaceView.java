package net.chakmeshma.bees;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;


class CustomGLSurfaceView extends GLSurfaceView {
    private CustomRenderer renderer;
    private float lastX = Float.NaN;
    private float lastY = Float.NaN;
    private ScaleGestureDetector scaleGestureDetector;

    //region initialization/construction
    public CustomGLSurfaceView(Context context) {
        super(context);

        init(context);
    }

    public CustomGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    private void init(Context context) {
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        getHolder().setFormat(PixelFormat.RGBA_8888);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);

        setEGLContextClientVersion(2);

        initDetector(context);
    }

    public CustomRenderer setupRenderer(Context context) {
        renderer = new CustomRenderer(context);

        setRenderer(renderer);

        setRenderMode(RENDERMODE_CONTINUOUSLY);

        return renderer;
    }

    private void initDetector(Context context) {
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {

                float scaleFactor = detector.getScaleFactor() - 1;
                scaleFactor *= 10.0f;

                renderer.getCamera().zoomCamera(scaleFactor);

                return true;
            }
        });
    }
    //endregion

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final float rotationSpeed = 0.01f;

        scaleGestureDetector.onTouchEvent(event);

        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_MOVE:
                float x = event.getX();
                float y = event.getY();

                if (Float.isNaN(lastX))
                    lastX = x;

                if (Float.isNaN(lastY))
                    lastY = y;

                float dx = x - lastX;
                float dy = y - lastY;

                float vectorLength = (float) Math.sqrt((dx * dx) + (dy * dy));

                switch (event.getPointerCount()) {
                    case 1:
                        if (vectorLength > 0.0f)
                            renderer.getCamera().rotateCamera(vectorLength * rotationSpeed, dy / vectorLength, dx / vectorLength, 0.0f); //reverse x y order if in landscape mode
                        break;
                    case 2:

                        break;
                }

                break;
        }

        lastX = event.getX();
        lastY = event.getY();

        return true;
    }

    public CustomRenderer getRenderer() {
        return renderer;
    }
}
