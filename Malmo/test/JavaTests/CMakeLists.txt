# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

include( UseJava )

set( JAVA_TEST_SOURCES
  test_agent_host.java
  test_argument_parser.java
  test_mission.java
  test_parameter_set.java
  test_wrapping.java
)

if ( ALE_FOUND )
  set( JAVA_TEST_SOURCES
    ${JAVA_TEST_SOURCES}
    test_ALE_built.java
  )
endif()

foreach( file ${JAVA_TEST_SOURCES} )

  get_filename_component( test_name ${file} NAME_WE )
  set( decorated_test_name JavaTests_${test_name} )

  get_target_property( MalmoJarFile MalmoJavaJar JAR_FILE)
  set( CMAKE_JAVA_INCLUDE_PATH  ${MalmoJarFile} )
  set( CMAKE_JAVA_JAR_ENTRY_POINT ${test_name} )
  add_jar( 
    ${test_name} 
    SOURCES ${file} 
    INCLUDE_JARS MalmoJavaJar
    ENTRY_POINT ${test_name}
  )
  
  get_target_property( TestJarFile ${test_name} JAR_FILE)
  if( WIN32 )
    set( custom_classpath "${TestJarFile}$<SEMICOLON>${MalmoJarFile}" )
  else()
    set( custom_classpath ${TestJarFile}:${MalmoJarFile} )
  endif()

  add_test(
    NAME ${decorated_test_name} 
    COMMAND ${Java_JAVA_EXECUTABLE} -cp ${custom_classpath} -Djava.library.path=$<TARGET_FILE_DIR:MalmoJava> ${test_name}
  )
  set_tests_properties( ${decorated_test_name} PROPERTIES ENVIRONMENT "MALMO_XSD_PATH=$ENV{MALMO_XSD_PATH}" )
        
endforeach()
