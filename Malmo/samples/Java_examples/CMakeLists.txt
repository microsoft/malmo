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

  # add an integration test for the sample
  get_filename_component( test_name ${file} NAME_WE )
  set( decorated_test_name JavaIntegrationTests_${test_name} )
  get_target_property( TestJarFile ${test_name} JAR_FILE)
  if( WIN32 )
    set( custom_classpath "${TestJarFile}$<SEMICOLON>${MalmoJarFile}" )
  else()
    set( custom_classpath ${TestJarFile}:${MalmoJarFile} )
  endif()
  set( test_params -cp ${custom_classpath} -Djava.library.path=$<TARGET_FILE_DIR:MalmoJava> ${test_name} --help )
  add_test( NAME ${decorated_test_name} COMMAND ${Java_JAVA_EXECUTABLE} ${test_params} )
  # copy the schema files to the test build folder
  foreach( xsd_file ${SCHEMA_FILES} )
    get_filename_component( out_file ${xsd_file} NAME )
    set( out_file ${CMAKE_CURRENT_BINARY_DIR}/${out_file} )
    add_custom_command( 
      TARGET ${sample_name}
      POST_BUILD
      COMMAND ${CMAKE_COMMAND}
      ARGS -E copy_if_different ${xsd_file} ${out_file}
    )
  endforeach()
        

endforeach()

install( FILES ${SOURCES} DESTINATION Java_Examples )
