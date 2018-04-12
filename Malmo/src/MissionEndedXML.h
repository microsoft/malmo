#ifndef _MISSIONENDEDXML_H_
#define _MISSIONENDEDXML_H_

// STL:
#include <string>

// Boost:
#include <boost/property_tree/ptree.hpp>

#include "RewardXML.h"

namespace malmo {

    class MissionEndedXML {
    public:

        // MissionResult values
        static constexpr const char* ENDED = "ENDED";
        static constexpr const char* PLAYER_DIED = "PLAYER_DIED";
        static constexpr const char* AGENT_QUIT = "AGENT_QUIT";
        static constexpr const char* MOD_FAILED_TO_INSTANTIATE_HANDLERS = "MOD_FAILED_TO_INSTANTIATE_HANDLERS";
        static constexpr const char* MOD_HAS_NO_WORLD_LOADED = "MOD_HAS_NO_WORLD_LOADED";
        static constexpr const char* MOD_FAILED_TO_CREATE_WORLD = "MOD_FAILED_TO_CREATE_WORLD";
        static constexpr const char* MOD_HAS_NO_AGENT_AVAILABLE = "MOD_HAS_NO_AGENT_AVAILABLE";
        static constexpr const char* MOD_SERVER_UNREACHABLE = "MOD_SERVER_UNREACHABLE";
        static constexpr const char* MOD_SERVER_ABORTED_MISSION = "MOD_SERVER_ABORTED_MISSION";
        static constexpr const char* MOD_CONNECTION_FAILED = "MOD_CONNECTION_FAILED";
        static constexpr const char* MOD_CRASHED = "MOD_CRASHED";
        // MissionResult values end

        MissionEndedXML(std::string xml_text);
        std::string toXml() const;

        struct VideoDataAttributes {
            std::string frame_type;
            int frames_sent;
            boost::optional<int> frames_received;
            boost::optional<int> frames_written;
        };

        const std::string& getStatus() { return status; }

        const std::string& getHumanReadableStatus() { return human_readable_status;  }

        const RewardXML& getReward() { return reward;  }

        std::vector<VideoDataAttributes>& videoDataAttributes() { return video_data_attributes;  }

    private:
        boost::optional<std::string> schema_version;
        std::string status;
        std::string human_readable_status;
        bool have_rewards;
        RewardXML  reward;
        std::vector<VideoDataAttributes> video_data_attributes;
    };
}
#endif