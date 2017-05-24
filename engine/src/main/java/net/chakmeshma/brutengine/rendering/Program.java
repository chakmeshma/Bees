package net.chakmeshma.brutengine.rendering;

import net.chakmeshma.brutengine.development.exceptions.GLCustomException;
import net.chakmeshma.brutengine.development.exceptions.GLCustomShaderException;
import net.chakmeshma.brutengine.development.exceptions.InitializationException;
import net.chakmeshma.brutengine.development.exceptions.InvalidOperationException;
import net.chakmeshma.brutengine.utilities.AssetFileReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.GL_FALSE;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.GL_MAX_VERTEX_ATTRIBS;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glBindAttribLocation;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glDetachShader;
import static android.opengl.GLES20.glGetError;
import static android.opengl.GLES20.glGetIntegerv;
import static android.opengl.GLES20.glGetProgramInfoLog;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glGetShaderInfoLog;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniform2fv;
import static android.opengl.GLES20.glUniform2iv;
import static android.opengl.GLES20.glUniform3fv;
import static android.opengl.GLES20.glUniform3iv;
import static android.opengl.GLES20.glUniform4fv;
import static android.opengl.GLES20.glUniform4iv;
import static android.opengl.GLES20.glUniformMatrix2fv;
import static android.opengl.GLES20.glUniformMatrix3fv;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;

public final class Program {
    //region parsing patterns
    private static Pattern vertexShaderAttributePattern;
    //endregion
    private static int _maxGenericAttributes = -1;
    private static Pattern shaderUniformPattern;
    private static Pattern shaderUniformGroupPattern;
    private static int _nextGenericAttributeIndex = 0;

    static {
        vertexShaderAttributePattern = Pattern.compile("\\A\\s*attribute\\s+([a-zA-Z0-9]+)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*;.*\\z");
        shaderUniformPattern = Pattern.compile("\\A\\s*uniform\\s+([a-zA-Z0-9]+)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*;.*\\z");
        shaderUniformGroupPattern = Pattern.compile("\\A\\s*uniform\\s+([a-zA-Z0-9]+)\\s+([a-zA-Z_][a-zA-Z0-9_]*\\s*(\\s*,\\s*([a-zA-Z_][a-zA-Z0-9_]*))+)\\s*;.*\\z");
    }

    private int id;
    private ArrayList<AttributeReference> attributes;
    private ArrayList<Uniform> uniforms;
    private Map<DefinedUniformType, List<Uniform>> definedUniforms;
    private Map<DefinedAttributeType, List<AttributeReference>> definedAttributes;

    public Program(String vertexShaderFileName, String fragmentShaderFileName,
                   Map<DefinedUniformType, VariableReferenceable.VariableMatcher> definedUniforms,
                   Map<DefinedAttributeType, VariableReferenceable.VariableMatcher> definedAttributes) throws InitializationException {
        this(vertexShaderFileName, fragmentShaderFileName);

        //region assert definitions not null
        if (definedAttributes == null)
            throw new InitializationException("defined attributes matcher map null");

        if (definedUniforms == null) {
            throw new InitializationException("defined uniforms matcher map null");
        }
        //endregion

        this.definedAttributes = new HashMap<>();

        for (AttributeReference attributeReference : attributes) {
            for (DefinedAttributeType definedAttributeType : definedAttributes.keySet()) {
                if (definedAttributes.get(definedAttributeType).matches(attributeReference)) {
                    if (!this.definedAttributes.containsKey(definedAttributeType))
                        this.definedAttributes.put(definedAttributeType, new ArrayList<AttributeReference>());

                    this.definedAttributes.get(definedAttributeType).add(attributeReference);
                }
            }
        }

        this.definedUniforms = new HashMap<>();

        for (Uniform uniform : uniforms) {
            for (DefinedUniformType definedUniformType : definedUniforms.keySet()) {
                if (definedUniforms.get(definedUniformType).matches(uniform)) {
                    if (!this.definedUniforms.containsKey(definedUniformType))
                        this.definedUniforms.put(definedUniformType, new ArrayList<Uniform>());

                    this.definedUniforms.get(definedUniformType).add(uniform);
                }
            }
        }
    }

