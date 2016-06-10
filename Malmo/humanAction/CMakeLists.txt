# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

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

configure_file( ${CMAKE_CURRENT_SOURCE_DIR}/MalmoHumanAction.csproj.pre ${CMAKE_CURRENT_BINARY_DIR}/MalmoHumanAction.csproj )

ADD_CUSTOM_COMMAND(
   OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/$(Configuration)/MalmoHumanAction.exe
   COMMAND ${CMAKE_VS_MSBUILD_COMMAND}
   ARGS ${CMAKE_CURRENT_BINARY_DIR}/MalmoHumanAction.csproj /p:Configuration=$<CONFIG> /p:Platform=${CSHARP_PLATFORM} /p:OutputPath=${CMAKE_CURRENT_BINARY_DIR}/$<CONFIG>
)

ADD_CUSTOM_TARGET(
   MalmoHumanAction ALL
   DEPENDS ${CMAKE_CURRENT_BINARY_DIR}/$(Configuration)/MalmoHumanAction.exe
   SOURCES ${MALMO_HUMAN_ACTION_SOURCES}
)

ADD_DEPENDENCIES( MalmoHumanAction MalmoNET )

INSTALL( PROGRAMS ${CMAKE_CURRENT_BINARY_DIR}/$<CONFIG>/MalmoHumanAction.exe DESTINATION HumanAction )
INSTALL( FILES ${CMAKE_CURRENT_BINARY_DIR}/$<CONFIG>/MalmoHumanAction.exe.config DESTINATION HumanAction )
INSTALL( FILES ${SCHEMA_FILES} DESTINATION HumanAction )
