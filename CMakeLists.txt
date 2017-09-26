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

cmake_minimum_required( VERSION 2.8 )

project( Malmo )

# -------------------- Options --------------------------------

set( MALMO_VERSION_MAJOR 0)
set( MALMO_VERSION_MINOR 31)
set( MALMO_VERSION_REVISION 0)
set( MALMO_VERSION ${MALMO_VERSION_MAJOR}.${MALMO_VERSION_MINOR}.${MALMO_VERSION_REVISION} )
# N.B. Check that this version number matches the ones in the schemas.

set( BUILD_MOD_DESC           "Specifies whether to build the Malmo Minecraft Mod" )
set( BUILD_DOCUMENTATION_DESC "Specifies whether to build the documentation for the API and XML" )
set( INCLUDE_ALE_DESC         "Specifies whether to build Malmo with bindings to the Arcade Learning Environment" )
set( INCLUDE_CSHARP_DESC      "Specifies whether to build Malmo with C# bindings" )
set( INCLUDE_JAVA_DESC        "Specifies whether to build Malmo with Java bindings" )
set( INCLUDE_LUA_DESC         "Specifies whether to build Malmo with Lua bindings (Linux only)" )
set( INCLUDE_PYTHON_DESC      "Specifies whether to build Malmo with Python bindings" )
set( INCLUDE_TORCH_DESC       "Specifies whether to build Malmo with Torch bindings (Linux only)" )
set( STATIC_BOOST_DESC        "Specifies whether to link Malmo statically against Boost" )
set( BUILD_MOD           ON  CACHE BOOL ${BUILD_MOD_DESC} )
set( BUILD_DOCUMENTATION ON  CACHE BOOL ${BUILD_DOCUMENTATION_DESC} )
set( INCLUDE_ALE         OFF CACHE BOOL ${INCLUDE_ALE_DESC} )
set( INCLUDE_CSHARP      ON  CACHE BOOL ${INCLUDE_CSHARP_DESC} )
set( INCLUDE_JAVA        ON  CACHE BOOL ${INCLUDE_JAVA_DESC} )
set( INCLUDE_PYTHON      ON  CACHE BOOL ${INCLUDE_PYTHON_DESC} )
set( STATIC_BOOST        OFF CACHE BOOL ${STATIC_BOOST_DESC} )

if( INCLUDE_PYTHON )
  set( USE_PYTHON_VERSIONS_DESC "Specifies which versions of Python to build Malmo with Python bindings" )
  set( USE_PYTHON_VERSIONS 2.7 CACHE STRING ${USE_PYTHON_VERSIONS_DESC} )
endif()
if( UNIX AND NOT APPLE )
  set( INCLUDE_LUA       ON  CACHE BOOL ${INCLUDE_LUA_DESC} )
  set( INCLUDE_TORCH     ON  CACHE BOOL ${INCLUDE_TORCH_DESC} )
else()
  # Lua and Torch not currently supported
  set( INCLUDE_LUA       OFF CACHE BOOL ${INCLUDE_LUA_DESC} )
  set( INCLUDE_TORCH     OFF CACHE BOOL ${INCLUDE_TORCH_DESC} )
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
  elseif( EXISTS "/etc/redhat-release")
    set( SYSTEM_NAME "${SYSTEM_NAME}-Redhat")
  endif()
elseif( WIN32)
  set( SYSTEM_NAME "Windows" )
endif()
if( CMAKE_SIZEOF_VOID_P EQUAL 8 )
  set( SYSTEM_NAME "${SYSTEM_NAME}-64bit" )
else()
  set( SYSTEM_NAME "${SYSTEM_NAME}-32bit" )
endif()

# -------------------- Find packages --------------------------
set( CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} ${CMAKE_CURRENT_SOURCE_DIR}/cmake )

