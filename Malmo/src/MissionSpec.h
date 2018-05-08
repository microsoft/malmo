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

#ifndef _MISSIONSPEC_H_
#define _MISSIONSPEC_H_

// Boost:
#include <boost/property_tree/ptree.hpp>

// STL:
#include <string>
#include <vector>

// Local:
#include "Logger.h"

namespace malmo
{
    //! Specifies a mission to be run.
    class MissionSpec
    {
        MALMO_LOGGABLE_OBJECT(MissionSpec)
        public:

            //! Constructs a mission with default parameters: a flat world with a 10 seconds time limit and continuous movement.
            MissionSpec();

            //! Constructs a mission from the supplied XML as specified here: <a href="../Schemas/Mission.html">Schemas/Mission.html</a>
            //! \param xml The full XML of the mission.
            //! \param validate If true, then throws an xml_schema::exception if the XML is not compliant with the schema.
            MissionSpec(const std::string& xml, bool validate);

            //! Gets the mission specification as an XML string. Only use if you want to save the mission to file.
            //! \param prettyPrint If true, add indentation and newlines to the XML to make it more readable.
            //! \returns The mission specification as an XML string.
            std::string getAsXML( bool prettyPrint ) const;

            // -------------------- settings for the server -------------------------
            
            //! Sets the summary description of the mission.
            //! \param summary The string describing the mission. Shorter strings display best.
            void setSummary( const std::string& summary );

            //! Sets the time limit for the mission.
            //! \param s The time limit in seconds.
            void timeLimitInSeconds(float s);
            
            //! Instead of the default flat world, make a world using Minecraft's terrain generator.
            //! Calling this will reset the world seed and forceReset flag - see setWorldSeed() and forceWorldReset().
            void createDefaultTerrain();

            //! Set the seed used for Minecraft's terrain generation.
            //! Call this after the world generator has been set (eg after calling createDefaultTerrain() ).
            void setWorldSeed(const std::string& seed);

            //! Force Minecraft to reload the world rather than use the current one (if appropriate).
            //! Call this after the world generator has been set (eg after calling createDefaultTerrain() ).
            void forceWorldReset();
            
            //! Sets the time of day for the start of the mission.
            //! \param t The time of day, in Minecraft ticks (thousandths of an hour since dawn).
            //! eg. 0 = Dawn, 6000 = Noon, 12000 = Sunset, 18000 = Midnight.
            //! \param allowTimeToPass If false then the sun does not move. 
            void setTimeOfDay(int t,bool allowTimeToPass);

            //! Draw a Minecraft block in the world.
            //! \param x The east-west location.
            //! \param y The up-down location.
            //! \param z The north-south location.
            //! \param blockType A string corresponding to one of the Minecraft block types.
            void drawBlock(int x, int y, int z, const std::string& blockType);
            
            //! Draw a solid cuboid in the world.
            //! \param x1 The west-most location.
            //! \param y1 The down-most location.
            //! \param z1 The north-most location.
            //! \param x2 The east-most location.
            //! \param y2 The up-most location.
            //! \param z2 The south-most location.
            //! \param blockType A string corresponding to one of the Minecraft block types.
            void drawCuboid(int x1, int y1, int z1, int x2, int y2, int z2, const std::string& blockType);

            //! Draw a Minecraft item in the world.
            //! \param x The east-west location.
            //! \param y The up-down location.
            //! \param z The north-south location.
            //! \param itemType A string corresponding to one of the Minecraft item types.
            void drawItem(int x, int y, int z, const std::string& itemType);

            //! Draw a solid sphere of blocks in the world.
            //! \param x The east-west location of the center.
            //! \param y The up-down location of the center.
            //! \param z The north-south location of the center.
            //! \param radius The radius of the sphere.
            //! \param blockType A string corresponding to one of the Minecraft block types.
            void drawSphere(int x, int y, int z, int radius, const std::string& blockType);

            //! Draw a line of blocks in the world.
            //! \param x1 The east-west location of the first end.
            //! \param y1 The up-down location of the first end.
            //! \param z1 The north-south location of the first end.
            //! \param x2 The east-west location of the second end.
            //! \param y2 The up-down location of the second end.
            //! \param z2 The north-south location of the second end.
            //! \param blockType A string corresponding to one of the Minecraft block types.
            void drawLine(int x1, int y1, int z1, int x2, int y2, int z2, const std::string& blockType);
            
            // -------------------- settings for the agents -------------------------

            //! Sets the start location for the agent. Only supports single agent missions.
            //! Integer coordinates are at the corners of blocks, so to start in the center of a block, use e.g. 4.5 instead of 4.0.
            //! \param x The east-west location.
            //! \param y The up-down location.
            //! \param z The north-south location.
            void startAt(float x, float y, float z);

