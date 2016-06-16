# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

set( SAMPLE_MISSIONS
  classroom/basic.xml
  classroom/obstacles.xml
  classroom/simpleMaze.xml
  classroom/vertical.xml
  classroom/medium.xml
  classroom/hard.xml
  classroom/complexity_usage.xml
  classroom/specification_usage.xml
  cliff_walking_1.xml
  default_mission.xml
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
install( FILES ${SAMPLE_MISSIONS} DESTINATION Sample_missions )

