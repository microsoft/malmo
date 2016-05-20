// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

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