    private Program(String vertexShaderFileName, String fragmentShaderFileName) throws InitializationException {
        int[] shaderCompileStatusIntegers = new int[2];
        int[] shaderLinkStatusIntegers = new int[1];
        int vertexShader;
        int fragmentShader;
        String vertexShaderSource;
        String fragmentShaderSource;

        attributes = new ArrayList<AttributeReference>();
        uniforms = new ArrayList<Uniform>();

        shaderCompileStatusIntegers[0] = -1;
        shaderCompileStatusIntegers[1] = -1;
        shaderLinkStatusIntegers[0] = -1;

        vertexShader = glCreateShader(GL_VERTEX_SHADER);
        fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);

        try {
            vertexShaderSource = AssetFileReader.getSingleton().getAssetFileAsString(vertexShaderFileName);
        } catch (IOException e) {
            throw new InitializationException(e.getMessage());
        }

        glShaderSource(vertexShader, vertexShaderSource);

        try {
            fragmentShaderSource = AssetFileReader.getSingleton().getAssetFileAsString(fragmentShaderFileName);
        } catch (IOException e) {
            throw new InitializationException(e.getMessage());
        }

        glShaderSource(fragmentShader, fragmentShaderSource);

        glCompileShader(vertexShader);

        glGetShaderiv(vertexShader, GL_COMPILE_STATUS, shaderCompileStatusIntegers, 0);

        if (shaderCompileStatusIntegers[0] == GL_FALSE) {
            String vertexShaderInfoLog = glGetShaderInfoLog(vertexShader);

            glDeleteShader(vertexShader);

            throw new GLCustomShaderException(vertexShaderInfoLog, vertexShaderSource);
        }

        glCompileShader(fragmentShader);

        glGetShaderiv(vertexShader, GL_COMPILE_STATUS, shaderCompileStatusIntegers, 1);

        if (shaderCompileStatusIntegers[1] == GL_FALSE) {
            String fragmentShaderInfoLog = glGetShaderInfoLog(fragmentShader);

            glDeleteShader(vertexShader);
            glDeleteShader(fragmentShader);


            throw new GLCustomShaderException(fragmentShaderInfoLog, fragmentShaderSource);
        }

        id = glCreateProgram();

        glAttachShader(id, vertexShader);
        glAttachShader(id, fragmentShader);

        inflateAttributes(vertexShaderSource);

        for (AttributeReference attributeReference : attributes) {
            glBindAttribLocation(id, attributeReference.getIndex(), attributeReference.getName());
        }

        glLinkProgram(id);

        glGetProgramiv(id, GL_LINK_STATUS, shaderLinkStatusIntegers, 0);

        if (shaderLinkStatusIntegers[0] == GL_FALSE) {
            String programLinkInfoLog = glGetProgramInfoLog(id);

            glDeleteProgram(id);
            glDeleteShader(vertexShader);
            glDeleteShader(fragmentShader);

            throw new GLCustomException(glGetError(), programLinkInfoLog);
        }

        glDetachShader(id, vertexShader);
        glDetachShader(id, fragmentShader);

        inflateUniforms(vertexShaderSource);
        inflateUniforms(fragmentShaderSource);

