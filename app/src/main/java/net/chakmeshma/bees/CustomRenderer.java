package net.chakmeshma.bees;

import android.content.Context;
import android.opengl.GLSurfaceView.Renderer;

import net.chakmeshma.brutengine.development.DebugUtilities;
import net.chakmeshma.brutengine.development.exceptions.InitializationException;
import net.chakmeshma.brutengine.development.exceptions.RenderException;
import net.chakmeshma.brutengine.mathematics.Camera;
import net.chakmeshma.brutengine.mathematics.Transform;
import net.chakmeshma.brutengine.rendering.Mesh;
import net.chakmeshma.brutengine.rendering.Mesh.ObjFile;
import net.chakmeshma.brutengine.rendering.Program;
import net.chakmeshma.brutengine.rendering.Renderable;
import net.chakmeshma.brutengine.rendering.Renderable.SimpleRenderable;
import net.chakmeshma.brutengine.rendering.StepLoadListener;
import net.chakmeshma.brutengine.rendering.VariableReferenceable;

import java.util.HashMap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_BACK;
import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_CULL_FACE;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_DITHER;
import static android.opengl.GLES20.GL_FUNC_ADD;
import static android.opengl.GLES20.GL_LESS;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;
import static android.opengl.GLES20.GL_SRC_ALPHA;
import static android.opengl.GLES20.glBlendEquation;
import static android.opengl.GLES20.glBlendFunc;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glCullFace;
import static android.opengl.GLES20.glDepthFunc;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glFlush;
import static android.opengl.GLES20.glViewport;
import static net.chakmeshma.bees.GameActivity.MESSAGE_EXTEND_LOAD_PARTS_COUNT;
import static net.chakmeshma.bees.GameActivity.MESSAGE_PART_LOADED;
import static net.chakmeshma.bees.GameActivity.sendMessageToUIThreadHandler;
import static net.chakmeshma.brutengine.development.DebugUtilities.FramerateCapture.pushTimestamp;


class CustomRenderer implements Renderer {
    private final Object renderingPausedLock = new Object();
    private Camera camera;
    private Context context;
    private Renderable[] renderables;
    private boolean _renderingPaused = false;

    //region initialization/construction
    CustomRenderer(Context context) {
        this.context = context;
    }

    private void initState() {
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        glEnable(GL_BLEND);
        glBlendEquation(GL_FUNC_ADD);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);

        glDisable(GL_DITHER);

        glFlush();
    }

    private void init() throws InitializationException {
        initState();

        try {
            initDrawables();
        } catch (Exception e) {
            throw new InitializationException(e.getMessage());
        }
    }

    private void initDrawables() throws InitializationException {
        renderables = new Renderable[1];

        HashMap<Program.DefinedUniformType, VariableReferenceable.VariableMatcher> definedUniforms = new HashMap<>();
        definedUniforms.put(Program.DefinedUniformType.MODEL_MATRIX_UNIFORM, new VariableReferenceable.VariableMatcher.EqualityMatcher("mat4", "modelMatrix"));
        definedUniforms.put(Program.DefinedUniformType.VIEW_MATRIX_UNIFORM, new VariableReferenceable.VariableMatcher.EqualityMatcher("mat4", "viewMatrix"));
        definedUniforms.put(Program.DefinedUniformType.PROJECTION_MATRIX_UNIFORM, new VariableReferenceable.VariableMatcher.EqualityMatcher("mat4", "projectionMatrix"));
        definedUniforms.put(Program.DefinedUniformType.ROTATION_MATRIX_UNIFORM, new VariableReferenceable.VariableMatcher.EqualityMatcher("mat3", "rotationMatrix"));

        Program brutProgram = new Program(context, "shader.vert", "shader.frag", definedUniforms);

        camera = new Camera(
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                10.0f,
                0.1f,
                100.0f,
                60.0f,
                300,
                300);

        HashMap<String, Integer> mappingWithNormal = new HashMap<>();

        mappingWithNormal.put("inputPosition", 0);
        mappingWithNormal.put("inputNormal", 1);

        ObjFile[] objFiles = new ObjFile[1];

        //objFiles[0] = new ObjFile(context, "ico outersphere.obj");
        objFiles[0] = new ObjFile(context, "ico.obj");

        Mesh[] meshes = new Mesh[1];

        StepLoadListener meshStepLoadListener = new StepLoadListener() {
            @Override
            public void setExtraPartCount(int extraPartCount) {
                sendMessageToUIThreadHandler(MESSAGE_EXTEND_LOAD_PARTS_COUNT, extraPartCount);
            }

            @Override
            public void partLoaded() {
                sendMessageToUIThreadHandler(MESSAGE_PART_LOADED);
            }
        };

        meshes[0] = new Mesh(objFiles[0], meshStepLoadListener);

        renderables[0] = new SimpleRenderable(brutProgram, meshes[0], new Transform(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f), camera);
    }
    //endregion

    //region rendering pause/resume handling
    void resumeRendering() {
        synchronized (renderingPausedLock) {
            this._renderingPaused = false;
        }
    }

    void pauseRendering() {
        synchronized (renderingPausedLock) {
            this._renderingPaused = true;
        }
    }

    boolean isRenderingPaused() {
        boolean paused = false;

        synchronized (renderingPausedLock) {
            paused = this._renderingPaused;
        }

        return paused;
    }
    //endregion

    //region Renderer implementation
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

        camera.setViewport(width, height);

        DebugUtilities.checkAssertGLError("directly after state onSurfaceChanged");
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (isRenderingPaused())
            return;

        if (renderables != null && renderables.length > 0) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            for (int i = 0; i < renderables.length; i++) {
                try {
                    renderables[i].render();
                } catch (RenderException | InitializationException e) {
                    throw new RuntimeException(e);
                }
            }

            glFlush();
            GameActivity.incrementCountGLFlushes();
            pushTimestamp();
        } else {
//            DebugUtilities.logWarning("nothing to draw!");
        }

//        DebugUtilities.checkAssertGLError("directly after state onDrawFrame");
    }
    //endregion

    Camera getCamera() {
        return camera;
    }
}
