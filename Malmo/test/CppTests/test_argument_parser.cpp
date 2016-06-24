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
#include <ArgumentParser.h>
using namespace malmo;

// STL:
#include <cmath>
#include <iostream>
using namespace std;

int main() 
{
    ArgumentParser parser( "test_argument_parser.cpp" );
    
    parser.addOptionalIntArgument( "runs", "how many runs to perform", 1 );
    parser.addOptionalFlag( "verbose", "print lots of debugging information" );
    parser.addOptionalFloatArgument( "q", "what is the q parameter", 0.01 );
    parser.addOptionalStringArgument( "mission", "the mission filename", "" );
    
    vector< string > args1 = { "filename", "--runs", "3", "--q", "0.2", "--mission", "test.xml", "--verbose" };
    try {
        parser.parse( args1 );
    }
    catch( exception& ) {
        return EXIT_FAILURE;
    }
    if( !parser.receivedArgument( "runs" ) || parser.getIntArgument( "runs" ) != 3 )
        return EXIT_FAILURE;
    if( !parser.receivedArgument( "q" ) || std::fabs( 0.2 - parser.getFloatArgument( "q" ) ) > 1E-06 )
        return EXIT_FAILURE;
    if( !parser.receivedArgument( "mission" ) || parser.getStringArgument( "mission" ) != "test.xml" )
        return EXIT_FAILURE;
    if( !parser.receivedArgument( "verbose" ) )
        return EXIT_FAILURE;
    if( parser.receivedArgument( "test.xml" ) )
        return EXIT_FAILURE;

    vector< string > args2 = { "filename", "--runs" }; // we expect this to give an error
    try {
        parser.parse( args2 );
    }
    catch( exception& ) {
        return EXIT_SUCCESS; // this is what we expect to happen
    }

    return EXIT_FAILURE;
}
