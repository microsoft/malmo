# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

set( README_FILES
  readme.html
  ../changelog.txt
  ../MSR-LA_2772.pdf
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
