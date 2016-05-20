-- --------------------------------------------------------------------------------------------------------------------
-- Copyright (C) Microsoft Corporation.  All rights reserved.
-- --------------------------------------------------------------------------------------------------------------------

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