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

set( SOURCES
  cart_test.py
  chat_reward.py
  chunk_test.py
  craft_work.py
  default_world_test.py
  depth_map_runner.py
  drawing_test.py
  human_action.py
  manual_input_test.py
  MazeRunner.py
  mission_quit_command_example.py
  MultiMaze.py
  mob_fun.py
  overclock_test.py
  patchwork_quilt.py
  quit_from_reaching_position_test.py
  render_speed_test.py
  reward_for_discarding_items_test.py
  reward_for_items_test.py
  reward_for_mission_end_test.py
  robust_frames.py
  run_mission.py
  sample_missions_loader.py
  tabular_q_learning.py
  to_string_test.py
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
  two_diggers.py
  use_file.py
)

set( ALESOURCES
  ALE_HAC.py
)

set( OTHER_FILES
  tutorial_6.xml
  Tutorial.pdf
)

install( FILES ${SOURCES} ${OTHER_FILES} DESTINATION Python_Examples )

if( ALE_FOUND )
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
  set_tests_properties( ${test_name}_help PROPERTIES ENVIRONMENT "PYTHONPATH=${MALMO_PYTHON_DIR};LD_LIBRARY_PATH=${MALMO_PYTHON_DIR};${MALMO_TEST_ENVIRONMENT}" )

  add_test(NAME ${test_name}
    WORKING_DIRECTORY ${MALMO_PYTHON_DIR}
    COMMAND ${PYTHON_EXECUTABLE} ${CMAKE_CURRENT_SOURCE_DIR}/${file} --test
  )
  set_tests_properties( ${test_name} PROPERTIES ENVIRONMENT "PYTHONPATH=${MALMO_PYTHON_DIR};LD_LIBRARY_PATH=${MALMO_PYTHON_DIR};${MALMO_TEST_ENVIRONMENT}" )

endforeach()
