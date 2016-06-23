# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

set( CPP_SOURCES
  python_module.cpp
)

set( OTHER_FILES
  ${CMAKE_SOURCE_DIR}/Malmo/samples/Python_examples/tutorial_6.xml
)

python_add_module( MalmoPython SHARED ${CPP_SOURCES} )
include_directories( MalmoPython ${PYTHON_INCLUDE_DIRS} )
target_link_libraries( MalmoPython Malmo ${PYTHON_LIBRARIES} )
if( MSVC )
  set_property( TARGET MalmoPython APPEND PROPERTY COMPILE_DEFINITIONS "BOOST_PYTHON_STATIC_LIB=1" ) 
elseif( APPLE )
  set_property( TARGET MalmoPython PROPERTY SUFFIX ".so" )
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
