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
#include <ParameterSet.h>
using namespace malmo;

// STL:
#include <exception>
#include <iostream>
#include <string>
using namespace std;

int main()
{
    ParameterSet pset0;

    pset0.set("foo", "bar");
    pset0.setInt("bar", 1);
    pset0.setDouble("baz", 0.5);
    pset0.setBool("kludge", false);

    if (pset0.get("foo") != "bar"){
        return EXIT_FAILURE;
    }

    if (pset0.get("bar") != "1" || pset0.getInt("bar") != 1 || pset0.getDouble("bar") != 1.0){
        return EXIT_FAILURE;
    }

    if (pset0.get("baz") != "0.5" || pset0.getDouble("baz") != 0.5){
        return EXIT_FAILURE;
    }

    if (pset0.get("kludge") != "false" || pset0.getBool("kludge")){
        return EXIT_FAILURE;
    }

    ParameterSet pset1(pset0.toJson());

    if (pset0.get("foo") != pset1.get("foo")){
        return EXIT_FAILURE;
    }

    if (pset0.getInt("bar") != pset1.getInt("bar")){
        return EXIT_FAILURE;
    }

    if (pset0.getDouble("baz") != pset1.getDouble("baz")){
        return EXIT_FAILURE;
    }

    if (pset0.getBool("kludge") != pset1.getBool("kludge")){
        return EXIT_FAILURE;
    }
    
    return EXIT_SUCCESS;
}
