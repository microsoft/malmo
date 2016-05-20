// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

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
