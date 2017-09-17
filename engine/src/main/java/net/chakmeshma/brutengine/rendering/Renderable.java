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
            attributeLinkMap.put(Program.DefinedAttributeType.UV_ATTRIBUTE, Mesh.DefinedBufferType.UVS_BUFFER);
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
        private Texture[] textures;
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

            uniformLinkMap.put(Program.DefinedUniformType.TEXTURE_SAMPLER_ID_UNIFORM, new TextureUniformSetter() {

                @Override
                void setTextureUniform(Program.Uniform uniform, Texture texture) throws InvalidOperationException {
                    if (texture != null) {
                        int[] ids = new int[1];
                        ids[0] = texture.getTextureUnitIndex();
                        uniform.setIntValues(ids);
                    }
                }

                @Override
                int getHash() {
                    return 1;
                }

                @Override
                int getLastUploadedHash() {
                    return 2;
                }

                @Override
                void setLastUploadedHash(int hash) {
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

        public SimpleRenderable(Program program,
                                Mesh mesh,
                                Transform transform,
                                final Texture[] textures,
                                Camera camera) throws InitializationException {

            setProgram(program);
            setMesh(mesh);

            if (!isLinked()) //probably always true (Logikhalber)
                link();

            setTransform(transform);
            setCamera(camera);
            setTextures(textures);

            _hasTexture = true;
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

        int getCombinedRotationMatrixHash() {
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

            for (Map.Entry<Program.DefinedUniformType, UniformSetter> entry : uniformLinkMap.entrySet()) {
                Program.Uniform[] uniforms = program.getDefinedUniforms(entry.getKey());

                if (uniforms != null) {
                    for (Program.Uniform uniform : uniforms) {
                        uniformSetterMap.put(uniform, uniformLinkMap.get(entry.getKey()));
                    }
                }
            }

            setLinked();
        }

        boolean isSet() {
            return _meshSet && _programSet && _transformSet && _cameraSet && (!_hasTexture || _textureSet);
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

        private void setTextures(Texture[] textures) {
            this.textures = textures;

            if (textures != null && textures.length > 0) {
                for (int i = 0; i < textures.length - 1; i++) {
                    for (int j = 0; j < textures.length - 1; j++) {
                        if (textures[j].getTextureUnitIndex() > textures[j + 1].getTextureUnitIndex()) {
                            Texture tmpTexture = textures[j];
                            textures[j] = textures[j + 1];
                            textures[j + 1] = tmpTexture;
                        }
                    }
                }
            }

            this._textureSet = true;
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

            //region textures binding
            if (textures != null && textures.length > 0) {
                for (int i = 0; i < textures.length; i++) {
                    textures[i].activateCorrespondingTextureUnit();
                    textures[i].bind();
                }
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



            \
            mesh.getIndicesBuffer().bind();
            //endregion

            //region uniforms update

            for (Map.Entry<Program.Uniform, UniformSetter> entry : uniformSetterMap.entrySet()) {
                Program.Uniform uniform = entry.getKey();
                UniformSetter uniformSetter = entry.getValue();


                if (!(uniformSetter instanceof TextureUniformSetter)) {
                    if (uniformSetter.getHash() != uniformSetter.getLastUploadedHash()) {
                        try {
                            uniformSetter.set(uniform);
                            uniformSetter.setLastUploadedHash(uniformSetter.getHash());
                        } catch (InvalidOperationException e) {
                            throw new RenderException(e.getMessage());
                        }
                    }
                }
            }

            int textureUniformSetterCount = 0;

            for (Map.Entry<Program.Uniform, UniformSetter> entry : uniformSetterMap.entrySet()) {
                Program.Uniform uniform = entry.getKey();
                UniformSetter uniformSetter = entry.getValue();
                if ((uniformSetter instanceof TextureUniformSetter))
                    textureUniformSetterCount++;
            }

            Program.Uniform[] sortedTextureUniforms = new Program.Uniform[textureUniformSetterCount];

            textureUniformSetterCount = 0;

            for (Map.Entry<Program.Uniform, UniformSetter> entry : uniformSetterMap.entrySet()) {
                Program.Uniform uniform = entry.getKey();
                UniformSetter uniformSetter = entry.getValue();
                if ((uniformSetter instanceof TextureUniformSetter)) {
                    sortedTextureUniforms[textureUniformSetterCount] = uniform;
                    textureUniformSetterCount++;
                }
            }

            for (int i = 0; i < sortedTextureUniforms.length - 1; i++) {
                for (int j = 0; j < sortedTextureUniforms.length - 1; j++) {
                    if (sortedTextureUniforms[j].getApperanceOrder() > sortedTextureUniforms[j + 1].getApperanceOrder()) {
                        Program.Uniform tmpUniform = sortedTextureUniforms[j];
                        sortedTextureUniforms[j] = sortedTextureUniforms[j + 1];
                        sortedTextureUniforms[j + 1] = tmpUniform;
                    }
                }
            }

            textureUniformSetterCount = 0;

            for (Map.Entry<Program.Uniform, UniformSetter> entry : uniformSetterMap.entrySet()) {
                Program.Uniform uniform = entry.getKey();
                UniformSetter uniformSetter = entry.getValue();
                if ((uniformSetter instanceof TextureUniformSetter)) {
                    TextureUniformSetter textureUniformSetter = (TextureUniformSetter) uniformSetter;

                    try {
                        if (textureUniformSetterCount < textures.length)
                            textureUniformSetter.setTextureUniform(sortedTextureUniforms[textureUniformSetterCount], textures[textureUniformSetterCount]);
                    } catch (InvalidOperationException e) {
                        throw new RenderException(e.getMessage());
                    }

                    textureUniformSetterCount++;
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

        private abstract class TextureUniformSetter extends UniformSetter {
            void set(Program.Uniform uniform) {
            }

            abstract void setTextureUniform(Program.Uniform uniform, Texture texture) throws InvalidOperationException;
        }
        //endregion
    }
}