            //! Sets the start location and angles for the agent. Only supports single agent missions.
            //! Integer coordinates are at the corners of blocks, so to start in the center of a block, use e.g. 4.5 instead of 4.0.
            //! \param x The east-west location.
            //! \param y The up-down location.
            //! \param z The north-south location.
            //! \param yaw The yaw in degrees (180 = north, 270 = east, 0 = south, 90 = west)
            //! \param pitch The pitch in degrees (-90 = straight up, 90 = straight down, 0 = horizontal)
            void startAtWithPitchAndYaw(float x, float y, float z, float pitch, float yaw);

            //! Sets the end location for the agent. Only supports single agent missions.
            //! Can be called more than once if there are multiple positions that end the mission for this agent.
            //! Integer coordinates are at the corners of blocks, so to end in the center of a block, use e.g. 4.5 instead of 4.0.
            //! \param x The east-west location.
            //! \param y The up-down location.
            //! \param z The north-south location.
            //! \param tolerance The radius that the agent must be within. Euclidean distance.
            void endAt(float x, float y, float z, float tolerance);
            
            //! Sets the player mode for the agent to creative, allowing them to fly and to not sustain damage. Only supports single agent missions.
            void setModeToCreative();
            
            //! Sets the player mode for the agent to spectator, allowing them to fly and pass through objects. Only supports single agent missions.
            void setModeToSpectator();
            
            //! Asks for image data to be sent from Minecraft for the agent. Only supports single agent missions.
            //! Data will be delivered in a TimestampedVideoFrame structure as RGBRGBRGB...
            //! The default camera viewpoint will be used (first-person view) - use setViewpoint to change this.
            //! \param width The width of the image in pixels. Ensure this is divisible by 4.
            //! \param height The height of the image in pixels. Ensure this is divisible by 2.
            void requestVideo(int width, int height);

            //! Asks for 8bpp greyscale image data to be sent from Minecraft for the agent. Only supports single agent missions.
            //! Data will be delivered in a TimestampedVideoFrame structure as LLLL...
            //! The default camera viewpoint will be used (first-person view) - use setViewpoint to change this.
            //! \param width The width of the image in pixels. Ensure this is divisible by 4.
            //! \param height The height of the image in pixels. Ensure this is divisible by 2.
            void requestLuminance(int width, int height);

            //! Asks for 24bpp colourmap image data to be sent from Minecraft for the agent. Only supports single agent missions.
            //! Data will be delivered in a TimestampedVideoFrame structure as RGBRGB...
            //! The default camera viewpoint will be used (first-person view) - use setViewpoint to change this.
            //! \param width The width of the image in pixels. Ensure this is divisible by 4.
            //! \param height The height of the image in pixels. Ensure this is divisible by 2.
            void requestColourMap(int width, int height);

            //! Asks for 32bpp depth data to be sent from Minecraft for the agent. Only supports single agent missions.
            //! Data will be delivered in a TimestampedVideoFrame structure as an array of floats.
            //! The default camera viewpoint will be used (first-person view) - use setViewpoint to change this.
            //! \param width The width of the image in pixels. Ensure this is divisible by 4.
            //! \param height The height of the image in pixels. Ensure this is divisible by 2.
            void request32bppDepth(int width, int height);

            //! Asks for image data and depth data to be sent from Minecraft for the agent. Only supports single agent missions.
            //! Data will be delivered in a TimestampedVideoFrame structure as RGBDRGBDRGBD...
            //! If saving the video to file only the depth will be recorded, as greyscale.
            //! \param width The width of the image in pixels. Ensure this is divisible by 4.
            //! \param height The height of the image in pixels. Ensure this is divisible by 2.
            void requestVideoWithDepth(int width, int height);

            //! Sets the camera position. Modifies the existing video request, so call this after requestVideo or requestVideoWithDepth.
            //! \param viewpoint The camera position to use. 0 = first person, 1 = behind, 2 = facing.
            void setViewpoint(int viewpoint);

            //! Asks for a reward to be sent to the agent when it reaches a certain position. Only supports single agent missions.
            //! Integer coordinates are at the corners of blocks, so for rewards in the center of a block, use e.g. 4.5 instead of 4.0.
            //! \param x The east-west location.
            //! \param y The up-down location.
            //! \param z The north-south location.
            //! \param amount The reward value to send.
            //! \param tolerance The radius that the agent must be within to receive the reward. Euclidean distance.
            void rewardForReachingPosition(float x, float y, float z, float amount, float tolerance);

            //! Asks for the list of commands acted upon since the last timestep to be returned in the observations. Only supports single agent missions.
            //! The commands are returned in a JSON entry called 'CommandsSinceLastObservation'.
            //! Documentation link: <a href="../Schemas/MissionHandlers.html#element_ObservationFromRecentCommands">Schemas/MissionHandlers.html</a>
            void observeRecentCommands();

