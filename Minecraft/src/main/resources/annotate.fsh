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
        // No colour was passed to us, so pick a colour based on our texture coords.
        // This is tailored for the Minecraft block texture atlas, which should be a 512x512 texture,
        // with each block occupying a 32x32 pixel cell.
        // First get an x and y index for the block:
        float x = floor(texture_coordinate.x * 32.0);
        float y = floor(texture_coordinate.y * 32.0);
        // Convert to a flat block ID:
        i = x + y * 32.0;
        // Now turn that into an RGB tuple:
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
        // A specific colour was passed to us as an int. Convert to RGB tuple.
        b = float(entityColour & 0xff) / 255.0;
        g = float((entityColour >> 8) & 0xff) / 255.0;
        r = float((entityColour >> 16) & 0xff) / 255.0;
        gl_FragColor = vec4(r, g, b, 1.0);
    }
}