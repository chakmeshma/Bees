package net.chakmeshma.brutengine.android;

import android.opengl.GLSurfaceView;

import net.chakmeshma.brutengine.Engine;
import net.chakmeshma.brutengine.development.DebugUtilities;
import net.chakmeshma.brutengine.development.exceptions.InitializationException;
import net.chakmeshma.brutengine.development.exceptions.RenderException;
import net.chakmeshma.brutengine.mathematics.Camera;
import net.chakmeshma.brutengine.rendering.Renderable;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glFlush;
import static android.opengl.GLES20.glViewport;
import static net.chakmeshma.brutengine.development.DebugUtilities.FramerateCapture.pushTimestamp;


public abstract class GameRenderer implements GLSurfaceView.Renderer {
    private final Object renderingPausedLock = new Object();
    protected Camera theCamera;
    //    protected List<Renderable> renderables;
    protected Renderable.SimpleSharedCameraGroupRenderable renderableGroup;
    private boolean _renderingPaused = false;

    //region initialization/construction
    protected abstract void initState();

    private void init() throws InitializationException {
        initState();

        try {
            initDrawables();
        } catch (Exception e) {
            throw new InitializationException(e.getMessage());
        }
    }

    protected abstract void initDrawables() throws InitializationException;
    //endregion

    //region rendering pause/resume handling
    public void resumeRendering() {
        synchronized (renderingPausedLock) {
            this._renderingPaused = false;
        }
    }

    public void pauseRendering() {
        synchronized (renderingPausedLock) {
            this._renderingPaused = true;
        }
    }

    private boolean isRenderingPaused() {
        boolean paused = false;

        synchronized (renderingPausedLock) {
            paused = this._renderingPaused;
        }

        return paused;
    }
    //endregion

    //region GameRenderer implementation
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        try {
            init();
        } catch (InitializationException e) {
            throw new RuntimeException(e);
        }

        DebugUtilities.checkAssertGLError("directly after state onSurfaceCreated");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        glViewport(0, 0, width, height);

        theCamera.setViewport(width, height);

        DebugUtilities.checkAssertGLError("directly after state onSurfaceChanged");
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (_renderingPaused)
            return;

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);


        try {
            this.renderableGroup.render();
        } catch (RenderException | InitializationException e) {
            throw new RuntimeException(e);
        }

        glFlush();
        Engine.context.incrementCountGLFlushes();
        pushTimestamp();
//        DebugUtilities.checkAssertGLError("directly after state onDrawFrame");
    }
    //endregion

    public Camera getCamera() {
        return this.theCamera;
    }
}
