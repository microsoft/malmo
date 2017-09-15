#version 150
varying vec2 texture_coordinate;
uniform sampler2D my_color_texture;
uniform int entityColour;

void main()
{
    float i = 0.0;
    float r = 0.0;
    float g = 0.0;
    float b = 0.0;
    if (entityColour == -1)
    {
        float x = floor(texture_coordinate.x * 32.0);
        float y = floor(texture_coordinate.y * 32.0);
        i = x + y * 32.0;
        float base = 11.0;
        r = i - base * floor(i / base);
        i = (i - r) / base;
        g = i - base * floor(i / base);
        i = (i - g) / base;
        b = i;
        gl_FragColor = vec4(r / base, g / base, b / base, 1.0);
    }
    else
    {
        b = float(entityColour & 0xff) / 255.0;
        g = float((entityColour >> 8) & 0xff) / 255.0;
        r = float((entityColour >> 16) & 0xff) / 255.0;
        gl_FragColor = vec4(r, g, b, 1.0);
    }
}