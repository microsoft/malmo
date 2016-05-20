# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

set( ROM_FILES
    readme.md
)

# copy the files to the distributable in an ALE_ROMS folder
install( FILES ${ROM_FILES} DESTINATION ALE_ROMS )
