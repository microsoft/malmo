# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

set( SCHEMA_FILES
  Mission.xsd
  MissionEnded.xsd
  MissionHandlers.xsd
  MissionInit.xsd
  Types.xsd
)

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
