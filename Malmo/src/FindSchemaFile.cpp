// --------------------------------------------------------------------------------------------------
//  Copyright (c) 2016 Microsoft Corporation
//  
//  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
//  associated documentation files (the "Software"), to deal in the Software without restriction,
//  including without limitation the rights to use, copy, modify, merge, publish, distribute,
//  sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions:
//  
//  The above copyright notice and this permission notice shall be included in all copies or
//  substantial portions of the Software.
//  
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
//  NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
//  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
//  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// --------------------------------------------------------------------------------------------------

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
