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

set( SOURCES
   AgentHost.cpp
   ArgumentParser.cpp
   ClientConnection.cpp
   ClientInfo.cpp
   ClientPool.cpp
   FindSchemaFile.cpp
   Init.cpp
   Logger.cpp
   MissionInitSpec.cpp
   MissionRecord.cpp
   MissionRecordSpec.cpp
   MissionSpec.cpp
   ParameterSet.cpp
   StringServer.cpp
   TCPClient.cpp
   TCPConnection.cpp
   TCPServer.cpp
   TimestampedReward.cpp
   TimestampedString.cpp
   TimestampedVideoFrame.cpp
   VideoFrameWriter.cpp
   VideoServer.cpp
   WorldState.cpp
   ${CMAKE_CURRENT_BINARY_DIR}/Mission.cpp
   ${CMAKE_CURRENT_BINARY_DIR}/MissionEnded.cpp
   ${CMAKE_CURRENT_BINARY_DIR}/MissionHandlers.cpp
   ${CMAKE_CURRENT_BINARY_DIR}/MissionInit.cpp
   ${CMAKE_CURRENT_BINARY_DIR}/Types.cpp
)

set( HEADERS
   AgentHost.h
   ArgumentParser.h
   ClientConnection.h
   ClientInfo.h
   ClientPool.h
   FindSchemaFile.h
   Init.h
   Logger.h
   MissionInitSpec.h
   MissionRecord.h
   MissionRecordSpec.h
   MissionSpec.h
   ParameterSet.h
   StringServer.h
   Tarball.hpp
   TCPClient.h
   TCPConnection.h
   TCPServer.h
   TimestampedUnsignedCharVector.h
   TimestampedReward.h
   TimestampedString.h
   TimestampedVideoFrame.h
   VideoFrameWriter.h
   VideoServer.h
   WorldState.h
   ${CMAKE_CURRENT_BINARY_DIR}/Mission.h
   ${CMAKE_CURRENT_BINARY_DIR}/MissionEnded.h
   ${CMAKE_CURRENT_BINARY_DIR}/MissionHandlers.h
   ${CMAKE_CURRENT_BINARY_DIR}/MissionInit.h
   ${CMAKE_CURRENT_BINARY_DIR}/Types.h
)

if( MSVC )
  set( SOURCES ${SOURCES} WindowsFrameWriter.cpp )
  set( HEADERS ${HEADERS} WindowsFrameWriter.h )
else()
  set( SOURCES ${SOURCES} PosixFrameWriter.cpp )
  set( HEADERS ${HEADERS} PosixFrameWriter.h )
endif()

if( INCLUDE_TORCH )
  set( SOURCES ${SOURCES} TorchTensorFromPixels.cpp )
  set( HEADERS ${HEADERS} TorchTensorFromPixels.h )
  set( TORCH_LIBRARIES TH )
  add_definitions( -DTORCH )
else()
  set( TORCH_LIBRARIES "" )
endif()

if ( INCLUDE_ALE )
  set( SOURCES ${SOURCES} ALEAgentHost.cpp )
  set( HEADERS ${HEADERS} ALEAgentHost.h )
endif()

add_definitions( -DMALMO_VERSION=${MALMO_VERSION} )

add_custom_command(   # Run CodeSynthesis on the .xsd files to generate C++ sources we can use
    OUTPUT Mission.h Mission.cpp MissionHandlers.h MissionHandlers.cpp MissionEnded.h MissionEnded.cpp MissionInit.h MissionInit.cpp Types.h Types.cpp
    COMMAND ${XSD_EXECUTABLE} cxx-tree --generate-polymorphic --namespace-map http://ProjectMalmo.microsoft.com=malmo::schemas 
    --root-element Mission --root-element MissionInit --root-element MissionEnded --root-element Reward
    --generate-serialization --hxx-suffix .h --cxx-suffix .cpp #--std c++11
    ${CMAKE_SOURCE_DIR}/Schemas/Mission.xsd ${CMAKE_SOURCE_DIR}/Schemas/MissionEnded.xsd  ${CMAKE_SOURCE_DIR}/Schemas/MissionHandlers.xsd  
    ${CMAKE_SOURCE_DIR}/Schemas/MissionInit.xsd ${CMAKE_SOURCE_DIR}/Schemas/Types.xsd
    DEPENDS ${CMAKE_SOURCE_DIR}/Schemas/Mission.xsd ${CMAKE_SOURCE_DIR}/Schemas/MissionEnded.xsd  ${CMAKE_SOURCE_DIR}/Schemas/MissionHandlers.xsd
    ${CMAKE_SOURCE_DIR}/Schemas/MissionInit.xsd ${CMAKE_SOURCE_DIR}/Schemas/Types.xsd
    COMMENT "Generating C++ from XSD files..."
)

set( MALMO_INCLUDE_FOLDERS 
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${Boost_INCLUDE_DIR} 
    ${XSD_INCLUDE_DIRS} 
    ${CMAKE_CURRENT_BINARY_DIR}
    ${ZLIB_INCLUDE_DIRS}
)
if ( INCLUDE_ALE )
  set( MALMO_INCLUDE_FOLDERS ${MALMO_INCLUDE_FOLDERS} ${ALE_INCLUDE_DIRS} )
endif()

include_directories( ${MALMO_INCLUDE_FOLDERS} )

add_library( Malmo STATIC ${SOURCES} ${HEADERS} )

set( MALMO_LINK_LIBRARIES 
  ${Boost_LIBRARIES} 
  ${XSD_LIBRARIES}
  ${ZLIB_LIBRARIES}
)
if( INCLUDE_ALE )
  set( MALMO_LINK_LIBRARIES ${MALMO_LINK_LIBRARIES} ${ALE_LIBRARIES} )
endif()
if( INCLUDE_PYTHON )
  set( MALMO_LINK_LIBRARIES ${MALMO_LINK_LIBRARIES} ${PYTHON_LIBRARIES} )
endif()
if( INCLUDE_TORCH )
  set( MALMO_LINK_LIBRARIES ${MALMO_LINK_LIBRARIES} ${TORCH_LIBRARIES} )
endif()
if( UNIX AND NOT APPLE )
  set( MALMO_LINK_LIBRARIES ${MALMO_LINK_LIBRARIES} rt )
endif()
if( NOT WIN32 )
  set( MALMO_LINK_LIBRARIES ${MALMO_LINK_LIBRARIES} ${CMAKE_THREAD_LIBS_INIT} )
endif()

target_link_libraries( Malmo ${MALMO_LINK_LIBRARIES} )

# -------------- Install the library in Cpp_Examples --------------

install( FILES ${HEADERS} DESTINATION Cpp_Examples/include )
install( TARGETS Malmo DESTINATION Cpp_Examples/lib )
install( FILES ${CMAKE_SOURCE_DIR}/cmake/FindXsd.cmake DESTINATION Cpp_Examples/cmake )

# -------------------- Walk the subdirectories --------------------
if( INCLUDE_CSHARP )
  add_subdirectory( CSharpWrapper )
endif()
if( INCLUDE_JAVA )
  add_subdirectory( JavaWrapper )
endif()
if( INCLUDE_PYTHON )
  add_subdirectory( PythonWrapper )
endif()
IF( INCLUDE_LUA )
  add_subdirectory( LuaWrapper )
ENDIF()
