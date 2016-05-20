# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

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
install( FILES ${SCHEMA_FILES} DESTINATION Java_Examples )

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
  ${CMAKE_CURRENT_BINARY_DIR}/TimestampedFloat.java
  ${CMAKE_CURRENT_BINARY_DIR}/TimestampedFloatVector.java
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
