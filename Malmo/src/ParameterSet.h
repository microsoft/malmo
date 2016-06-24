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

#ifndef _PARAMETERSET_H_
#define _PARAMETERSET_H_

// Boost:
#include <boost/property_tree/ptree.hpp>

// STL:
#include <string>
#include <vector>

namespace malmo
{
    //! Class which contains a list of key/value parameters for a mission.  Only supports
    //! a flat hierarchy.
    class ParameterSet
    {
        public:
            //! Constructs a parameter set.
            ParameterSet();

            //! Constructs a parameter set using the provided property tree.
            //! \param parameters A property tree containing the parameters.
            ParameterSet(const boost::property_tree::ptree parameters);

            //! Constructs a parameter set from the provided JSON string.
            //! \param json A JSON string describing the parameter set.
            ParameterSet(const std::string& json);

            //! Returns a JSON representation of the parameter set.
            //! \returns A JSON string representing the parameter set.
            std::string toJson();

            //! Sets the value of a key as a string.
            //! \param key The parameter name.
            //! \param value The parameter value.
            void set(const std::string& key, const std::string& value);

            //! Gets the value of a key as a string.
            //! \param key The parameter name.
            //! \returns The value of the key.
            std::string get(const std::string& key) const;

            //! Sets the value of a key as an integer.
            //! \param key The parameter name.
            //! \param value The parameter value.
            void setInt(const std::string& key, const int value);

            //! Gets the value of a key as an integer.
            //! \param key The parameter name.
            //! \returns The key value.
            int getInt(const std::string& key) const;

            //! Sets the value of a key as a double.
            //! \param key The parameter name.
            //! \param value The parameter value.
            void setDouble(const std::string& key, const double value);

            //! Gets the value of a key as a double.
            //! \param key The parameter name.
            //! \returns The key value.
            double getDouble(const std::string& key) const;

            //! Sets the value of a key as a boolean.
            //! \param key The parameter name.
            //! \param value The parameter value.
            void setBool(const std::string& key, const bool value);

            //! Gets the value of a key as a boolean.
            //! \param key The parameter name.
            //! \returns The key value.
            bool getBool(const std::string& key) const;

            //! Gets the keys in the parameter set.
            //! \returns The keys as a list of strings.
            std::vector<std::string> keys() const;

            //! Sets the number of iterations that these parameters should be tested.
            //! \param iteration_count The number of iterations.
            void setIterationCount(const int iteration_count);

            //! Gets the number of iterations that these parameters should be tested.
            //! \returns The number of iterations.
            int getIterationCount() const;

        private:
            boost::property_tree::ptree parameters;
            int iteration_count;
    };
}

#endif
