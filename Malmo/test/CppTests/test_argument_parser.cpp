// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

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
