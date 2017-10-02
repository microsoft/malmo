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

#ifndef DEFINITIONS_H
#define DEFINITIONS_H

#ifdef __cplusplus
extern "C" {
#endif

#include "stdlib.h"

// type to hold timestamp data
typedef struct {
    int  year;
    int  month;
    int  day;
    long hours;
    long minutes;
    long seconds;
    long nanoseconds;
} timestamp_t;

// type to hold videoframe data
typedef struct {
    short width;
    short height;
    short channels;
    float pitch;
    float yaw;
    float xPos;
    float yPos;
    float zPos;
} videoframe_t;

// type to hold mission record spec data
typedef struct {
    int record_mp4;
    int record_observations;
    int record_rewards;
    int record_commands;
    long mp4_bit_rate;
    int mp4_fps;
    const char* destination;
} mission_record_spec_t;

// type to hold client pool data
typedef struct {
    int size;
    long* ports;
    const char** addresses;
} client_pool_t;

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif // DEFINITIONS_H
