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

cmake_minimum_required( VERSION 3.8 )

project( Malmo )

# -------------------- Options --------------------------------

file( STRINGS "VERSION" MALMO_VERSION_FILE )
string( REPLACE "." ";" MALMO_VERSION_LIST ${MALMO_VERSION_FILE} )
list( GET MALMO_VERSION_LIST 0 MALMO_VERSION_MAJOR )
list( GET MALMO_VERSION_LIST 1 MALMO_VERSION_MINOR )
list( GET MALMO_VERSION_LIST 2 MALMO_VERSION_REVISION )
set( MALMO_VERSION ${MALMO_VERSION_MAJOR}.${MALMO_VERSION_MINOR}.${MALMO_VERSION_REVISION} )

message("Configure MALMO_VERSION at ${MALMO_VERSION}")

# N.B. Check that this version number matches the ones in the schemas.

set( BUILD_MOD_DESC           "Specifies whether to build the Malmo Minecraft Mod" )
set( BUILD_DOCUMENTATION_DESC "Specifies whether to build the documentation for the API and XML" )
set( INCLUDE_ALE_DESC         "Specifies whether to build Malmo with bindings to the Arcade Learning Environment" )
set( INCLUDE_CSHARP_DESC      "Specifies whether to build Malmo with C# bindings" )
set( INCLUDE_JAVA_DESC        "Specifies whether to build Malmo with Java bindings" )
set( INCLUDE_PYTHON_DESC      "Specifies whether to build Malmo with Python bindings" )
set( STATIC_BOOST_DESC        "Specifies whether to link Malmo statically against Boost" )
set( BUILD_MOD           ON  CACHE BOOL ${BUILD_MOD_DESC} )
set( BUILD_DOCUMENTATION ON  CACHE BOOL ${BUILD_DOCUMENTATION_DESC} )
set( INCLUDE_ALE         OFF CACHE BOOL ${INCLUDE_ALE_DESC} )
set( INCLUDE_CSHARP      ON  CACHE BOOL ${INCLUDE_CSHARP_DESC} )
set( INCLUDE_JAVA        ON  CACHE BOOL ${INCLUDE_JAVA_DESC} )
set( INCLUDE_PYTHON      ON  CACHE BOOL ${INCLUDE_PYTHON_DESC} )
set( STATIC_BOOST        ON CACHE BOOL ${STATIC_BOOST_DESC} )

if( INCLUDE_PYTHON )
  set( USE_PYTHON_VERSIONS_DESC "Specifies which version of Python to build Malmo with Python bindings" )
  set( USE_PYTHON_VERSIONS 3.6 CACHE STRING ${USE_PYTHON_VERSIONS_DESC} )
  set( BOOST_PYTHON_NAME_DESC "Specifies which Boost Python module to build Malmo with" )
  set( BOOST_PYTHON_NAME "python3" CACHE STRING ${BOOST_PYTHON_NAME_DESC} )
endif()

set( WARNINGS_AS_ERRORS OFF )

# ------------------- Detect the system -----------------------

if( APPLE )
  set( SYSTEM_NAME "Mac" )
elseif( UNIX )
  set( SYSTEM_NAME "Linux" )
  execute_process(COMMAND lsb_release -is OUTPUT_VARIABLE LSB_ID)
  execute_process(COMMAND lsb_release -rs OUTPUT_VARIABLE LSB_RELEASE)
  if( LSB_ID AND LSB_RELEASE )
    string(STRIP "${LSB_ID}" LSB_ID)
    string(STRIP "${LSB_RELEASE}" LSB_RELEASE)
    set( SYSTEM_NAME "${SYSTEM_NAME}-${LSB_ID}-${LSB_RELEASE}")
  elseif( EXISTS "/etc/debian_version")
    file( READ /etc/debian_version DEBIAN_VERSION )
    set( SYSTEM_NAME "${SYSTEM_NAME}-${DEBIAN_VERSION}")
  elseif( EXISTS "/etc/os-release")
    execute_process(COMMAND "sed" "-ne" "s/^ID=\"\\?\\([a-z]\\+\\)\"\\?$/\\1/p" "/etc/os-release" OUTPUT_VARIABLE OS_RELEASE_ID OUTPUT_STRIP_TRAILING_WHITESPACE)
    execute_process(COMMAND "sed" "-ne" "s/^VERSION_ID=\"\\?\\([0-9\\.]\\+\\)\"\\?$/\\1/p" "/etc/os-release" OUTPUT_VARIABLE OS_RELEASE_VERSION_ID OUTPUT_STRIP_TRAILING_WHITESPACE)
    set( SYSTEM_NAME "${SYSTEM_NAME}-${OS_RELEASE_ID}-${OS_RELEASE_VERSION_ID}")
  elseif( EXISTS "/etc/redhat-release")
    set( SYSTEM_NAME "${SYSTEM_NAME}-Redhat")
  endif()
