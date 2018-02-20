# ------------------------------------------------------------------------------------------------
# Copyright (c) 2016 Microsoft Corporation
# 
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
# associated documentation files (the "Software"), to deal in the Software without restriction,
# including without limitation the rights to use, copy, modify, merge, publish, distribute,
# sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
# 
# The above copyright notice and this permission notice shall be included in all copies or
# substantial portions of the Software.
# 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
# NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
# DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
# ------------------------------------------------------------------------------------------------

import os
import sys
import time
import errno
import matplotlib
import numpy
import pylab
import argparse
from dateutil.parser import parse

# Frame rate plotter.
# Parses a Malmo platform-side log file, extracting information about frame writing, and plots a graph
# of frames written vs frames received.
# Originally created as a tool for diagnosing performance problems with Malmo's frame recording code,
# tracking write speeds, spotting client-side rendering glitches etc - but
# also provides a helpful starting point for other log-parsing scripts.

# To generate the log file which this script parses, add the following to your Malmo agent code:

# MalmoPython.setLogging(path_to_log, MalmoPython.LoggingSeverityLevel.LOG_TRACE)   # TRACE includes information about frames written / queued.
# MalmoPython.setLoggingComponent(MalmoPython.LoggingComponent.LOG_TCP, False)      # Switch off TCP logging to substantially reduce the size of the log file!

# This will produce a log file which contains lines like this:
# 2018-Jan-24 12:24:51.172127 P TRACE   Writing frame 69, 432x240x3

COL_DATE, COL_TIME, COL_SIDE, COL_SEVERITY, COL_ACTION, COL_FRAME, COL_FRAMENO = range(7)

parser = argparse.ArgumentParser(description='Script for extracting frame rate data from malmo log file.')
parser.add_argument("logfile", help="specifies the log file to parse for data")

args = parser.parse_args()
print("Parsing {}:".format(args.logfile))
queue_times = []
queue_values = []
write_times = []
write_values = []
drop_times = []
drop_values = []
mission_split_points = []
frames_dropped = 0
line_number = 0
mission_count = -1
terms_of_interest=["BmpFrameWriter dropping frame - buffer is full", "Writing frame", "Pushing frame", "Tarring frame"]
with open(args.logfile) as file:
    for line in file:
        line_number += 1
        if "Initialising servers..." in line:
            frames_dropped = 0
            mission_count += 1
            if mission_count > 0:
                mission_split_points.append((len(queue_values), len(write_values), len(drop_values)))
        if not any(term in line for term in terms_of_interest):
            continue
        cols = line.split()
        dt = parse(cols[COL_DATE] + " " + cols[COL_TIME])
        if "dropping frame" in line:
            # (This log line only happens when recording as bitmaps, rather than video.)
            drop_times.append(dt)
            drop_values.append(frames_dropped)
            frames_dropped += 1
        else:
            fn = int(cols[COL_FRAMENO][:-1])
            if cols[COL_ACTION] in ["Writing", "Tarring"]:
                write_times.append(dt)
                write_values.append(fn)
            elif cols[COL_ACTION] == "Pushing":
                queue_times.append(dt)
                queue_values.append(fn)
            else:
                print("Incomprehensible frame action in line ", line_number)

# Done parsing the log file - now plot some graphs:
queue_speeds = []
write_speeds = []
# The following is very noisy - could be useful to apply some smoothing.
# Left an as exercise for the reader...
for i, qt in enumerate(queue_times):
    if i > 0:
        speed = queue_times[i] - queue_times[i - 1]
        fps = 1.0 / speed.total_seconds()
        queue_speeds.append(fps)

for i, wt in enumerate(write_times):
    if i > 0:
        speed = write_times[i] - write_times[i - 1]
        fps = 1.0 / speed.total_seconds()
        write_speeds.append(fps)

fig, (ax1, ax2) = pylab.subplots(nrows=2, sharex=True)
ax1.set_title('Frame write data')
qa, wa, da = 0, 0, 0
for i, mission in enumerate(mission_split_points):
    qb, wb, db = mission
    plot_queues = ax1.plot(queue_times[qa:qb], queue_values[qa:qb], 'r-', label='Frames queued')
    plot_writes = ax1.plot(write_times[wa:wb], write_values[wa:wb], 'g-', label='Frames written')
    plot_drops = ax1.plot(drop_times[da:db], drop_values[da:db], 'b-', label='Frames dropped')
    plot_queue_rate = ax2.plot(queue_times[qa:qb-1], queue_speeds[qa:qb-1], 'r-', label='Frames queued')
    plot_write_rate = ax2.plot(write_times[qa:qb-1], write_speeds[qa:qb-1], 'g-', label='Frames written')
    qa, wa, da = qb, wb, db

ax1.set_ylabel('Frame number')
ax2.set_ylabel('Frames per second')
ax2.set_xlabel('Event time')
legend = fig.legend(fontsize='x-small')
pylab.tight_layout()
pylab.show()
