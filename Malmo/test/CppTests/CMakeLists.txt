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

set( CPP_TEST_SOURCES
  create_tcp_server.cpp 
  test_agent_host.cpp
  test_argument_parser.cpp 
  test_client_server.cpp 
  test_mission.cpp
  test_parameter_set.cpp
  test_persistence.cpp
  test_string_server.cpp
  test_video_server.cpp
  test_video_writer.cpp
)

if ( ALE_FOUND )
  set( CPP_TEST_SOURCES
    ${CPP_TEST_SOURCES}
    test_ALE_built.cpp
  )
endif()

set( MALMO_INCLUDE_FOLDERS 
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src
    ${CMAKE_CURRENT_BINARY_DIR}/../../src
    ${Boost_INCLUDE_DIR} 
    ${XSD_INCLUDE_DIRS} 
)
include_directories( ${MALMO_INCLUDE_FOLDERS} )

foreach( file ${CPP_TEST_SOURCES} )

  get_filename_component( test_name "${file}" NAME_WE )
  set( test_name "CppTests_${test_name}" )
  
  add_executable( ${test_name} ${file} )
  target_link_libraries( ${test_name} Malmo )
  
  add_test( NAME ${test_name} COMMAND ${test_name} )
  set_tests_properties( ${test_name} PROPERTIES ENVIRONMENT "MALMO_XSD_PATH=$ENV{MALMO_XSD_PATH}" )
        
endforeach()
