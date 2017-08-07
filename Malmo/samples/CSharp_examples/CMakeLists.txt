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

set( CSHARP_SAMPLES
    RunMission
    tutorial_1
    tutorial_2
    tutorial_3
    tutorial_4
    tutorial_4_solved
    tutorial_5
    #tutorial_5_solved
    tutorial_7
    tutorial_8
    animation_test
    default_world_test
    hit_test
)

foreach ( CMAKE_SAMPLE_NAME IN LISTS CSHARP_SAMPLES )
    configure_file( ${CMAKE_CURRENT_SOURCE_DIR}/template.csproj.pre ${CMAKE_CURRENT_SOURCE_DIR}/${CMAKE_SAMPLE_NAME}.csproj )
    configure_file( ${CMAKE_CURRENT_SOURCE_DIR}/template.csproj.in ${CMAKE_CURRENT_SOURCE_DIR}/${CMAKE_SAMPLE_NAME}.csproj.in )
    configure_file( ${CMAKE_CURRENT_SOURCE_DIR}/Properties/AssemblyInfo.cs ${CMAKE_CURRENT_SOURCE_DIR}/Properties/${CMAKE_SAMPLE_NAME}_AssemblyInfo.cs )
endforeach ( CMAKE_SAMPLE_NAME )

if( WIN32 )
  SET( CSHARP_BUILD ${CMAKE_VS_MSBUILD_COMMAND} )
  set( CSHARP_RUN "" )
  set( CURRENT_BUILD_DIR ${CMAKE_CURRENT_BINARY_DIR}/$<CONFIG> )
  foreach ( CMAKE_SAMPLE_NAME IN LISTS CSHARP_SAMPLES )
      ADD_CUSTOM_COMMAND(
        OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/$(Configuration)/CSharpExamples_${CMAKE_SAMPLE_NAME}.exe
        COMMAND ${CSHARP_BUILD}
        ARGS ${CMAKE_CURRENT_SOURCE_DIR}/${CMAKE_SAMPLE_NAME}.csproj /p:Configuration=$<CONFIG> /p:Platform=${CSHARP_PLATFORM} /p:OutputPath=${CURRENT_BUILD_DIR} /t:Clean,Build
        DEPENDS Malmo MalmoNET Newtonsoft.Json App.config ${CMAKE_SAMPLE_NAME}.cs
      )
      add_custom_target( 
        CSharpExamples_${CMAKE_SAMPLE_NAME} ALL 
        DEPENDS ${CMAKE_CURRENT_BINARY_DIR}/$(Configuration)/CSharpExamples_${CMAKE_SAMPLE_NAME}.exe
        SOURCES App.config ${CMAKE_SAMPLE_NAME}.cs
      )
  endforeach ( CMAKE_SAMPLE_NAME )
else()
  foreach ( CMAKE_SAMPLE_NAME IN LISTS CSHARP_SAMPLES )
      SET( CSHARP_BUILD xbuild )
      set( CSHARP_RUN "mono" )
      set( CURRENT_BUILD_DIR ${CMAKE_CURRENT_BINARY_DIR} )
      ADD_CUSTOM_COMMAND(
        OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/CSharpExamples_${CMAKE_SAMPLE_NAME}.exe
        COMMAND ${CSHARP_BUILD}
        ARGS ${CMAKE_CURRENT_SOURCE_DIR}/${CMAKE_SAMPLE_NAME}.csproj /p:Platform=${CSHARP_PLATFORM} /p:OutputPath=${CMAKE_CURRENT_BINARY_DIR} /t:clean /t:build /p:TargetFrameworkVersion=v4.5
        DEPENDS Malmo MalmoNET Newtonsoft.Json App.config ${CMAKE_SAMPLE_NAME}
      )
      add_custom_target(
        CSharpExamples_${CMAKE_SAMPLE_NAME} ALL 
        DEPENDS ${CURRENT_BUILD_DIR}/CSharpExamples_${CMAKE_SAMPLE_NAME}.exe
        SOURCES App.config ${CMAKE_SAMPLE_NAME}.cs
      )
  endforeach ( CMAKE_SAMPLE_NAME )
endif()

foreach ( CMAKE_SAMPLE_NAME IN LISTS CSHARP_SAMPLES )
    INSTALL( FILES ${CURRENT_BUILD_DIR}/CSharpExamples_${CMAKE_SAMPLE_NAME}.exe DESTINATION CSharp_Examples )
    INSTALL( FILES ${CMAKE_SAMPLE_NAME}.cs DESTINATION CSharp_Examples )
    INSTALL( FILES ${CMAKE_SAMPLE_NAME}.csproj.in DESTINATION CSharp_Examples RENAME ${CMAKE_SAMPLE_NAME}.csproj )
    INSTALL( FILES Properties/${CMAKE_SAMPLE_NAME}_AssemblyInfo.cs DESTINATION CSharp_Examples/Properties/ )
endforeach ( CMAKE_SAMPLE_NAME )

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
