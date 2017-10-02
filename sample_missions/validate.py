from __future__ import print_function
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

import sys

if not len(sys.argv) == 2:
    print('\nA utility script that takes a filename argument and attempts to validate it as a valid mission file.')
    print('\nUsage:\n\npython', sys.argv[0], 'file_to_validate.xml\n')
    exit(1)

try:
    import MalmoPython
except:
    print('\nError: Requires the MalmoPython module to be present in the python path or the current directory.\n')
    exit(1)

filename = sys.argv[1]
xml_string = open( filename, 'r' ).read()

try:
    MalmoPython.MissionSpec( xml_string, True )
except RuntimeError as e:
    print('Error validating',filename,':',e) 
    exit(1)

print(filename,'validated correctly.')
