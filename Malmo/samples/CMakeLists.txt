# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

add_subdirectory( Cpp_examples )
add_subdirectory( CSharp_examples )
add_subdirectory( Java_examples )
add_subdirectory( Python_examples )
if( UNIX AND NOT APPLE )
  add_subdirectory( Lua_examples )
  if( Torch_FOUND )
    add_subdirectory( Torch_examples )
  endif()
endif()