            //! Asks for the contents of the player's hot-bar to be included in the observations. Only supports single agent missions.
            //! The commands are returned in JSON entries 'Hotbar_0_size', 'Hotbar_0_item', etc.
            //! Documentation link: <a href="../Schemas/MissionHandlers.html#element_ObservationFromHotBar">Schemas/MissionHandlers.html</a>
            void observeHotBar();

            //! Asks for the full item inventory of the player to be included in the observations. Only supports single agent missions.
            //! The commands are returned in JSON entries 'Inventory_0_size', 'Inventory_0_item', etc.
            //! Documentation link: <a href="../Schemas/MissionHandlers.html#element_ObservationFromFullInventory">Schemas/MissionHandlers.html</a>
            void observeFullInventory();
            
            //! Asks for observations of the block types within a cuboid relative to the agent's position. Only supports single agent missions.
            //! The commands are returned in a JSON entry 'Cells'.
            //! Documentation link: <a href="../Schemas/MissionHandlers.html#element_ObservationFromGrid">Schemas/MissionHandlers.html</a>
            //! \param x1 The west-most location.
            //! \param y1 The down-most location.
            //! \param z1 The north-most location.
            //! \param x2 The east-most location.
            //! \param y2 The up-most location.
            //! \param z2 The south-most location.
            //! \param name An name to identify the JSON array that will be returned.
            void observeGrid(int x1,int y1,int z1,int x2,int y2,int z2,const std::string& name);
            
            //! Asks for the Euclidean distance to a location to be included in the observations. Only supports single agent missions.
            //! Integer coordinates are at the corners of blocks, so for distances from the center of a block, use e.g. 4.5 instead of 4.0.
            //! The commands are returned in a JSON element 'distanceFromNAME', where NAME is replaced with the name of the point.
            //! Documentation link: <a href="../Schemas/MissionHandlers.html#element_ObservationFromDistance">Schemas/MissionHandlers.html</a>
            //! \param x The east-west location.
            //! \param y The up-down location.
            //! \param z The north-south location.
            //! \param name A label for this observation. The observation will be called "distanceFrom<name>".
            void observeDistance(float x,float y,float z,const std::string& name);
            
            //! Asks for chat messages to be included in the observations.
            void observeChat();
            
            // -------------------- settings for the agents : command handlers -------------------------

            //! Remove any existing command handlers from the mission specification. Use with other functions to add exactly the command handlers you want.
            //! Only applies to the first agent in the mission. For multi-agent missions, specify the command handlers for each in the XML.
            void removeAllCommandHandlers();
            
            //! Adds a continuous movement command handler if none present, with neither an allow-list or a deny-list, thus allowing any command to be sent.
            //! Only applies to the first agent in the mission. For multi-agent missions, specify the command handlers for each in the XML.
            void allowAllContinuousMovementCommands();
            
            //! Adds an allow-list to the continuous movement command handler if none present. Adds the specified verb to the allow-list.
            //! Note that when an allow-list is present, only the commands listed are allowed.
            //! Adds a continuous movement command handler if none present. Removes the deny-list from the continuous movement command handler if present.
            //! Only applies to the first agent in the mission. For multi-agent missions, specify the command handlers for each in the XML.
            //! \param verb The command verb, e.g. "move".
            void allowContinuousMovementCommand(const std::string& verb);
            
            //! Adds a discrete movement command handler if none present, with neither an allow-list or a deny-list, thus allowing any command to be sent.
            //! Only applies to the first agent in the mission. For multi-agent missions, specify the command handlers for each in the XML.
            void allowAllDiscreteMovementCommands();
            
            //! Adds an allow-list to the discrete movement command handler if none present. Adds the specified verb to the allow-list.
            //! Note that when an allow-list is present, only the commands listed are allowed.
            //! Adds a discrete movement command handler if none present. Removes the deny-list from the discrete movement command handler if present.
            //! Only applies to the first agent in the mission. For multi-agent missions, specify the command handlers for each in the XML.
            //! \param verb The command verb, e.g. "movenorth".
            void allowDiscreteMovementCommand(const std::string& verb);
            
            //! Adds an absolute movement command handler if none present, with neither an allow-list or a deny-list, thus allowing any command to be sent.
            //! Only applies to the first agent in the mission. For multi-agent missions, specify the command handlers for each in the XML.
            void allowAllAbsoluteMovementCommands();
            
            //! Adds an allow-list to the absolute movement command handler if none present. Adds the specified verb to the allow-list.
            //! Note that when an allow-list is present, only the commands listed are allowed.
            //! Adds an absolute movement command handler if none present. Removes the deny-list from the absolute movement command handler if present.
            //! Only applies to the first agent in the mission. For multi-agent missions, specify the command handlers for each in the XML.
            //! \param verb The command verb, e.g. "tpx".
            void allowAbsoluteMovementCommand(const std::string& verb);
            
