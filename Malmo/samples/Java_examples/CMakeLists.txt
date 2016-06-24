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
