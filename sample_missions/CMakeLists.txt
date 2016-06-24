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
      set_tests_properties( ${test_name} PROPERTIES ENVIRONMENT "PYTHONPATH=${MALMO_PYTHON_DIR};LD_LIBRARY_PATH=${MALMO_PYTHON_DIR};MALMO_XSD_PATH=$ENV{MALMO_XSD_PATH}" )
      
    endforeach()
endif()

# copy the files to the distributable in a Sample_missions folder
foreach ( file ${SAMPLE_MISSIONS} )
    get_filename_component( dir ${file} PATH )
    install( FILES ${file} DESTINATION Sample_missions/${dir} )
endforeach()
install( FILES ${OTHER_FILES} DESTINATION Sample_missions )

