//This vertex shader will be called once for every single vertex that we’ve defined.
//When it’s called, it will receive the current vertex’s position in the a_Position attribute,
//which is defined to be a vec4. A vec4 is a vector consisting of four components. In the context
//of a position, we can think of the four components as the position’s x, y, z, and w coordinates.

attribute vec4 a_Position;

//We’ve added a new uniform definition, u_Matrix, and we’ve defined it as a mat4,
//meaning that this uniform will represent a 4 x 4 matrix.It hold the transformation required
//to map from virtual coordinate space to normalise coordinate space.
uniform mat4 u_Matrix;



//this variable is introduced if you and blending
attribute vec4 a_Color;


//A varying is a special type of variable that blends the values given to it and sends these
//values to the fragment shader. Using the line above as an example, if a_Color was red at
//vertex 0 and green at vertex 1, then by assigning a_Color to v_Color, we’re telling OpenGL
//that we want each fragment to receive a blended color. Near vertex 0, the blended color will
//be mostly red, and as the fragments get closer to vertex 1, the color will start to become
//green.
varying vec4 v_Color;

void main(){
    v_Color = a_Color;

    //uncomment this if you are not using orthognal projection matrix
    //gl_Position = a_Position;

    gl_Position = u_Matrix * a_Position;


    gl_PointSize = 10.0;

}