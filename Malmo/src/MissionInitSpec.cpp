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
#include "FindSchemaFile.h"
#include "MissionInitSpec.h"
#include "Init.h"

// Boost:
#include <boost/make_shared.hpp>
#include <boost/preprocessor/stringize.hpp>

// Schemas:
using namespace malmo::schemas;

// STL:
#include <sstream>
using namespace std;

namespace malmo
{
    MissionInitSpec::MissionInitSpec( const MissionSpec& mission_spec, std::string unique_experiment_id, int role )
    {
        initialiser::initXSD();

        // construct a default MissionInit using the provided MissionSpec
        const string client_IP_address = "127.0.0.1";
        const int client_commands_port = 0;
        const string agent_IP_address = "127.0.0.1";
        const int agent_mission_control_port = 0;
        const int agent_video_port = 0;
        const int agent_depth_port = 0;
        const int agent_luminance_port = 0;
        const int agent_colourmap_port = 0;
        const int agent_observations_port = 0;
        const int agent_rewards_port = 0;
        ClientAgentConnection cac(
              client_IP_address 
            , default_client_mission_control_port
            , client_commands_port
            , agent_IP_address
            , agent_mission_control_port
            , agent_video_port
            , agent_depth_port
            , agent_colourmap_port
            , agent_luminance_port
            , agent_observations_port
            , agent_rewards_port
            );
        this->mission_init = boost::make_shared<MissionInit>(
              *mission_spec.mission
            , unique_experiment_id
            , role
            , cac
            );
        this->mission_init->PlatformVersion(BOOST_PP_STRINGIZE(MALMO_VERSION));
    }

    MissionInitSpec::MissionInitSpec(const std::string& xml, bool validate)
    {
        initialiser::initXSD();

        xml_schema::properties props;
        props.schema_location(xml_namespace, FindSchemaFile("MissionInit.xsd"));

        xml_schema::flags flags = xml_schema::flags::dont_initialize;
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
        
        xml_schema::flags flags = xml_schema::flags::dont_initialize;
        if( !prettyPrint )
            flags = flags | xml_schema::flags::dont_pretty_print;

        MissionInit_( oss, *this->mission_init, map, "UTF-8", flags );
        
        return oss.str();
    }

    std::string MissionInitSpec::getExperimentID() const
    {
        return this->mission_init->ExperimentUID();
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
    
    int MissionInitSpec::getAgentDepthPort() const
    {
        return this->mission_init->ClientAgentConnection().AgentDepthPort();
    }

    int MissionInitSpec::getAgentLuminancePort() const
    {
        return this->mission_init->ClientAgentConnection().AgentLuminancePort();
    }

    int MissionInitSpec::getAgentColourMapPort() const
    {
        return this->mission_init->ClientAgentConnection().AgentColourMapPort();
    }

    void MissionInitSpec::setAgentVideoPort(int port)
    {
        this->mission_init->ClientAgentConnection().AgentVideoPort() = port;
    }
    
    void MissionInitSpec::setAgentDepthPort(int port)
    {
        this->mission_init->ClientAgentConnection().AgentDepthPort() = port;
    }

    void MissionInitSpec::setAgentLuminancePort(int port)
    {
        this->mission_init->ClientAgentConnection().AgentLuminancePort() = port;
    }

    void MissionInitSpec::setAgentColourMapPort(int port)
    {
        this->mission_init->ClientAgentConnection().AgentColourMapPort() = port;
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
