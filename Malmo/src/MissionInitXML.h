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

#ifndef _MISSIONINITXML_H_
#define _MISSIONINITXML_H_

// STL:
#include <string>

// Boost:
#include <boost/property_tree/ptree.hpp>

namespace malmo {

    class MissionInitXML {
    public:
        MissionInitXML();
        MissionInitXML(std::string xml_text);

        void parse(std::string xml_text);
        
        std::string toXml() const;

        struct MinecraftServer {

            boost::optional<std::string> connection_address;
            boost::optional<int> connection_port;
        };

        struct ClientAgentConnection {

            ClientAgentConnection() {
                client_mission_control_port = 0;
                client_commands_port = 0;
                agent_mission_control_port = 0;
                agent_video_port = agent_depth_port = agent_lumunance_port = 0;
                agent_observations_port = agent_rewards_port =  0;
                agent_colour_map_port = 0;
            }

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
        int client_role;

        MinecraftServer minecraft_server;
        ClientAgentConnection client_agent_connection;
    };
}
#endif