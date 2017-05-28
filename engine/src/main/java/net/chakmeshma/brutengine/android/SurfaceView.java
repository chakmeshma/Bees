package net.chakmeshma.brutengine.android;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import net.chakmeshma.brutengine.Engine;

public class SurfaceView extends GLSurfaceView {
    private final float rotationSpeed = 0.1f;
    private final float zoomSpeed = 20f;
    private GameRenderer renderer;
    private float lastX = Float.NaN;
    private float lastY = Float.NaN;
    private ScaleGestureDetector scaleGestureDetector;

    //region initialization/construction
    public SurfaceView(GameRenderer renderer) {
        super((Context) Engine.context);

        this.renderer = renderer;

        init();
    }

    public SurfaceView(AttributeSet attrs, GameRenderer renderer) {
        super((Context) Engine.context, attrs);

        this.renderer = renderer;

        init();
    }

    private void init() {
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        getHolder().setFormat(PixelFormat.RGBA_8888);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);

        setEGLContextClientVersion(2);

        setRenderer(this.renderer);

        setRenderMode(RENDERMODE_CONTINUOUSLY);

        initDetector();
    }

    private void initDetector() {
        scaleGestureDetector = new ScaleGestureDetector((Context) Engine.context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {

                float scaleFactor = detector.getScaleFactor() - 1;
                scaleFactor *= SurfaceView.this.zoomSpeed;

                renderer.getCamera().zoomCamera(-scaleFactor);

                return true;
            }
        });
    }
    //endregion

    @Override
    public boolean onTouchEvent(MotionEvent event) {
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
                            renderer.getCamera().rotateCamera(dx * rotationSpeed, dy * rotationSpeed, 0.0f); //reverse x y order if in landscape mode
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

    public GameRenderer getRenderer() {
        return this.renderer;
    }
}
