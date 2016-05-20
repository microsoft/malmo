# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

set( XSLT_EXECUTABLE
  xsltproc
)

foreach( xsd_file ${SCHEMA_FILES} )

  get_filename_component( out_file ${xsd_file} NAME_WE )

  add_custom_target( xslt_${out_file}
    ALL
    ${XSLT_EXECUTABLE} --stringparam searchIncludedSchemas true --stringparam linksFile ${CMAKE_SOURCE_DIR}/Schemas/links.xml ${CMAKE_SOURCE_DIR}/Schemas/xs3p.xsl ${xsd_file} > ${out_file}.html
    DEPENDS ${SCHEMA_FILES}
    COMMENT "Generating HTML from XSDs using xsltproc" VERBATIM
  )
  
  install( FILES ${CMAKE_CURRENT_BINARY_DIR}/${out_file}.html DESTINATION Schemas )
  
endforeach()

