package com.social.airhockey;


import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_LINES;
import static android.opengl.GLES20.GL_POINTS;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform4f;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;
import static android.opengl.Matrix.invertM;
import static android.opengl.Matrix.multiplyMM;
import static android.opengl.Matrix.multiplyMV;
import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.setIdentityM;
import static android.opengl.Matrix.setLookAtM;
import static android.opengl.Matrix.translateM;
import static javax.microedition.khronos.opengles.GL10.GL_TRIANGLE_FAN;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Color;
import android.opengl.GLSurfaceView.Renderer;

import objects.Mallet;
import objects.Puck;
import programs.ColorShaderProgram;
import util.Geometry;
import util.LoggerConfig;
import util.MatrixHelper;
import util.ShaderHelper;
import util.TextResourceReader;



public class AirHockeyRenderer implements Renderer {
    private static final String U_COLOR = "u_Color";
    private static final String A_POSITION = "a_Position";
    private static final int POSITION_COMPONENT_COUNT = 2;
    private static final int BYTES_PER_FLOAT = 4;
    private final FloatBuffer vertexData;
    private final Context context;
    private int program;
    private ColorShaderProgram colorProgram;
    private int uColorLocation;
    private int aPositionLocation;


    //these constants we ll use in
    private static final String A_COLOR = "a_Color";
    private static final int COLOR_COMPONENT_COUNT = 3;
    private static final int STRIDE = (POSITION_COMPONENT_COUNT +
            COLOR_COMPONENT_COUNT) * BYTES_PER_FLOAT;
    private int aColorLocation;

    //use to define constants and array for projection matrix
    private static final String U_MATRIX = "u_Matrix";
    private final float[] projectionMatrix = new float[16];
    private int uMatrixLocation;

