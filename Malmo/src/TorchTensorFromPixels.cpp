// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

// Local:
#include "TorchTensorFromPixels.h"

// Torch:
#include <THStorage.h>
#include <THTensor.h>

namespace malmo
{
    void getTorchTensorFromPixels( const TimestampedVideoFrame& image, intptr_t tensor_ptr )
    {
        THFloatTensor* tensor = reinterpret_cast< THFloatTensor* >( tensor_ptr );

        if( tensor == NULL || tensor->size == NULL || tensor->storage == NULL || tensor->storage->data == NULL )
            throw std::runtime_error( "Torch tensor hasn't been allocated" );
        
        if( tensor->nDimension != 3 
                || tensor->size[0] != image.channels
                || tensor->size[1] != image.height 
                || tensor->size[2] != image.width )
            throw std::runtime_error( "Tensor size doesn't match frame size" );
        
        int i = 0;
        for( size_t rgb = 0; rgb < image.channels; rgb++ )
        {
            for( size_t y = 0; y < image.height; y++ )
            {
                for( size_t x = 0; x < image.width; x++ )
                {
                    tensor->storage->data[i] = static_cast<float>( image.pixels[ ((y*image.width) + x)*image.channels + rgb ] );
                    i++;
                }
            }
        }
    }
}
