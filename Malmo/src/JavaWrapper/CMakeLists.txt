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

include( UseJava )
include( UseSWIG )
include_directories( ${JNI_INCLUDE_DIRS} )

set( CMAKE_SWIG_OUTDIR ${CMAKE_CURRENT_BINARY_DIR} )

# Build the SWIG C++ classes into a .so/.dll module for dynamic loading
set_source_files_properties( MalmoJava.i PROPERTIES CPLUSPLUS 1 )
if( ALE_FOUND )
  set_source_files_properties( MalmoJava.i PROPERTIES SWIG_FLAGS "-DWRAP_ALE" )
endif()

swig_add_module(
  MalmoJava
  java
  MalmoJava.i
)
swig_link_libraries( MalmoJava Malmo )
install( TARGETS MalmoJava DESTINATION Java_Examples )

# build the SWIG Java files into a .jar
set( SWIG_JAVA_SOURCES
  ${CMAKE_CURRENT_BINARY_DIR}/AgentHost.java
  ${CMAKE_CURRENT_BINARY_DIR}/MalmoJava.java
  ${CMAKE_CURRENT_BINARY_DIR}/MalmoJavaJNI.java
  ${CMAKE_CURRENT_BINARY_DIR}/ArgumentParser.java
  ${CMAKE_CURRENT_BINARY_DIR}/ByteVector.java
  ${CMAKE_CURRENT_BINARY_DIR}/ClientInfo.java
  ${CMAKE_CURRENT_BINARY_DIR}/ClientPool.java
  ${CMAKE_CURRENT_BINARY_DIR}/MissionRecordSpec.java
  ${CMAKE_CURRENT_BINARY_DIR}/MissionSpec.java
  ${CMAKE_CURRENT_BINARY_DIR}/ParameterSet.java
  ${CMAKE_CURRENT_BINARY_DIR}/TimestampedReward.java
  ${CMAKE_CURRENT_BINARY_DIR}/TimestampedRewardVector.java
  ${CMAKE_CURRENT_BINARY_DIR}/TimestampedString.java
  ${CMAKE_CURRENT_BINARY_DIR}/TimestampedStringVector.java
  ${CMAKE_CURRENT_BINARY_DIR}/TimestampedUnsignedCharVector.java
  ${CMAKE_CURRENT_BINARY_DIR}/TimestampedVideoFrame.java
  ${CMAKE_CURRENT_BINARY_DIR}/TimestampedVideoFrameVector.java
  ${CMAKE_CURRENT_BINARY_DIR}/WorldState.java
)
if ( ALE_FOUND )
    set( SWIG_JAVA_SOURCES ${SWIG_JAVA_SOURCES} ${CMAKE_CURRENT_BINARY_DIR}/ALEAgentHost.java )
endif()

add_jar( 
    MalmoJavaJar 
    SOURCES ${SWIG_JAVA_SOURCES} 
)
add_dependencies( MalmoJavaJar MalmoJava )
install_jar( MalmoJavaJar Java_Examples )
