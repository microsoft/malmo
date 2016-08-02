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

import com.microsoft.msr.malmo.*;

public class test_argument_parser
{
    static 
    {
        System.loadLibrary("MalmoJava");
    }  

    public static void main(String argv[]) 
    {
        ArgumentParser parser = new ArgumentParser( "test_argument_parser.java" );
        
        parser.addOptionalIntArgument( "runs", "how many runs to perform", 1 );
        parser.addOptionalFlag( "verbose", "print lots of debugging information" );
        parser.addOptionalFloatArgument( "q", "what is the q parameter", 0.01 );
        parser.addOptionalStringArgument( "mission", "the mission filename", "" );
        
        StringVector args1 = new StringVector();
        args1.add( "filename" );
        args1.add( "--runs" );
        args1.add( "3" );
        args1.add( "--q" );
        args1.add( "0.2" );
        args1.add( "--mission" );
        args1.add( "test.xml" );
        args1.add( "--verbose" );
        try {
            parser.parse( args1 );
        }
        catch( Exception e ) {
            System.out.println( e );
            System.exit(1);
        }
        if( !parser.receivedArgument( "runs" ) || parser.getIntArgument( "runs" ) != 3 ) {
            System.out.println( "runs != 3" );
            System.exit(1);
        }
        if( !parser.receivedArgument( "q" ) || Math.abs( 0.2 - parser.getFloatArgument( "q" ) ) > 1E-06 ) {
            System.out.println( "q != 0.2" );
            System.exit(1);
        }
        if( !parser.receivedArgument( "mission" ) || !parser.getStringArgument( "mission" ).equals( "test.xml" ) ) {
            System.out.println( "mission != test.xml : " + parser.getStringArgument( "mission" ) );
            System.exit(1);
        }
        if( !parser.receivedArgument( "verbose" ) ) {
            System.out.println( "verbose not received" );
            System.exit(1);
        }
        if( parser.receivedArgument( "test.xml" ) ) {
            System.out.println( "test.xml received" );
            System.exit(1);
        }

        StringVector args2 = new StringVector();
        args2.add( "filename" );
        args2.add( "--runs" ); // we expect this to give an error
        try {
            parser.parse( args2 );
        }
        catch( Exception e ) {
            System.exit(0); // this is what we expect to happen
        }
        System.out.println( "invalid sequence gave no error" );
        System.exit(1);
    }
}
