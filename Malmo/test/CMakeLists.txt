# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

add_subdirectory( CppTests )

if( INCLUDE_CSHARP )
  add_subdirectory( CSharpTests )
endif()

if( INCLUDE_JAVA )
  add_subdirectory( JavaTests )
endif()

if( INCLUDE_PYTHON )
  add_subdirectory( PythonTests )
endif()

if( INCLUDE_LUA )
  add_subdirectory( LuaTests )
endif()

