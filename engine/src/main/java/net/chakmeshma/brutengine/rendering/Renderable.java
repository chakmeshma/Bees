package net.chakmeshma.brutengine.rendering;

import android.opengl.GLES20;
import android.opengl.Matrix;

import net.chakmeshma.brutengine.development.DebugUtilities;
import net.chakmeshma.brutengine.development.exceptions.InitializationException;
import net.chakmeshma.brutengine.development.exceptions.InvalidOperationException;
import net.chakmeshma.brutengine.development.exceptions.RenderException;
import net.chakmeshma.brutengine.mathematics.Camera;
import net.chakmeshma.brutengine.mathematics.Transform;
import net.chakmeshma.brutengine.utilities.MathUtilities;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;


//  This class is not thread-safe and should only be run by GL-thread.
public interface Renderable {
    void render() throws RenderException, InitializationException;

    class SimpleRenderable implements Renderable {
        public static int lastUploadedCombinedRotationMatrixHash;
        private static EnumMap<Program.DefinedAttributeType, Mesh.DefinedBufferType> attributeLinkMap;
        private static Map<Program.AttributeReference, Mesh.ARBuffer> attributeARBufferMap;

        static {
            attributeLinkMap = new EnumMap<>(Program.DefinedAttributeType.class);

            attributeLinkMap.put(Program.DefinedAttributeType.POSITION_ATTRIBUTE, Mesh.DefinedBufferType.POSITIONS_BUFFER);
            attributeLinkMap.put(Program.DefinedAttributeType.NORMAL_ATTRIBUTE, Mesh.DefinedBufferType.NORMALS_BUFFER);
        }

        private EnumMap<Program.DefinedUniformType, UniformSetter> uniformLinkMap;
        private Map<Program.Uniform, UniformSetter> uniformSetterMap;
        //region sonst
        private boolean _meshSet;
        private boolean _programSet;
        private boolean _transformSet;
        private boolean _cameraSet;
        private boolean _hasTexture;
        private boolean _textureSet;
        private boolean _linked;        //is the attribute buffer mapping there
        private Mesh mesh;
        private Program program;
        private Transform transform;
        private Camera camera;
        private float[] combinedRotationMatrix;
        private int combinedRotationMatrixHash;

        {
            uniformLinkMap = new EnumMap<>(Program.DefinedUniformType.class);

            uniformLinkMap.put(Program.DefinedUniformType.MODEL_MATRIX_UNIFORM, new UniformSetter() {
                @Override
                public void set(Program.Uniform uniform) throws InvalidOperationException {
                    uniform.setFloatValues(SimpleRenderable.this.transform.getModelMatrix());
                }

                @Override
                int getHash() {
                    return SimpleRenderable.this.transform.getModelMatrixHash();
                }

                @Override
                int getLastUploadedHash() {
                    return Transform.lastUploadedModelMatrixHash;
                }

                @Override
                void setLastUploadedHash(int hash) {
                    Transform.lastUploadedModelMatrixHash = hash;
                }

            });

            uniformLinkMap.put(Program.DefinedUniformType.VIEW_MATRIX_UNIFORM, new UniformSetter() {
                @Override
                public void set(Program.Uniform uniform) throws InvalidOperationException {
                    uniform.setFloatValues(SimpleRenderable.this.camera.getViewMatrix());
                }

                @Override
                int getHash() {
                    return SimpleRenderable.this.camera.getViewMatrixHash();
                }

                @Override
                int getLastUploadedHash() {
                    return Camera.lastUploadedViewMatrixHash;
                }

                @Override
                void setLastUploadedHash(int hash) {
                    Camera.lastUploadedViewMatrixHash = hash;
                }

            });

            uniformLinkMap.put(Program.DefinedUniformType.PROJECTION_MATRIX_UNIFORM, new UniformSetter() {
                @Override
                public void set(Program.Uniform uniform) throws InvalidOperationException {
                    uniform.setFloatValues(SimpleRenderable.this.camera.getProjectionMatrix());
                }

                @Override
                int getHash() {
                    return SimpleRenderable.this.camera.getProjectionMatrixHash();
                }

                @Override
                int getLastUploadedHash() {
                    return Camera.lastUploadedProjectionMatrixHash;
                }

                @Override
                void setLastUploadedHash(int hash) {
                    Camera.lastUploadedProjectionMatrixHash = hash;
                }

            });

            uniformLinkMap.put(Program.DefinedUniformType.ROTATION_MATRIX_UNIFORM, new UniformSetter() {
                @Override
                public void set(Program.Uniform uniform) throws InvalidOperationException {
                    uniform.setFloatValues(SimpleRenderable.this.getCombinedRotationMatrix());
                }

                @Override
                int getHash() {
                    return SimpleRenderable.this.getCombinedRotationMatrixHash();
                }

                @Override
                int getLastUploadedHash() {
                    return lastUploadedCombinedRotationMatrixHash;
                }

                @Override
                void setLastUploadedHash(int hash) {
                    lastUploadedCombinedRotationMatrixHash = hash;
                }

            });
        }

