# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

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
        
endforeach()
