# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

include( ${SWIG_USE_FILE} )
include( ${CSHARP_USE_FILE} )

execute_process(
    COMMAND ${GIT_EXECUTABLE} rev-parse --abbrev-ref HEAD
    WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}
    OUTPUT_VARIABLE GIT_BRANCH
    OUTPUT_STRIP_TRAILING_WHITESPACE
)

# Get the latest abbreviated commit hash of the working branch
execute_process(
    COMMAND ${GIT_EXECUTABLE} log -1 --format=%h
    WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}
    OUTPUT_VARIABLE GIT_COMMIT_HASH
    OUTPUT_STRIP_TRAILING_WHITESPACE
)

set_source_files_properties ( MalmoNETNative.i PROPERTIES CPLUSPLUS ON )

# CSharp version requirements: http://msdn.microsoft.com/en-us/library/system.reflection.assemblyversionattribute.aspx
# major.minor[.build[.revision]] where all components are 16-bit unsigned integers

set(MALMONET_VERSION_CSHARP_AssemblyVersion "${MALMO_VERSION_MAJOR}.${MALMO_VERSION_MINOR}.${GIT_BRANCH}.${GIT_COMMIT_HASH}")

  # Make sure the nested directory structure exists
set(CSHARP_SOURCE_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/swig CACHE INTERNAL "")
file(MAKE_DIRECTORY ${CSHARP_SOURCE_DIRECTORY})

  # Create swig target
set(CMAKE_SWIG_OUTDIR ${CSHARP_SOURCE_DIRECTORY})

set(CMAKE_SWIG_FLAGS -I${CMAKE_CURRENT_SOURCE_DIR} -namespace \"Microsoft.Research.Malmo\" ${CMAKE_SWIG_GLOBAL_FLAGS} ${CMAKE_SWIG_FLAGS})
if( ALE_FOUND )
  set (CMAKE_SWIG_FLAGS ${CMAKE_SWIG_FLAGS} "-DWRAP_ALE" )
endif()

SET_SOURCE_FILES_PROPERTIES(MalmoNETNative.i PROPERTIES SWIG_FLAGS "-includeall")
SWIG_ADD_MODULE(MalmoNETNative csharp MalmoNETNative.i )
INCLUDE_DIRECTORIES(${CMAKE_CURRENT_SOURCE_DIR})
SWIG_LINK_LIBRARIES(${SWIG_MODULE_MalmoNETNative_REAL_NAME} Malmo )

if( UNIX )
  set_target_properties(${SWIG_MODULE_MalmoNETNative_REAL_NAME} PROPERTIES SUFFIX ".so")
  set( MALMO_NET_NATIVE "${SWIG_MODULE_MalmoNETNative_REAL_NAME}.so" CACHE INTERNAL "The MalmoNETNative built library" )
else()
  set_target_properties(${SWIG_MODULE_MalmoNETNative_REAL_NAME} PROPERTIES SUFFIX ".dll")
  set( MALMO_NET_NATIVE "${SWIG_MODULE_MalmoNETNative_REAL_NAME}.dll" CACHE INTERNAL "The MalmoNETNative built library" )
endif()

# Configure AssemblyInfo.cs
configure_file(
    ${CMAKE_CURRENT_SOURCE_DIR}/AssemblyInfo.cs.in
    ${CSHARP_SOURCE_DIRECTORY}/AssemblyInfo.cs
    @ONLY
)

# Add managed wrapper
csharp_add_library(
    MalmoNET
    ${CSHARP_SOURCE_DIRECTORY}/*.cs
)
add_dependencies(MalmoNET ${SWIG_MODULE_MalmoNETNative_REAL_NAME})

install( TARGETS ${SWIG_MODULE_MalmoNETNative_REAL_NAME} DESTINATION CSharp_Examples )
install( TARGETS ${SWIG_MODULE_MalmoNETNative_REAL_NAME} DESTINATION HumanAction )
IF( WIN32 )
	INSTALL(FILES ${CMAKE_CURRENT_BINARY_DIR}/$<CONFIG>/MalmoNET.dll DESTINATION CSharp_Examples )
	INSTALL(FILES ${CMAKE_CURRENT_BINARY_DIR}/$<CONFIG>/MalmoNET.dll DESTINATION HumanAction )
ELSE( UNIX )
	INSTALL(FILES ${CMAKE_CURRENT_BINARY_DIR}/MalmoNET.dll DESTINATION CSharp_Examples )
	INSTALL(FILES ${CMAKE_CURRENT_BINARY_DIR}/MalmoNET.dll DESTINATION HumanAction )
ENDIF( WIN32 )
