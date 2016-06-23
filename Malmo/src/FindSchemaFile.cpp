// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

// Local:
#include "FindSchemaFile.h"

// STL:
#include <fstream>
#include <sstream>
#include <stdexcept>
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
        // first preference: location specified in MALMO_XSD_PATH environment variable
        char *malmo_xsd_path = getenv("MALMO_XSD_PATH");
        if( malmo_xsd_path )
        {
            ostringstream path_using_env;
            path_using_env << malmo_xsd_path << "/" << name;
            if( fileExists( path_using_env.str() ) ) 
            {
                return path_using_env.str();
            }
            else
            {
                ostringstream error_message;
                error_message << "Schema file " << name << " not found in folder specified by MALMO_XSD_PATH environment variable: " << malmo_xsd_path;
                throw runtime_error( error_message.str() );
            }
        }    
        
        // second preference: current directory
        if( fileExists( name ) )
            return name;
            
        // third preference: ../Schemas
        ostringstream path_using_relative_dir;
        path_using_relative_dir << "../Schemas/" << name;
        if( fileExists( path_using_relative_dir.str() ) )
            return path_using_relative_dir.str();

        // file not found
        ostringstream error_message;
        error_message << "Schema file " << name << " not found. Please set the MALMO_XSD_PATH environment variable to the location of the .xsd schema files.";
        throw runtime_error( error_message.str() );
    }
}
