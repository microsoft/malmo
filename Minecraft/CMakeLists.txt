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

if( MSVC )
    set( GRADLE ${CMAKE_CURRENT_SOURCE_DIR}/gradlew.bat )
else()
    set( GRADLE ${CMAKE_CURRENT_SOURCE_DIR}/gradlew )
endif()

# We currently do an in-source build for the Mod, because of gradle.
add_custom_target( Minecraft
  ALL
  COMMAND ${GRADLE} setupDecompWorkspace build testClasses -x test --stacktrace -Pversion=${MALMO_VERSION}
  DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/src/main/resources/version.properties
  WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
  COMMENT "Building Minecraft..."
)

# Write the Malmo version number into a properties file for the Mod and gradle to use:
add_custom_command(
  OUTPUT ${CMAKE_CURRENT_SOURCE_DIR}/src/main/resources/version.properties
  COMMAND ${CMAKE_COMMAND} -E echo "malmomod.version=" ${MALMO_VERSION} > 
          ${CMAKE_CURRENT_SOURCE_DIR}/src/main/resources/version.properties
  COMMENT "Creating version file..."
)

set( TEST_MOD OFF )
if( TEST_MOD )
  add_test( 
    NAME MinecraftTests 
    COMMAND ${GRADLE} test --info
    WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
  )
  set_tests_properties( MinecraftTests PROPERTIES ENVIRONMENT "MALMO_XSD_PATH=$ENV{MALMO_XSD_PATH}" )
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
list(REMOVE_ITEM MOD_FILES launch_minecraft_in_background.py)
list(REMOVE_ITEM MOD_FILES CMakeLists.txt)
foreach ( file ${MOD_FILES} )
    get_filename_component( dir ${file} PATH )
    install( FILES ${file} DESTINATION Minecraft/${dir} )
endforeach()
install( PROGRAMS gradlew DESTINATION Minecraft/ )
install( PROGRAMS launchClient.sh DESTINATION Minecraft/ )
# Make sure the version number is part of the installation:
install( FILES ${CMAKE_CURRENT_SOURCE_DIR}/src/main/resources/version.properties DESTINATION Minecraft/src/main/resources )
install( FILES ${CMAKE_CURRENT_SOURCE_DIR}/build/libs/MalmoMod-${MALMO_VERSION}.jar DESTINATION "Mod" )

