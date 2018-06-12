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

#ifndef _REWARDXML_H_
#define _REWARDXML_H_

// STL:
#include <map>
#include <iostream>

// Boost:
#include <boost/property_tree/ptree.hpp>

namespace malmo
{
    class TimestampedReward;

    class RewardXML {
    public:

        RewardXML() {}
        RewardXML(std::string xml_text);

        std::string toXml() const;

        void parse_rewards(std::string xml_text);
        void parse_rewards(boost::property_tree::ptree& reward);
        void add_rewards(boost::property_tree::ptree& reward) const;

        size_t size() const {
            return reward_values.size();
        }

        friend class TimestampedReward;
        friend std::ostream& operator<<(std::ostream& os, const TimestampedReward& tsf);

    private:
        std::map<int, double> reward_values;
    };
}

#endif
