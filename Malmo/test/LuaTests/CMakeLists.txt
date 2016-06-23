# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

set( LUA_TEST_SOURCES
  test_wrapping.lua
  test_argument_parser.lua
  test_mission.lua
  test_agent_host.lua
  test_parameter_set.lua
)

if ( ALE_FOUND )
  set( LUA_TEST_SOURCES
    ${LUA_TEST_SOURCES}
    test_ALE_built.lua
  )
endif()

# Each of these files is a Lua test.
foreach( file ${LUA_TEST_SOURCES} )

  get_filename_component( test_name "${file}" NAME_WE )
  set( test_name "LuaTests_${test_name}" )
  
  add_test(NAME ${test_name}
    WORKING_DIRECTORY ${MALMO_LUA_DIR}
    COMMAND ${LUA_EXECUTABLE} ${CMAKE_CURRENT_SOURCE_DIR}/${file}
  )
  set_tests_properties( ${test_name} PROPERTIES ENVIRONMENT "MALMO_XSD_PATH=$ENV{MALMO_XSD_PATH}" )

endforeach()

# If Torch is installed, each of these files can be tested in Torch too.
if( Torch_FOUND )

  set( TORCH_EXECUTABLE ${Torch_INSTALL_PREFIX}/${Torch_INSTALL_BIN_SUBDIR}/th )

  foreach( file ${LUA_TEST_SOURCES} )
  
    get_filename_component( test_name "${file}" NAME_WE )
    set( test_name "TorchTests_${test_name}" )
    
    add_test(NAME ${test_name}
      WORKING_DIRECTORY ${MALMO_LUA_DIR}
      COMMAND ${TORCH_EXECUTABLE} ${CMAKE_CURRENT_SOURCE_DIR}/${file}
    )
    set_tests_properties( ${test_name} PROPERTIES ENVIRONMENT "MALMO_XSD_PATH=$ENV{MALMO_XSD_PATH}" )
  
  endforeach()
  
endif()
