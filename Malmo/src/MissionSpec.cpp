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
#include "MissionSpec.h"

// Boost:
#include <boost/make_shared.hpp>
#include <boost/property_tree/xml_parser.hpp>

// STL:
#include <iostream>
using namespace std;

namespace malmo
{
    const int MillisecondsInOneSecond = 1000;

    const std::vector<std::string> MissionSpec::all_continuous_movement_commands = { "jump", "move", "pitch", "strafe", "turn", "crouch", "attack", "use" };
    const std::vector<std::string> MissionSpec::all_absolute_movement_commands = { "tpx", "tpy", "tpz", "tp", "setYaw", "setPitch" };
    const std::vector<std::string> MissionSpec::all_discrete_movement_commands = { "move", "jumpmove", "strafe", "jumpstrafe", "turn", "movenorth", "moveeast", "movesouth", "movewest", 
        "jumpnorth", "jumpeast", "jumpsouth", "jumpwest", "jump", "look", "attack", "use", "jumpuse" };
    const std::vector<std::string> MissionSpec::all_inventory_commands = { "swapInventoryItems", "combineInventoryItems", "discardCurrentItem", 
        "hotbar.1", "hotbar.2", "hotbar.3", "hotbar.4", "hotbar.5", "hotbar.6", "hotbar.7", "hotbar.8", "hotbar.9" };
    const std::vector<std::string> MissionSpec::all_simplecraft_commands = { "craft" };
    const std::vector<std::string> MissionSpec::all_nearbycraft_commands = { "craftNearby" };
    const std::vector<std::string> MissionSpec::all_nearbysmelt_commands = { "smeltNearby" };
    const std::vector<std::string> MissionSpec::all_chat_commands = { "chat" };
    const std::vector<std::string> MissionSpec::all_mission_quit_commands = { "quit" };
    const std::vector<std::string> MissionSpec::all_human_level_commands = { "forward", "left", "right", "jump", "sneak", "sprint", "inventory", "swapHands", "drop", "use", "attack", "moveMouse", 
        "hotbar.1", "hotbar.2", "hotbar.3", "hotbar.4", "hotbar.5", "hotbar.6", "hotbar.7", "hotbar.8", "hotbar.9" };

    const std::string MissionSpec::XMLNS_XSI = "http://www.w3.org/2001/XMLSchema-instance";
    const std::string MissionSpec::MALMO_NAMESPACE = "http://ProjectMalmo.microsoft.com";

    MissionSpec::MissionSpec()
    {
        std::string defaultMission = R"(<?xml version="1.0" encoding="UTF-8" ?><Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
<About><Summary>Defaut Mission</Summary></About><ServerSection><ServerHandlers><FlatWorldGenerator generatorString="3;7,220*1,5*3,2;3;,biome_1" /><ServerQuitFromTimeUp timeLimitMs="10000"/>
<ServerQuitWhenAnyAgentFinishes/></ServerHandlers></ServerSection><AgentSection><Name>A default agent</Name><AgentStart></AgentStart><AgentHandlers><ObservationFromFullStats/>
<ContinuousMovementCommands/></AgentHandlers></AgentSection></Mission>)";

