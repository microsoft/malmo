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

set( JAVA_OUTDIR ${CMAKE_CURRENT_BINARY_DIR}/com/microsoft/msr/malmo )
set( CMAKE_SWIG_OUTDIR ${JAVA_OUTDIR} )
set( PACKAGE_NAME com.microsoft.msr.malmo )
# Build the SWIG C++ classes into a .so/.dll module for dynamic loading
set_source_files_properties( MalmoJava.i PROPERTIES CPLUSPLUS 1 )
if( ALE_FOUND )
  set_source_files_properties( MalmoJava.i PROPERTIES SWIG_FLAGS "-DWRAP_ALE" )
endif()
set( CMAKE_SWIG_FLAGS -package ${PACKAGE_NAME} )

SWIG_ADD_LIBRARY( MalmoJava
                  LANGUAGE java
                  TYPE MODULE
                  SOURCES MalmoJava.i )

swig_link_libraries( MalmoJava Malmo )
install( TARGETS MalmoJava DESTINATION Java_Examples )

# build the SWIG Java files into a .jar
set( SWIG_JAVA_SOURCES
  ${JAVA_OUTDIR}/AgentHost.java
  ${JAVA_OUTDIR}/MalmoJava.java
  ${JAVA_OUTDIR}/MalmoJavaJNI.java
  ${JAVA_OUTDIR}/ArgumentParser.java
  ${JAVA_OUTDIR}/ByteVector.java
  ${JAVA_OUTDIR}/ClientInfo.java
  ${JAVA_OUTDIR}/ClientPool.java
  ${JAVA_OUTDIR}/MissionRecordSpec.java
  ${JAVA_OUTDIR}/MissionSpec.java
  ${JAVA_OUTDIR}/ParameterSet.java
  ${JAVA_OUTDIR}/TimestampedReward.java
  ${JAVA_OUTDIR}/TimestampedRewardVector.java
  ${JAVA_OUTDIR}/TimestampedString.java
  ${JAVA_OUTDIR}/TimestampedStringVector.java
  ${JAVA_OUTDIR}/TimestampedUnsignedCharVector.java
  ${JAVA_OUTDIR}/TimestampedVideoFrame.java
  ${JAVA_OUTDIR}/TimestampedVideoFrameVector.java
  ${JAVA_OUTDIR}/WorldState.java
)
if ( ALE_FOUND )
    set( SWIG_JAVA_SOURCES ${SWIG_JAVA_SOURCES} ${JAVA_OUTDIR}/ALEAgentHost.java )
endif()

add_jar( 
    MalmoJavaJar 
    SOURCES ${SWIG_JAVA_SOURCES} 
)
add_dependencies( MalmoJavaJar MalmoJava )
install_jar( MalmoJavaJar Java_Examples )
