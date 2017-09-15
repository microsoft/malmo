varying vec2 texture_coordinate;

void main()
{
    // Transforming The Vertex
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    texture_coordinate = vec2(gl_MultiTexCoord0);
}