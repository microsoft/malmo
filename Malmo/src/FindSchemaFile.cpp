// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

// Local:
#include "FindSchemaFile.h"

// STL:
#include <fstream>
#include <sstream>
using namespace std;

namespace malmo
{
    bool fileExists( const std::string& filename )
    {
        ifstream in( filename.c_str() );
        return in.good();
    }
    
    std::string FindSchemaFile( const std::string& name )
    {
        char *malmo_xsd_path = getenv("MALMO_XSD_PATH");

        bool exists_on_env_path = false;
        string path_using_env;
        if( malmo_xsd_path )
        {
            ostringstream oss;
            oss << malmo_xsd_path << "/" << name;
            path_using_env = oss.str();
            exists_on_env_path = fileExists( path_using_env );
        }    
            
        bool exists_in_current_dir = fileExists( name );
        
        if( !exists_on_env_path && !exists_in_current_dir )
            throw runtime_error( "Set the MALMO_XSD_PATH environment variable to the location of the .xsd schema files." );

        // prefer the file that exists in the location given by the MALMO_XSD_PATH environment variable
        if( exists_on_env_path )
            return path_using_env;
        
        // file exists in current folder, which is fine too
        return name;
    }
}
   