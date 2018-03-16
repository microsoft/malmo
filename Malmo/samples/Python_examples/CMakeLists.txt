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
  animation_test.py
  braitenberg_simulation.py
  build_test.py
  chat_reward.py
  chunk_test.py
  craft_work.py
  decision_tree_test.py
  default_world_test.py
  depth_map_runner.py
  discrete_3d_test.py
  drawing_test.py
  file_test.py
  hit_test.py
  inventory_test.py
  MazeRunner.py
  mission_quit_command_example.py
  mob_fun.py
  mouse_steering_test.py
  moving_target_test.py
  overclock_test.py
  patchwork_quilt.py
  quit_from_reaching_position_test.py
  radar_test.py
  render_speed_test.py
  reward_for_discarding_items_test.py
  reward_for_items_test.py
  reward_for_mission_end_test.py
  robust_frames.py
  run_mission.py
  tabular_q_learning.py
  teleport_test.py
  to_string_test.py
)

set( ALESOURCES
  ALE_HAC.py
)

set( MULTI_AGENT_TESTS
    two_diggers.py
    team_reward_test.py
    MultiMaze.py
)

set( OTHER_FILES
  malmoutils.py
  Tutorial.pdf
  note_block_test.py    # Currently no way to test audio
  cart_test.py          # Currently unreliable
  mob_zoo.py            # Currently unreliable
  block_type_test.py    # WIP
  human_action.py       # Doesn't make much sense to run this as an integration test
  manual_input_test.py  # Doesn't make much sense to run this as an integration test
  sample_missions_loader.py # Ditto
  use_file.py           # Ditto
  tutorial_1.py
  tutorial_2.py
  tutorial_3.py
  tutorial_4.py
  tutorial_4_solved.py
  tutorial_5.py
  tutorial_5_solved.py
  tutorial_6.py
  tutorial_6.xml
  tutorial_7.py
  tutorial_8.py
  multi_agent_test.py       # Moving these here for now, as running
  agent_visibility_test.py  # sufficient copies of Minecraft to test them
  tsp_race.py               # requires a pretty high-spec machine.
  turn_based_test.py        # Remember to add more ports to the launch_minecraft_in_background call below when restoring.
)

install( FILES ${SOURCES} ${MULTI_AGENT_TESTS} ${OTHER_FILES} DESTINATION Python_Examples )

if( ALE_FOUND )
  install( FILES ${ALESOURCES} DESTINATION Python_Examples )
endif()

function(addTestsFromFiles SOURCES_TO_TEST)
    # add an integration test for each sample
    foreach( file ${SOURCES_TO_TEST} )

      get_filename_component( script_name "${file}" NAME_WE )
      set( test_name "PythonIntegrationTests_${script_name}" )
      
      add_test(NAME ${test_name}_help
        WORKING_DIRECTORY ${MALMO_PYTHON_DIR}
        COMMAND ${PYTHON_EXECUTABLE} ${CMAKE_CURRENT_SOURCE_DIR}/${file} --help
      )
      set_tests_properties( ${test_name}_help PROPERTIES ENVIRONMENT "PYTHONPATH=${MALMO_PYTHON_DIR};LD_LIBRARY_PATH=${MALMO_PYTHON_DIR};${MALMO_TEST_ENVIRONMENT}" )

      add_test(NAME ${test_name}
        WORKING_DIRECTORY ${MALMO_PYTHON_DIR}
        COMMAND ${PYTHON_EXECUTABLE} ${CMAKE_CURRENT_SOURCE_DIR}/${file} --test --recording_dir ${script_name} --record_video
      )
      set_tests_properties( ${test_name} PROPERTIES ENVIRONMENT "PYTHONPATH=${MALMO_PYTHON_DIR};LD_LIBRARY_PATH=${MALMO_PYTHON_DIR};${MALMO_TEST_ENVIRONMENT}" )

    endforeach()
endfunction(addTestsFromFiles)

# First run the single-agent tests:
addTestsFromFiles( "${SOURCES}" )

# Now run the multi-agent tests - which will require launching more copies of Minecraft
add_test(
  NAME PreMultiAgentIntegrationTests
  WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}/Minecraft
  COMMAND ${PYTHON_EXECUTABLE} launch_minecraft_in_background.py 10001
)
set_tests_properties( PreMultiAgentIntegrationTests PROPERTIES ENVIRONMENT "MALMO_XSD_PATH=$ENV{MALMO_XSD_PATH}" )
addTestsFromFiles( "${MULTI_AGENT_TESTS}" )