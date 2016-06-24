-- --------------------------------------------------------------------------------------------------
--  Copyright (c) 2016 Microsoft Corporation
--  
--  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
--  associated documentation files (the "Software"), to deal in the Software without restriction,
--  including without limitation the rights to use, copy, modify, merge, publish, distribute,
--  sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
--  furnished to do so, subject to the following conditions:
--  
--  The above copyright notice and this permission notice shall be included in all copies or
--  substantial portions of the Software.
--  
--  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
--  NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
--  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
--  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
--  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
-- --------------------------------------------------------------------------------------------------

require 'libMalmoLua'

local pset0 = ParameterSet()
pset0:set("foo", "bar")
pset0:setInt("bar", 1)
pset0:setDouble("baz", 0.5)
pset0:setBool("kludge", false)

assert(pset0:get("foo") == "bar") 

assert(pset0:get("bar") == "1" and pset0:getInt("bar") == 1 and pset0:getDouble("bar") == 1.0) 

assert(pset0:get("baz") == "0.5" and pset0:getDouble("baz") == 0.5) 

assert(pset0:get("kludge") == "false" and not pset0:getBool("kludge"))

local pset1 = ParameterSet(pset0:toJson());

assert(pset0:get("foo") == pset1:get("foo")) 

assert(pset0:getInt("bar") == pset1:getInt("bar"))

assert(pset0:getDouble("baz") == pset1:getDouble("baz"))

assert(pset0:getBool("kludge") == pset1:getBool("kludge"))