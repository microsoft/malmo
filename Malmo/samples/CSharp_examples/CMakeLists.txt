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
    ARGS ${CMAKE_CURRENT_SOURCE_DIR}/RunMission.csproj /p:Configuration=$<CONFIG> /p:Platform=${CSHARP_PLATFORM} /p:OutputPath=${CURRENT_BUILD_DIR} /t:Clean,Build
    DEPENDS Malmo MalmoNET ${CSHARP_SAMPLE_SOURCES}
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
    ARGS ${CMAKE_CURRENT_SOURCE_DIR}/RunMission.csproj /p:Platform=${CSHARP_PLATFORM} /p:OutputPath=${CMAKE_CURRENT_BINARY_DIR} /t:clean /t:build /p:TargetFrameworkVersion=v4.5
    DEPENDS Malmo MalmoNET ${CSHARP_SAMPLE_SOURCES}
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

INSTALL( FILES App.config DESTINATION CSharp_Examples )
INSTALL( FILES Program.cs DESTINATION CSharp_Examples )
INSTALL( FILES Properties/AssemblyInfo.cs DESTINATION CSharp_Examples/Properties/ )
INSTALL( FILES RunMission.csproj.in DESTINATION CSharp_Examples RENAME RunMission.csproj )
