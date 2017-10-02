#version 120
varying vec2 texture_coordinate;
uniform sampler2D my_color_texture;
// Pass entity colour in as separate RGB integer values - passing as a single float
// leads to accuracy issues; passing as a single int requires bit-twiddling (not available in GLSL1.2)
// or lots of messy division/flooring.
uniform int entityColourR;
uniform int entityColourG;
uniform int entityColourB;

void main()
{
    float i = 0.0;
    float r = 0.0;
    float g = 0.0;
    float b = 0.0;
    if (entityColourR == -1)
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
        // A specific colour was passed to us as ints.
        gl_FragColor = vec4(entityColourR / 255.0, entityColourG / 255.0, entityColourB / 255.0, 1.0);
    }
}
