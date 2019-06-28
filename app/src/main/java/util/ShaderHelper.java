package util;

import android.util.Log;

import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.GL_VALIDATE_STATUS;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glGetProgramInfoLog;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glGetShaderInfoLog;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glValidateProgram;


//we us this class to compile the vertex shader code and the fragment shader code and return coresponding
//shaderObject id


public class ShaderHelper {
    private static final String TAG = "ShaderHelper";


    public static int compileVertexShader(String shaderCode){
        return compileShader(GL_VERTEX_SHADER,shaderCode);
    }

    public static int compileFragmentShader(String shaderCode){
        return compileShader(GL_FRAGMENT_SHADER,shaderCode);
    }

    private static int compileShader(int type,String shaderCode){
        //We first create an object using a call such as glCreateShader(). This call will return
        // an integer. This integer is the reference to our OpenGL object. Whenever we want to
        // refer to this object in the future, we’ll pass the same integer back to OpenGL.
        final int shaderObjectId = glCreateShader(type);
        if (shaderObjectId == 0) { if (LoggerConfig.ON) {
            Log.w(TAG, "Could not create new shader.");
        }
            return 0;
        }

       // following code to upload our shader source code into the shader object:
        //This call tells OpenGL to read in the source code defined in the String shaderCode and
        // associate it with the shader object referred to by shaderObjectId.
        glShaderSource(shaderObjectId,shaderCode);
        //to compile the shaderOjetId
        glCompileShader(shaderObjectId);

        //to check if OpenGL was able to successfully compile the shader
        //This tells OpenGL to read the compile status associated with shaderObjectId and
        // write it to the 0th element of compileStatus.
        final int[] compileStatus = new int[1];
        glGetShaderiv(shaderObjectId,GL_COMPILE_STATUS,compileStatus,0);


        if (LoggerConfig.ON) {
            // Print the shader info log to the Android log output.
            Log.v(TAG, "Results of compiling source:" + "\n" + shaderCode + "\n:" +
             glGetShaderInfoLog(shaderObjectId));
        }
        if (compileStatus[0] == 0) {
            // If it failed, delete the shader object. glDeleteShader(shaderObjectId);
            if (LoggerConfig.ON) {
                Log.w(TAG, "Compilation of shader failed.");
            }
            return 0;
        }


        return shaderObjectId;
    }

    public static int linkProgram(int vertexShaderId,int fragmentShaderId) {
        final int programObjectId = glCreateProgram();
        if (programObjectId==0){
            if(LoggerConfig.ON){
                Log.w(TAG,"could not create new program");
            }
            return 0;
        }
        //Using glAttachShader(), we attach both our vertex shader and our fragment shader to
        // the program object.
        glAttachShader(programObjectId,vertexShaderId);
        glAttachShader(programObjectId,fragmentShaderId);
        //Using glAttachShader(), we attach both our vertex shader and our fragment shader to
        // the program object.
        glLinkProgram(programObjectId);

        final int[] linkStatus = new int[1];
        glGetProgramiv(programObjectId,GL_LINK_STATUS,linkStatus,0);

        if(LoggerConfig.ON){
            Log.v(TAG,"results of linking program:\n"+glGetProgramInfoLog(programObjectId));
        }
        if (linkStatus[0] == 0) { // If it failed, delete the program object.
             glDeleteProgram(programObjectId);
             if (LoggerConfig.ON) { Log.w(TAG, "Linking of program failed.");
             }
             return 0;
        }
        return programObjectId;
    }
    public static boolean validateProgram(int programObjectId) {
        glValidateProgram(programObjectId);
        final int[] validateStatus = new int[1];
        glGetProgramiv(programObjectId, GL_VALIDATE_STATUS, validateStatus, 0);
        Log.v(TAG, "Results of validating program: " + validateStatus[0] + "\nLog:" +
                glGetProgramInfoLog(programObjectId));
        return validateStatus[0] != 0;
    }

    public static int buildProgram(String vertexShaderSource, String fragmentShaderSource) {
        int program;
// Compile the shaders.
        int vertexShader = compileVertexShader(vertexShaderSource);
        int fragmentShader = compileFragmentShader(fragmentShaderSource);
// Link them into a shader program.
        program = linkProgram(vertexShader, fragmentShader);
        if (LoggerConfig.ON) {
            validateProgram(program);
        }
        return program;
    }





}
