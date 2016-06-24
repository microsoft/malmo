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

set( CPP_SOURCES
  lua_module.cpp
)

set( DESTINATIONS
  Lua_Examples
)

if( Torch_FOUND )
  set( DESTINATIONS
    ${DESTINATIONS}
    Torch_Examples
  )
endif()

if( MSVC )
  set( LUA_LIB_NAME "libMalmoLua" )
else()
  set( LUA_LIB_NAME "MalmoLua" )
endif()

add_library( ${LUA_LIB_NAME} MODULE ${CPP_SOURCES} )
include_directories( ${LUA_LIB_NAME} ${LUA_INCLUDE_DIR} ${LUABIND_INCLUDE_DIRS} )
target_link_libraries( ${LUA_LIB_NAME} ${LUABIND_LIBRARIES} Malmo )

if ( ALE_FOUND )
  target_compile_definitions(${LUA_LIB_NAME} PRIVATE WRAP_ALE)
endif()

foreach( dest ${DESTINATIONS} )
  install( TARGETS ${LUA_LIB_NAME} DESTINATION ${dest} )
endforeach()
