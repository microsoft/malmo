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

# - Try to find ALE
#
# The following variables are optionally searched for defaults
#  ALE_ROOT_DIR:            Base directory where all ALE components are found
#
# The following are set after configuration is done:
#  ALE_FOUND
#  ALE_INCLUDE_DIRS
#  ALE_LIBRARIES

include(FindPackageHandleStandardArgs)

set(ALE_ROOT_DIR ${ALE_ROOT_DIR} CACHE PATH "Folder containing ALE")
set(ALE_NAMES ale)

find_path(ALE_INCLUDE_DIR ale_interface.hpp
  PATHS ${ALE_ROOT_DIR}
  PATH_SUFFIXES
  src)

find_library(ALE_LIBRARY
  NAMES ${ALE_NAMES}
  PATHS ${ALE_ROOT_DIR}
  PATH_SUFFIXES
  Debug Release)

find_package_handle_standard_args(ALE DEFAULT_MSG
  ALE_INCLUDE_DIR ALE_LIBRARY)

if(ALE_FOUND)
  set(ALE_INCLUDE_DIRS ${ALE_INCLUDE_DIR})
  set(ALE_LIBRARIES ${ALE_LIBRARY})
endif()
