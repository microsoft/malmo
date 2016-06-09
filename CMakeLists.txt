# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

cmake_minimum_required( VERSION 2.8 )

project( Malmo )

# -------------------- Options --------------------------------

set( MALMO_VERSION_MAJOR 0)
set( MALMO_VERSION_MINOR 11)
set( MALMO_VERSION_REVISION 2)
set( MALMO_VERSION ${MALMO_VERSION_MAJOR}.${MALMO_VERSION_MINOR}.${MALMO_VERSION_REVISION} )
# N.B. Check that this version number matches the one in Minecraft/src/main/java/com/microsoft/Malmo/MalmoMod.java

set( WARNINGS_AS_ERRORS OFF )
set( BUILD_MOD           ON  CACHE BOOL "Specifies whether to build the Malmo Minecraft Mod" )
set( BUILD_DOCUMENTATION ON  CACHE BOOL "Specifies whether to build the documentation for the API and XML" )
set( INCLUDE_ALE         OFF CACHE BOOL "Specifies whether to build Malmo with bindings to the Atari Learning Environment" )
set( INCLUDE_CSHARP      ON  CACHE BOOL "Specifies whether to build Malmo with C# bindings" )
set( INCLUDE_JAVA        ON  CACHE BOOL "Specifies whether to build Malmo with Java bindings" )
set( INCLUDE_LUA         ON  CACHE BOOL "Specifies whether to build Malmo with Lua bindings" )
set( INCLUDE_PYTHON      ON  CACHE BOOL "Specifies whether to build Malmo with Python bindings" )
set( INCLUDE_TORCH       ON  CACHE BOOL "Specifies whether to build Malmo with Torch bindings" )

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
if( WIN32 )
  set( Boost_USE_STATIC_LIBS ON )
  find_package( Boost COMPONENTS chrono date_time filesystem iostreams program_options python regex system thread zlib REQUIRED )
else()
  find_package( Boost COMPONENTS chrono date_time filesystem iostreams program_options python regex system thread REQUIRED )
endif()

# suggested install paths - see readme.md
if( WIN32)
  set( XSD_ROOT "C:/Program Files (x86)/CodeSynthesis XSD 4.0" )
  set( INCLUDE_LUA OFF )   # We don't currently support Lua on Windows
  set( INCLUDE_TORCH OFF ) # Torch is not currently available on Windows
elseif( APPLE )
  set( CMAKE_MACOSX_RPATH ON )
  set( ALE_ROOT_DIR ~/ALE )
  set( INCLUDE_LUA OFF )   # We don't currently support Lua on MacOSX
  set( INCLUDE_TORCH OFF ) # Consequence of lack of Lua on MacOSX
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

if( INCLUDE_JAVA )
  find_package( JNI REQUIRED) 
endif()

if( INCLUDE_PYTHON )
  find_package( PythonInterp REQUIRED )
  find_package( PythonLibs REQUIRED )
endif()

find_package( SWIG REQUIRED )
find_package( Xsd REQUIRED )

if( INCLUDE_LUA )
  set( LUABIND_ROOT_DIR ~/rpavlik-luabind/build/src )
  find_package( Luabind REQUIRED )
else()
  set( INCLUDE_TORCH OFF ) # (can't build for Torch without Lua)
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
    set( INCLUDE_TORCH OFF )
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
    set( INCLUDE_ALE OFF )
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
  endif()  
endif()

if( APPLE )
  add_definitions( -DAPPLE )
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

set( SCHEMA_FILES
  ${CMAKE_SOURCE_DIR}/Schemas/Mission.xsd
  ${CMAKE_SOURCE_DIR}/Schemas/MissionEnded.xsd
  ${CMAKE_SOURCE_DIR}/Schemas/MissionHandlers.xsd
  ${CMAKE_SOURCE_DIR}/Schemas/MissionInit.xsd
  ${CMAKE_SOURCE_DIR}/Schemas/Types.xsd
)

add_subdirectory( Malmo )
if( BUILD_MOD )
  add_subdirectory( Minecraft )
endif()
if( BUILD_DOCUMENTATION )
  add_subdirectory( doc )
  add_subdirectory( Schemas )
endif()
add_subdirectory( sample_missions )
if( INCLUDE_ALE )
  add_subdirectory( ALE_ROMS )
endif()

# -------------------- Package ------------------------------------
if( INCLUDE_ALE )
  set( SYSTEM_NAME "${SYSTEM_NAME}_withALE" )
endif()
set( CPACK_SYSTEM_NAME ${SYSTEM_NAME} )
set( CPACK_PACKAGE_VERSION "${MALMO_VERSION}" )
set( CPACK_GENERATOR "ZIP" )
set( CPACK_SOURCE_GENERATOR "ZIP" )
include( CPack )