        for (Uniform uniform : uniforms) {
            uniform.setUniformLocation(glGetUniformLocation(id, uniform.getName()));
        }

    }

    private static int getMaxGAsCount() {
        if (_maxGenericAttributes == -1) {
            int[] maxAttribs = new int[1];
            glGetIntegerv(GL_MAX_VERTEX_ATTRIBS, maxAttribs, 0);
            _maxGenericAttributes = maxAttribs[0];
        }

        return _maxGenericAttributes;
    }

    public List<Uniform> getDefinedUniform(DefinedUniformType definedUniformType) {
        if (definedUniforms.containsKey(definedUniformType))
            return definedUniforms.get(definedUniformType);
        else
            return new ArrayList<>();
    }

    public List<AttributeReference> getDefinedAttributes(DefinedAttributeType definedAttributeType) {
        if (definedAttributes.containsKey(definedAttributeType))
            return definedAttributes.get(definedAttributeType);
        else
            return new ArrayList<>();
    }

    void bind() {
        glUseProgram(id);
    }

    void unbind() {
        glUseProgram(0);
    }

    private void inflateUniforms(String shaderSource) throws InitializationException {
        Matcher uniformMatcher = null;

        Scanner scanner = new Scanner(shaderSource);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();

            if (uniformMatcher == null)
                uniformMatcher = shaderUniformPattern.matcher(line);
            else
                uniformMatcher.reset(line);

            if (uniformMatcher.matches()) {
                boolean duplicate = false;

                String uniformTypeName = uniformMatcher.group(1);
                String uniformName = uniformMatcher.group(2);

                for (Uniform uniform : uniforms) {
                    if (uniform.getTypeName().equals(uniformTypeName) && uniform.getName().equals(uniformName)) {
                        duplicate = true;
                        break;
                    }
                }

                if (!duplicate) {
                    uniforms.add(new Uniform(uniformTypeName, uniformName));
                }
            } else {
                Matcher uniformGroupMatcher = shaderUniformGroupPattern.matcher(line);

                if (uniformGroupMatcher.matches()) {
                    String variableNamesTogether = uniformGroupMatcher.group(2);

                    String[] parts = variableNamesTogether.split("\\s*,\\s*");

                    for (String part : parts) {
                        boolean duplicate = false;

                        String uniformTypeName = uniformGroupMatcher.group(1);

                        for (Uniform uniform : uniforms) {
                            if (uniform.getTypeName().equals(uniformTypeName) && uniform.getName().equals(part)) {
                                duplicate = true;
                                break;
                            }
                        }

                        if (!duplicate) {
                            uniforms.add(new Uniform(uniformTypeName, part));
                        }
                    }
                }
            }
        }
        scanner.close();
    }

    private void inflateAttributes(String vertexShaderSource) {
        Matcher attributePatternMatcher = null;

        Scanner scanner = new Scanner(vertexShaderSource);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();

            if (attributePatternMatcher == null)
                attributePatternMatcher = vertexShaderAttributePattern.matcher(line);
            else
                attributePatternMatcher.reset(line);

            if (attributePatternMatcher.matches()) {
                attributes.add(new AttributeReference(attributePatternMatcher.group(1), attributePatternMatcher.group(2)));
            }
        }
        scanner.close();
    }

    private ArrayList<AttributeReference> getAttributeReferences() {
        return attributes;
    }

    private ArrayList<Uniform> getUniforms() {
        return uniforms;
    }

    //region inner classes
    public enum DefinedUniformType {
        MODEL_MATRIX_UNIFORM,
        VIEW_MATRIX_UNIFORM,
        PROJECTION_MATRIX_UNIFORM,
        ROTATION_MATRIX_UNIFORM,
        CUSTOM_UNIFORM,
    }

    public enum DefinedAttributeType {
        POSITION_ATTRIBUTE,
        NORMAL_ATTRIBUTE,
        CUSTOM_ATTRIBUTE
    }

    abstract class ProgramVariableReference implements VariableReferenceable {
        private String _typeName;
        private String _name;

        ProgramVariableReference(String typeName, String name) {
            this._typeName = typeName;
            this._name = name;
        }

        @Override
        public String
        getTypeName() {
            return _typeName;
        }

        @Override
        public String
        getName() {
            return _name;
        }

        @Override
        public Class
        getValueType() {
            switch (getTypeName()) {
                case "float":
                case "vec2":
                case "vec3":
                case "vec4":
                case "mat2":
                case "mat3":
                case "mat4":
                    return float.class;
                case "bool":
                case "bvec2":
                case "bvec3":
                case "bvec4":
                    return boolean.class;
                case "int":
                case "ivec2":
                case "ivec3":
                case "ivec4":
                case "sampler2D":
                case "samplerCube":
                    return int.class;
                case "void":
                    return void.class;
            }

            return null;
        }

        @Override
        public int
        getValuesCount() {
            switch (getTypeName()) {
                case "float":
                case "bool":
                case "int":
                case "sampler2D":
                case "samplerCube":
                    return 1;
                case "ivec2":
                case "vec2":
                case "bvec2":
                    return 2;
                case "ivec3":
                case "vec3":
                case "bvec3":
                    return 3;
                case "ivec4":
                case "vec4":
                case "bvec4":
                case "mat2":
                    return 4;
                case "mat3":
                    return 9;
                case "mat4":
                    return 16;
                case "void":
                    return 0;
            }

            return -1;
        }
    }

    final class Uniform extends ProgramVariableReference {
        private final Object valuesLock = new Object();
        private int _uniformLocation = -1;
        private Object[] values;

        Uniform(String typeName, String name) {
            super(typeName, name);
        }

        synchronized void setUniformLocation(int id) {
            this._uniformLocation = id;
        }

        synchronized void setFloatValues(float[] values) throws InvalidOperationException {
            if (!getValueType().toString().equals("float"))
                throw new InvalidOperationException("incompatible value type");
            else if (values.length < getValuesCount())
                throw new InvalidOperationException("incompatible value count");

            switch (getTypeName()) {
                case "float":
                    glUniform1f(_uniformLocation, values[0]);
                    break;
                case "vec2":
                    glUniform2fv(_uniformLocation, 1, values, 0);
                    break;
                case "vec3":
                    glUniform3fv(_uniformLocation, 1, values, 0);
                    break;
                case "vec4":
                    glUniform4fv(_uniformLocation, 1, values, 0);
                    break;
                case "mat2":
                    glUniformMatrix2fv(_uniformLocation, 1, false, values, 0);
                    break;
                case "mat3":
                    glUniformMatrix3fv(_uniformLocation, 1, false, values, 0);
                    break;
                case "mat4":
                    glUniformMatrix4fv(_uniformLocation, 1, false, values, 0);
                    break;
            }
        }

        synchronized void setIntValues(int[] values) throws InvalidOperationException {
            if (!getValueType().toString().equals("int"))
                throw new InvalidOperationException("incompatible value type");
            else if (values.length < getValuesCount())
                throw new InvalidOperationException("incompatible value count");

            switch (getTypeName()) {
                case "int":
                    glUniform1i(_uniformLocation, values[0]);
                    break;
                case "ivec2":
                    glUniform2iv(_uniformLocation, 1, values, 0);
                    break;
                case "ivec3":
                    glUniform3iv(_uniformLocation, 1, values, 0);
                    break;
                case "ivec4":
                    glUniform4iv(_uniformLocation, 1, values, 0);
                    break;
                case "sampler2D":
                    glUniform1i(_uniformLocation, values[0]);
                    break;
                case "samplerCube":
                    glUniform1i(_uniformLocation, values[0]);
                    break;
            }
        }
    }

    final class AttributeReference extends ProgramVariableReference {
        private int _genericVertexAttributeIndex = -1;

        AttributeReference(String typeName, String name) {
            super(typeName, name);

            if (_nextGenericAttributeIndex >= getMaxGAsCount()) // GA: Generic Attribute
                throw new GLCustomException(glGetError(), "Maximum allowed generic attriubtes allocated!");

            _genericVertexAttributeIndex = _nextGenericAttributeIndex;

            _nextGenericAttributeIndex++;
        }

        int getIndex() {
            return _genericVertexAttributeIndex;
        }
    }
    //endregion
}
