# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

find_package( CSharp REQUIRED )
include( ${CSHARP_USE_FILE} )

set( CSHARP_SAMPLE_SOURCES
  App.config
  Program.cs 
)

IF( WIN32 )
  SET( CSHARP_BUILD ${CMAKE_VS_MSBUILD_COMMAND} )
ELSE()
  SET( CSHARP_BUILD xbuild )
ENDIF()

configure_file( ${CMAKE_CURRENT_SOURCE_DIR}/RunMission.csproj.pre ${CMAKE_CURRENT_SOURCE_DIR}/RunMission.csproj )

if( WIN32 )
  set( CURRENT_BUILD_DIR ${CMAKE_CURRENT_BINARY_DIR}/$<CONFIG> )
  ADD_CUSTOM_COMMAND(
     OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/$(Configuration)/CSharpExamples_RunMission.exe
     COMMAND ${CSHARP_BUILD}
     ARGS ${CMAKE_CURRENT_SOURCE_DIR}/RunMission.csproj /p:Configuration=$<CONFIG> /p:Platform=${CSHARP_PLATFORM} /p:OutputPath=${CURRENT_BUILD_DIR}
  )
  add_custom_target( 
    CSharpExamples_RunMission ALL 
    DEPENDS ${CMAKE_CURRENT_BINARY_DIR}/$(Configuration)/CSharpExamples_RunMission.exe 
    SOURCES ${CSHARP_SAMPLE_SOURCES} 
  )
else()
  set( CURRENT_BUILD_DIR ${CMAKE_CURRENT_BINARY_DIR} )
  ADD_CUSTOM_COMMAND(
    OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/CSharpExamples_RunMission.exe
    COMMAND ${CSHARP_BUILD}
    ARGS ${CMAKE_CURRENT_SOURCE_DIR}/RunMission.csproj /p:Platform=${CSHARP_PLATFORM} /p:OutputPath=${CMAKE_CURRENT_BINARY_DIR}
  )
  add_custom_target(
    CSharpExamples_RunMission ALL 
    DEPENDS ${CURRENT_BUILD_DIR}/CSharpExamples_RunMission.exe 
    SOURCES ${CSHARP_SAMPLE_SOURCES} 
  )
endif()

# copy the schema files to the test build folder
foreach( xsd_file ${SCHEMA_FILES} )
  get_filename_component( out_file ${xsd_file} NAME )
  set( out_file ${CMAKE_CURRENT_BINARY_DIR}/${out_file} )
  add_custom_command( 
    TARGET CSharpExamples_RunMission
    POST_BUILD
    COMMAND ${CMAKE_COMMAND}
    ARGS -E copy_if_different ${xsd_file} ${out_file}
  )
endforeach()

INSTALL( FILES ${CURRENT_BUILD_DIR}/CSharpExamples_RunMission.exe DESTINATION CSharp_Examples )
    
# add integration tests
add_test(
  NAME CSharpIntegrationTests_RunMission_help 
  WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR} 
  COMMAND ${CURRENT_BUILD_DIR}/CSharpExamples_RunMission.exe --help 
)
add_test( 
  NAME CSharpIntegrationTests_RunMission
  WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}
  COMMAND ${CURRENT_BUILD_DIR}/CSharpExamples_RunMission.exe
)

ADD_DEPENDENCIES( CSharpExamples_RunMission MalmoNET )

INSTALL( FILES ${SCHEMA_FILES} DESTINATION CSharp_Examples )
INSTALL( FILES App.config DESTINATION CSharp_Examples )
INSTALL( FILES Program.cs DESTINATION CSharp_Examples )
INSTALL( FILES Properties/AssemblyInfo.cs DESTINATION CSharp_Examples/Properties/ )
INSTALL( FILES RunMission.csproj.in DESTINATION CSharp_Examples RENAME RunMission.csproj )
