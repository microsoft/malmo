varying vec2 texture_coordinate;

// Simple pass-through vertex shader - just pass the texture_coord onto the fragment shader.

void main()
{
    // Transforming The Vertex
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    texture_coordinate = vec2(gl_MultiTexCoord0);
}