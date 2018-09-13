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
   ErrorCodeSync.cpp
   ClientConnection.cpp
   ClientInfo.cpp
   ClientPool.cpp
   Logger.cpp
   FindSchemaFile.cpp
   RewardXML.cpp
   MissionInitXML.cpp
   MissionEndedXML.cpp
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
   BmpFrameWriter.cpp
   VideoServer.cpp
   WorldState.cpp
)

set( HEADERS
   AgentHost.h
   ArgumentParser.h
   ErrorCodeSync.h
   ClientConnection.h
   ClientInfo.h
   ClientPool.h
   Logger.h
   FindSchemaFile.h
   XMLParseException.h
   RewardXML.h
   MissionInitXML.h
   MissionEndedXML.h
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
   BmpFrameWriter.h
   VideoServer.h
   WorldState.h
)

if( MSVC )
  set( SOURCES ${SOURCES} WindowsFrameWriter.cpp )
  set( HEADERS ${HEADERS} WindowsFrameWriter.h )
else()
  set( SOURCES ${SOURCES} PosixFrameWriter.cpp )
  set( HEADERS ${HEADERS} PosixFrameWriter.h )
endif()

if ( INCLUDE_ALE )
  set( SOURCES ${SOURCES} ALEAgentHost.cpp )
  set( HEADERS ${HEADERS} ALEAgentHost.h )
endif()

add_definitions( -DMALMO_VERSION=${MALMO_VERSION} )

set( MALMO_INCLUDE_FOLDERS 
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${Boost_INCLUDE_DIR} 
    #${CMAKE_CURRENT_BINARY_DIR}
    ${ZLIB_INCLUDE_DIRS}
)
if ( INCLUDE_ALE )
  set( MALMO_INCLUDE_FOLDERS ${MALMO_INCLUDE_FOLDERS} ${ALE_INCLUDE_DIRS} )
endif()

include_directories( ${MALMO_INCLUDE_FOLDERS} )

add_library( Malmo STATIC ${SOURCES} ${HEADERS} )

set( MALMO_LINK_LIBRARIES 
  ${Boost_LIBRARIES} 
  ${ZLIB_LIBRARIES}
)
if( INCLUDE_ALE )
  set( MALMO_LINK_LIBRARIES ${MALMO_LINK_LIBRARIES} ${ALE_LIBRARIES} )
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
