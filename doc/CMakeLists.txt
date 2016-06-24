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

set( README_FILES
  readme.html
  ../changelog.txt
  ../LICENSE.txt
  install_linux.md
  install_macosx.md
  install_windows.md
)

# copy these files into the root of the distribution zip
install( FILES ${README_FILES} DESTINATION "." )

# also install the Mod jar readme
install( FILES jar_readme.md DESTINATION "Mod" )

# build the documentation from the C++ comments
configure_file( 
  Doxyfile.in 
  ${CMAKE_CURRENT_BINARY_DIR}/Doxyfile @ONLY 
)
add_custom_target( doc
  ALL
  ${DOXYGEN_EXECUTABLE} 
  ${CMAKE_CURRENT_BINARY_DIR}/Doxyfile
  WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}
  COMMENT "Generating API documentation with Doxygen" VERBATIM
)
install( DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/doc/html/ DESTINATION Documentation )
