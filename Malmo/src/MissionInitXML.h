#ifndef _MISSIONINITXML_H_
#define _MISSIONINITXML_H_

// STL:
#include <cstdlib>

// Boost:
#include <boost/thread.hpp>
#include <boost/property_tree/ptree.hpp>

namespace malmo {

    class MissionInitXML {
    public:
        MissionInitXML() {}
        MissionInitXML(std::string xml_text);

        void parse(std::string xml_text);
        
        std::string toXml() const;

        struct MineCraftServer {
            boost::optional<std::string> connection_address;
            boost::optional<int> connection_port;
        };

        struct ClientAgentConnection {
            std::string client_ip_address;
            int client_mission_control_port;
            int client_commands_port;
            std::string agent_ip_address;
            int agent_mission_control_port;
            int agent_video_port;
            int agent_depth_port;
            int agent_lumunance_port;
            int agent_observations_port;
            int agent_rewards_port;
            int agent_colour_map_port;
        };

        friend class MissionInitSpec;

    private:
        std::string schema_version;
        std::string platform_version;

        boost::property_tree::ptree mission;

        std::string experiment_uid;

        MineCraftServer minecraft_server;

        int client_role;

        ClientAgentConnection client_agent_connection;
    };
}
#endif