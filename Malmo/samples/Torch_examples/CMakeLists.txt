# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

set( SOURCES
  run_mission.lua
)

install( FILES ${SOURCES} DESTINATION Torch_Examples )

set( TORCH_EXECUTABLE ${Torch_INSTALL_PREFIX}/${Torch_INSTALL_BIN_SUBDIR}/th )

# add an integration test for each sample
foreach( file ${SOURCES} )

  get_filename_component( test_name "${file}" NAME_WE )
  set( test_name "TorchIntegrationTests_${test_name}" )
    
  add_test(NAME ${test_name}_help
    WORKING_DIRECTORY ${MALMO_LUA_DIR}
    COMMAND ${TORCH_EXECUTABLE} ${CMAKE_CURRENT_SOURCE_DIR}/${file} --help
  )
  set_tests_properties( ${test_name}_help PROPERTIES ENVIRONMENT "MALMO_XSD_PATH=$ENV{MALMO_XSD_PATH}" )
  add_test(NAME ${test_name}
    WORKING_DIRECTORY ${MALMO_LUA_DIR}
    COMMAND ${TORCH_EXECUTABLE} ${CMAKE_CURRENT_SOURCE_DIR}/${file} --test
  )
  set_tests_properties( ${test_name} PROPERTIES ENVIRONMENT "MALMO_XSD_PATH=$ENV{MALMO_XSD_PATH}" )
  
endforeach()
