// --------------------------------------------------------------------------------------------------
//  Copyright (c) 2016 Microsoft Corporation
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
//  associated documentation files (the "Software"), to deal in the Software without restriction,
//  including without limitation the rights to use, copy, modify, merge, publish, distribute,
//  sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions:
//
//  The above copyright notice and this permission notice shall be included in all copies or
//  substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
//  NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
//  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
//  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// --------------------------------------------------------------------------------------------------

package malmo

import "time"

type TimestampedVideoFrame struct {
	Timestamp time.Time // The timestamp.
	Width     int16     // The width of the image in pixels.
	Height    int16     // The height of the image in pixels.
	Channels  int16     // The number of channels. e.g. 3 for RGB data, 4 for RGBD
	Pitch     float32   // The pitch of the player at render time
	Yaw       float32   // The yaw of the player at render time
	Xpos      float32   // The x pos of the player at render time
	Ypos      float32   // The y pos of the player at render time
	Zpos      float32   // The z pos of the player at render time
	Pixels    []uint8   // The pixels, stored as channels then columns then rows. Length should be width*height*channels.
}
