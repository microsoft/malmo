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

# Tool for turning 32bpp float depthmaps into 24bpp RGB images.
# Will read a Malmo mission recording file, extract each depthmap image in turn, and
# use numpy / matplot to turn it into a coloured image.
# The main purpose is to check Malmo's 32bpp depthmap generation, and to check the numpy file
# format code in BmpFrameWriter.cpp, but this script also provides a useful starting point for
# anyone who wants to do data processing on the saved depthmap data.

import numpy as np
from matplotlib import cm
import tarfile
from io import BytesIO
from PIL import Image
import argparse
import errno
import os

parser = argparse.ArgumentParser(description='Script for converting floating point numpy depthmaps into coloured images.')
parser.add_argument("--recording", help="specifies the Malmo mission recording tar.gz file to parse for data")
parser.add_argument("--near_clip", help="near clipping plane", default=0, type=float)
parser.add_argument("--far_clip", help="far clipping plane", default=64, type=float)
parser.add_argument("--colourmap", help="name of colourmap to use (see https://matplotlib.org/users/colormaps.html)", default="viridis")
args = parser.parse_args()

near_val = args.near_clip
far_val = args.far_clip
tarpath = args.recording

global_min, global_max = None, None
frames_processed = 0

cmap = cm.get_cmap(args.colourmap)

# Open the gzipped tarfile:
print("Loading tarfile...")
tar = tarfile.open(tarpath)
missionname = os.path.splitext(os.path.basename(tarpath))[0]
output_destination = os.path.join(os.path.dirname(tarpath), missionname + "_depthmaps")
try:
    os.makedirs(output_destination)
except OSError as exception:
    if exception.errno != errno.EEXIST: # ignore error if already existed
        raise

# Depthmaps are stored in a series of gzipped tars, within the main tarfile.
# These tars will be within a folder named "depth_frames":
bmparchive_filenames = [t for t in tar.getnames() if "/depth_frames/" in t and t.endswith("tar.gz")]
# Now iterate through each of these files, extracting the actual frames:
for bmparchive_filename in bmparchive_filenames:
    print("--->Loading ", bmparchive_filename)
    bmparchive = tar.extractfile(bmparchive_filename)
    bmptar = tarfile.open(fileobj=bmparchive)
    for bmp_filename in bmptar.getnames():
        print("------->Converting ", bmp_filename)
        # Can't pass the file straight from the tar to numpy -
        # need to write it into a BytesIO object first.
        array_file = BytesIO()
        array_file.write(bmptar.extractfile(bmp_filename).read())
        array_file.seek(0)
        bmp = np.load(array_file)
        # Update our global min/max values, for information's sake:
        mindist = bmp.min()
        maxdist = bmp.max()
        if global_min == None or global_min > mindist:
            global_min = mindist
        if global_max == None or global_max < maxdist:
            global_max = maxdist
        # Do some normalisation. First clip to our near/far values:
        bmp = bmp.clip(near_val, far_val)
        # Now normalise to values between 0 and 1:
        bmp = (bmp - near_val) / (far_val - near_val)
        # Use matplotlib colourmap to convert to RGB:
        im = Image.fromarray(np.uint8(cmap(bmp)*255))
        # And save:
        im.save(os.path.join(output_destination, bmp_filename.split(".")[0] + "_" + args.colourmap + ".png"))
        frames_processed += 1

print("Frames processed: ", frames_processed)
print("Overall depth range: ", global_min, " to ", global_max)