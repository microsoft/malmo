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
