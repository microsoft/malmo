# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

import MalmoPython
import sys

parser = MalmoPython.ArgumentParser( 'test_argument_parser.py' )

parser.addOptionalFloatArgument( 'run', 'how far to run', 24.7 )
parser.addOptionalIntArgument( 'remote', 'who has the remote', -1 )
parser.addOptionalFlag( 'verbose', 'print lots of debugging information' )
parser.addOptionalStringArgument( 'mission', 'the mission filename', '' )

args = [ 'filename', '--run', '3', '--remote' ] # we expect this to give an error

try:
    parser.parse( args )
except RuntimeError as e:
    sys.exit(0) # this outcome is what we expect

sys.exit(1) # the parse() should have given an error 
