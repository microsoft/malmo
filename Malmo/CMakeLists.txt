# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

add_subdirectory( src )
add_subdirectory( test )
add_subdirectory( samples )

IF( WIN32 AND INCLUDE_CSHARP )
    ADD_SUBDIRECTORY( humanAction )
ENDIF()
