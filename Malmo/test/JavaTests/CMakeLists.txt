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
