# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

add_subdirectory( CppTests )
add_subdirectory( CSharpTests )
add_subdirectory( JavaTests )
add_subdirectory( PythonTests )

IF( UNIX AND NOT APPLE )
  add_subdirectory( LuaTests )
ENDIF()

