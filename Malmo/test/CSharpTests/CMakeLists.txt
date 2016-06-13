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

if( WIN32 )
  set( TEST_FOLDER ${CMAKE_CURRENT_BINARY_DIR}/$<CONFIG> )
else()
  set( TEST_FOLDER ${CMAKE_CURRENT_BINARY_DIR} )
endif()

foreach( file ${CSHARP_TEST_SOURCES} )
  get_filename_component( test_name "${file}" NAME_WE )
  set( test_name "CSharpTests_${test_name}" )  

  csharp_add_executable( ${test_name} ${file} "${MALMO_CSHARP_DIR}/MalmoNET.dll" )
  add_dependencies( ${test_name} MalmoNET )

  # copy the MalmoNET dynamic libraries to the test build folder
  add_custom_command( TARGET ${test_name} POST_BUILD COMMAND ${CMAKE_COMMAND} ARGS -E copy_if_different ${MALMO_CSHARP_DIR}/MalmoNET.dll ${TEST_FOLDER}/MalmoNET.dll )
  add_custom_command( TARGET ${test_name} POST_BUILD COMMAND ${CMAKE_COMMAND} ARGS -E copy_if_different ${MALMO_CSHARP_DIR}/${MALMO_NET_NATIVE} ${TEST_FOLDER}/${MALMO_NET_NATIVE} )

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
