# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

include( UseJava )

set( SOURCES
  JavaExamples_run_mission.java
)

foreach( file ${SOURCES} )

  get_filename_component( sample_name ${file} NAME_WE )
  
  get_target_property( MalmoJarFile MalmoJavaJar JAR_FILE)
  set( CMAKE_JAVA_INCLUDE_PATH  ${MalmoJarFile} )
  set( CMAKE_JAVA_JAR_ENTRY_POINT ${sample_name} )
  add_jar( 
    ${sample_name} 
    SOURCES ${file} 
    INCLUDE_JARS MalmoJavaJar
    ENTRY_POINT ${sample_name}
  )
  install_jar( ${sample_name} Java_Examples )
  
endforeach()

install( FILES ${SOURCES} DESTINATION Java_Examples )
