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
