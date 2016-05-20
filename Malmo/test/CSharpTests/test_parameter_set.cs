// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

using System;
using Microsoft.Research.Malmo;

class Program
{
    public static void Main(string[] args)
    {
        ParameterSet pset0 = new ParameterSet();

        pset0.set("foo", "bar");
        pset0.setInt("bar", 1);
        pset0.setDouble("baz", 0.5);
        pset0.setBool("kludge", false);

        if (pset0.get("foo") != "bar") {
            Environment.Exit(1);
        }

        if (pset0.get("bar") != "1" || pset0.getInt("bar") != 1 || pset0.getDouble("bar") != 1.0) {
            Environment.Exit(1);
        }

        if (pset0.get("baz") != "0.5" || pset0.getDouble("baz") != 0.5) {
            Environment.Exit(1);
        }

        if (pset0.get("kludge") != "false" || pset0.getBool("kludge")) {
            Environment.Exit(1);
        }

        ParameterSet pset1 = new ParameterSet(pset0.toJson());

        if (pset0.get("foo") != pset1.get("foo")) {
            Environment.Exit(1);
        }

        if (pset0.getInt("bar") != pset1.getInt("bar")) {
            Environment.Exit(1);
        }

        if (pset0.getDouble("baz") != pset1.getDouble("baz")) {
            Environment.Exit(1);
        }

        if (pset0.getBool("kludge") != pset1.getBool("kludge")) {
            Environment.Exit(1);
        }
    }
}