            //! Adds an inventory command handler if none present, with neither an allow-list or a deny-list, thus allowing any command to be sent.
            //! Only applies to the first agent in the mission. For multi-agent missions, specify the command handlers for each in the XML.
            void allowAllInventoryCommands();
            
            //! Adds an allow-list to the inventory command handler if none present. Adds the specified verb to the allow-list.
            //! Note that when an allow-list is present, only the commands listed are allowed.
            //! Adds an inventory command handler if none present. Removes the deny-list from the inventory command handler if present.
            //! Only applies to the first agent in the mission. For multi-agent missions, specify the command handlers for each in the XML.
            //! \param verb The command verb, e.g. "selectInventoryItem".
            void allowInventoryCommand(const std::string& verb);
            
            //! Adds a chat command handler if none present, with neither an allow-list or a deny-list, thus allowing any command to be sent.
            //! Only applies to the first agent in the mission. For multi-agent missions, specify the command handlers for each in the XML.
            void allowAllChatCommands();
            
            // ------------------------- information --------------------------------------
            
            //! Returns the short description of the mission.
            //! \returns A string containing the summary.
            std::string getSummary() const;
            
            //! Returns the number of agents involved in this mission.
            //! \returns The number of agents.
            int getNumberOfAgents() const;

            //! Gets whether video has been requested for one of the agents involved in this mission.
            //! \param role The agent index. Zero based.
            //! \returns True if video was requested.
            bool isVideoRequested(int role) const;

            //! Gets whether depthmap video has been requested for one of the agents involved in this mission.
            //! \param role The agent index. Zero based.
            //! \returns True if depthmap video was requested.
            bool isDepthRequested(int role) const;

            //! Gets whether luminance video has been requested for one of the agents involved in this mission.
            //! \param role The agent index. Zero based.
            //! \returns True if luminance video was requested.
            bool isLuminanceRequested(int role) const;

            //! Gets whether colourmap video has been requested for one of the agents involved in this mission.
            //! \param role The agent index. Zero based.
            //! \returns True if colourmap video was requested.
            bool isColourMapRequested(int role) const;

            //! Returns the width of the requested video for one of the agents involved in this mission.
            //! \param role The agent index. Zero based.
            //! \returns The width of the video in pixels.
            int getVideoWidth(int role) const;
            
            //! Returns the height of the requested video for one of the agents involved in this mission.
            //! \param role The agent index. Zero based.
            //! \returns The height of the video in pixels.
            int getVideoHeight(int role) const;
            
            //! Returns the number of channels in the requested video for one of the agents involved in this mission.
            //! \param role The agent index. Zero based.
            //! \returns The number of channels in the requested video: 3 for RGB, 4 for RGBD.
            int getVideoChannels(int role) const;

            //! Returns a list of the names of the active command handlers for one of the agents involved in this mission.
            //! \param role The agent index. Zero based.
            //! \returns The list of command handler names: 'ContinuousMovement', 'DiscreteMovement', 'Chat', 'Inventory' etc.
            std::vector<std::string> getListOfCommandHandlers(int role) const;

            //! Returns a list of the names of the allowed commands for one of the agents involved in this mission.
            //! \param role The agent index. Zero based.
            //! \param command_handler The name of the command handler, as returned by getListOfCommandHandlers().
            //! \returns The list of allowed commands: 'move', 'turn', 'attack' etc.
            std::vector<std::string> getAllowedCommands(int role,const std::string& command_handler) const;

            //! Count the number of children with the given child name or -1 if no such element path is present.
            //! Useful for quick litmus tests on mission XML documents.
            //! \param elementPath The element's path.
            //! \param childName The name of child elements.
            //! \returns The count of child elements with given name or -1 if no such element path is present.
            int getChildCount(const std::string& element, const std::string& childName) const;

            friend std::ostream& operator<<(std::ostream& os, const MissionSpec& ms);
            friend class MissionInitSpec;

            static const std::string XMLNS_XSI;
            static const std::string MALMO_NAMESPACE;
        private:
        
            boost::optional<int> getRoleValue(int role, std::string videoType, char what) const;
            void addVerbToCommandType(std::string verb, std::string commandType);
            void worldGeneratorReset();

            boost::property_tree::ptree& getDrawingDecorator();

            boost::property_tree::ptree mission;

            static const std::vector<std::string> all_continuous_movement_commands;
            static const std::vector<std::string> all_absolute_movement_commands;
            static const std::vector<std::string> all_discrete_movement_commands;
            static const std::vector<std::string> all_inventory_commands;
            static const std::vector<std::string> all_simplecraft_commands;
            static const std::vector<std::string> all_chat_commands;
            static const std::vector<std::string> all_mission_quit_commands;
            static const std::vector<std::string> all_human_level_commands;
    };
}

#endif
