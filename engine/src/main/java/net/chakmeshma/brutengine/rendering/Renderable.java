package net.chakmeshma.brutengine.rendering;

import net.chakmeshma.brutengine.development.DebugUtilities;
import net.chakmeshma.brutengine.development.exceptions.InitializationException;
import net.chakmeshma.brutengine.development.exceptions.InvalidAdaptionOperationException;
import net.chakmeshma.brutengine.development.exceptions.InvalidMappingOperationException;
import net.chakmeshma.brutengine.development.exceptions.InvalidOperationException;
import net.chakmeshma.brutengine.development.exceptions.RenderException;
import net.chakmeshma.brutengine.mathematics.Camera;
import net.chakmeshma.brutengine.mathematics.Transform;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static android.opengl.GLES20.glDisableVertexAttribArray;
import static android.opengl.GLES20.glDrawElements;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glVertexAttribPointer;
import static net.chakmeshma.brutengine.utilities.GLUtilities.GLAdaptionRule.GL_DRAW_ELEMENTS__MESH_INDICES_TYPE;
import static net.chakmeshma.brutengine.utilities.GLUtilities.GLAdaptionRule.GL_VERTEX_ATTRIB_POINTER__ATTRIBUTE_REF_SINGLE_TYPE;
import static net.chakmeshma.brutengine.utilities.GLUtilities.getGLTypeIdentifier;


public interface Renderable {
    void render() throws RenderException, InitializationException;

    //region inner classes
    class AttributeBufferMapping {
        HashMap<String, Integer> bufferNameMapping;
        HashMap<String, Integer> bufferOffsetMapping;
        HashMap<String, Integer> bufferStrideMapping;

        public AttributeBufferMapping(HashMap<String, Integer> bufferNameMapping) throws InitializationException {
            if (bufferNameMapping == null && bufferNameMapping.size() < 1)
                throw new InitializationException("not a valid attribute buffer name mapping");

            this.bufferNameMapping = bufferNameMapping;
            bufferOffsetMapping = new HashMap<>();
            bufferStrideMapping = new HashMap<>();

            Iterator it = this.bufferNameMapping.entrySet().iterator();

            while (it.hasNext()) {
                Map.Entry<String, Integer> entry = (Map.Entry<String, Integer>) it.next();
                bufferOffsetMapping.put(entry.getKey(), 0);
                bufferStrideMapping.put(entry.getKey(), 0);
            }
        }

        boolean attributeNameExist(Program.AttributeReference attributeReference) {
            String attributeName = attributeReference.getName();

            Iterator it = bufferNameMapping.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Integer> entry = (Map.Entry<String, Integer>) it.next();

                if (entry.getKey().equals(attributeName))
                    return true;
            }

            return false;
        }

        int getBufferIndex(Program.AttributeReference attributeReference) throws InvalidOperationException {
            String attributeName = attributeReference.getName();

            Iterator it = bufferNameMapping.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Integer> entry = (Map.Entry<String, Integer>) it.next();

                if (entry.getKey().equals(attributeName))
                    return entry.getValue();
            }