        std::istringstream is(defaultMission);
        boost::property_tree::read_xml(is, mission);
    }

    MissionSpec::MissionSpec(const std::string& xml, bool validate)
    {
        std::istringstream is(xml);
        boost::property_tree::read_xml(is, mission);

        if (validate) {
            // Agent side schema validation is lacking but Minecraft client will perform full validation (which is not optional).
            const auto& xmlns = mission.get_optional<string>("Mission.<xmlattr>.xmlns");
            if (xmlns == boost::none || MALMO_NAMESPACE != xmlns.get()) {
                throw runtime_error("MissionSpec is invalid (namespace)");
            }
        }
    }

    std::string MissionSpec::getAsXML( bool prettyPrint ) const
    {
        std::ostringstream oss;

        write_xml(oss, mission);

        std::string xml = oss.str();
        if (!prettyPrint) 
            xml.erase(std::remove(xml.begin(), xml.end(), '\n'), xml.end());
        return xml;
    }
    
    // -------------------- settings for the server ---------------------------------
    
    void MissionSpec::setSummary( const std::string& summary )
    {
        mission.put("Mission.About.Summary", summary);
    }

    void MissionSpec::timeLimitInSeconds(float s)
    {
        mission.put("Mission.ServerSection.ServerHandlers.ServerQuitFromTimeUp.<xmlattr>.timeLimitMs", s * MillisecondsInOneSecond);
    }
    
    void MissionSpec::createDefaultTerrain()
    {
        worldGeneratorReset();
        mission.put("Mission.ServerSection.ServerHandlers.DefaultWorldGenerator","");
    }

    void MissionSpec::worldGeneratorReset()
    {
        const auto& parent = mission.get_child_optional("Mission.ServerSection.ServerHandlers");
        if (parent) {
            auto& child = mission.get_child("Mission.ServerSection.ServerHandlers");
            child.erase("FlatWorldGenerator");
            child.erase("FileWorldGenerator");
            child.erase("DefaultWorldGenerator");
            child.erase("BiomeGenerator");
        }
    }

    void MissionSpec::setWorldSeed(const std::string& seed)
    {
        const auto& default_wg = mission.get_child_optional("Mission.ServerSection.ServerHandlers.DefaultWorldGenerator");
        if (default_wg)
            default_wg.get().put("<xmlattr>.seed", seed);
        const auto& flat_wg = mission.get_child_optional("Mission.ServerSection.ServerHandlers.FlatWorldGenerator");
        if (flat_wg)
            flat_wg.get().put("<xmlattr>.seed", seed);
    }

    void MissionSpec::forceWorldReset()
    {
        const auto& flatWorldGenerator = mission.get_child_optional("Mission.ServerSection.ServerHandlers.FlatWorldGenerator");
        if (flatWorldGenerator) {
            flatWorldGenerator.get().put("<xmlattr>.forceReset", true);
        }
        const auto& fileWorldGenerator = mission.get_child_optional("Mission.ServerSection.ServerHandlers.FileWorldGenerator");
        if (fileWorldGenerator) {
            fileWorldGenerator.get().put("<xmlattr>.forceReset", true);
        }
        const auto& defaultWorldGenerator = mission.get_child_optional("Mission.ServerSection.ServerHandlers.DefaultWorldGenerator");
        if (defaultWorldGenerator) {
           defaultWorldGenerator.get().put("<xmlattr>.forceReset", true);
        }
        const auto& biomeWorldGenerator = mission.get_child_optional("Mission.ServerSection.ServerHandlers.BiomeGenerator");
        if (biomeWorldGenerator) {
            biomeWorldGenerator.get().put("<xmlattr>.forceReset", true);
        }
    }

    void MissionSpec::setTimeOfDay(int t,bool allowTimeToPass)
    {
        mission.put("Mission.ServerSection.ServerInitialConditions.Time.StartTime", t);
        mission.put("Mission.ServerSection.ServerInitialConditions.Time.AllowPassageOfTime", allowTimeToPass);
    }

    void MissionSpec::drawBlock(int x, int y, int z, const string& blockType)
    {
        auto& drawing_decorator = getDrawingDecorator();
        boost::property_tree::ptree block;
        block.put("<xmlattr>.type", blockType);
        block.put("<xmlattr>.x", x);
        block.put("<xmlattr>.y", y);
        block.put("<xmlattr>.z", z);
        drawing_decorator.add_child("DrawBlock", block);
    }

    void MissionSpec::drawCuboid(int x1, int y1, int z1, int x2, int y2, int z2, const std::string& blockType)
    {
        auto& drawing_decorator = getDrawingDecorator();
        boost::property_tree::ptree cuboid;
        cuboid.put("<xmlattr>.type", blockType);
        cuboid.put("<xmlattr>.x1", x1);
        cuboid.put("<xmlattr>.y1", y1);
        cuboid.put("<xmlattr>.z1", z1);
        cuboid.put("<xmlattr>.x2", x2);
        cuboid.put("<xmlattr>.y2", y2);
        cuboid.put("<xmlattr>.z2", z2);
        drawing_decorator.add_child("DrawCuboid", cuboid);
    }

    void MissionSpec::drawItem(int x, int y, int z, const std::string& itemType)
    {
        auto& drawing_decorator = getDrawingDecorator();
        boost::property_tree::ptree item;
        item.put("<xmlattr>.type", itemType);
        item.put("<xmlattr>.x", x);
        item.put("<xmlattr>.y", y);
        item.put("<xmlattr>.z", z);
        drawing_decorator.add_child("DrawItem", item);
    }

    void MissionSpec::drawSphere(int x, int y, int z, int radius, const std::string& blockType)
    {
        auto& drawing_decorator = getDrawingDecorator();
        boost::property_tree::ptree sphere;
        sphere.put("<xmlattr>.type", blockType);
        sphere.put("<xmlattr>.x", x);
        sphere.put("<xmlattr>.y", y);
        sphere.put("<xmlattr>.z", z);
        sphere.put("<xmlattr>.radius", radius);
        drawing_decorator.add_child("DrawSphere", sphere);
    }

    void MissionSpec::drawLine(int x1, int y1, int z1, int x2, int y2, int z2, const std::string& blockType)
    {
        auto& drawing_decorator = getDrawingDecorator();
        boost::property_tree::ptree sphere;
        sphere.put("<xmlattr>.type", blockType);
        sphere.put("<xmlattr>.x1", x1);
        sphere.put("<xmlattr>.y1", y1);
        sphere.put("<xmlattr>.z1", z1);
        sphere.put("<xmlattr>.x2", x2);
        sphere.put("<xmlattr>.y2", y2);
        sphere.put("<xmlattr>.z2", z2);
        drawing_decorator.add_child("DrawLine", sphere);
    }
    
    // ------------------ settings for the agents --------------------------------

    void MissionSpec::startAt(float x, float y, float z)
    {
        mission.put("Mission.AgentSection.AgentStart.Placement.<xmlattr>.x", x);
        mission.put("Mission.AgentSection.AgentStart.Placement.<xmlattr>.y", y);
        mission.put("Mission.AgentSection.AgentStart.Placement.<xmlattr>.z", z);
    }

    void MissionSpec::startAtWithPitchAndYaw(float x, float y, float z, float pitch, float yaw)
    {
        startAt(x, y, z);
        mission.put("Mission.AgentSection.AgentStart.Placement.<xmlattr>.pitch", pitch);
        mission.put("Mission.AgentSection.AgentStart.Placement.<xmlattr>.yaw", yaw);
    }

    void MissionSpec::endAt(float x, float y, float z, float tolerance)
    {
        const auto& elementName = "Mission.AgentSection.AgentHandlers.AgentQuitFromReachingPosition";
        const auto& agentQuitFromReachingPosition = mission.get_child_optional(elementName);
        if (agentQuitFromReachingPosition == boost::none) {
            mission.add(elementName, "");
        }
        auto& element = mission.get_child(elementName);
        boost::property_tree::ptree marker;
        marker.add("<xmlattr>.x", x);
        marker.add("<xmlattr>.y", y);
        marker.add("<xmlattr>.z", z);
        marker.add("<xmlattr>.tolerance", tolerance);
        element.add_child("Marker", marker);
    }
    
    void MissionSpec::setModeToCreative()
    {
        mission.put("Mission.AgentSection.<xmlattr>.mode", "Creative");
    }

    void MissionSpec::setModeToSpectator()
    {
        mission.put("Mission.AgentSection.<xmlattr>.mode", "Spectator");
    }

    void MissionSpec::requestVideo(int width, int height)
    {
        mission.put("Mission.AgentSection.AgentHandlers.VideoProducer.Width", width);
        mission.put("Mission.AgentSection.AgentHandlers.VideoProducer.Height", height);
    }

    void MissionSpec::requestLuminance(int width, int height)
    {
        mission.put("Mission.AgentSection.AgentHandlers.LuminanceProducer.Width", width);
        mission.put("Mission.AgentSection.AgentHandlers.LuminanceProducer.Height", height);
    }

    void MissionSpec::requestColourMap(int width, int height)
    {
        mission.put("Mission.AgentSection.AgentHandlers.ColourMapProducer.Width", width);
        mission.put("Mission.AgentSection.AgentHandlers.ColourMapProducer.Height", height);
    }

    void MissionSpec::request32bppDepth(int width, int height)
    {
        mission.put("Mission.AgentSection.AgentHandlers.DepthProducer.Width", width);
        mission.put("Mission.AgentSection.AgentHandlers.DepthProducer.Height", height);
    }

    void MissionSpec::requestVideoWithDepth(int width, int height)
    {
        requestVideo(width, height);
        mission.put("Mission.AgentSection.AgentHandlers.VideoProducer.<xmlattr>.want_depth", true);
    }
    
    void MissionSpec::setViewpoint(int viewpoint)
    {
        const auto& v = mission.get_child_optional("Mission.AgentSection.AgentHandlers.VideoProducer");
        if (v) {
            mission.put("Mission.AgentSection.AgentHandlers.VideoProducer.<xmlattr>.viewpoint", viewpoint);
        }
    }

    void MissionSpec::rewardForReachingPosition(float x, float y, float z, float amount, float tolerance)
    {
        const auto& elementName = "Mission.AgentSection.AgentHandlers.RewardForReachingPosition";
        const auto& rewardForReachingPosition = mission.get_child_optional(elementName);
        if (rewardForReachingPosition == boost::none) {
            mission.add(elementName, "");
        }
        auto& element = mission.get_child(elementName);
        boost::property_tree::ptree marker;
        marker.add("<xmlattr>.x", x);
        marker.add("<xmlattr>.y", y);
        marker.add("<xmlattr>.z", z);
        marker.add("<xmlattr>.reward", amount);
        marker.add("<xmlattr>.tolerance", tolerance);
        element.add_child("Marker", marker);
    }
    
    void MissionSpec::observeRecentCommands()
    {
        mission.put("Mission.AgentSection.AgentHandlers.ObservationFromRecentCommands", "");
    }
    
    void MissionSpec::observeHotBar()
    {
        mission.put("Mission.AgentSection.AgentHandlers.ObservationFromHotBar", "");
    }
    
    void MissionSpec::observeFullInventory()
    {
        mission.put("Mission.AgentSection.AgentHandlers.ObservationFromFullInventory", "");
    }
    
    void MissionSpec::observeGrid(int x1,int y1,int z1,int x2,int y2,int z2,const std::string& name)
    {
        const auto& elementName = "Mission.AgentSection.AgentHandlers.ObservationFromGrid";
        const auto& observationFromGrid = mission.get_child_optional(elementName);
        if (observationFromGrid == boost::none) {
            mission.add(elementName, "");
        }
        auto& element = mission.get_child(elementName);
        boost::property_tree::ptree grid;
        grid.add("min.<xmlattr>.x", x1);
        grid.add("min.<xmlattr>.y", y1);
        grid.add("min.<xmlattr>.z", z1);
        grid.add("max.<xmlattr>.x", x2);
        grid.add("max.<xmlattr>.y", y2);
        grid.add("max.<xmlattr>.z", z2);
        grid.add("<xmlattr>.name", name);
        element.add_child("Grid", grid);
    }
    
    void MissionSpec::observeDistance(float x, float y, float z, const std::string& name)
    {
        const auto& elementName = "Mission.AgentSection.AgentHandlers.ObservationFromDistance";
        const auto& observationFromDistance = mission.get_child_optional(elementName);
        if (observationFromDistance == boost::none) {
            mission.add(elementName, "");
        }
        auto& element = mission.get_child(elementName);
        boost::property_tree::ptree marker;
        marker.add("<xmlattr>.x", x);
        marker.add("<xmlattr>.y", y);
        marker.add("<xmlattr>.z", z);
        marker.add("<xmlattr>.name", name);
        element.add_child("Marker", marker);
    }
    
    void MissionSpec::observeChat()
    {
        mission.put("Mission.AgentSection.AgentHandlers.ObservationFromChat", "");
    }
    
    void MissionSpec::observeCompass()
    {
        mission.put("Mission.AgentSection.AgentHandlers.ObservationFromCompass", "");
    }
    
    // ------------------ settings for the agents : command handlers --------------------------------
    
    void MissionSpec::removeAllCommandHandlers()
    {
        const auto& agent_handlers = mission.get_child_optional("Mission.AgentSection.AgentHandlers");
        if (agent_handlers) {
            auto& child = mission.get_child("Mission.AgentSection.AgentHandlers");

            child.erase("ContinuousMovementCommands");
            child.erase("DiscreteMovementCommands");
            child.erase("AbsoluteMovementCommands");
            child.erase("SimpleCraftCommands");
            child.erase("NearbyCraftCommands");
            child.erase("NearbySmeltCommands");
            child.erase("PlaceCommands");
            child.erase("ChatCommands");
            child.erase("MissionQuitCommands");
        }
    }

    void MissionSpec::allowAllContinuousMovementCommands()
    {
        mission.put("Mission.AgentSection.AgentHandlers.ContinuousMovementCommands", "");
    }

    void MissionSpec::allowContinuousMovementCommand(const std::string& verb)
    {
        addVerbToCommandType(verb, "Mission.AgentSection.AgentHandlers.ContinuousMovementCommands");
    }

    void MissionSpec::allowAllDiscreteMovementCommands()
    {
        mission.put("Mission.AgentSection.AgentHandlers.DiscreteMovementCommands", "");
    }

    void MissionSpec::allowDiscreteMovementCommand(const std::string& verb)
    {
        addVerbToCommandType(verb, "Mission.AgentSection.AgentHandlers.DiscreteMovementCommands");
    }

    void MissionSpec::allowAllAbsoluteMovementCommands()
    {
        mission.put("Mission.AgentSection.AgentHandlers.AbsoluteMovementCommands", "");
    }

    void MissionSpec::allowAbsoluteMovementCommand(const std::string& verb)
    {
        addVerbToCommandType(verb, "Mission.AgentSection.AgentHandlers.AbsoluteMovementCommands");
    }

    void MissionSpec::allowAllInventoryCommands()
    {
        mission.put("Mission.AgentSection.AgentHandlers.InventoryCommands", "");
    }

    void MissionSpec::allowInventoryCommand(const std::string& verb)
    {
        addVerbToCommandType(verb, "Mission.AgentSection.AgentHandlers.InventoryCommands");
    }

    void MissionSpec::allowAllChatCommands()
    {
        mission.put("Mission.AgentSection.AgentHandlers.ChatCommands", "");
    }
    
    void MissionSpec::allowAllPlaceCommands()
    {
        mission.put("Mission.AgentSection.AgentHandlers.PlaceCommands", "");
    }

    // ------------------------------- information ---------------------------------------------------
    
    string MissionSpec::getSummary() const
    {
        return mission.get<std::string>("Mission.About.Summary");
    }
    
    int MissionSpec::getNumberOfAgents() const
    {
        int i = 0;
        for (auto& e : mission.get_child("Mission"))
            if (e.first == "AgentSection") i++;
        return i;
    }

    bool MissionSpec::isVideoRequested(int role) const
    {
        return getRoleValue(role, "AgentHandlers.VideoProducer", 'x') != boost::none;
    }
    
    bool MissionSpec::isDepthRequested(int role) const
    {
        return getRoleValue(role, "AgentHandlers.DepthProducer", 'x') != boost::none;
    }

    bool MissionSpec::isLuminanceRequested(int role) const
    {
        return getRoleValue(role, "AgentHandlers.LuminanceProducer", 'x') != boost::none;
    }

    bool MissionSpec::isColourMapRequested(int role) const
    {
        return getRoleValue(role, "AgentHandlers.ColourMapProducer", 'x') != boost::none;
    }

    int MissionSpec::getVideoWidth(int role) const
    {
        auto w = getRoleValue(role, "AgentHandlers.VideoProducer", 'w');
        if (w) return w.get();
        w = getRoleValue(role, "AgentHandlers.DepthProducer", 'w');
        if (w) return w.get();
        w = getRoleValue(role, "AgentHandlers.LuminanceProducer", 'w');
        if (w) return w.get();
        w = getRoleValue(role, "AgentHandlers.ColourMapProducer", 'w');
        if (w) return w.get();
        throw runtime_error("MissionInitSpec::getVideoWidth : video has not been requested for this role");
    }

    int MissionSpec::getVideoHeight(int role) const
    {
        auto h = getRoleValue(role, "AgentHandlers.VideoProducer", 'h');
        if (h) return h.get();
        h = getRoleValue(role, "AgentHandlers.DepthProducer", 'h');
        if (h) return h.get();
        h = getRoleValue(role, "AgentHandlers.LuminanceProducer", 'h');
        if (h) return h.get();
        h = getRoleValue(role, "AgentHandlers.ColourMapProducer", 'h');
        if (h) return h.get();

        throw runtime_error("MissionInitSpec::getVideoHeight : video has not been requested for this role");
    }

    int MissionSpec::getVideoChannels(int role) const
    {
        auto c = getRoleValue(role, "AgentHandlers.VideoProducer", 'c');
        if (c) return c.get() == 1? 4 : 3;
        throw runtime_error("MissionInitSpec::getVideoChannels : video has not been requested for this role");
    }

    vector<string> MissionSpec::getListOfCommandHandlers(int role) const
    {
        const boost::property_tree::ptree& m = mission.get_child("Mission");
        for (auto& e : m) {
            if (e.first != "AgentSection")
                continue;

            if (role-- == 0) {
                vector<string> command_handlers;

                if (e.second.get_child_optional("AgentHandlers.ContinuousMovementCommands"))
                    command_handlers.push_back("ContinuousMovement");

                if (e.second.get_child_optional("AgentHandlers.AbsoluteMovementCommands"))
                    command_handlers.push_back("AbsoluteMovement");

                if (e.second.get_child_optional("AgentHandlers.DiscreteMovementCommands"))
                    command_handlers.push_back("DiscreteMovement");

                if (e.second.get_child_optional("AgentHandlers.InventoryCommands"))
                    command_handlers.push_back("Inventory");

                if (e.second.get_child_optional("AgentHandlers.ChatCommands"))
                    command_handlers.push_back("Chat");

                if (e.second.get_child_optional("AgentHandlers.SimpleCraftCommands"))
                    command_handlers.push_back("SimpleCraft");
                
                if (e.second.get_child_optional("AgentHandlers.NearbyCraftCommands"))
                    command_handlers.push_back("NearbyCraft");
                
                if (e.second.get_child_optional("AgentHandlers.NearbySmeltCommands"))
                    command_handlers.push_back("NearbySmelt");
                
                if (e.second.get_child_optional("AgentHandlers.PlaceCommands"))
                    command_handlers.push_back("Place");

                if (e.second.get_child_optional("AgentHandlers.MissionQuitCommands"))
                    command_handlers.push_back("MissionQuit");

                if (e.second.get_child_optional("AgentHandlers.HumanLevelCommands"))
                    command_handlers.push_back("HumanLevel");

                return command_handlers;
            }
        }

        throw runtime_error("No such role in agent section");
    }
    
    vector<string> MissionSpec::getAllowedCommands(int role,const string& command_handler) const
    {
        vector<string> allowed_commands;

        const boost::property_tree::ptree& m = mission.get_child("Mission");
        for (auto& e : m) {
            if (e.first != "AgentSection")
                continue;

            if (role-- == 0) {
                const auto& commands = e.second.get_child_optional("AgentHandlers." + command_handler + "Commands");
                if (commands == boost::none)
                    return allowed_commands;

                bool explicit_allow = false;
                // Collect all allowed verbs first and then remove any that are denied.
                for (auto& ml : commands.get()) {
                    if (ml.first == "ModifierList") {
                        const auto& t = ml.second.get_optional<std::string>("<xmlattr>.type");
                        if (t != boost::none && t.get() == "allow-list") {
                            explicit_allow = true;
                            for (auto& c : ml.second) {
                                if (c.first == "command") {
                                    allowed_commands.push_back(c.second.data());
                                }
                            }
                        }
                    }
                } 
                if (!explicit_allow) {
                    // Command defaulting.
                    if (command_handler == "ContinuousMovement") {
                        allowed_commands = all_continuous_movement_commands;
                    }
                    else if (command_handler == "AbsoluteMovement") {
                        allowed_commands = all_absolute_movement_commands;
                    }
                    else if (command_handler == "DiscreteMovement") {
                        allowed_commands = all_discrete_movement_commands;
                    }
                    else if (command_handler == "Inventory") {
                        allowed_commands = all_inventory_commands;
                    }
                    else if (command_handler == "SimpleCraft") {
                        allowed_commands = all_simplecraft_commands;
                    }
                    else if (command_handler == "NearbyCraft") {
                        allowed_commands = all_nearbycraft_commands;
                    }
                    else if (command_handler == "NearbySmelt") {
                        allowed_commands = all_nearbysmelt_commands;
                    }
                    else if (command_handler == "Chat") {
                        allowed_commands = all_chat_commands;
                    }
                    else if (command_handler == "MissionQuit") {
                        allowed_commands = all_mission_quit_commands;
                    }
                    else if (command_handler == "HumanLevel") {
                        allowed_commands = all_human_level_commands;
                    }
                    else
                        throw runtime_error("Unknown command handler");
                }
                for (auto& ml : commands.get()) {
                    if (ml.first == "ModifierList") {
                        const auto& t = ml.second.get_optional<std::string>("<xmlattr>.type");
                        if (t == boost::none || t.get() != "allow-list") {
                            for (auto& c : ml.second) {
                                if (c.first == "command") {
                                    allowed_commands.erase(std::remove(allowed_commands.begin(), allowed_commands.end(), c.second.data()), allowed_commands.end());
                                }
                            }
                        }
                    }
                }
            }
        }

        return allowed_commands;
    }
    
    int MissionSpec::getChildCount(const std::string& elementPath, const std::string& childName) const {
        const auto& element = mission.get_child_optional(elementPath);
        int count = 0;
        if (element == boost::none)
            return -1;
        for (auto& c : element.get()) {
            if (c.first == childName)
                count++;
        }
        return count;
    }

    // ---------------------------- private functions -----------------------------------------------

    boost::property_tree::ptree& MissionSpec::getDrawingDecorator() {
        const auto& drawing_decorator = mission.get_child_optional("Mission.ServerSection.ServerHandlers.DrawingDecorator");
        if (drawing_decorator == boost::none) {
            mission.put("Mission.ServerSection.ServerHandlers.DrawingDecorator", "");
            return mission.get_child("Mission.ServerSection.ServerHandlers.DrawingDecorator");
        }
        return drawing_decorator.get();
    }

    boost::optional<int> MissionSpec::getRoleValue(int role, std::string videoType, char what) const {
        const boost::property_tree::ptree& m = mission.get_child("Mission");
        for (auto& e : m) {
            if (e.first != "AgentSection")
                continue;

            if (role-- == 0) {
                const auto& v = e.second.get_child_optional(videoType);
                if (v == boost::none) {
                    return boost::optional<int>();
                }
                switch (what) {
                case 'x':
                    return boost::optional<int>(0);
                case 'w':
                    return boost::optional<int>(v.get().get<int>("Width"));
                case 'h':
                    return boost::optional<int>(v.get().get<int>("Height"));
                case 'c': {
                    // Default want_depth attribute to false.
                    const auto& want_depth = v.get().get_optional<string>("<xmlattr>.want_depth");
                    if (want_depth) {
                        return boost::optional<int>(want_depth.get() == "true" || want_depth.get() == "1");
                    }
                    return boost::optional<int>(0);
                }
                default:
                    throw runtime_error("Invalid video attribute");
                }
            }
        }
        throw runtime_error("No such role in agent section");
    }

    void MissionSpec::addVerbToCommandType(std::string verb, std::string commandType) {
        const auto& c = mission.get_child_optional(commandType);
        if (c == boost::none) {
            mission.put(commandType, "");
        }
        boost::property_tree::ptree& commands = mission.get_child(commandType);
        bool found = false;
        for (auto& e : commands) {
            if (e.first == "ModifierList") {
                const auto& t = e.second.get_optional<std::string>("<xmlattr>.type");
                if (t != boost::none && t.get() == "allow-list") {
                    for (auto& c : e.second) {
                        if (c.first == "command" &&
                            verb == c.second.data()) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        e.second.add("command", verb);
                        found = true;
                    }
                    break;
                }
                else {
                    // Found deny list.
                    throw runtime_error("Sorry, can't add command verb when deny-list present.");
                }
            }
        }
        if (!found) {
            boost::property_tree::ptree ml;
            ml.put("<xmlattr>.type", "allow-list");
            ml.put("command", verb);
            commands.add_child("ModifierList", ml);
        }
    }

    std::ostream& operator<<(std::ostream& os, const MissionSpec& ms)
    {
        os << "MissionSpec:\n";
        os << ms.getAsXML(true);
        return os;
    }
}
