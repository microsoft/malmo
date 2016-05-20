# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

set( SOURCES
   AgentHost.cpp
   ArgumentParser.cpp
   ClientConnection.cpp
   ClientInfo.cpp
   ClientPool.cpp
   MissionInitSpec.cpp
   MissionRecord.cpp
   MissionRecordSpec.cpp
   MissionSpec.cpp
   ParameterSet.cpp
   StringServer.cpp
   TCPClient.cpp
   TCPConnection.cpp
   TCPServer.cpp
   TimestampedFloat.cpp
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
   TimestampedFloat.h
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

if( Torch_FOUND )
  set( SOURCES ${SOURCES} TorchTensorFromPixels.cpp )
  set( HEADERS ${HEADERS} TorchTensorFromPixels.h )
  set( TORCH_LIBRARIES TH )
  add_definitions( -DTORCH )
else()
  set( TORCH_LIBRARIES "" )
endif()

if ( ALE_FOUND )
  set( SOURCES ${SOURCES} ALEAgentHost.cpp )
  set( HEADERS ${HEADERS} ALEAgentHost.h )
endif()

add_definitions( -DMALMO_VERSION=${MALMO_VERSION} )

add_custom_command(   # Run CodeSynthesis on the .xsd files to generate C++ sources we can use
    OUTPUT Mission.h Mission.cpp MissionHandlers.h MissionHandlers.cpp MissionEnded.h MissionEnded.cpp MissionInit.h MissionInit.cpp Types.h Types.cpp
    COMMAND ${XSD_EXECUTABLE} cxx-tree --generate-polymorphic --namespace-map http://ProjectMalmo.microsoft.com=malmo::schemas --root-element Mission --root-element MissionInit --root-element MissionEnded
    --generate-serialization --hxx-suffix .h --cxx-suffix .cpp #--std c++11
    ${CMAKE_SOURCE_DIR}/Schemas/Mission.xsd ${CMAKE_SOURCE_DIR}/Schemas/MissionEnded.xsd  ${CMAKE_SOURCE_DIR}/Schemas/MissionHandlers.xsd  ${CMAKE_SOURCE_DIR}/Schemas/MissionInit.xsd
    ${CMAKE_SOURCE_DIR}/Schemas/Types.xsd
    DEPENDS ${SCHEMA_FILES}
    COMMENT "Generating C++ from XSD files..."
)

set( MALMO_INCLUDE_FOLDERS 
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${Boost_INCLUDE_DIR} 
    ${XSD_INCLUDE_DIRS} 
    ${CMAKE_CURRENT_BINARY_DIR}
)
if ( ALE_FOUND )
  set( MALMO_INCLUDE_FOLDERS ${MALMO_INCLUDE_FOLDERS} ${ALE_INCLUDE_DIRS} )
endif()

include_directories( ${MALMO_INCLUDE_FOLDERS} )

add_library( Malmo STATIC ${SOURCES} ${HEADERS} )

set( MALMO_LINK_LIBRARIES 
    ${Boost_LIBRARIES} 
    ${PYTHON_LIBRARIES}
    ${TORCH_LIBRARIES} 
    ${XSD_LIBRARIES}
)
if ( ALE_FOUND )
  set( MALMO_LINK_LIBRARIES ${MALMO_LINK_LIBRARIES} ${ALE_LIBRARIES} )
endif()
if( UNIX AND NOT APPLE )
  set( MALMO_LINK_LIBRARIES ${MALMO_LINK_LIBRARIES} rt )
endif()

target_link_libraries( Malmo ${MALMO_LINK_LIBRARIES} )

# -------------- Install the library in Cpp_Examples --------------

install( FILES ${HEADERS} DESTINATION Cpp_Examples/include )
install( TARGETS Malmo DESTINATION Cpp_Examples/lib )
install( FILES ${CMAKE_SOURCE_DIR}/cmake/FindXsd.cmake DESTINATION Cpp_Examples/cmake )

# -------------------- Walk the subdirectories --------------------
add_subdirectory( CSharpWrapper )
add_subdirectory( JavaWrapper )
add_subdirectory( PythonWrapper )

IF( UNIX AND NOT APPLE )
  add_subdirectory( LuaWrapper )
ENDIF()
