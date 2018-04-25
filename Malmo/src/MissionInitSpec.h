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

#ifndef _MISSIONINITSPEC_H_
#define _MISSIONINITSPEC_H_

// Local:
#include "ClientInfo.h"
#include "MissionSpec.h"
#include "Logger.h"
#include "MissionInitXML.h"

namespace malmo
{
    //! Specifies a mission to be run together with the IP addresses and ports of the agents and Mod instances to run it on.
    class MissionInitSpec
    {
        MALMO_LOGGABLE_OBJECT(MissionInitSpec)

    public:

        //! Constructs a mission init specification with default settings from the supplied mission specification.
        //! \param mission_spec The specification of the mission to run.
        //! \param unique_experiment_id An arbitrary identifier that is used to disambiguate our mission from others using the same ClientPool.
        //! \param role Index of the agent that this agent host is to manage. Zero-based index. Use zero if there is only one agent in this mission.
        MissionInitSpec(const MissionSpec& mission_spec, std::string unique_experiment_id, int role);

        //! Constructs a mission init from the supplied XML.
        //! \param xml The full XML of the mission init.
        //! \param validate If true, then throws an xml_schema::exception if the XML is not compliant with the schema.
        MissionInitSpec(const std::string& xml, bool validate);

        //! Gets the mission init specification as an XML string. Only use if you want to save the mission init to file.
        //! \param prettyPrint If true, add indentation and newlines to the XML to make it more readable.
        //! \returns The mission init specification as an XML string.
        std::string getAsXML(bool prettyPrint) const;

        //! Gets the unique experiment ID.
        //! \returns The experiment ID.
        std::string getExperimentID() const;

        //! Gets the IP address of the client.
        //! \returns The IP address as a string.
        std::string getClientAddress() const;

        //! Sets the IP address of the client.
        //! \param address The IP address as a string.
        void setClientAddress(std::string address);

        //! Gets the mission control port of the client.
        //! \returns The port that the client listens to mission control messages on.
        int getClientMissionControlPort() const;

        //! Sets the mission control port of the client.
        //! \param port The port that the client listens to mission control messages on.
        void setClientMissionControlPort(int port);

        //! Gets the commands port of the client.
        //! \returns The port that the client listens to commands on.
        int getClientCommandsPort() const;

        //! Sets the commands port of the client.
        //! \param port The port that the client listens to commands on.
        void setClientCommandsPort(int port);

        //! Gets the IP address of the agent.
        //! \returns The IP address as a string.
        std::string getAgentAddress() const;

        //! Sets the IP address of the agent.
        //! \param address The IP address as a string.
        void setAgentAddress(std::string address);

        //! Gets the mission control port of the agent.
        //! \returns The port that the agent listens to mission control messages on.
        int getAgentMissionControlPort() const;

        //! Sets the mission control port of the agent.
        //! Optional. The default behavior is to assign a port that is free.
        //! \param port The port that the agent listens to mission control messages on.
        void setAgentMissionControlPort(int port);

        //! Gets the video port of the agent.
        //! \returns The port that the agent listens to video frames on.
        int getAgentVideoPort() const;

        //! Gets the depth port of the agent.
        //! \returns The port that the agent listens to depthmap video frames on.
        int getAgentDepthPort() const;

        //! Gets the luminance port of the agent.
        //! \returns The port that the agent listens to luminance video frames on.
        int getAgentLuminancePort() const;

        //! Gets the colourmap port of the agent.
        //! \returns The port that the agent listens to colourmap video frames on.
        int getAgentColourMapPort() const;

        //! Sets the video port of the agent.
        //! Optional. The default behavior is to assign a port that is free.
        //! \param port The port that the agent listens to video frames on.
        void setAgentVideoPort(int port);

        //! Sets the depth port of the agent.
        //! Optional. The default behavior is to assign a port that is free.
        //! \param port The port that the agent listens to depthmap video frames on.
        void setAgentDepthPort(int port);

        //! Sets the luminance port of the agent.
        //! Optional. The default behavior is to assign a port that is free.
        //! \param port The port that the agent listens to luminance video frames on.
        void setAgentLuminancePort(int port);

        //! Sets the colourmap port of the agent.
        //! Optional. The default behavior is to assign a port that is free.
        //! \param port The port that the agent listens to colourmap video frames on.
        void setAgentColourMapPort(int port);

        //! Gets the observations port of the agent.
        //! \returns The port that the agent listens to observations on.
        int getAgentObservationsPort() const;

        //! Sets the observations port of the agent.
        //! Optional. The default behavior is to assign a port that is free.
        //! \param port The port that the agent listens to observations on.
        void setAgentObservationsPort(int port);

        //! Gets the rewards port of the agent.
        //! \returns The port that the agent listens to rewards on.
        int getAgentRewardsPort() const;

        //! Sets the rewards port of the agent.
        //! Optional. The default behavior is to assign a port that is free.
        //! \param port The port that the agent listens to rewards on.
        void setAgentRewardsPort(int port);

        //! Gets whether the Minecraft server port is known.
        //! \returns True if the Minecraft server port is known.
        bool hasMinecraftServerInformation() const;

        //! Sets the Minecraft server information.
        //! \param address The Minecraft server address.
        //! \param port The Minecraft server port.
        void setMinecraftServerInformation(const std::string& address, int port);

    private:
        MissionInitXML mission_init;
    };
}

#endif
