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

// Malmo:
#include <VideoServer.h>
#include <TCPClient.h>
using namespace malmo;

// Boost:
#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/filesystem.hpp>
#include <boost/thread.hpp>
using namespace boost::posix_time;

// STL:
#include <atomic>
#include <fstream>
#include <iostream>
#include <vector>
using namespace std;

const short width = 240;
const int channels = 3;
const int port = 10013;
const int num_pixels = width * width * channels;
const milliseconds sleep_time(100);
const int num_frames = 50;
const std::string filename = "video_server_test.mp4";
std::atomic<int> num_messages_received(0);

void handleFrame(TimestampedVideoFrame frame)
{
    if (frame.pixels[0] != num_messages_received)
    {
        std::cout << "Pixel not set, frames passed out of order." << endl;
        exit(EXIT_FAILURE);
    }

    if (frame.width != width || frame.height != width || frame.channels != channels)
    {
        cout << "Mismatch in frame dimensions." << endl;
        exit(EXIT_FAILURE);
    }

    if (frame.xPos != num_messages_received)
    {
        cout << "xPos not set correctly - frames out of order, or float passing has failed." << endl;
        exit(EXIT_FAILURE);
    }

    if (frame.pitch != 90.0f)
    {
        cout << "Pitch not set correctly - float passing has failed - got " << frame.pitch << " - expected 90.0" << endl;
        exit(EXIT_FAILURE);
    }

    num_messages_received++;
}

uint32_t hton_float(float value)
{
    uint32_t temp;
    *((float*)&temp) = value;
    return htonl(temp);
}

int main()
{
    boost::filesystem::remove(filename);

    try{
        boost::asio::io_service io_service;
        boost::shared_ptr<VideoServer> server = boost::make_shared<VideoServer>(io_service, port, width, width, channels, TimestampedVideoFrame::VIDEO, boost::function<void(const TimestampedVideoFrame)>(handleFrame));
        server->recordMP4(filename, 10, 400000, true);
        server->startRecording();
        server->start(server);

        // start the io_service on a background thread
        boost::thread bt(boost::bind(&boost::asio::io_service::run, &io_service));

        boost::this_thread::sleep(sleep_time);

        vector<unsigned char> buffer(num_pixels + TimestampedVideoFrame::FRAME_HEADER_SIZE);
        for (int i = 0; i < num_frames; i++){
            uint32_t* ptr = reinterpret_cast<uint32_t*>(&buffer[0]);
            *ptr = hton_float((float)i); ptr++; //xPos
            *ptr = hton_float(3.1415f); ptr++;   //yPos
            *ptr = hton_float(6.6666f); ptr++;   //zPos
            *ptr = hton_float(0); ptr++;        //yaw
            *ptr = hton_float(90.0f);           //pitch
            for (int r = width - 1, p = TimestampedVideoFrame::FRAME_HEADER_SIZE; r >= 0; r--){
                for (int c = 0; c < width; c++, p += 3){
                    buffer[p] = width - c;
                    buffer[p + 2] = r;
                }
            }

            int boxPos = (i * 200) / num_frames;           
            for (int r = 0; r < 40; r++){
                for (int c = 0; c < 40; c++){
                    int p = TimestampedVideoFrame::FRAME_HEADER_SIZE + (width - boxPos - r - 1) * width * 3 + (boxPos + c) * 3;
                    buffer[p] = 255;
                    buffer[p + 1] = 255;
                    buffer[p + 2] = 255;
                }
            }

            buffer[TimestampedVideoFrame::FRAME_HEADER_SIZE + (width - 1) * width * 3] = i;
            SendOverTCP(io_service, "127.0.0.1", port, buffer, true);

            boost::this_thread::sleep(sleep_time);
        }

        io_service.stop();
        bt.join();
    }
    catch (runtime_error& error){
        cout << "Error: " << error.what() << endl;
        return EXIT_FAILURE;
    }

    if (num_messages_received != num_frames){
        cout << num_messages_received << " != " << num_frames;
        return EXIT_FAILURE;
    }

    if(!boost::filesystem::exists(filename))
    {
        return EXIT_FAILURE;
    }

    return EXIT_SUCCESS;
}
