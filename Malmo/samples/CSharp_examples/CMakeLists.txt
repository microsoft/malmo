# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

find_package( CSharp REQUIRED )
include( ${CSHARP_USE_FILE} )

set( CSHARP_SAMPLE_SOURCES
  App.config
  Program.cs 
)

configure_file( ${CMAKE_CURRENT_SOURCE_DIR}/RunMission.csproj.pre ${CMAKE_CURRENT_SOURCE_DIR}/RunMission.csproj )

if( WIN32 )
  SET( CSHARP_BUILD ${CMAKE_VS_MSBUILD_COMMAND} )
  set( CSHARP_RUN "" )
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
  SET( CSHARP_BUILD xbuild )
  set( CSHARP_RUN "mono" )
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

INSTALL( FILES ${CURRENT_BUILD_DIR}/CSharpExamples_RunMission.exe DESTINATION CSharp_Examples )
    
# add integration tests
add_test(
  NAME CSharpIntegrationTests_RunMission_help 
  WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR} 
  COMMAND ${CSHARP_RUN} ${CURRENT_BUILD_DIR}/CSharpExamples_RunMission.exe --help 
)
set_tests_properties( CSharpIntegrationTests_RunMission_help PROPERTIES ENVIRONMENT "MALMO_XSD_PATH=$ENV{MALMO_XSD_PATH}" )
add_test( 
  NAME CSharpIntegrationTests_RunMission
  WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}
  COMMAND ${CSHARP_RUN} ${CURRENT_BUILD_DIR}/CSharpExamples_RunMission.exe --test
)
set_tests_properties( CSharpIntegrationTests_RunMission PROPERTIES ENVIRONMENT "MALMO_XSD_PATH=$ENV{MALMO_XSD_PATH}" )

ADD_DEPENDENCIES( CSharpExamples_RunMission MalmoNET )

INSTALL( FILES App.config DESTINATION CSharp_Examples )
INSTALL( FILES Program.cs DESTINATION CSharp_Examples )
INSTALL( FILES Properties/AssemblyInfo.cs DESTINATION CSharp_Examples/Properties/ )
INSTALL( FILES RunMission.csproj.in DESTINATION CSharp_Examples RENAME RunMission.csproj )