        public SimpleRenderable(Program program,
                                Mesh mesh,
                                Transform transform,
                                Camera camera) throws InitializationException {
            setProgram(program);
            setMesh(mesh);

            if (!isLinked()) //probably always true (Logikhalber)
                link();

            setTransform(transform);
            setCamera(camera);
        }

        private float[] getCombinedRotationMatrix() {
            float[] tempMatrix = new float[16];
            float[] rotationMatrix = new float[9];

            Matrix.multiplyMM(tempMatrix, 0, camera.getRotationMatrix(), 0, transform.getRotationMatrix(), 0);

            rotationMatrix[0] = tempMatrix[0];
            rotationMatrix[1] = tempMatrix[1];
            rotationMatrix[2] = tempMatrix[2];

            rotationMatrix[3] = tempMatrix[4];
            rotationMatrix[4] = tempMatrix[5];
            rotationMatrix[5] = tempMatrix[6];

            rotationMatrix[6] = tempMatrix[8];
            rotationMatrix[7] = tempMatrix[9];
            rotationMatrix[8] = tempMatrix[10];

            this.combinedRotationMatrix = rotationMatrix;

            calculateCombinedRotationMatrixHash();

            return this.combinedRotationMatrix;
        }

        private void calculateCombinedRotationMatrixHash() {
            this.combinedRotationMatrixHash = MathUtilities.calculateMatrixHash(this.combinedRotationMatrix);
        }

        public int getCombinedRotationMatrixHash() {
            return combinedRotationMatrixHash;
        }

        private void link() {
            attributeARBufferMap = new HashMap<Program.AttributeReference, Mesh.ARBuffer>();

            for (Program.DefinedAttributeType definedAttributeType : Program.DefinedAttributeType.values()) {
                Program.AttributeReference attributeReference = program.getDefinedAttribute(definedAttributeType);

                if (attributeReference != null) {
                    for (Mesh.ARBuffer arBuffer : mesh.getARBuffers()) {
                        if (arBuffer.getBufferType() == attributeLinkMap.get(definedAttributeType)) {
                            attributeARBufferMap.put(attributeReference, arBuffer);
                            break;
                        }
                    }
                }
            }

            this.uniformSetterMap = new HashMap<>();

            for (Program.DefinedUniformType definedUniformType : Program.DefinedUniformType.values()) {
                Program.Uniform uniform = program.getDefinedUniform(definedUniformType);

                if (uniform != null) {
                    if (uniformLinkMap.containsKey(definedUniformType)) {
                        uniformSetterMap.put(uniform, uniformLinkMap.get(definedUniformType));
                    }
                }
            }

            setLinked();
        }

        boolean isSet() {
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
                DebugUtilities.logWarning("not set to be rendered!");
                return;
            }

            if (!isLinked()) {
                DebugUtilities.logWarning("not linked to be rendered!");
                return;
            }
            //endregion

            //region binding
            program.bind();

            for (Map.Entry<Program.AttributeReference, Mesh.ARBuffer> entry : attributeARBufferMap.entrySet()) {
                Program.AttributeReference attributeReference = entry.getKey();
                Mesh.ARBuffer arBuffer = entry.getValue();

                GLES20.glEnableVertexAttribArray(attributeReference.getIndex());

                arBuffer.bind();

                GLES20.glVertexAttribPointer(
                        attributeReference.getIndex(),
                        attributeReference.getValuesCount(),
                        attributeReference.GL_TYPE,
                        false,
                        arBuffer.getBufferStride(),
                        arBuffer.getBufferOffset());

            }

            mesh.getIndicesBuffer().bind();
            //endregion

            //region uniforms update
            for (Map.Entry<Program.Uniform, UniformSetter> entry : uniformSetterMap.entrySet()) {
                Program.Uniform uniform = entry.getKey();
                UniformSetter uniformSetter = entry.getValue();

                if (uniformSetter.getHash() != uniformSetter.getLastUploadedHash()) {
                    try {
                        uniformSetter.set(uniform);
                        uniformSetter.setLastUploadedHash(uniformSetter.getHash());
                    } catch (InvalidOperationException e) {
                        throw new RenderException(e.getMessage());
                    }
                }
            }
            //endregion

            //region drawing
            GLES20.glDrawElements(mesh.getPrimitiveAssemblyMode(), mesh.getIndicesCount(), mesh.INDICES_GL_TYPE, mesh.getIndicesOffset());
            //endregion

            //region unbinding
            mesh.getIndicesBuffer().unbind();

            for (Map.Entry<Program.AttributeReference, Mesh.ARBuffer> entry : attributeARBufferMap.entrySet()) {
                Program.AttributeReference attributeReference = entry.getKey();
                Mesh.ARBuffer arBuffer = entry.getValue();
                arBuffer.unbind();

                GLES20.glDisableVertexAttribArray(attributeReference.getIndex());
            }

            program.unbind();
            //endregion
        }

        //region inner classes
        private abstract class UniformSetter {
            abstract void set(Program.Uniform uniform) throws InvalidOperationException;

            abstract int getHash();

            abstract int getLastUploadedHash();

            abstract void setLastUploadedHash(int hash);
        }
        //endregion
    }
}
