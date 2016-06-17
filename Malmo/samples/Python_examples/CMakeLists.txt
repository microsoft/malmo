# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

set( SOURCES
  run_mission.py
  depth_map_runner.py
  manual_input_test.py
  reward_for_items_test.py
  reward_for_mission_end_test.py
  drawing_test.py
  patchwork_quilt.py
  MazeRunner.py
  MultiMaze.py
  tabular_q_learning.py
  default_world_test.py
  use_file.py
  tutorial_1.py
  tutorial_2.py
  tutorial_3.py
  tutorial_4.py
  tutorial_4_solved.py
  tutorial_5.py
  tutorial_5_solved.py
  tutorial_6.py
  tutorial_7.py
  tutorial_8.py
  to_string_test.py
  overclock_test.py
  render_speed_test.py
  reward_for_discarding_items_test.py
)

set( ALESOURCES
    ALE_HAC.py
)

set( OTHER_FILES
  tutorial_6.xml
  Tutorial.pdf
)

install( FILES ${SOURCES} ${OTHER_FILES} DESTINATION Python_Examples )

if ( ALE_FOUND )
    install( FILES ${ALESOURCES} DESTINATION Python_Examples )
endif()

# add an integration test for each sample
foreach( file ${SOURCES} )

  get_filename_component( test_name "${file}" NAME_WE )
  set( test_name "PythonIntegrationTests_${test_name}" )
  
  add_test(NAME ${test_name}_help
    WORKING_DIRECTORY ${MALMO_PYTHON_DIR}
    COMMAND ${PYTHON_EXECUTABLE} ${CMAKE_CURRENT_SOURCE_DIR}/${file} --help
  )
  set_tests_properties( ${test_name}_help PROPERTIES ENVIRONMENT "PYTHONPATH=${MALMO_PYTHON_DIR};LD_LIBRARY_PATH=${MALMO_PYTHON_DIR}" )

  add_test(NAME ${test_name}
    WORKING_DIRECTORY ${MALMO_PYTHON_DIR}
    COMMAND ${PYTHON_EXECUTABLE} ${CMAKE_CURRENT_SOURCE_DIR}/${file} --test
  )
  set_tests_properties( ${test_name} PROPERTIES ENVIRONMENT "PYTHONPATH=${MALMO_PYTHON_DIR};LD_LIBRARY_PATH=${MALMO_PYTHON_DIR}" )

endforeach()
