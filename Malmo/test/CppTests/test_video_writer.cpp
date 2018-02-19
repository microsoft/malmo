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
#include <VideoFrameWriter.h>
using namespace malmo;

// Boost:
#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/filesystem.hpp>
using namespace boost::posix_time;

// STL:
#include <vector>
#include <iostream>
#include <fstream>
using namespace std;

const std::string mp4_filename = "mp4_writer_test.mp4";
const std::string info_filename = "frame_info.txt";
const int width = 240;
const int bit_rate = 400000;
const int frequency = 10;
const int frames = 300;
const int box_size = 40;
const int frame_length = 100;

void runTest(std::unique_ptr<VideoFrameWriter> writer)
{
    writer->open();
    ptime timestamp = microsec_clock::universal_time();
    vector<unsigned char> buffer(width * width * 3);
    for (int i = 0; i < frames; i++){
        for (int r = 0, p = 0; r < width; r++){
            for (int c = 0; c < width; c++, p += 3){
                buffer[p] = width - c;
                buffer[p + 2] = r;
            }
        }

        int boxPos = (i * (width - box_size)) / frames;
        for (int r = 0; r < box_size; r++){
            for (int c = 0; c < box_size; c++){
                int p = (boxPos + r) * width * 3 + (boxPos + c) * 3;
                buffer[p] = 255;
                buffer[p + 1] = i < 5 ? 0 : 255;
                buffer[p + 2] = i < 5 ? 0 : 255;
            }
        }

        TimestampedUnsignedCharVector message(timestamp, buffer);
        writer->write(TimestampedVideoFrame(width, width, 3, message));
        timestamp += milliseconds(frame_length);
    }

    writer->close();
}

int main() 
{
    try{

        boost::filesystem::remove(info_filename);
        boost::filesystem::remove(mp4_filename);

        cout << "beginning video writer test...";
        runTest(VideoFrameWriter::create(mp4_filename, "frame_info.txt", width, width, frequency, bit_rate, 3, true));
        cout << "complete." << endl;

        if (!boost::filesystem::exists(info_filename)) {
            cout << "frame info file missing." << endl;
            return EXIT_FAILURE;
        }

        std::string line;
        std::ifstream frame_info_file(info_filename);

        {
            std::stringstream expected;
            expected << "width=" << width;
            std::getline(frame_info_file, line);
            if (line != expected.str()){
                cout << "width line does not match: '" << line << "' != '" << expected.str() << "'";
                return EXIT_FAILURE;
            }
        }

        {
            std::stringstream expected;
            expected << "height=" << width;
            std::getline(frame_info_file, line);
            if (line != expected.str()){
                cout << "height line does not match: '" << line << "' != '" << expected.str() << "'";
                return EXIT_FAILURE;
            }
        }

        int count = 0;
        while (std::getline(frame_info_file, line))
            ++count;

        int correct_frame_count = (frame_length * frames * frequency) / 1000;
        if (count != correct_frame_count) {
            cout << "incorrect number of frames (" << count << " != " << correct_frame_count << ")" << endl;
            return EXIT_FAILURE;
        }
    }
    catch (runtime_error ex){
        cerr << ex.what() << endl;
        return EXIT_FAILURE;
    }

    cout << "test complete." << endl;

    return EXIT_SUCCESS;
}
