-- --------------------------------------------------------------------------------------------------
--  Copyright (c) 2016 Microsoft Corporation
--  
--  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
--  associated documentation files (the "Software"), to deal in the Software without restriction,
--  including without limitation the rights to use, copy, modify, merge, publish, distribute,
--  sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
--  furnished to do so, subject to the following conditions:
--  
--  The above copyright notice and this permission notice shall be included in all copies or
--  substantial portions of the Software.
--  
--  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
--  NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
--  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
--  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
--  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
-- --------------------------------------------------------------------------------------------------

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
