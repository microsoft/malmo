# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

add_subdirectory( Cpp_examples )

if( INCLUDE_CSHARP )
  add_subdirectory( CSharp_examples )
endif()

if( INCLUDE_JAVA )
  add_subdirectory( Java_examples )
endif()

if( INCLUDE_PYTHON )
  add_subdirectory( Python_examples )
endif()

if( INCLUDE_LUA )
  add_subdirectory( Lua_examples )
endif()

if( INCLUDE_TORCH )
  add_subdirectory( Torch_examples )
endif()
