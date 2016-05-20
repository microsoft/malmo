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

IF( WIN32 )
    ADD_CUSTOM_COMMAND(
       OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/$(Configuration)/CSharpExamples_RunMission.exe
       COMMAND ${CSHARP_BUILD}
       ARGS ${CMAKE_CURRENT_SOURCE_DIR}/RunMission.csproj /p:Configuration=$<CONFIG> /p:Platform=${CSHARP_PLATFORM} /p:OutputPath=${CMAKE_CURRENT_BINARY_DIR}/$<CONFIG>
    )
    
    ADD_CUSTOM_TARGET(
       CSharpExamples_RunMission ALL
       DEPENDS ${CMAKE_CURRENT_BINARY_DIR}/$(Configuration)/CSharpExamples_RunMission.exe
       SOURCES ${CSHARP_SAMPLE_SOURCES}
    )

    INSTALL( FILES ${CMAKE_CURRENT_BINARY_DIR}/$<CONFIG>/CSharpExamples_RunMission.exe DESTINATION CSharp_Examples )
    
ELSE()
    ADD_CUSTOM_COMMAND(
       OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/CSharpExamples_RunMission.exe
       COMMAND ${CSHARP_BUILD}
       ARGS ${CMAKE_CURRENT_SOURCE_DIR}/RunMission.csproj /p:Platform=${CSHARP_PLATFORM} /p:OutputPath=${CMAKE_CURRENT_BINARY_DIR}
    )
    
    ADD_CUSTOM_TARGET(
       CSharpExamples_RunMission ALL
       DEPENDS ${CMAKE_CURRENT_BINARY_DIR}/CSharpExamples_RunMission.exe
       SOURCES ${CSHARP_SAMPLE_SOURCES}
    )
    
    INSTALL( FILES ${CMAKE_CURRENT_BINARY_DIR}/CSharpExamples_RunMission.exe DESTINATION CSharp_Examples )
    
ENDIF()

ADD_DEPENDENCIES( CSharpExamples_RunMission MalmoNET )

INSTALL( FILES ${SCHEMA_FILES} DESTINATION CSharp_Examples )
INSTALL( FILES App.config DESTINATION CSharp_Examples )
INSTALL( FILES Program.cs DESTINATION CSharp_Examples )
INSTALL( FILES Properties/AssemblyInfo.cs DESTINATION CSharp_Examples/Properties/ )
INSTALL( FILES RunMission.csproj.in DESTINATION CSharp_Examples RENAME RunMission.csproj )
