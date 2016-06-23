// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

// Local:
#include "FindSchemaFile.h"
#include "MissionInitSpec.h"

// Boost:
#include <boost/make_shared.hpp>

// Schemas:
using namespace malmo::schemas;

// STL:
#include <sstream>
using namespace std;

namespace malmo
{
    MissionInitSpec::MissionInitSpec( const MissionSpec& mission_spec, std::string unique_experiment_id, int role )
    {
        // construct a default MissionInit using the provided MissionSpec
        const string client_IP_address = "127.0.0.1";
        const int client_commands_port = 0;
        const string agent_IP_address = "127.0.0.1";
        const int agent_mission_control_port = 0;
        const int agent_video_port = 0;
        const int agent_observations_port = 0;
        const int agent_rewards_port = 0;
        ClientAgentConnection cac(
              client_IP_address 
            , default_client_mission_control_port
            , client_commands_port
            , agent_IP_address
            , agent_mission_control_port
            , agent_video_port
            , agent_observations_port
            , agent_rewards_port
            );
        this->mission_init = boost::make_shared<MissionInit>(
              *mission_spec.mission
            , unique_experiment_id
            , role
            , cac
            );
    }

    MissionInitSpec::MissionInitSpec(const std::string& xml, bool validate)
    {
        xml_schema::properties props;
        props.schema_location(xml_namespace, FindSchemaFile("MissionInit.xsd"));

        xml_schema::flags flags = 0;
        if( !validate )
            flags = flags | xml_schema::flags::dont_validate;

        istringstream iss(xml);
        this->mission_init = MissionInit_(iss, flags, props);
    }

    std::string MissionInitSpec::getAsXML( bool prettyPrint ) const
    { 
        std::ostringstream oss;
        
        xml_schema::namespace_infomap map;
        map[""].name = xml_namespace;
        map[""].schema = "MissionInit.xsd";
        
        xml_schema::flags flags = 0;
        if( !prettyPrint )
            flags = flags | xml_schema::flags::dont_pretty_print;

        MissionInit_( oss, *this->mission_init, map, "UTF-8", flags );
        
        return oss.str();
    }
    
    std::string MissionInitSpec::getClientAddress() const
    {
        return this->mission_init->ClientAgentConnection().ClientIPAddress();
    }
    
    void MissionInitSpec::setClientAddress(std::string address)
    {
        this->mission_init->ClientAgentConnection().ClientIPAddress() = address;
    }
    
    int MissionInitSpec::getClientMissionControlPort() const
    {
        return this->mission_init->ClientAgentConnection().ClientMissionControlPort();
    }
    
    void MissionInitSpec::setClientMissionControlPort(int port)
    {
        this->mission_init->ClientAgentConnection().ClientMissionControlPort() = port;
    }
    
    int MissionInitSpec::getClientCommandsPort() const
    {
        return this->mission_init->ClientAgentConnection().ClientCommandsPort();
    }
    
    void MissionInitSpec::setClientCommandsPort(int port)
    {
        this->mission_init->ClientAgentConnection().ClientCommandsPort() = port;
    }
    
    std::string MissionInitSpec::getAgentAddress() const
    {
        return this->mission_init->ClientAgentConnection().AgentIPAddress();
    }
    
    void MissionInitSpec::setAgentAddress(std::string address)
    {
        this->mission_init->ClientAgentConnection().AgentIPAddress() = address;
    }
    
    int MissionInitSpec::getAgentMissionControlPort() const
    {
        return this->mission_init->ClientAgentConnection().AgentMissionControlPort();
    }
    
    void MissionInitSpec::setAgentMissionControlPort(int port)
    {
        this->mission_init->ClientAgentConnection().AgentMissionControlPort() = port;
    }
    
    int MissionInitSpec::getAgentVideoPort() const
    {
        return this->mission_init->ClientAgentConnection().AgentVideoPort();
    }
    
    void MissionInitSpec::setAgentVideoPort(int port)
    {
        this->mission_init->ClientAgentConnection().AgentVideoPort() = port;
    }
    
    int MissionInitSpec::getAgentObservationsPort() const
    {
        return this->mission_init->ClientAgentConnection().AgentObservationsPort();
    }
    
    void MissionInitSpec::setAgentObservationsPort(int port)
    {
        this->mission_init->ClientAgentConnection().AgentObservationsPort() = port;
    }
    
    int MissionInitSpec::getAgentRewardsPort() const
    {
        return this->mission_init->ClientAgentConnection().AgentRewardsPort();
    }
    
    void MissionInitSpec::setAgentRewardsPort(int port)
    {
        this->mission_init->ClientAgentConnection().AgentRewardsPort() = port;
    }
    
    bool MissionInitSpec::hasMinecraftServerInformation() const
    {
        return this->mission_init->MinecraftServerConnection().present();
    }

    void MissionInitSpec::setMinecraftServerInformation(const std::string& address, int port)
    {
        this->mission_init->MinecraftServerConnection() = MinecraftServerConnection( address, port );
    }
}