    //use of ModelMatrix for translation in various direction
    private final float[] modelMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] viewProjectionMatrix = new float[16];
    private final float[] modelViewProjectionMatrix = new float[16];
    private final float[] invertedViewProjectionMatrix = new float[16];


    //Objects
    private Mallet mallet;
    private Puck puck;

    //variables to add th touch support
    private boolean malletPressed = false;
    private Geometry.Point blueMalletPosition;
    private Geometry.Point previousBlueMalletPosition;
    private Geometry.Point puckPosition;
    private Geometry.Vector puckVector;

    private final float leftBound = -0.5f;
    private final float rightBound = 0.5f;
    private final float farBound = -0.8f;
    private final float nearBound = 0.8f;



    public AirHockeyRenderer(Context context) {
        this.context = context;


//        float tableVerticesWithTriangles[] = {
//                    0f,0f,
//                    -0.5f,-0.5f,
//                    0.5f,-0.5f,
//                    0.5f,0.5f,
//                    -0.5f,0.5f,
//                    -0.5f,-0.5f,
//
//                // Line 1
//               -0.5f, 0f,
//               0.5f, 0f,
////
////                // Mallets
//                0f, -0.25f,
//                0f,  0.25f
//
//        };
        int parsedColor = Color.parseColor("#5cb8b2");
        float red = Color.red(parsedColor) / 255f;
        float green = Color.green(parsedColor) / 255f;
        float blue = Color.blue(parsedColor) / 255f;


        float[] tableVerticesWithTriangles = {
                // Order of coordinates: X, Y, R, G, B
                // Triangle Fan


                0f, 0f, 1f, 1f, 1f,
                -0.5f, -0.8f, red, green, blue,
                0.5f, -0.8f, red, green, blue,
                0.5f, 0.8f, red, green, blue,
                -0.5f, 0.8f, red, green, blue,
                -0.5f, -0.8f, red, green, blue,
                // Line 1
                -0.5f, 0f, 1f, 0f, 0f,
                0.5f, 0f, 1f, 0f, 0f,
                // Mallets
                0f, -0.4f, 0f, 0f, 1f,
                0f, 0.4f, 1f, 0f, 0f
        };


        vertexData = ByteBuffer
                .allocateDirect(tableVerticesWithTriangles.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        vertexData.put(tableVerticesWithTriangles);
    }

    private float clamp(float value, float min, float max) {
        return Math.min(max, Math.max(value, min));
    }


    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        /*
		// Set the background clear color to red. The first component is red,
		// the second is green, the third is blue, and the last component is
		// alpha, which we don't use in this lesson.
		glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
         */

        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        String vertexShaderSource = TextResourceReader
                .readTextFileFromResource(context, R.raw.varying_vertex_shader);
        String fragmentShaderSource = TextResourceReader
                .readTextFileFromResource(context, R.raw.varying_fragment_shader);

        int vertexShader = ShaderHelper.compileVertexShader(vertexShaderSource);
        int fragmentShader = ShaderHelper.compileFragmentShader(fragmentShaderSource);

        program = ShaderHelper.linkProgram(vertexShader, fragmentShader);

        if (LoggerConfig.ON) {
            ShaderHelper.validateProgram(program);
        }

        glUseProgram(program);

        //uncomment to apply a uniform color
        // uColorLocation = glGetUniformLocation(program, U_COLOR);

        aPositionLocation = glGetAttribLocation(program, A_POSITION);

        aColorLocation = glGetAttribLocation(program, A_COLOR);

        uMatrixLocation = glGetUniformLocation(program, U_MATRIX);

        // Bind our data, specified by the variable vertexData, to the vertex
        // attribute at location A_POSITION_LOCATION.
        vertexData.position(0);
        glVertexAttribPointer(aPositionLocation, POSITION_COMPONENT_COUNT, GL_FLOAT,
                false, STRIDE, vertexData);

        glEnableVertexAttribArray(aPositionLocation);

        vertexData.position(POSITION_COMPONENT_COUNT);
        glVertexAttribPointer(aColorLocation, COLOR_COMPONENT_COUNT, GL_FLOAT, false,
                STRIDE, vertexData);


        glEnableVertexAttribArray(aColorLocation);

        //Draw the Mallet and puck
        mallet = new Mallet(0.05f, 0.15f, 32);
        puck = new Puck(0.06f, 0.02f, 32);

        blueMalletPosition = new Geometry.Point(0f,mallet.height/2f,0.4f);
        puckPosition = new Geometry.Point(0f, puck.height / 2f, 0f);
        puckVector = new Geometry.Vector(0f, 0f, 0f);

        colorProgram = new ColorShaderProgram(context);


    }

    /**
     * onSurfaceChanged is called whenever the surface has changed. This is
     * called at least once when the surface is initialized. Keep in mind that
     * Android normally restarts an Activity on rotation, and in that case, the
     * renderer will be destroyed and a new one created.
     *
     * @param width  The new width, in pixels.
     * @param height The new height, in pixels.
     */
    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        // Set the OpenGL viewport to fill the entire surface.
        glViewport(0, 0, width, height);


        //uncomment this if you want only orthogonal projection
//        final float aspectRatio = width > height ? (float) width / (float) height
//                : (float) height / (float) width;
//        if (width > height) {
//            orthoM(projectionMatrix, 0, -aspectRatio, aspectRatio, -1f,
//                    1f, -1f, 1f);
//        } else {
//            orthoM(projectionMatrix, 0, -1f, 1f, -aspectRatio,
//                    aspectRatio, -1f, 1f);
//        }

        //use this to get a 3d projection matrix
        //yFov is angle of view
        //near is the distance of near end of frustum

//        MatrixHelper.perspectiveM(projectionMatrix, 45,
//                (float) width / (float) height, 0f, 10f);


        //modelMatrix for translation and rotation
//        setIdentityM(modelMatrix, 0);
//        translateM(modelMatrix, 0, 0f, 0f, -3f);
//        rotateM(modelMatrix, 0, 0f, 1f, 0f, 0f);
//        final float[] temp = new float[16];
//        multiplyMM(temp, 0, projectionMatrix, 0, modelMatrix, 0);
//        System.arraycopy(temp, 0, projectionMatrix, 0, temp.length);


        MatrixHelper.perspectiveM(projectionMatrix, 45, (float) width / (float) height, 1f, 10f);
        setLookAtM(viewMatrix, 0, 0f, 1.2f, 2.2f, 0f, 0f, 0f, 0f, 1f, 0f);

        // setIdentityM(viewMatrix, 0);
