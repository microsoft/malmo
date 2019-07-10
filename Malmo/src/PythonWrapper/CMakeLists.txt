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

set( CPP_SOURCES
  python_module.cpp
)

set( OTHER_FILES
  ${PROJECT_SOURCE_DIR}/Malmo/samples/Python_examples/tutorial_6.xml
)

python_add_module( MalmoPython SHARED ${CPP_SOURCES} )
include_directories( MalmoPython ${PYTHON_INCLUDE_DIRS} )
target_link_libraries( MalmoPython Malmo )
if( MSVC )
  set_property( TARGET MalmoPython APPEND PROPERTY COMPILE_DEFINITIONS "BOOST_PYTHON_STATIC_LIB=1" ) 
  target_link_libraries( MalmoPython ${PYTHON_LIBRARIES} )
elseif( APPLE )
  set_target_properties(MalmoPython
      PROPERTIES
      SUFFIX ".so"
      LINK_FLAGS "-undefined dynamic_lookup")
endif()

# copy the extra files to the build folder for use with testing
foreach( file ${OTHER_FILES} )
  get_filename_component( out_file ${file} NAME )
  if( MSVC )
    set( out_file ${CMAKE_CURRENT_BINARY_DIR}/$<CONFIG>/${out_file} )
  else()
    set( out_file ${CMAKE_CURRENT_BINARY_DIR}/${out_file} )
  endif()

  add_custom_command(
    TARGET MalmoPython
    POST_BUILD
    COMMAND ${CMAKE_COMMAND}
    ARGS -E copy_if_different ${file} ${out_file}
  )
endforeach()

if ( ALE_FOUND )
  target_compile_definitions(MalmoPython PRIVATE WRAP_ALE)
endif()

install( TARGETS MalmoPython DESTINATION Python_Examples )
