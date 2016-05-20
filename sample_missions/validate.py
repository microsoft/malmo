# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

import sys

if not len(sys.argv) == 2:
    print '\nA utility script that takes a filename argument and attempts to validate it as a valid mission file.'
    print '\nUsage:\n\npython', sys.argv[0], 'file_to_validate.xml\n'
    exit(1)

try:
    import MalmoPython
except:
    print '\nError: Requires the MalmoPython module to be present in the python path or the current directory.\n'
    exit(1)

filename = sys.argv[1]
xml_string = open( filename, 'r' ).read()

try:
    MalmoPython.MissionSpec( xml_string, True )
except RuntimeError as e:
    print 'Error validating',filename,':',e 
    exit(1)

print filename,'validated correctly.'