//


    }

    /**
     * OnDrawFrame is called whenever a new frame needs to be drawn. Normally,
     * this is done at the refresh rate of the screen.
     */
    @Override
    public void onDrawFrame(GL10 glUnused) {


        // Clear the rendering surface.
        glClear(GL_COLOR_BUFFER_BIT);
        multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        invertM(invertedViewProjectionMatrix, 0, viewProjectionMatrix, 0);


        glUseProgram(program);



        //uncomment to apply a uniform color
        // uColorLocation = glGetUniformLocation(program, U_COLOR);

        // aPositionLocation = glGetAttribLocation(program, A_POSITION);

        //   aColorLocation = glGetAttribLocation(program, A_COLOR);

        //  uMatrixLocation = glGetUniformLocation(program,U_MATRIX);

        // Bind our data, specified by the variable vertexData, to the vertex
        // attribute at location A_POSITION_LOCATION.
        vertexData.position(0);
        glVertexAttribPointer(aPositionLocation, POSITION_COMPONENT_COUNT, GL_FLOAT,
                false, STRIDE, vertexData);

        glEnableVertexAttribArray(aPositionLocation);
//
        vertexData.position(POSITION_COMPONENT_COUNT);
        glVertexAttribPointer(aColorLocation, COLOR_COMPONENT_COUNT, GL_FLOAT, false,
                STRIDE, vertexData);
//

        glEnableVertexAttribArray(aColorLocation);


        positionTableInScene();
        glUniformMatrix4fv(uMatrixLocation, 1, false, modelViewProjectionMatrix, 0);


        // Draw the table.
        //uncomment gluniform calls when giving a uniform color
        //glUniform4f(uColorLocation, 1.0f, 1.0f, 1.0f, 1.0f);
        glDrawArrays(GL_TRIANGLE_FAN, 0, 6);

//        positionObjectInScene(0f, mallet.height / 2f, 2.5f-0.4f);


        // Draw the center dividing line.
        //glUniform4f(uColorLocation, 1.0f, 0.0f, 0.0f, 1.0f);
        glDrawArrays(GL_LINES, 6, 2);

        // Draw the first mallet blue.
        //glUniform4f(uColorLocation, 0.0f, 0.0f, 1.0f, 1.0f);
        glDrawArrays(GL_POINTS, 8, 1);

        // Draw the second mallet red.
        // glUniform4f(uColorLocation, 1.0f, 0.0f, 0.0f, 1.0f);
        glDrawArrays(GL_POINTS, 9, 1);


        positionObjectInScene(0f, mallet.height / 2f, -0.4f);
        colorProgram.useProgram();
        colorProgram.setUniforms(modelViewProjectionMatrix, 1f, 0f, 0f);
        mallet.bindData(colorProgram);
        mallet.draw();


        positionObjectInScene(blueMalletPosition.x, blueMalletPosition.y, blueMalletPosition.z);
        colorProgram.useProgram();
        colorProgram.setUniforms(modelViewProjectionMatrix, 0f, 0f, 1f);
        mallet.bindData(colorProgram);
        mallet.draw();

        // Draw the puck.
        puckPosition = puckPosition.translate(puckVector);
        puckVector = puckVector.scale(0.99f);
        if (puckPosition.x < leftBound + puck.radius || puckPosition.x > rightBound - puck.radius) {
            puckVector = new Geometry.Vector(-puckVector.x, puckVector.y, puckVector.z);
        }
        if (puckPosition.z < farBound + puck.radius || puckPosition.z > nearBound - puck.radius) {
            puckVector = new Geometry.Vector(puckVector.x, puckVector.y, -puckVector.z); }
        // Clamp the puck position.
        puckPosition = new Geometry.Point( clamp(puckPosition.x, leftBound + puck.radius, rightBound - puck.radius), puckPosition.y,
                clamp(puckPosition.z, farBound + puck.radius, nearBound - puck.radius) );
        positionObjectInScene(puckPosition.x, puckPosition.y, puckPosition.z);
        colorProgram.setUniforms(modelViewProjectionMatrix, 0.8f, 0.8f, 1f);
        puck.bindData(colorProgram);
        puck.draw();


    }

    private void positionTableInScene() {
        // The table is defined in terms of X & Y coordinates, so we rotate it // 90 degrees to lie flat on the XZ plane.
        setIdentityM(modelMatrix, 0);
        rotateM(modelMatrix, 0, -90f, 1f, 0f, 0f);
        multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0);
    }






        private void positionObjectInScene(float x, float y, float z) {
        setIdentityM(modelMatrix, 0);
        //rotateM(modelMatrix, 0, -90f, 1f, 0f, 0f);
        translateM(modelMatrix, 0, x, y, z);
        multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix,
                0, modelMatrix, 0);
    }


    private Geometry.Ray convertNormalized2DPointToRay(float normalizedX, float normalizedY) {
        // We'll convert these normalized device coordinates into world-space // coordinates. We'll pick a point on the near and f
        // ar planes, and draw a // line between them. To do this transform, we need to first multiply by // the inverse matrix,
        // and then we need to undo the perspective divide.
        final float[] nearPointNdc = {normalizedX, normalizedY, -1, 1};
        final float[] farPointNdc = {normalizedX, normalizedY, 1, 1};
        final float[] nearPointWorld = new float[4];
        final float[] farPointWorld = new float[4];
        multiplyMV(nearPointWorld, 0, invertedViewProjectionMatrix, 0, nearPointNdc, 0);
        multiplyMV(farPointWorld, 0, invertedViewProjectionMatrix, 0, farPointNdc, 0);
        divideByW(nearPointWorld);
        divideByW(farPointWorld);
        Geometry.Point nearPointRay = new Geometry.Point(nearPointWorld[0], nearPointWorld[1], nearPointWorld[2]);
        Geometry.Point farPointRay = new Geometry.Point(farPointWorld[0], farPointWorld[1], farPointWorld[2]);
        return new Geometry.Ray(nearPointRay, Geometry.vectorBetween(nearPointRay, farPointRay));


    }
    private void divideByW(float[] vector)
    {
        vector[0] /= vector[3];
        vector[1] /= vector[3];
        vector[2] /= vector[3];
    }


    public void handleTouchPress(float normalizedX, float normalizedY) {
         Geometry.Ray ray = convertNormalized2DPointToRay(normalizedX,normalizedY);
        // Now test if this ray intersects with the mallet by creating a // bounding sphere that wraps the mallet.
         Geometry.Sphere malletBoundingSphere = new Geometry.Sphere(new Geometry.Point( blueMalletPosition.x, blueMalletPosition.y, blueMalletPosition.z), mallet.height / 2f);
        // If the ray intersects (if the user touched a part of the screen that // intersects the mallet's bounding sphere), then set malletPressed = // true.
         malletPressed = Geometry.intersects(malletBoundingSphere, ray);
         //if(malletPressed)System.out.println("malletpressed");

    }
    public void handleTouchDrag(float normalizedX, float normalizedY) {
        if (malletPressed) {
            Geometry.Ray ray = convertNormalized2DPointToRay(normalizedX, normalizedY);
            // Define a plane representing our air hockey table.
            Geometry.Plane plane = new Geometry.Plane(new Geometry.Point(0, 0, 0),
                    new Geometry.Vector(0, 1, 0));
            // Find out where the touched point intersects the plane
            // representing our table. We'll move the mallet along this plane.
            Geometry.Point touchedPoint = Geometry.intersectionPoint(ray, plane);
            previousBlueMalletPosition = blueMalletPosition;
            blueMalletPosition = new Geometry.Point( clamp(touchedPoint.x, leftBound + mallet.radius, rightBound - mallet.radius),
                    mallet.height / 2f, clamp(touchedPoint.z, 0f + mallet.radius, nearBound - mallet.radius));

            float distance = Geometry.vectorBetween(blueMalletPosition, puckPosition).length();
            if (distance < (puck.radius + mallet.radius)) {
                // The mallet has struck the puck. Now send the puck flying
                // based on the mallet velocity.
                puckVector = Geometry.vectorBetween( previousBlueMalletPosition, blueMalletPosition);
            }
            }
        }

}
