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

set( CSHARP_TEST_SOURCES
  test_agent_host.cs
  test_argument_parser.cs 
  test_mission.cs
  test_parameter_set.cs
)

if ( ALE_FOUND )
  set( CSHARP_TEST_SOURCES
    ${CSHARP_TEST_SOURCES}
    test_ALE_built.cs
  )
endif()

if( WIN32 )
  set( TEST_FOLDER ${CMAKE_CURRENT_BINARY_DIR}/$<CONFIG> )
else()
  set( TEST_FOLDER ${CMAKE_CURRENT_BINARY_DIR} )
endif()

foreach( file ${CSHARP_TEST_SOURCES} )
  get_filename_component( test_name "${file}" NAME_WE )
  set( test_name "CSharpTests_${test_name}" )  

  add_executable( ${test_name} ${file} )
  target_link_libraries( ${test_name} MalmoNET ${SWIG_MODULE_MalmoNETNative_REAL_NAME} )

  # copy the MalmoNET dynamic libraries to the test build folder
  add_custom_command( TARGET ${test_name} POST_BUILD COMMAND ${CMAKE_COMMAND} ARGS -E copy_if_different ${MALMO_CSHARP_DIR}/MalmoNET.dll ${TEST_FOLDER}/MalmoNET.dll )
  add_custom_command( TARGET ${test_name} POST_BUILD COMMAND ${CMAKE_COMMAND} ARGS -E copy_if_different ${MALMO_CSHARP_DIR}/${MALMO_NET_NATIVE} ${TEST_FOLDER}/${MALMO_NET_NATIVE} )

  if( NOT WIN32 )
    add_test( NAME ${test_name} COMMAND ${CSHARP_INTERPRETER} ${CMAKE_CURRENT_BINARY_DIR}/${test_name}.exe )
  else()
    add_test( NAME ${test_name} COMMAND ${CMAKE_CURRENT_BINARY_DIR}/${test_name}.exe )
  endif()
  set_tests_properties( ${test_name} PROPERTIES ENVIRONMENT "MALMO_XSD_PATH=$ENV{MALMO_XSD_PATH}" )

endforeach()