SET(Boost_ADDITIONAL_VERSIONS "1.59" "1.59.0")
SET(Boost_ADDITIONAL_VERSIONS "1.60" "1.60.0")
SET(Boost_ADDITIONAL_VERSIONS "1.61" "1.61.0")
SET(Boost_ADDITIONAL_VERSIONS "1.62" "1.62.0")
SET(Boost_ADDITIONAL_VERSIONS "1.63" "1.63.0")
if( WIN32 )
  SET(Boost_USE_STATIC_LIBS ON)
  find_package( Boost COMPONENTS chrono date_time filesystem iostreams program_options python regex system thread zlib REQUIRED )
else()
  find_package( Boost COMPONENTS chrono date_time filesystem iostreams program_options python regex system thread REQUIRED )
  set(CMAKE_THREAD_PREFER_PTHREAD TRUE)
  find_package( Threads REQUIRED )
  SET(Boost_USE_STATIC_LIBS ${STATIC_BOOST})
endif()

# suggested install paths - see readme.md
if( WIN32)
  set( XSD_ROOT "C:/Program Files (x86)/CodeSynthesis XSD 4.0" )
else()
  set( ALE_ROOT_DIR ~/ALE )
endif()

if( INCLUDE_CSHARP )
  if( CMAKE_SIZEOF_VOID_P EQUAL 8 )
    set( CSHARP_PLATFORM "x64" CACHE STRING "C# target platform: x86, x64, anycpu, or itanium")
  else()
    set( CSHARP_PLATFORM "x86" CACHE STRING "C# target platform: x86, x64, anycpu, or itanium")
  endif()
  find_package( CSharp REQUIRED )
endif()

if( BUILD_DOCUMENTATION )
  find_package( Doxygen REQUIRED )
endif()

find_package( Git REQUIRED )
find_package( Java REQUIRED COMPONENTS Runtime Development ) 
find_package( ZLIB REQUIRED )

if( INCLUDE_JAVA )
  find_package( JNI REQUIRED) 
endif()

if( INCLUDE_PYTHON )
  set( Python_ADDITIONAL_VERSIONS ${USE_PYTHON_VERSIONS} )
  find_package( PythonInterp ${USE_PYTHON_VERSIONS} REQUIRED )
  find_package( PythonLibs ${USE_PYTHON_VERSIONS} REQUIRED )
endif()

find_package( SWIG REQUIRED )
find_package( Xsd REQUIRED )

if( INCLUDE_LUA )
  set( LUABIND_ROOT_DIR ~/rpavlik-luabind/build/src )
  find_package( Luabind REQUIRED )
else()
  set( INCLUDE_TORCH OFF CACHE BOOL ${INCLUDE_TORCH_DESC} FORCE ) # (can't build for Torch without Lua)
endif()

if( INCLUDE_TORCH )
  find_package( Torch QUIET HINTS ~/torch/install/share/cmake/torch ) 
  if( NOT Torch_FOUND )
    # for now Torch is only available on Ubuntu and a few other platforms, so we warn but continue
    message( "" )
    message( " ====   WARNING: ====" )
    message( "" )
    message( " Torch was not found! Build will proceed but without the Torch parts included." )
    message( "" )
    set( INCLUDE_TORCH OFF CACHE BOOL ${INCLUDE_TORCH_DESC} FORCE )
  endif()
endif()

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
  set( MALMO_LUA_DIR ${CMAKE_BINARY_DIR}/Malmo/src/LuaWrapper/$<CONFIG> )
  set( MALMO_CSHARP_DIR ${CMAKE_BINARY_DIR}/Malmo/src/CSharpWrapper/$<CONFIG> )
  if( WARNINGS_AS_ERRORS )
    set( CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /WX" )
  endif()
  # set the Windows target version to WinXP or later
  set( CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -D_WIN32_WINNT=0x0501" )
  set( CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -D_CRT_SECURE_NO_WARNINGS -D_SCL_SECURE_NO_WARNINGS" )
else()
  set( MALMO_PYTHON_DIR ${CMAKE_BINARY_DIR}/Malmo/src/PythonWrapper )
  set( MALMO_LUA_DIR ${CMAKE_BINARY_DIR}/Malmo/src/LuaWrapper )
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
