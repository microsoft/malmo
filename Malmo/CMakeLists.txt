# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

add_test(
  NAME PreIntegrationTests
  WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../Minecraft
  COMMAND ${PYTHON_EXECUTABLE} launch_minecraft_in_background.py
)

add_subdirectory( src )
add_subdirectory( test )
add_subdirectory( samples )

IF( WIN32 AND INCLUDE_CSHARP )
    ADD_SUBDIRECTORY( humanAction )
ENDIF()