            throw new InvalidMappingOperationException("attribute name doesn't exist in mapping", InvalidMappingOperationException.MappingOperationBufferProperty.ATTRIBUTE_MESH_BUFFER_INDEX);
        }

        int getBufferStride(Program.AttributeReference attributeReference) throws InvalidOperationException {
            String attributeName = attributeReference.getName();

            Iterator it = bufferStrideMapping.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Integer> entry = (Map.Entry<String, Integer>) it.next();

                if (entry.getKey().equals(attributeName))
                    return entry.getValue();
            }

            throw new InvalidMappingOperationException("attribute name doesn't exist in mapping", InvalidMappingOperationException.MappingOperationBufferProperty.ATTRIBUTE_BUFFER_POINTER_STRIDE);
        }

        int getBufferOffset(Program.AttributeReference attributeReference) throws InvalidOperationException {
            String attributeName = attributeReference.getName();

            Iterator it = bufferOffsetMapping.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Integer> entry = (Map.Entry<String, Integer>) it.next();

                if (entry.getKey().equals(attributeName))
                    return entry.getValue();
            }

            throw new InvalidMappingOperationException("attribute name doesn't exist in mapping", InvalidMappingOperationException.MappingOperationBufferProperty.ATTRIBUTE_BUFFER_POINTER_OFFSET);
        }
    }

    class SimpleRenderable implements Renderable {
        private boolean _meshSet = false;
        private boolean _programSet = false;
        private boolean _transformSet = false;
        private boolean _cameraSet = false;
        private AttributeBufferMapping attributeBufferMapping;
        private Mesh mesh;
        private Program program;
        private Transform transform;
        private Camera camera;

        public SimpleRenderable(Program program,
                                Mesh mesh,
                                Transform transform,
                                Camera camera) throws InitializationException {
            setProgram(program);
            setMesh(mesh);
            setTransform(transform);
            setCamera(camera);
        }

        private boolean isSet() {
            return _meshSet && _programSet && _transformSet && _cameraSet;
        }

        public void render() throws RenderException, InitializationException {
            if (!isSet())
                throw new RenderException("Renderable not set!");

            program.bind();

            for (Program.AttributeReference attributeReference : program.getAttributeReferences()) {
                if (attributeBufferMapping.attributeNameExist(attributeReference)) {
                    int bufferIndex;

                    try {
                        bufferIndex = attributeBufferMapping.getBufferIndex(attributeReference);
                    } catch (InvalidOperationException e) {
                        throw new RenderException(e.getMessage());
                    }

                    glEnableVertexAttribArray(attributeReference.getIndex());

                    try {
                        mesh.getVertexArrayBuffers()[bufferIndex].bind();

                        try {
                            glVertexAttribPointer(
                                    attributeReference.getIndex(),
                                    attributeReference.getValuesCount(),
                                    getGLTypeIdentifier(attributeReference.getValueType(), GL_VERTEX_ATTRIB_POINTER__ATTRIBUTE_REF_SINGLE_TYPE),
                                    false,
                                    attributeBufferMapping.getBufferStride(attributeReference),
                                    attributeBufferMapping.getBufferOffset(attributeReference));
                        } catch (InvalidOperationException e) {
                            throw new RenderException(e.getMessage());
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw new RenderException(e.getMessage());
                    }
                } else
                    DebugUtilities.logWarning(String.format("unused/[unbound to buffer] attribute \"%s\" defined in shader(s)", attributeReference.getName()));
            }

            for (Program.UniformReference uniformReference : program.getDefinedUniforms(Program.DefinedUniformType.MODEL_MATRIX_UNIFORM)) {
                try {
                    uniformReference.setFloatValues(transform.getModelMatrix());
                } catch (InvalidOperationException e) {
                    throw new RenderException(e.getMessage());
                }
            }

            for (Program.UniformReference uniformReference : program.getDefinedUniforms(Program.DefinedUniformType.VIEW_MATRIX_UNIFORM)) {
                try {
                    uniformReference.setFloatValues(camera.getViewMatrix());
                } catch (InvalidOperationException e) {
                    throw new RenderException(e.getMessage());
                }
            }

            for (Program.UniformReference uniformReference : program.getDefinedUniforms(Program.DefinedUniformType.PROJECTION_MATRIX_UNIFORM)) {
                try {
                    uniformReference.setFloatValues(camera.getProjectionMatrix());
                } catch (InvalidOperationException e) {
                    throw new RenderException(e.getMessage());
                }
            }

            for (Program.UniformReference uniformReference : program.getDefinedUniforms(Program.DefinedUniformType.ROTATION_MATRIX_UNIFORM)) {
                try {
                    uniformReference.setFloatValues(getRotationMatrix());
                } catch (InvalidOperationException e) {
                    throw new RenderException(e.getMessage());
                }
            }

            mesh.getIndicesBuffer().bind();

            try {
                glDrawElements(mesh.getPrimitiveAssemblyMode(), mesh.getIndicesCount(), getGLTypeIdentifier(mesh.getIndicesClass(), GL_DRAW_ELEMENTS__MESH_INDICES_TYPE), mesh.getIndicesOffset());
            } catch (InvalidAdaptionOperationException e) {
                throw new RenderException(e.getMessage());
            }

            mesh.getIndicesBuffer().unbind();

            for (Program.AttributeReference attributeReference : program.getAttributeReferences()) {
                if (attributeBufferMapping.attributeNameExist(attributeReference)) {
                    int bufferIndex;

                    try {
                        bufferIndex = attributeBufferMapping.getBufferIndex(attributeReference);
                    } catch (InvalidOperationException e) {
                        throw new RenderException(e.getMessage());
                    }

                    glDisableVertexAttribArray(attributeReference.getIndex());

                    try {
                        mesh.getVertexArrayBuffers()[bufferIndex].unbind();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw new RenderException(e.getMessage());
                    }
                }
            }

            program.unbind();

        }

        void setMesh(Mesh mesh) throws InitializationException {
            this.mesh = mesh;

            this.attributeBufferMapping = mesh.getAttributeBufferMapping();

            this._meshSet = true;
        }

        void setMeshes(Mesh[] meshes) throws InitializationException {
            setMesh(meshes[0]);
        }

        void setProgram(Program program) throws InitializationException {
            this.program = program;

            this._programSet = true;
        }

        void setPrograms(Program[] programs) throws InitializationException {
            setProgram(programs[0]);
        }

        void setTransform(Transform transform) {
            this.transform = transform;

            this._transformSet = true;
        }

        void setCamera(Camera camera) {
            this.camera = camera;

            this._cameraSet = true;
        }
    }
    //endregion
}
