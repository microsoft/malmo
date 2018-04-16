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

#include "RewardXML.h"

// STL:
#include <exception>
#include <iostream>

// Boost:
#include <boost/property_tree/xml_parser.hpp>

namespace malmo {

    RewardXML::RewardXML(std::string xml_text) {
        parse_rewards(xml_text);
    }

    void RewardXML::parse_rewards(std::string xml_text) {
        boost::property_tree::ptree xml;

        std::istringstream is(xml_text);
        boost::property_tree::read_xml(is, xml);

        auto rewardElement = xml.get_child("Reward");
        parse_rewards(rewardElement);
    }

    void RewardXML::parse_rewards(boost::property_tree::ptree& reward_element) {
        for (boost::property_tree::ptree::value_type const& v : reward_element) {
            if (v.first == "Value") {
                int dimension = v.second.get<int>("<xmlattr>.dimension");
                double value = v.second.get<double>("<xmlattr>.value");

                reward_values[dimension] = value;
            }
        }
    }

    void RewardXML::add_rewards(boost::property_tree::ptree& reward_element) const {
        for (auto &r : reward_values) {
            boost::property_tree::ptree reward_value;
            reward_value.put("<xmlattr>.dimension", r.first);
            reward_value.put("<xmlattr>.value", r.second);
            reward_element.add_child("Rewards.Value", reward_value);
        }
    }

    std::string RewardXML::toXml() const {
        std::ostringstream oss;
        boost::property_tree::ptree xml;

        add_rewards(xml);

        write_xml(oss, xml);

        std::string xml_str = oss.str();
        xml_str.erase(std::remove(xml_str.begin(), xml_str.end(), '\n'), xml_str.end());

        return xml_str;
    }
}
