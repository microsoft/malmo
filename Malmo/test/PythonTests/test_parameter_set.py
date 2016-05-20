# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

import MalmoPython

pset0 = MalmoPython.ParameterSet()
pset0.set("foo", "bar")
pset0.setInt("bar", 1)
pset0.setDouble("baz", 0.5)
pset0.setBool("kludge", False)

if (pset0.get("foo") != "bar"): 
    print("foo not set correctly")
    exit(1)

if (pset0.get("bar") != "1" or pset0.getInt("bar") != 1 or pset0.getDouble("bar") != 1.0): 
    print("bar not set correctly")
    exit(1)

if (pset0.get("baz") != "0.5" or pset0.getDouble("baz") != 0.5): 
    print("baz not set correctly")
    exit(1)

if (pset0.get("kludge") != "false" or pset0.getBool("kludge") != False):
    print("kludge not set correctly")
    exit(1)


pset1 = MalmoPython.ParameterSet(pset0.toJson());

if (pset0.get("foo") != pset1.get("foo")): 
    exit(1)

if (pset0.getInt("bar") != pset1.getInt("bar")):
    exit(1)

if (pset0.getDouble("baz") != pset1.getDouble("baz")):
    exit(1)

if (pset0.getBool("kludge") != pset1.getBool("kludge")):
    exit(1)