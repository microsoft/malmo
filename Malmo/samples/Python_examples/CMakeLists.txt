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
  tutorial_6.xml
  tutorial_7.py
  tutorial_8.py
  Tutorial.pdf
  to_string_test.py
  overclock_test.py
)

set( ALESOURCES
    ALE_HAC.py
)

install( FILES ${SOURCES} DESTINATION Python_Examples )

if ( ALE_FOUND )
    install( FILES ${ALESOURCES} DESTINATION Python_Examples )
endif()
