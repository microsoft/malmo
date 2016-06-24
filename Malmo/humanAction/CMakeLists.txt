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

SET( MALMO_HUMAN_ACTION_SOURCES
  App.config
  App.xaml
  App.xaml.cs
  CurriculumDialog.xaml
  CurriculumDialog.xaml.cs
  CurriculumItem.xaml
  CurriculumItem.xaml.cs
  GamepadState.cs
  HighScoreWindow.xaml
  HighScoreWindow.xaml.cs
  MainWindow.xaml
  MainWindow.xaml.cs
  NameDialog.xaml
  NameDialog.xaml.cs
  PreferencesDialog.xaml
  PreferencesDialog.xaml.cs
  RecordWindow.xaml
  RecordWindow.xaml.cs
  Score.cs
)

configure_file( ${CMAKE_CURRENT_SOURCE_DIR}/MalmoHumanAction.csproj.pre ${CMAKE_CURRENT_SOURCE_DIR}/MalmoHumanAction.csproj )

ADD_CUSTOM_COMMAND(
   OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/$(Configuration)/MalmoHumanAction.exe
   COMMAND ${CMAKE_VS_MSBUILD_COMMAND}
   ARGS ${CMAKE_CURRENT_SOURCE_DIR}/MalmoHumanAction.csproj /p:Configuration=$<CONFIG> /p:Platform=${CSHARP_PLATFORM} /p:OutputPath=${CMAKE_CURRENT_BINARY_DIR}/$<CONFIG>
)

ADD_CUSTOM_TARGET(
   MalmoHumanAction ALL
   DEPENDS ${CMAKE_CURRENT_BINARY_DIR}/$(Configuration)/MalmoHumanAction.exe
   SOURCES ${MALMO_HUMAN_ACTION_SOURCES}
)

ADD_DEPENDENCIES( MalmoHumanAction MalmoNET )

INSTALL( PROGRAMS ${CMAKE_CURRENT_BINARY_DIR}/$<CONFIG>/MalmoHumanAction.exe DESTINATION HumanAction )
INSTALL( FILES ${CMAKE_CURRENT_BINARY_DIR}/$<CONFIG>/MalmoHumanAction.exe.config DESTINATION HumanAction )
