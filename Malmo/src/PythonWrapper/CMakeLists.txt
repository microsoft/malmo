# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

set( CPP_SOURCES
  python_module.cpp
)

python_add_module( MalmoPython SHARED ${CPP_SOURCES} )
include_directories( MalmoPython ${PYTHON_INCLUDE_DIRS} )
target_link_libraries( MalmoPython Malmo ${PYTHON_LIBRARIES} )
if( MSVC )
  set_property( TARGET MalmoPython APPEND PROPERTY COMPILE_DEFINITIONS "BOOST_PYTHON_STATIC_LIB=1" ) 
elseif( APPLE )
  set_property( TARGET MalmoPython PROPERTY SUFFIX ".so" )
endif()

if ( ALE_FOUND )
  target_compile_definitions(MalmoPython PRIVATE WRAP_ALE)
endif()

install( TARGETS MalmoPython DESTINATION Python_Examples )
install( FILES ${SCHEMA_FILES} DESTINATION Python_Examples )

# copy the schema files to the build folder
foreach( xsd_file ${SCHEMA_FILES} )

  get_filename_component( out_file ${xsd_file} NAME )
  if( MSVC )
    set( out_file ${CMAKE_CURRENT_BINARY_DIR}/$<CONFIG>/${out_file} )
  else()
    set( out_file ${CMAKE_CURRENT_BINARY_DIR}/${out_file} )
  endif()

  add_custom_command( 
    TARGET MalmoPython
    POST_BUILD
    COMMAND ${CMAKE_COMMAND}
    ARGS -E copy ${xsd_file} ${out_file}
  )

endforeach()
