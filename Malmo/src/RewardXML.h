#ifndef _REWARD_H_
#define _REWARD_H_

// STL:
#include <cstdlib>

// Boost:
#include <boost/thread.hpp>
#include <boost/property_tree/ptree.hpp>

namespace malmo
{
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
