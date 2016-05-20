# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

set( PYTHON_TEST_SOURCES
  test_wrapping.py
  test_argument_parser.py
  test_agent_host.py
  test_mission.py
  test_parameter_set.py
)

if ( ALE_FOUND )
  set( PYTHON_TEST_SOURCES
    ${PYTHON_TEST_SOURCES}
    test_ALE_built.py
  )
endif()

foreach( file ${PYTHON_TEST_SOURCES} )

  get_filename_component( test_name "${file}" NAME_WE )
  set( test_name "PythonTests_${test_name}" )
  
  add_test(NAME ${test_name}
    WORKING_DIRECTORY ${MALMO_PYTHON_DIR}
    COMMAND ${PYTHON_EXECUTABLE} ${CMAKE_CURRENT_SOURCE_DIR}/${file}
  )
  
  set_tests_properties( ${test_name} PROPERTIES ENVIRONMENT "PYTHONPATH=${MALMO_PYTHON_DIR};LD_LIBRARY_PATH=${MALMO_PYTHON_DIR}" )
  
endforeach()

