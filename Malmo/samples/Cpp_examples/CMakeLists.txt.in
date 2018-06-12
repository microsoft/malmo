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

set( SOURCES
  run_mission.cpp
)

# ------------ Find the libraries we need --------------------------

set( CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} ${CMAKE_SOURCE_DIR}/cmake )

SET(Boost_ADDITIONAL_VERSIONS "1.65" "1.65.0")
SET(Boost_ADDITIONAL_VERSIONS "1.66" "1.66.0")
SET(Boost_ADDITIONAL_VERSIONS "1.67" "1.67.0")

if( MSVC )
  set( Boost_USE_STATIC_LIBS ON )
  find_package( Boost COMPONENTS chrono date_time filesystem iostreams program_options regex system thread zlib REQUIRED )
  set( CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -D_WIN32_WINNT=0x0501" ) # build for WinXP or later
else()
  set( Boost_USE_STATIC_LIBS OFF )
  find_package( Boost COMPONENTS chrono date_time filesystem iostreams program_options regex system thread REQUIRED )
endif()

find_library( Malmo_LIBRARIES Malmo ${CMAKE_SOURCE_DIR}/lib )

# ------------------- Settings --------------------------------------

# use C++11
if (CMAKE_VERSION VERSION_LESS "3.1")
  if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
    set (CMAKE_CXX_FLAGS "--std=gnu++11 ${CMAKE_CXX_FLAGS}")
  endif ()
else ()
  set (CMAKE_CXX_STANDARD 11)
endif ()
  
# ------------ Build the executable ---------------------------------

add_executable( run_mission ${SOURCES} )

include_directories( 
  ${CMAKE_SOURCE_DIR}/include
  ${Boost_INCLUDE_DIR}
)

target_link_libraries( run_mission 
  ${Malmo_LIBRARIES}
  ${Boost_LIBRARIES}
)
