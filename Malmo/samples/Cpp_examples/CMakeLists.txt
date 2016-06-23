# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

set( CPP_SAMPLE_SOURCES
  run_mission.cpp 
)

set( MALMO_INCLUDE_FOLDERS 
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src
    ${CMAKE_CURRENT_BINARY_DIR}/../../src
    ${Boost_INCLUDE_DIR} 
    ${XSD_INCLUDE_DIRS} 
)
include_directories( ${MALMO_INCLUDE_FOLDERS} )

# before running the integration tests, launch Minecraft (if needed)
add_test(
  NAME PreIntegrationTests
  WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}/Minecraft
  COMMAND ${PYTHON_EXECUTABLE} launch_minecraft_in_background.py
)
set_tests_properties( PreIntegrationTests PROPERTIES ENVIRONMENT "MALMO_XSD_PATH=$ENV{MALMO_XSD_PATH}" )

foreach( file ${CPP_SAMPLE_SOURCES} )

  get_filename_component( sample_name ${file} NAME_WE )
  
  add_executable( ${sample_name} ${file} )
  target_link_libraries( ${sample_name} Malmo )

  install( TARGETS ${sample_name} DESTINATION Cpp_Examples )
  
  # add an integration test
  get_filename_component( test_name "${file}" NAME_WE )
  set( test_name "CppIntegrationTests_${test_name}" )
  add_test(NAME ${test_name}_help COMMAND ${sample_name} --help )
  set_tests_properties( ${test_name}_help PROPERTIES ENVIRONMENT "MALMO_XSD_PATH=$ENV{MALMO_XSD_PATH}" )
  add_test(NAME ${test_name} COMMAND ${sample_name} --test )
  set_tests_properties( ${test_name} PROPERTIES ENVIRONMENT "MALMO_XSD_PATH=$ENV{MALMO_XSD_PATH}" )
  
endforeach()

install( FILES ${CPP_SAMPLE_SOURCES} DESTINATION Cpp_Examples )
install( FILES CMakeLists.txt.in DESTINATION Cpp_Examples RENAME CMakeLists.txt )
