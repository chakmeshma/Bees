package net.chakmeshma.brutengine.rendering;

import android.opengl.GLES20;

import net.chakmeshma.brutengine.development.DebugUtilities;
import net.chakmeshma.brutengine.development.exceptions.InitializationException;
import net.chakmeshma.brutengine.development.exceptions.InvalidOperationException;
import net.chakmeshma.brutengine.development.exceptions.RenderException;
import net.chakmeshma.brutengine.mathematics.Camera;
import net.chakmeshma.brutengine.mathematics.Transform;

import java.util.HashMap;
import java.util.Map;

import static net.chakmeshma.brutengine.utilities.GLUtilities.GLAdaptionRule.GL_DRAW_ELEMENTS__MESH_INDICES_TYPE;
import static net.chakmeshma.brutengine.utilities.GLUtilities.GLAdaptionRule.GL_VERTEX_ATTRIB_POINTER__ATTRIBUTE_REF_SINGLE_TYPE;
import static net.chakmeshma.brutengine.utilities.GLUtilities.getGLTypeIdentifier;


//  This class is not thread-safe and should only be run by GL-thread.
public interface Renderable {
    void render() throws RenderException, InitializationException;

    class SimpleRenderable implements Renderable {
        //region sonst
        private boolean _meshSet;
        private boolean _programSet;
        private boolean _transformSet;
        private boolean _cameraSet;
        private boolean _linked; //is the attribute buffer mapping there
        private Mesh mesh;
        private Program program;
        private Transform transform;
        private Camera camera;
        private Map<Program.AttributeReference, Mesh.ARBuffer> attributeBufferMap;

        public SimpleRenderable(Program program,
                                Mesh mesh,
                                Transform transform,
                                Camera camera) throws InitializationException {
            setProgram(program);
            setMesh(mesh);

            this.attributeBufferMap = new HashMap<Program.AttributeReference, Mesh.ARBuffer>();


            setTransform(transform);
            setCamera(camera);
        }

        protected boolean isSet() {
            return _meshSet && _programSet && _transformSet && _cameraSet;
        }

        private void setMesh(Mesh mesh) throws InitializationException {
            this.mesh = mesh;

            this._meshSet = true;

            clearLinked();
        }

        private void setProgram(Program program) throws InitializationException {
            this.program = program;

            this._programSet = true;

            clearLinked();
        }

        private void setTransform(Transform transform) {
            this.transform = transform;

            this._transformSet = true;
        }

        private void setCamera(Camera camera) {
            this.camera = camera;

            this._cameraSet = true;
        }

        private boolean isLinked() {
            return _linked;
        }

        private void setLinked() {
            this._linked = true;
        }

        private void clearLinked() {
            this._linked = false;
        }
        //endregion

        public void render() throws RenderException, InitializationException {
            //region assert renderable
            if (!isSet()) {
                DebugUtilities.logWarning("nothing to be rendered!");
                return;
            }
            //endregion

            program.bind();

            //region attribute buffer linking
            for (Program.AttributeReference attributeReference : program.getAttributeReferences()) {
                if (attributeBufferMap.containsKey(attributeReference)) {
                    Mesh.ARBuffer arBuffer = attributeBufferMap.get(attributeReference);



                    GLES20.glEnableVertexAttribArray(attributeReference.getIndex());

                    mesh.getVertexArrayBuffers()[bufferIndex].bind();

                    GLES20.glVertexAttribPointer(
                            attributeReference.getIndex(),
                            attributeReference.getValuesCount(),
                            getGLTypeIdentifier(attributeReference.getValueType(), GL_VERTEX_ATTRIB_POINTER__ATTRIBUTE_REF_SINGLE_TYPE),
                            false,
                            attributeBufferMap.get(attributeReference).getBufferStride(),
                            attributeBufferMap.get(attributeReference).getBufferOffset());
                } else
                    DebugUtilities.logWarning(String.format("unused/[unbound to buffer] attribute \"%s\" defined in shader(s)", attributeReference.getName()));
            }
            //endregion

            //region uniforms update
            for (Program.Uniform uniform : program.getDefinedUniforms(Program.DefinedUniformType.MODEL_MATRIX_UNIFORM)) {
                try {
                    uniform.setFloatValues(transform.getModelMatrix());
                } catch (InvalidOperationException e) {
                    throw new RenderException(e.getMessage());
                }
            }

            for (Program.Uniform uniform : program.getDefinedUniforms(Program.DefinedUniformType.VIEW_MATRIX_UNIFORM)) {
                try {
                    uniform.setFloatValues(camera.getViewMatrix());
                } catch (InvalidOperationException e) {
                    throw new RenderException(e.getMessage());
                }
            }

            for (Program.Uniform uniform : program.getDefinedUniforms(Program.DefinedUniformType.PROJECTION_MATRIX_UNIFORM)) {
                try {
                    uniform.setFloatValues(camera.getProjectionMatrix());
                } catch (InvalidOperationException e) {
                    throw new RenderException(e.getMessage());
                }
            }

            for (Program.Uniform uniform : program.getDefinedUniforms(Program.DefinedUniformType.ROTATION_MATRIX_UNIFORM)) {
                try {
                    uniform.setFloatValues(getRotationMatrix());
                } catch (InvalidOperationException e) {
                    throw new RenderException(e.getMessage());
                }
            }
            //endregion

            mesh.getIndicesBuffer().bind();

            //region drawing
            GLES20.glDrawElements(mesh.getPrimitiveAssemblyMode(), mesh.getIndicesCount(), getGLTypeIdentifier(mesh.getIndicesClass(), GL_DRAW_ELEMENTS__MESH_INDICES_TYPE), mesh.getIndicesOffset());
            //endregion

            //region unbinding
            mesh.getIndicesBuffer().unbind();

            for (Program.AttributeReference attributeReference : program.getAttributeReferences()) {
                if (attributeBufferMap.containsKey(attributeReference)) {
                    int bufferIndex;

                    bufferIndex = attributeBufferMap.get(attributeReference).getBufferIndex();
                    GLES20.glDisableVertexAttribArray(attributeReference.getIndex());

                    mesh.getVertexArrayBuffers()[bufferIndex].unbind();
                }
            }

            program.unbind();
            //endregion
        }
    }
    //endregion
}
