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

### GOPATH
##########
set(GOPATH_FULL $ENV{GOPATH})
string(REPLACE ":" ";" GOPATH_LIST "${GOPATH_FULL}")
list(GET GOPATH_LIST 0 GOPATH)

### C++ source files
####################
set(CPPMALMO     "${CMAKE_SOURCE_DIR}/Malmo/src")
set(CPPMALMO_BIN "${CMAKE_BINARY_DIR}/Malmo/src")

### Go malmo lib
################
set(GOMALMO_IN  "${CMAKE_SOURCE_DIR}/Malmo/src/GoWrapper")
set(GOMALMO_OUT "${GOPATH}/src/malmo")

### replace CXXFLAGS with correct value
#######################################
configure_file(x_set_cgo_flags.go.in x_set_cgo_flags.go)

### Copy go files
#################
add_custom_target(CopyMalmoGoFiles ALL
    COMMAND ${CMAKE_COMMAND} -E remove_directory ${GOMALMO_OUT}
    COMMAND ${CMAKE_COMMAND} -E copy_directory ${GOMALMO_IN} ${GOMALMO_OUT}
    COMMAND ${CMAKE_COMMAND} -E copy x_set_cgo_flags.go ${GOMALMO_OUT}
)

### Go build and install
########################
add_custom_target(MalmoGo ALL DEPENDS Malmo
    COMMAND go install malmo
    COMMENT "building and installing Go malmo lib"
    COMMENT ${GOMALMO_SAMPLES}
)

# make sure that all files are copied to GOPATH before calling 'go install'
add_dependencies(MalmoGo CopyMalmoGoFiles)

### Install MalmoGo
###################
file(GLOB GOMALMO_SAMPLES "${CMAKE_SOURCE_DIR}/Malmo/samples/Go_examples/*.go")
install(FILES ${GOMALMO_SAMPLES} DESTINATION Go_Examples)
