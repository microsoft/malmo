# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

set( SOURCES
  run_mission.lua
  to_string_test.lua
)

install( FILES ${SOURCES} DESTINATION Lua_Examples )

if( Torch_FOUND )
  set( TORCH_EXECUTABLE ${Torch_INSTALL_PREFIX}/${Torch_INSTALL_BIN_SUBDIR}/th )
endif()

# add an integration test for each sample
foreach( file ${SOURCES} )

  get_filename_component( test_name "${file}" NAME_WE )
  set( test_name "LuaIntegrationTests_${test_name}" )
  
  add_test(NAME ${test_name}
    WORKING_DIRECTORY ${MALMO_LUA_DIR}
    COMMAND ${LUA_EXECUTABLE} ${CMAKE_CURRENT_SOURCE_DIR}/${file} --help
  )

endforeach()
  
# If Torch is installed, each of these files can be tested in Torch too.
if( INCLUDE_TORCH )

  foreach( file ${SOURCES} )
  
    get_filename_component( test_name "${file}" NAME_WE )
    set( test_name "TorchIntegrationTests_${test_name}" )
    
    add_test(NAME ${test_name}
      WORKING_DIRECTORY ${MALMO_LUA_DIR}
      COMMAND ${TORCH_EXECUTABLE} ${CMAKE_CURRENT_SOURCE_DIR}/${file} --help
    )
    
  endforeach()
  
endif()
  
