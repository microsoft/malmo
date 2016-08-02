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

public class test_parameter_set
{
    static 
    {
        System.loadLibrary("MalmoJava");
    }  

    public static void main(String argv[]) 
    {
        ParameterSet pset0 = new ParameterSet();

        pset0.set("foo", "bar");
        pset0.setInt("bar", 1);
        pset0.setDouble("baz", 0.5);
        pset0.setBool("kludge", false);

        if( !pset0.get("foo").equals( "bar" ) ){
            System.out.println( "foo != bar : " + pset0.get("foo") );
            System.exit(1);
        }

        if( !pset0.get("bar").equals("1") || pset0.getInt("bar") != 1 || pset0.getDouble("bar") != 1.0){
            System.out.println( "bar != 1" );
            System.exit(1);
        }

        if( !pset0.get("baz").equals("0.5") || pset0.getDouble("baz") != 0.5){
            System.out.println( "baz != 0.5" );
            System.exit(1);
        }

        if( !pset0.get("kludge").equals("false") || pset0.getBool("kludge")){
            System.out.println( "kludge != false" );
            System.exit(1);
        }

        ParameterSet pset1 = new ParameterSet(pset0.toJson());

        if( !pset0.get("foo").equals(pset1.get("foo")) ){
            System.out.println( "foo != foo" );
            System.exit(1);
        }

        if (pset0.getInt("bar") != pset1.getInt("bar")){
            System.out.println( "bar != bar" );
            System.exit(1);
        }

        if (pset0.getDouble("baz") != pset1.getDouble("baz")){
            System.out.println( "baz != baz" );
            System.exit(1);
        }

        if (pset0.getBool("kludge") != pset1.getBool("kludge")){
            System.out.println( "kludge != kludge" );
            System.exit(1);
        }
    }
}
