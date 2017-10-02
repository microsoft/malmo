varying vec2 texture_coordinate;
uniform sampler2D my_color_texture;

void main()
{
    vec4 texCol = texture2D(my_color_texture, texture_coordinate);
    gl_FragColor = vec4(0.2126 * texCol.r + 0.7152 * texCol.g + 0.0722 * texCol.b, 0.0, 0.0, 1.0);
}