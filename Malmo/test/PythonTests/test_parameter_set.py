from __future__ import print_function
# ------------------------------------------------------------------------------------------------
# Copyright (c) 2016 Microsoft Corporation
# 
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
# associated documentation files (the "Software"), to deal in the Software without restriction,
# including without limitation the rights to use, copy, modify, merge, publish, distribute,
# sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
# 
# The above copyright notice and this permission notice shall be included in all copies or
# substantial portions of the Software.
# 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
# NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
# DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
# ------------------------------------------------------------------------------------------------

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