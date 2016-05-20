# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

if( MSVC )
    set( GRADLE ${CMAKE_CURRENT_SOURCE_DIR}/gradlew.bat )
else()
    set( GRADLE ${CMAKE_CURRENT_SOURCE_DIR}/gradlew )
endif()

# We currently do an in-source build for the Mod, because of gradle.
add_custom_target( Minecraft
  ALL
  COMMAND ${GRADLE} setupDecompWorkspace build testClasses -x test --stacktrace --info -Pversion=${MALMO_VERSION}
  WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
)

set( TEST_MOD OFF )
if( TEST_MOD )
  add_test( 
    NAME MinecraftTests 
    COMMAND ${GRADLE} test --info
    WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
  )
endif()

# install the Minecraft folder
execute_process(
    COMMAND ${GIT_EXECUTABLE} rev-parse --abbrev-ref HEAD
    WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}
    OUTPUT_VARIABLE GIT_BRANCH
    OUTPUT_STRIP_TRAILING_WHITESPACE
)
execute_process(
    COMMAND ${GIT_EXECUTABLE} ls-tree -r ${GIT_BRANCH} --name-only .
    WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
    OUTPUT_VARIABLE MOD_FILES
)
STRING(REGEX REPLACE "\n" ";" MOD_FILES "${MOD_FILES}")
foreach ( file ${MOD_FILES} )
    get_filename_component( dir ${file} PATH )
    install( FILES ${file} DESTINATION Minecraft/${dir} )
endforeach()
install( PROGRAMS gradlew DESTINATION Minecraft/ )
install( PROGRAMS launchClient.sh DESTINATION Minecraft/ )
install( FILES ${SCHEMA_FILES} DESTINATION Schemas ) # Mod needs Schema files in a relative location in order to build
install( FILES ${CMAKE_CURRENT_SOURCE_DIR}/build/libs/MalmoMod-${MALMO_VERSION}.jar DESTINATION "Mod" )
