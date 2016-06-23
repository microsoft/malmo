# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

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
