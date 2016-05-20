//--------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

// Local:
#include "ArgumentParser.h"

// Boost:
namespace po = boost::program_options;

// STL:
#include <sstream>
using namespace std;

namespace malmo
{
    ArgumentParser::ArgumentParser(const string& title)
        : spec(title + "\n\nAllowed options")
    {
    }

    void ArgumentParser::parseArgs(int argc, const char** argv)
    {
        po::store(po::parse_command_line(argc, argv, this->spec), this->opts);
        po::notify(this->opts);
    }

    void ArgumentParser::parse(const vector< string >& strings)
    {
        const char **pList = new const char*[strings.size()];
        for (size_t i = 0; i < strings.size(); ++i)
            pList[i] = strings[i].c_str();
        this->parseArgs((int)strings.size(), pList);
        delete[]pList;
    }

    void ArgumentParser::addOptionalIntArgument(const string& name, const string& description, int defaultValue)
    {
        this->spec.add_options()(name.c_str(), po::value<int>()->default_value(defaultValue), description.c_str());
    }

    void ArgumentParser::addOptionalFloatArgument(const string& name, const string& description, double defaultValue)
    {
        this->spec.add_options()(name.c_str(), po::value<double>()->default_value(defaultValue), description.c_str());
    }

    void ArgumentParser::addOptionalStringArgument(const string& name, const string& description, const string& defaultValue)
    {
        this->spec.add_options()(name.c_str(), po::value<string>()->default_value(defaultValue), description.c_str());
    }

    void ArgumentParser::addOptionalFlag(const string& name, const string& description)
    {
        this->spec.add_options()(name.c_str(), description.c_str());
    }

    std::string ArgumentParser::getUsage() const
    {
        ostringstream oss;
        oss << this->spec << endl;
        return oss.str();
    }

    bool ArgumentParser::receivedArgument(const string& name) const
    {
        return this->opts.count(name) > 0;
    }

    int ArgumentParser::getIntArgument(const string& name) const
    {
        if( !this->receivedArgument( name ) )
            throw runtime_error( "getIntArgument: have not received an argument named " + name );
        return this->opts[name].as<int>();
    }

    double ArgumentParser::getFloatArgument(const string& name) const
    {
        if( !this->receivedArgument( name ) )
            throw runtime_error( "getFloatArgument: have not received an argument named " + name );
        return this->opts[name].as<double>();
    }

    string ArgumentParser::getStringArgument(const string& name) const
    {
        if( !this->receivedArgument( name ) )
            throw runtime_error( "getStringArgument: have not received an argument named " + name );
        return this->opts[name].as<string>();
    }
}
