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

#ifndef _ARGUMENTPARSER_H_
#define _ARGUMENTPARSER_H_

// Boost:
#include <boost/program_options.hpp>

// STL:
#include <string>
#include <vector>

namespace malmo
{
    //! A general purpose command-line argument parser.
    class ArgumentParser
    {
    public:

        //! Construct an argument parser.
        //! \param title The title of the program to display.
        ArgumentParser(const std::string& title);

        //! Parses a list of strings given in the C style. Throws std::exception if parsing fails.
        /*! Available in C++ only. In other languages use the parse function instead. */
        //! \param argc The number of arguments.
        //! \param argv The arguments to parse.
        //! \see parse()
        void parseArgs(int argc, const char** argv);

        //! Parses a list of strings. Throws std::exception if parsing fails.
        //! In C++: takes a std::vector<std::string>.
        //! In Python: takes a list of strings.
        //! \param args The arguments to parse.
        void parse(const std::vector< std::string >& args);

        //! Specify an integer argument that can be given on the command line.
        //! \param name The name of the argument. To be given as "--name <value>"
        //! \param description The explanation of the argument that can be printed out.
        //! \param defaultValue The value that this argument should have if not given on the command line.
        void addOptionalIntArgument(const std::string& name, const std::string& description, int defaultValue);

        //! Specify a floating-point argument that can be given on the command line.
        //! \param name The name of the argument. To be given as "--name <value>"
        //! \param description The explanation of the argument that can be printed out.
        //! \param defaultValue The value that this argument should have if not given on the command line.
        void addOptionalFloatArgument(const std::string& name, const std::string& description, double defaultValue);

        //! Specify a string argument that can be given on the command line.
        //! \param name The name of the argument. To be given as "--name <value>"
        //! \param description The explanation of the argument that can be printed out.
        //! \param defaultValue The value that this argument should have if not given on the command line.
        void addOptionalStringArgument(const std::string& name, const std::string& description, const std::string& defaultValue);

        //! Specify a boolean flag that can be given on the command line.
        //! \param name The name of the flag. To be given as "--name"
        //! \param description The explanation of the flag that can be printed out.
        void addOptionalFlag(const std::string& name, const std::string& description);

        //! Gets a string that describes the current set of options we expect.
        //! \returns The usage string, for displaying.
        std::string getUsage() const;

        //! Gets whether a named argument was parsed on the command-line arguments.
        //! \param name The name of the argument.
        //! \returns True if the named argument was received.
        bool receivedArgument(const std::string& name) const;

        //! Retrieves the value of a named integer argument.
        //! \param name The name of the argument.
        //! \returns The value of the named argument.
        int getIntArgument(const std::string& name) const;

        //! Retrieves the value of a named floating-point argument.
        //! \param name The name of the argument.
        //! \returns The value of the named argument.
        double getFloatArgument(const std::string& name) const;

        //! Retrieves the value of a named string argument.
        //! \param name The name of the argument.
        //! \returns The value of the named argument.
        std::string getStringArgument(const std::string& name) const;

    private:

        boost::program_options::options_description spec;   // the specifications of the options we expect
        boost::program_options::variables_map       opts;   // the names and values of the options we received
    };
}

#endif
