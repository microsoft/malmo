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

set( PYTHON_TEST_SOURCES
  test_wrapping.py
  test_argument_parser.py
  test_agent_host.py
  test_mission.py
  test_parameter_set.py
  test_malmoutils.py
)

if ( ALE_FOUND )
  set( PYTHON_TEST_SOURCES
    ${PYTHON_TEST_SOURCES}
    test_ALE_built.py
  )
endif()

file(COPY ${CMAKE_CURRENT_SOURCE_DIR}/../../samples/Python_examples/malmoutils.py DESTINATION ${CMAKE_CURRENT_SOURCE_DIR})

foreach( file ${PYTHON_TEST_SOURCES} )

  get_filename_component( test_name "${file}" NAME_WE )
  set( test_name "PythonTests_${test_name}" )
  

  add_test(NAME ${test_name}
    WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
    COMMAND ${PYTHON_EXECUTABLE} ${file}
  )
  set_tests_properties( ${test_name} PROPERTIES ENVIRONMENT "PYTHONPATH=${MALMO_PYTHON_DIR};LD_LIBRARY_PATH=${MALMO_PYTHON_DIR};MALMO_XSD_PATH=$ENV{MALMO_XSD_PATH}" )
  
endforeach()

