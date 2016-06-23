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
