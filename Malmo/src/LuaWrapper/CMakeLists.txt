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
  install( FILES ${SCHEMA_FILES} DESTINATION ${dest} )
endforeach()

# copy the schema files to the build folder
foreach( xsd_file ${SCHEMA_FILES} )

  get_filename_component( out_file ${xsd_file} NAME )
  if( MSVC )
    set( out_file ${CMAKE_CURRENT_BINARY_DIR}/$<CONFIG>/${out_file} )
  else()
    set( out_file ${CMAKE_CURRENT_BINARY_DIR}/${out_file} )
  endif()

  add_custom_command( 
    TARGET ${LUA_LIB_NAME}
    POST_BUILD
    COMMAND ${CMAKE_COMMAND}
    ARGS -E copy ${xsd_file} ${out_file}
  )

endforeach()