elseif( WIN32 )
  set( SYSTEM_NAME "Windows" )
endif()
if( CMAKE_SIZEOF_VOID_P EQUAL 8 )
  set( SYSTEM_NAME "${SYSTEM_NAME}-64bit" )
else()
  set( SYSTEM_NAME "${SYSTEM_NAME}-32bit" )
endif()

# -------------------- Find packages --------------------------

if ( WIN32 )
    enable_language( CSharp )
else()
    set( INCLUDE_CSHARP OFF )
endif()

set( CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} ${CMAKE_CURRENT_SOURCE_DIR}/cmake )

SET(Boost_ADDITIONAL_VERSIONS "1.65" "1.65.0")
SET(Boost_ADDITIONAL_VERSIONS "1.66" "1.66.0")
SET(Boost_ADDITIONAL_VERSIONS "1.67" "1.67.0")

SET(BOOST_PYTHON_MODULE_NAME "python")
if ( USE_PYTHON_VERSIONS VERSION_GREATER 3 )
  SET( BOOST_PYTHON_MODULE_NAME ${BOOST_PYTHON_NAME} )
endif()

if( WIN32 )
  SET(Boost_USE_STATIC_LIBS ON)
  find_package( Boost COMPONENTS chrono date_time filesystem iostreams program_options ${BOOST_PYTHON_MODULE_NAME} regex system thread REQUIRED )
  add_definitions(-DBOOST_ALL_NO_LIB=1)  # Turn off auto-linking, creates problems when linking boost statically
else()
  SET(Boost_USE_STATIC_LIBS ${STATIC_BOOST})
  find_package( Boost COMPONENTS chrono date_time filesystem iostreams program_options ${BOOST_PYTHON_MODULE_NAME} regex system thread REQUIRED )
  set(CMAKE_THREAD_PREFER_PTHREAD TRUE)
  find_package( Threads REQUIRED )
endif()

# suggested install paths - see readme.md
if( NOT WIN32)
  set( ALE_ROOT_DIR ~/ALE )
endif()

if( INCLUDE_CSHARP )
  # Select the .NET architecture
  if( CMAKE_SIZEOF_VOID_P EQUAL 8 )
    set( CSHARP_PLATFORM "x64" CACHE STRING "C# target platform: x86, x64, anycpu, or itanium")
  else()
    set( CSHARP_PLATFORM "x86" CACHE STRING "C# target platform: x86, x64, anycpu, or itanium")
  endif()
endif()

if( BUILD_DOCUMENTATION )
  find_package( Doxygen REQUIRED )
endif()

find_package( Git REQUIRED )
find_package( ZLIB REQUIRED )

if ( INCLUDE_JAVA OR BUILD_MOD )
   find_package( Java REQUIRED COMPONENTS Runtime Development ) 
endif()

if( INCLUDE_JAVA )
  find_package( JNI REQUIRED) 
endif()

if( INCLUDE_PYTHON )
  set( Python_ADDITIONAL_VERSIONS ${USE_PYTHON_VERSIONS} )
  find_package( PythonInterp ${USE_PYTHON_VERSIONS} REQUIRED )
  find_package( PythonLibs ${USE_PYTHON_VERSIONS} REQUIRED )
endif()

find_package( SWIG REQUIRED )

