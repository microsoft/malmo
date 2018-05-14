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

set( SCHEMA_FILES
  Mission.xsd
  MissionEnded.xsd
  MissionHandlers.xsd
  MissionInit.xsd
  Types.xsd
)

# Configure the right version number into the xsd files.
configure_file( Mission.xsd.in  ${CMAKE_CURRENT_SOURCE_DIR}/Mission.xsd )
configure_file( MissionEnded.xsd.in  ${CMAKE_CURRENT_SOURCE_DIR}/MissionEnded.xsd )
configure_file( MissionHandlers.xsd.in  ${CMAKE_CURRENT_SOURCE_DIR}/MissionHandlers.xsd )
configure_file( MissionInit.xsd.in  ${CMAKE_CURRENT_SOURCE_DIR}/MissionInit.xsd )
configure_file( Types.xsd.in  ${CMAKE_CURRENT_SOURCE_DIR}/Types.xsd )

if( BUILD_DOCUMENTATION )
  set( XSLT_EXECUTABLE xsltproc )

  foreach( xsd_file ${SCHEMA_FILES} )

    get_filename_component( out_file ${xsd_file} NAME_WE )
    add_custom_target( xslt_${out_file}
      ALL
      ${XSLT_EXECUTABLE} --stringparam searchIncludedSchemas true --stringparam linksFile links.xml xs3p.xsl ${xsd_file} > ${CMAKE_CURRENT_BINARY_DIR}/${out_file}.html
      DEPENDS ${xsd_file}
      WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
      COMMENT "Generating HTML from ${xsd_file}" VERBATIM
    )
  
    install( FILES ${CMAKE_CURRENT_BINARY_DIR}/${out_file}.html DESTINATION Schemas )
  
  endforeach()
endif()

install( FILES ${SCHEMA_FILES} DESTINATION Schemas )
