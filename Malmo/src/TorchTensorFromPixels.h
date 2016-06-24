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

#ifndef _TORCHTENSORFROMPIXELS_H_
#define _TORCHTENSORFROMPIXELS_H_

// Local:
#include "TimestampedVideoFrame.h"

namespace malmo 
{
    //! Takes a THFloatTensor and fills it with values from the image. 
    //! Throws an exception if the tensor has not been allocated to the right size.
    //! Float values are in the range 0-255. To save as an image file, 
    //! use e.g. image.save( 'image.png', torch.div( ti, 255 ) )
    //! \param image The image to convert. Obtain from world_state.video_frames, see Torch_Examples/run_mission.lua
    //! \param tensor A pointer to an allocated THFloatTensor, for filling.
    /*! Example:
     *  
     *    local ti = torch.FloatTensor( frame.channels, frame.height, frame.width )
     *    getTorchTensorFromPixels( frame, tonumber( torch.cdata( ti, true ) ) )
     */
    void getTorchTensorFromPixels( const TimestampedVideoFrame& image, intptr_t tensor );
}

#endif
