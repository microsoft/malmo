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
  add_test( NAME ${decorated_test_name}_help COMMAND ${Java_JAVA_EXECUTABLE} ${test_params} )
  set_tests_properties( ${decorated_test_name}_help PROPERTIES ENVIRONMENT "MALMO_XSD_PATH=$ENV{MALMO_XSD_PATH}" )
  set( test_params -cp ${custom_classpath} -Djava.library.path=$<TARGET_FILE_DIR:MalmoJava> ${test_name} --test )
  add_test( NAME ${decorated_test_name} COMMAND ${Java_JAVA_EXECUTABLE} ${test_params} )
  set_tests_properties( ${decorated_test_name} PROPERTIES ENVIRONMENT "MALMO_XSD_PATH=$ENV{MALMO_XSD_PATH}" )

endforeach()

install( FILES ${SOURCES} DESTINATION Java_Examples )