if( INCLUDE_ALE )
  find_package(ALE QUIET)
  if( NOT ALE_FOUND )
    message( "" )
    message( " ====   WARNING: ====" )
    message( "" )
    message( " ALE was not found! Build will proceed but without the ALE parts included." )
    message( "" )
    set( INCLUDE_ALE OFF CACHE BOOL ${INCLUDE_ALE_DESC} FORCE )
  endif()
endif()

# -------------------- Build settings -----------------------------

# use C++11
if (CMAKE_VERSION VERSION_LESS "3.1")
  if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
    set (CMAKE_CXX_FLAGS "--std=gnu++11 ${CMAKE_CXX_FLAGS}")
  endif ()
else ()
  set (CMAKE_CXX_STANDARD 11)
endif ()
 
if( MSVC )
  set( MALMO_PYTHON_DIR ${CMAKE_BINARY_DIR}/Malmo/src/PythonWrapper/$<CONFIG> )
  set( MALMO_CSHARP_DIR ${CMAKE_BINARY_DIR}/Malmo/src/CSharpWrapper/$<CONFIG> )
  if( WARNINGS_AS_ERRORS )
    set( CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /WX" )
  endif()
  # set the Windows target version to WinXP or later
  set( CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -D_WIN32_WINNT=0x0501" )
  set( CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -D_CRT_SECURE_NO_WARNINGS -D_SCL_SECURE_NO_WARNINGS" )
else()
  set( MALMO_PYTHON_DIR ${CMAKE_BINARY_DIR}/Malmo/src/PythonWrapper )
  set( MALMO_CSHARP_DIR ${CMAKE_BINARY_DIR}/Malmo/src/CSharpWrapper )
  set( CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fPIC" )
  if( WARNINGS_AS_ERRORS )
    set( CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Werror" )
  else()
    set( CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-deprecated-declarations" ) # get a lot of warnings from Boost about auto_ptr
  endif()  
endif()

if( APPLE )
  add_definitions( -DAPPLE )
  set( CMAKE_MACOSX_RPATH ON )
endif()

# -------------------- Testing ------------------------------------

set( BUILDNAME ${CMAKE_SYSTEM} ) # including version numbers
if( MSVC )
  set( BUILDNAME "${BUILDNAME}_MSVC" )
elseif( CMAKE_COMPILER_IS_GNUCXX )
  set( BUILDNAME "${BUILDNAME}_gcc" )
else()
  set( BUILDNAME "${BUILDNAME}_${CMAKE_CXX_COMPILER}" )
endif()

if( CMAKE_SIZEOF_VOID_P EQUAL 8 )
  set( BUILDNAME "${BUILDNAME}_64bit" )
else()
  set( BUILDNAME "${BUILDNAME}_32bit" )
endif()

include( CTest )

set( CMAKE_INSTALL_PREFIX "install" ) # useful for testing the sample scripts

# -------------------- Walk the subdirectories --------------------

add_subdirectory( Malmo )
if( BUILD_MOD )
  add_subdirectory( Minecraft )
endif()
if( BUILD_DOCUMENTATION )
  add_subdirectory( doc )
endif()
add_subdirectory( Schemas )
add_subdirectory( scripts )
add_subdirectory( sample_missions )
if( INCLUDE_ALE )
  add_subdirectory( ALE_ROMS )
endif()

# Save a small properties file, so we can easily tell what version of python this was built against, etc.
configure_file( ${CMAKE_CURRENT_SOURCE_DIR}/malmo.properties malmo.properties )
INSTALL( FILES ${CMAKE_BINARY_DIR}/malmo.properties DESTINATION .)

# -------------------- Package ------------------------------------
if( INCLUDE_ALE )
  set( SYSTEM_NAME "${SYSTEM_NAME}_withALE" )
endif()
if ( STATIC_BOOST )
  set( SYSTEM_NAME "${SYSTEM_NAME}_withBoost" )
endif()
set( SYSTEM_NAME "${SYSTEM_NAME}_Python${USE_PYTHON_VERSIONS}" )
set( CPACK_SYSTEM_NAME ${SYSTEM_NAME} )
set( CPACK_PACKAGE_VERSION "${MALMO_VERSION}" )
set( CPACK_GENERATOR "ZIP" )
set( CPACK_SOURCE_GENERATOR "ZIP" )
include( CPack )
