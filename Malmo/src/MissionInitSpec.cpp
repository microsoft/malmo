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

// Local:
#include "MissionInitSpec.h"

// Boost:
#include <boost/preprocessor/stringize.hpp>
#include <boost/property_tree/xml_parser.hpp>
#include <boost/algorithm/string.hpp>

// STL:
#include <sstream>
using namespace std;

namespace malmo
{
    MissionInitSpec::MissionInitSpec( const MissionSpec& mission_spec, std::string unique_experiment_id, int role )
    {
        // construct a default MissionInit using the provided MissionSpec

        this->mission_init.client_agent_connection.client_ip_address = "127.0.0.1";
        this->mission_init.client_agent_connection.client_mission_control_port = default_client_mission_control_port;
        this->mission_init.client_agent_connection.client_commands_port = 0;
        this->mission_init.client_agent_connection.agent_ip_address = "127.0.0.1";
        this->mission_init.client_agent_connection.agent_mission_control_port = 0;
        this->mission_init.client_agent_connection.agent_video_port = 0;
        this->mission_init.client_agent_connection.agent_depth_port = 0;
        this->mission_init.client_agent_connection.agent_colour_map_port = 0;
        this->mission_init.client_agent_connection.agent_lumunance_port = 0;
        this->mission_init.client_agent_connection.agent_observations_port = 0;
        this->mission_init.client_agent_connection.agent_rewards_port = 0;

        this->mission_init.client_role = role;
        this->mission_init.experiment_uid = unique_experiment_id;
        this->mission_init.mission = mission_spec.mission.get_child("Mission");
        this->mission_init.mission.erase("<xmlattr>");

        mission_init.platform_version = BOOST_PP_STRINGIZE(MALMO_VERSION);
    }

    MissionInitSpec::MissionInitSpec(const std::string& xml, bool validate)
    {
        mission_init.parse(xml);
    }

    std::string MissionInitSpec::getAsXML( bool prettyPrint ) const
    { 
        return mission_init.toXml();
    }

    std::string MissionInitSpec::getExperimentID() const
    {
        return this->mission_init.experiment_uid;
    }

    std::string MissionInitSpec::getClientAddress() const
    {
        return this->mission_init.client_agent_connection.client_ip_address;
    }
    
    void MissionInitSpec::setClientAddress(std::string address)
    {
        boost::algorithm::trim(address);
        this->mission_init.client_agent_connection.client_ip_address = address;
    }
    
    int MissionInitSpec::getClientMissionControlPort() const
    {
        return this->mission_init.client_agent_connection.client_mission_control_port;
    }
    
    void MissionInitSpec::setClientMissionControlPort(int port)
    {
        this->mission_init.client_agent_connection.client_mission_control_port = port;
    }
    
    int MissionInitSpec::getClientCommandsPort() const
    {
        return this->mission_init.client_agent_connection.client_commands_port;
    }
    
    void MissionInitSpec::setClientCommandsPort(int port)
    {
        this->mission_init.client_agent_connection.client_commands_port = port;
    }
    
    std::string MissionInitSpec::getAgentAddress() const
    {
        return this->mission_init.client_agent_connection.agent_ip_address;
    }
    
    void MissionInitSpec::setAgentAddress(std::string address)
    {
        boost::algorithm::trim(address);
        this->mission_init.client_agent_connection.agent_ip_address = address;
    }
    
    int MissionInitSpec::getAgentMissionControlPort() const
    {
        return this->mission_init.client_agent_connection.agent_mission_control_port;
    }
    
    void MissionInitSpec::setAgentMissionControlPort(int port)
    {
        this->mission_init.client_agent_connection.agent_mission_control_port = port;
    }
    
    int MissionInitSpec::getAgentVideoPort() const
    {
        return this->mission_init.client_agent_connection.agent_video_port;
    }
    
    int MissionInitSpec::getAgentDepthPort() const
    {
        return this->mission_init.client_agent_connection.agent_depth_port;
    }

    int MissionInitSpec::getAgentLuminancePort() const
    {
        return this->mission_init.client_agent_connection.agent_lumunance_port;
    }

    int MissionInitSpec::getAgentColourMapPort() const
    {
        return this->mission_init.client_agent_connection.agent_colour_map_port;
    }

    void MissionInitSpec::setAgentVideoPort(int port)
    {
        this->mission_init.client_agent_connection.agent_video_port = port;
    }
    
    void MissionInitSpec::setAgentDepthPort(int port)
    {
        this->mission_init.client_agent_connection.agent_depth_port = port;
    }

    void MissionInitSpec::setAgentLuminancePort(int port)
    {
        this->mission_init.client_agent_connection.agent_lumunance_port = port;
    }

    void MissionInitSpec::setAgentColourMapPort(int port)
    {
        this->mission_init.client_agent_connection.agent_colour_map_port = port;
    }

    int MissionInitSpec::getAgentObservationsPort() const
    {
        return this->mission_init.client_agent_connection.agent_observations_port;
    }
    
    void MissionInitSpec::setAgentObservationsPort(int port)
    {
        this->mission_init.client_agent_connection.agent_observations_port = port;
    }
    
    int MissionInitSpec::getAgentRewardsPort() const
    {
        return this->mission_init.client_agent_connection.agent_rewards_port;
    }
    
    void MissionInitSpec::setAgentRewardsPort(int port)
    {
        this->mission_init.client_agent_connection.agent_rewards_port = port;
    }
    
    bool MissionInitSpec::hasMinecraftServerInformation() const
    {
        return this->mission_init.minecraft_server.connection_address || this->mission_init.minecraft_server.connection_port;
    }

    void MissionInitSpec::setMinecraftServerInformation(const std::string& address, int port)
    {
        this->mission_init.minecraft_server.connection_address = boost::algorithm::trim_copy(address);
        this->mission_init.minecraft_server.connection_port = port;
    }
}
