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

#include "ParameterSet.h"

#include <sstream>

#include <boost/property_tree/json_parser.hpp>

namespace malmo {
    ParameterSet::ParameterSet()
        : iteration_count(1)
    {
    }

    ParameterSet::ParameterSet(const boost::property_tree::ptree parameters)
        : parameters(parameters)
        , iteration_count(1)
    {
    }

    ParameterSet::ParameterSet(const std::string& json)
        : iteration_count(1)
    {
        std::istringstream is(json);
        boost::property_tree::read_json(is, this->parameters);
    }

    void ParameterSet::set(const std::string& key, const std::string& value)
    {
        this->parameters.put(key, value);
    }

    void ParameterSet::setInt(const std::string& key, const int value)
    {
        this->parameters.put(key, value);
    }

    void ParameterSet::setDouble(const std::string& key, const double value)
    {
        this->parameters.put(key, value);
    }

    void ParameterSet::setBool(const std::string& key, const bool value)
    {
        this->parameters.put(key, value);
    }

    std::string ParameterSet::get(const std::string& key) const
    {
        return this->parameters.get<std::string>(key);
    }

    double ParameterSet::getDouble(const std::string& key) const
    {
        return this->parameters.get<double>(key);
    }

    int ParameterSet::getInt(const std::string& key) const
    {
        return this->parameters.get<int>(key);
    }

    bool ParameterSet::getBool(const std::string& key) const
    {
        return this->parameters.get<bool>(key);
    }

    std::string ParameterSet::toJson()
    {
        std::ostringstream buf;
        boost::property_tree::write_json(buf, this->parameters, false);
        return buf.str();
    }

    void ParameterSet::setIterationCount(const int iteration_count)
    {
        this->iteration_count = iteration_count;
    }

    int ParameterSet::getIterationCount() const
    {
        return this->iteration_count;
    }

    std::vector<std::string> ParameterSet::keys() const
    {
        std::vector<std::string> keys;
        for (auto parameter : this->parameters){
            keys.push_back(parameter.first);
        }

        return keys;
    }
}
