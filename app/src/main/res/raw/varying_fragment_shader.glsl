//The first line at the top of the file defines the default precision for all floating point
//data types in the fragment shader
//we’ll select mediump for maximum compatibility and as a good tradeoff between speed and quality.
precision mediump float;

// we pass in a uniform called u_Color. Unlike an attribute that is set on each vertex, a uniform
//keeps the same value for all vertices until we change it again. Like the attribute we were using
//for position in the vertex shader, u_Color is also a four-component vector, and in the context of
// a color, its four components correspond to red, green, blue, and alpha.
uniform vec4 u_Color;

varying vec4 v_Color;


void main() {

    //uncomment if yo want a uniform colour in your all yourrasterized fragments
    //gl_FragColor = u_Color;

    //We’ve replaced the uniform that was there before with our varying, v_Color.
    //If the fragment belongs to a line, then OpenGL will use the two vertices that make
    //up that line to calculate the blended color. If the fragment belongs to a triangle,
    //then OpenGL will use the three vertices that make up that triangle to calculate the
    //blended color.
    gl_FragColor = v_Color;
}
