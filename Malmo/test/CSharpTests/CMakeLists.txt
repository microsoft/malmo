# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

find_package( CSharp REQUIRED )
include( ${CSHARP_USE_FILE} )

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

foreach( file ${CSHARP_TEST_SOURCES} )
  get_filename_component( test_name "${file}" NAME_WE )
  set( test_name "CSharpTests_${test_name}" )  
  
  csharp_add_executable( ${test_name} ${file} "${MALMO_CSHARP_DIR}/MalmoNET.dll" )
  add_dependencies( ${test_name} MalmoNET )
  
  IF( WIN32 )
    ADD_CUSTOM_COMMAND(TARGET ${test_name} 
    POST_BUILD
    COMMAND set
    ARGS malmonet_dir=$<TARGET_FILE_DIR:MalmoNETNative>
    COMMAND set
    ARGS malmonet_dir=%malmonet_dir:/=\\%
    COMMAND copy
    ARGS /Y %malmonet_dir%\\*.dll .
    WORKING_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}/$(Configuration)"
    COMMENT "Copying dependencies"  
    )
  ELSE( UNIX )
    ADD_CUSTOM_COMMAND(TARGET ${test_name}
    POST_BUILD
    COMMAND cp
    ARGS $<TARGET_FILE_DIR:MalmoNETNative>/MalmoNET.dll .
    COMMAND cp
    ARGS $<TARGET_FILE_DIR:MalmoNETNative>/libMalmoNETNative.so .
    WORKING_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}/$(Configuration)"
    COMMENT "Copying dependencies"
    )
  ENDIF( WIN32 )
      
  if( NOT WIN32 )
    add_test( NAME ${test_name} COMMAND ${CSHARP_INTERPRETER} ${CMAKE_CURRENT_BINARY_DIR}/${test_name}.exe )
  else()
    add_test( NAME ${test_name} COMMAND ${CMAKE_CURRENT_BINARY_DIR}/${test_name}.exe )
  endif()
        
endforeach()

# copy the schema files to the test build folder
foreach( xsd_file ${SCHEMA_FILES} )
get_filename_component( out_file ${xsd_file} NAME )
set( out_file ${CMAKE_CURRENT_BINARY_DIR}/${out_file} )
add_custom_command( 
  TARGET ${test_name}
  POST_BUILD
  COMMAND ${CMAKE_COMMAND}
  ARGS -E copy_if_different ${xsd_file} ${out_file}
)
endforeach()
