// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

#ifndef _FINDSCHEMAFILE_H_
#define _FINDSCHEMAFILE_H_

// STL:
#include <string>

namespace malmo {

    // Use the MALMO_XSD_PATH environment variable, if set, to find a .xsd file.
    // Or look in the current directory. Or in ../Schemas. If not found, throw an exception.
    std::string FindSchemaFile( const std::string& name );
}

#endif
