-- --------------------------------------------------------------------------------------------------------------------
-- Copyright (C) Microsoft Corporation.  All rights reserved.
-- --------------------------------------------------------------------------------------------------------------------

-- Tests basic functionality of ArgumentParser.

require 'libMalmoLua'

parser = ArgumentParser( 'test_argument_parser.lua' )

parser:addOptionalFloatArgument( 'run', 'how far to run', 728.23 )
parser:addOptionalIntArgument( 'remote', 'who has the remote', -1 )
parser:addOptionalFlag( 'verbose', 'print lots of debugging information' )
parser:addOptionalStringArgument( 'mission', 'the mission filename', '' )

args2 = { '--run', '3', '--remote' } -- debug

if not pcall( function() parser:parse( args2 ) end ) then
    os.exit(0) -- this is what we expect to happen
end

os.exit(1)
