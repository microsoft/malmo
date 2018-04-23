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

set( CPP_SAMPLE_SOURCES
  run_mission.cpp 
)

set( MALMO_INCLUDE_FOLDERS 
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src
    ${Boost_INCLUDE_DIR} 
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
