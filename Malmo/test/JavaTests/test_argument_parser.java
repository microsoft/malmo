// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

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
