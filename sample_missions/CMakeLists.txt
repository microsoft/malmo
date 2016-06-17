# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

set( SAMPLE_MISSIONS
  default_world_1.xml
  default_flat_1.xml
  tricky_arena_1.xml
  eating_1.xml
  cliff_walking_1.xml
  mazes/maze_1.xml
  mazes/maze_2.xml
  classroom/basic.xml
  classroom/obstacles.xml
  classroom/simpleRoomMaze.xml
  classroom/attic.xml
  classroom/vertical.xml
  classroom/complexity_usage.xml
  classroom/medium.xml
  classroom/hard.xml
)

set( OTHER_FILES
  readme.md
  MalmoMissionTable_CurrentTasks_2016_06_14.pdf
)

set( XML_MISSION_FILES
  ${SAMPLE_MISSIONS}
  ../Malmo/samples/Python_examples/tutorial_6.xml
)

# add tests to validate each mission file
if( INCLUDE_PYTHON ) # (TODO: validate in C++ instead)
    foreach( file ${XML_MISSION_FILES} )

      get_filename_component( test_name "${file}" NAME_WE )
      set( test_name "ValidationTests_${test_name}" )
      
      add_test(NAME ${test_name}
        WORKING_DIRECTORY ${MALMO_PYTHON_DIR}
        COMMAND ${PYTHON_EXECUTABLE} ${CMAKE_CURRENT_SOURCE_DIR}/validate.py ${CMAKE_CURRENT_SOURCE_DIR}/${file}
      )
      
      set_tests_properties( ${test_name} PROPERTIES ENVIRONMENT "PYTHONPATH=${MALMO_PYTHON_DIR};LD_LIBRARY_PATH=${MALMO_PYTHON_DIR}" )
      
    endforeach()
endif()

# copy the files to the distributable in a Sample_missions folder
foreach ( file ${SAMPLE_MISSIONS} )
    get_filename_component( dir ${file} PATH )
    install( FILES ${file} DESTINATION Sample_missions/${dir} )
endforeach()
install( FILES ${OTHER_FILES} DESTINATION Sample_missions )

