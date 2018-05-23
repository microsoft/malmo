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
#include "ErrorCodeSync.h"

// Boost:

#include <boost/asio.hpp>

namespace malmo {

    void ErrorCodeSync::init_error_code() {
        boost::unique_lock<boost::mutex> lk(mutex);
        error_code = boost::asio::error::would_block;
    }

    const boost::system::error_code ErrorCodeSync::await_error_code() {
        boost::unique_lock<boost::mutex> lk(mutex);
        while (error_code == boost::asio::error::would_block) {
            cond.wait(lk);
        }
        return error_code;
    }

    void ErrorCodeSync::signal_error_code(const boost::system::error_code& ec) {
        boost::unique_lock<boost::mutex> lk(mutex);
        error_code = ec;
        cond.notify_one();
    }
}
