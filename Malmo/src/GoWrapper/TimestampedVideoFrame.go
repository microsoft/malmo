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

/*
#include "x_definitions.h"
*/
import "C"

import (
	"image"
	"image/png"
	"os"
	"time"
	"unsafe"
)

type TimestampedVideoFrame struct {
	Timestamp time.Time // The timestamp.
	Width     int       // The width of the image in pixels.
	Height    int       // The height of the image in pixels.
	Channels  int       // The number of channels. e.g. 3 for RGB data, 4 for RGBD
	Pitch     float64   // The pitch of the player at render time
	Yaw       float64   // The yaw of the player at render time
	Xpos      float64   // The x pos of the player at render time
	Ypos      float64   // The y pos of the player at render time
	Zpos      float64   // The z pos of the player at render time
	Pixels    []uint8   // The pixels, stored as channels then columns then rows. Length should be width*height*channels.
}

// newTimestampedVideoFrameFromCpp creates new TimestampedVideoFrame from C++ data
func newTimestampedVideoFrameFromCpp(ts *C.timestamp_t, vf *C.videoframe_t, cnpixels C.int, ptPixels *C.uchar) (o *TimestampedVideoFrame) {
	o = new(TimestampedVideoFrame)
	o.Timestamp = time.Date(
		int(ts.year),
		time.Month(ts.month),
		int(ts.day),
		int(ts.hours),
		int(ts.minutes),
		int(ts.seconds),
		int(ts.nanoseconds),
		time.Now().Location(),
	)

	o.Width = int(vf.width)
	o.Height = int(vf.height)
	o.Channels = int(vf.channels)
	o.Pitch = float64(vf.pitch)
	o.Yaw = float64(vf.yaw)
	o.Xpos = float64(vf.xPos)
	o.Ypos = float64(vf.yPos)
	o.Zpos = float64(vf.zPos)

	npixels := int(cnpixels)
	pixels := (*[1 << 30]C.uchar)(unsafe.Pointer(ptPixels))
	o.Pixels = make([]uint8, npixels)
	for i := 0; i < npixels; i++ {
		o.Pixels[i] = uint8(pixels[i])
	}
	return
}

// WritePng writes frame (pixels) to png file
func (o TimestampedVideoFrame) WritePng(filename string) (err error) {

	// create image
	img := image.NewRGBA(image.Rect(0, 0, o.Width, o.Height))

	// set pixels
	i := 0
	for row := 0; row < o.Height; row++ {
		for col := 0; col < o.Width*o.Channels; col += o.Channels {
			pix := col + row*o.Width*o.Channels
			img.Pix[i+0] = o.Pixels[pix+0]
			img.Pix[i+1] = o.Pixels[pix+1]
			img.Pix[i+2] = o.Pixels[pix+2]
			img.Pix[i+3] = 255
			i += 4
		}
	}

	// save file
	outputFile, err := os.Create(filename)
	if err != nil {
		return
	}
	defer outputFile.Close()
	err = png.Encode(outputFile, img)
	return
}
