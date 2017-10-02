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

// Malmo:
#include <AgentHost.h>
#ifdef WRAP_ALE
  #include <ALEAgentHost.h>
#endif
#include <ClientPool.h>
#include <MissionSpec.h>
#include <ParameterSet.h>
#ifdef TORCH
  #include <TorchTensorFromPixels.h>
#endif
#include <WorldState.h>
using namespace malmo;

// Luabind:
#include <luabind/exception_handler.hpp>
#include <luabind/iterator_policy.hpp>
#include <luabind/luabind.hpp>
#include <luabind/operator.hpp>

// STL:
#include <sstream>

#ifdef _WINDOWS
  #define MODULE_EXPORT extern "C" __declspec( dllexport )
#else
  #define MODULE_EXPORT extern "C"
#endif

// Extracts strings from the non-negative entries in a Lua table. Throws a Lua exception if the cast fails.
std::vector< std::string > argTableToStrings( const luabind::object& args )
{
    std::vector< std::string > strings;
    int num_args = 0;
    for( luabind::iterator it( args ), end; it != end; ++it ) {
        int key = luabind::object_cast< int >( it.key() );
        num_args = std::max( key, num_args );
    }
    num_args++;
    for( int i = 0; i < num_args; ++i ) {
        strings.push_back( luabind::object_cast< std::string >( args[i] ) );
    }
    return strings;
}

// A Lua wrapper around ArgumentParser::parse().
void parseLuaTable( ArgumentParser* p, const luabind::object& args )
{
    p->parse( argTableToStrings( args ) );
}

// Make sure we get all of the useful information from xml_schema::exception
void translateXMLSchemaException(lua_State* L, xml_schema::exception const& e)
{
    std::ostringstream oss;
    oss << "Caught xml_schema::exception: " << e.what() << "\n" << e;
    lua_pushstring(L, oss.str().c_str());
}

// We create our own bindings for MissionException, rather than use luabind,
// so that we can manually return a MissionException object from translateMissionException.
// (If this is possible using luabind, I couldn't work out how to do it.)

static int newMissionException(lua_State* L)
{
    int code = luaL_checkint(L, 1);
    const char* str = luaL_checkstring(L, 2);
    MissionException *me = new(lua_newuserdata(L, sizeof(MissionException))) MissionException(std::string(str), (MissionException::MissionErrorCode)code);
    luaL_getmetatable(L, "Malmo.MissionException");
    lua_setmetatable(L, -2);
    return 1;
}

static MissionException* checkMissionException(lua_State *L)
{
    void *user_data = luaL_checkudata(L, 1, "Malmo.MissionException");
    luaL_argcheck(L, user_data != NULL, 1, "MissionException expected");
    return (MissionException*)user_data;
}

static int getCode(lua_State* L)
{
    MissionException *me = checkMissionException(L);
    lua_pushnumber(L, me->getMissionErrorCode());
    return 1;
}

static int getMessage(lua_State* L)
{
    MissionException *me = checkMissionException(L);
    lua_pushstring(L, me->what());
    return 1;
}

static int missionExceptionToString(lua_State *L)
{
    // Adding a __tostring() method means we don't have to break existing lua
    // code that expects a string error message rather than an object.
    MissionException* me = checkMissionException(L);
    lua_pushstring(L, me->what());
    return 1;
}

static int concatMissionException(lua_State *L)
{
    // For some reason, despite giving MissionException a __tostring method,
    // Lua's concatenation operator won't call it automatically, so code like
    // print("Got an error: "..err) will fail if err is a MissionException object.
    // Since this is exactly what Malmo's lua sample code does, it's worth fixing
    // it here, with a custom __concat operator.
    // All we do is iterate over the top two items on the stack, and either push
    // their string value (if they are a MissioException object), or simply copy them
    // untouched if not. We then use lua_concat to combine the new top two items
    // on the stack.
    int top = lua_gettop(L);
    for (int i = top - 1; i <= top; i++)
    {
        bool wasme = false;
        if (lua_type(L, i) == LUA_TUSERDATA)
        {
            void* p = lua_touserdata(L, i);
            if (lua_getmetatable(L, i))
            {
                lua_getfield(L, LUA_REGISTRYINDEX, "Malmo.MissionException");
                if (lua_rawequal(L, -1, -2))
                {
                    lua_pop(L, 2);
                    MissionException* me = (MissionException*)p;
                    wasme = true;
                    lua_pushstring(L, me->what());
                }
            }
        }
        if (!wasme)
        {
            lua_pushvalue(L, i);
        }
    }
    lua_concat(L, 2);
    return 1;
}

const struct luaL_reg missExLib_methods [] = {
    {"code", getCode},
    {"message", getMessage},
    {"__tostring", missionExceptionToString},
    {"__concat", concatMissionException},
    {NULL, NULL}
};

static const struct luaL_reg missExLib_functions [] = {
    {"new", newMissionException},
    {NULL, NULL}
};

static int registerMissionException(lua_State* L)
{
    luaL_newmetatable(L, "Malmo.MissionException");  // metatable now on the stack
    lua_pushstring(L, "__index");                    // "__index" now on the stack
    lua_pushvalue(L, -2);                            // -2 = the metatable
    lua_settable(L, -3);                             // metatable.__index = metatable
    luaL_openlib(L, NULL, missExLib_methods, 0);
    luaL_openlib(L, "MissionException", missExLib_functions, 0);
    return 1;
}

void translateMissionException(lua_State* L, const MissionException& e)
{
    // Use placement new to create a MissionException object on the lua C stack:
    void* pMem = lua_newuserdata(L, sizeof(MissionException));
    MissionException* newme = new(pMem) MissionException(e);
    // Give it a metatable
    luaL_getmetatable(L, "Malmo.MissionException");
    lua_setmetatable(L, -2);
}

// Turn a posix time into a long:
template<typename T> long getPosixTimeAsLong(T* obj)
{
    boost::posix_time::ptime tnow = obj->timestamp;
    boost::posix_time::ptime tepoch(boost::gregorian::date(1970, 1, 1));
    boost::posix_time::time_duration dur = tnow - tepoch;
    return dur.total_milliseconds();
}

void (AgentHost::*startMissionSimple)(const MissionSpec&, const MissionRecordSpec&) = &AgentHost::startMission;
void (AgentHost::*startMissionComplex)(const MissionSpec&, const ClientPool&, const MissionRecordSpec&, int, std::string) = &AgentHost::startMission;

void (AgentHost::*sendCommand)(std::string) = &AgentHost::sendCommand;
void (AgentHost::*sendCommandWithKey)(std::string, std::string) = &AgentHost::sendCommand;

#ifdef WRAP_ALE
  void (ALEAgentHost::*startALEMissionSimple)(const MissionSpec&, const MissionRecordSpec&) = &ALEAgentHost::startMission;
  void (ALEAgentHost::*startALEMissionComplex)(const MissionSpec&, const ClientPool&, const MissionRecordSpec&, int, std::string) = &ALEAgentHost::startMission;
#endif

void recordMP4(MissionRecordSpec* mrs, int frames_per_second, long bitrate)
{
    mrs->recordMP4(frames_per_second,static_cast<int64_t>(bitrate));
}

// wrapper for MissionSpec::getListOfCommandHandlers that returns a table
luabind::object getListOfCommandHandlers( lua_State *L, const MissionSpec& m, int role )
{
    luabind::object result = luabind::newtable(L);
    int index = 1;
    for( const std::string& ch : m.getListOfCommandHandlers( role ) ) {
        result[ index++ ] = ch;
    }
    return result;
}

// wrapper for MissionSpec::getAllowedCommands that returns a table
luabind::object getAllowedCommands( lua_State *L, const MissionSpec& m, int role, const std::string& command_handler )
{
    luabind::object result = luabind::newtable(L);
    int index = 1;
    for( const std::string& ac : m.getAllowedCommands( role, command_handler ) ) {
        result[ index++ ] = ac;
    }
    return result;
}

// Defines the API available to Lua.
MODULE_EXPORT int luaopen_libMalmoLua(lua_State* L)
{
    using namespace luabind;

    open(L);
    registerMissionException(L);
    module(L)
    [
        class_< MissionException >("MissionErrorCode")
        .enum_("MissionErrorCode")
        [
            value("MISSION_BAD_ROLE_REQUEST", MissionException::MISSION_BAD_ROLE_REQUEST),
            value("MISSION_BAD_VIDEO_REQUEST", MissionException::MISSION_BAD_VIDEO_REQUEST),
            value("MISSION_ALREADY_RUNNING", MissionException::MISSION_ALREADY_RUNNING),
            value("MISSION_INSUFFICIENT_CLIENTS_AVAILABLE", MissionException::MISSION_INSUFFICIENT_CLIENTS_AVAILABLE),
            value("MISSION_TRANSMISSION_ERROR", MissionException::MISSION_TRANSMISSION_ERROR),
            value("MISSION_SERVER_WARMING_UP", MissionException::MISSION_SERVER_WARMING_UP),
            value("MISSION_SERVER_NOT_FOUND", MissionException::MISSION_SERVER_NOT_FOUND),
            value("MISSION_NO_COMMAND_PORT", MissionException::MISSION_NO_COMMAND_PORT),
            value("MISSION_BAD_INSTALLATION", MissionException::MISSION_BAD_INSTALLATION)
        ]
    ,

        class_< Logger >("Logger")
        .enum_("LoggingSeverityLevel")
        [
            value("LOG_OFF", Logger::LOG_OFF),
            value("LOG_ERRORS", Logger::LOG_ERRORS),
            value("LOG_WARNINGS", Logger::LOG_WARNINGS),
            value("LOG_INFO", Logger::LOG_INFO),
            value("LOG_FINE", Logger::LOG_FINE),
            value("LOG_TRACE", Logger::LOG_TRACE),
            value("LOG_ALL", Logger::LOG_ALL)
        ],

        def("setLogging", &Logger::setLogging),
        def("appendToLog", &Logger::appendToLog),

        class_< ArgumentParser >("ArgumentParser")
            .def(constructor< const std::string& >())
            .def("parse", &parseLuaTable)
            .def("addOptionalIntArgument", &ArgumentParser::addOptionalIntArgument)
            .def("addOptionalFloatArgument", &ArgumentParser::addOptionalFloatArgument)
            .def("addOptionalStringArgument", &ArgumentParser::addOptionalStringArgument)
            .def("addOptionalFlag", &ArgumentParser::addOptionalFlag)
            .def("getUsage", &ArgumentParser::getUsage)
            .def("receivedArgument", &ArgumentParser::receivedArgument)
            .def("getIntArgument", &ArgumentParser::getIntArgument)
            .def("getFloatArgument", &ArgumentParser::getFloatArgument)
            .def("getStringArgument", &ArgumentParser::getStringArgument)
        ,
        class_< WorldState >( "WorldState" )
            .def_readonly( "is_mission_running",                      &WorldState::is_mission_running )
            .def_readonly( "has_mission_begun",                       &WorldState::has_mission_begun )
            .def_readonly( "number_of_observations_since_last_state", &WorldState::number_of_observations_since_last_state )
            .def_readonly( "number_of_rewards_since_last_state",      &WorldState::number_of_rewards_since_last_state )
            .def_readonly( "number_of_video_frames_since_last_state", &WorldState::number_of_video_frames_since_last_state )
            .def_readonly( "observations",                            &WorldState::observations,               return_stl_iterator )
            .def_readonly( "rewards",                                 &WorldState::rewards,                    return_stl_iterator )
            .def_readonly( "video_frames",                            &WorldState::video_frames,               return_stl_iterator )
            .def_readonly( "mission_control_messages",                &WorldState::mission_control_messages,   return_stl_iterator )
            .def_readonly( "errors",                                  &WorldState::errors,                     return_stl_iterator )
            .def(tostring(const_self))
        ,
        class_< AgentHost, bases< ArgumentParser > >("AgentHost")
            .enum_( "ImagePolicy" )
            [
                  value( "LATEST_FRAME_ONLY",  AgentHost::LATEST_FRAME_ONLY )
                , value( "KEEP_ALL_FRAMES",    AgentHost::KEEP_ALL_FRAMES )
            ]
            .enum_( "RewardsPolicy" )
            [
                  value( "LATEST_REWARD_ONLY",  AgentHost::LATEST_REWARD_ONLY )
                , value( "SUM_REWARDS",         AgentHost::SUM_REWARDS )
                , value( "KEEP_ALL_REWARDS",    AgentHost::KEEP_ALL_REWARDS )
            ]
            .enum_( "ObservationsPolicy" )
            [
                  value( "LATEST_OBSERVATION_ONLY",  AgentHost::LATEST_OBSERVATION_ONLY )
                , value( "KEEP_ALL_OBSERVATIONS",    AgentHost::KEEP_ALL_OBSERVATIONS )
            ]
            .def(constructor<>())
            .def("startMission",                    startMissionSimple)
            .def("startMission",                    startMissionComplex)
            .def("peekWorldState",                  &AgentHost::peekWorldState)
            .def("getWorldState",                   &AgentHost::getWorldState)
            .def("setVideoPolicy",                  &AgentHost::setVideoPolicy)
            .def("setRewardsPolicy",                &AgentHost::setRewardsPolicy)
            .def("setObservationsPolicy",           &AgentHost::setObservationsPolicy)
            .def("sendCommand",                     sendCommand)
            .def("sendCommand",                     sendCommandWithKey)
            .def("getRecordingTemporaryDirectory",  &AgentHost::getRecordingTemporaryDirectory)
            .def("setDebugOutput",                  &AgentHost::setDebugOutput)

            .def(tostring(const_self))
        ,
#ifdef WRAP_ALE
    class_< ALEAgentHost, bases< ArgumentParser > >("ALEAgentHost")
            .enum_( "ImagePolicy" )
            [
                  value( "LATEST_FRAME_ONLY",  AgentHost::LATEST_FRAME_ONLY )
                , value( "KEEP_ALL_FRAMES",    AgentHost::KEEP_ALL_FRAMES )
            ]
            .enum_( "RewardsPolicy" )
            [
                  value( "LATEST_REWARD_ONLY",  AgentHost::LATEST_REWARD_ONLY )
                , value( "SUM_REWARDS",         AgentHost::SUM_REWARDS )
                , value( "KEEP_ALL_REWARDS",    AgentHost::KEEP_ALL_REWARDS )
            ]
            .enum_( "ObservationsPolicy" )
            [
                  value( "LATEST_OBSERVATION_ONLY",  AgentHost::LATEST_OBSERVATION_ONLY )
                , value( "KEEP_ALL_OBSERVATIONS",    AgentHost::KEEP_ALL_OBSERVATIONS )
            ]
            .def(constructor<>())
            .def("startMission",                    startALEMissionSimple)
            .def("startMission",                    startALEMissionComplex)
            .def("peekWorldState",                  &ALEAgentHost::peekWorldState)
            .def("getWorldState",                   &ALEAgentHost::getWorldState)
            .def("setVideoPolicy",                  &ALEAgentHost::setVideoPolicy)
            .def("setRewardsPolicy",                &ALEAgentHost::setRewardsPolicy)
            .def("setObservationsPolicy",           &ALEAgentHost::setObservationsPolicy)
            .def("sendCommand",                     &ALEAgentHost::sendCommand)
            .def("getRecordingTemporaryDirectory",  &ALEAgentHost::getRecordingTemporaryDirectory)
            .def("setSeed",                         &ALEAgentHost::setSeed)
            .def(tostring(const_self))
    ,
#endif
        class_< MissionSpec >("MissionSpec")
            .def(constructor<>())
            .def(constructor< const std::string&, bool >())
            .def("getAsXML",                  &MissionSpec::getAsXML)
            .def("setSummary",                &MissionSpec::setSummary)
            .def("timeLimitInSeconds",        &MissionSpec::timeLimitInSeconds)
            .def("forceWorldReset",           &MissionSpec::forceWorldReset)
            .def("setWorldSeed",              &MissionSpec::setWorldSeed)
            .def("createDefaultTerrain",      &MissionSpec::createDefaultTerrain)
            .def("setTimeOfDay",              &MissionSpec::setTimeOfDay)
            .def("drawBlock",                 &MissionSpec::drawBlock)
            .def("drawCuboid",                &MissionSpec::drawCuboid)
            .def("drawItem",                  &MissionSpec::drawItem)
            .def("drawSphere",                &MissionSpec::drawSphere)
            .def("drawLine",                  &MissionSpec::drawLine)
            .def("startAt",                   &MissionSpec::startAt)
            .def("startAtWithPitchAndYaw",    &MissionSpec::startAtWithPitchAndYaw)
            .def("endAt",                     &MissionSpec::endAt)
            .def("setModeToCreative",         &MissionSpec::setModeToCreative)
            .def("setModeToSpectator",        &MissionSpec::setModeToSpectator)
            .def("requestVideo",              &MissionSpec::requestVideo)
            .def("requestVideoWithDepth",     &MissionSpec::requestVideoWithDepth)
            .def("setViewpoint",              &MissionSpec::setViewpoint)
            .def("rewardForReachingPosition", &MissionSpec::rewardForReachingPosition)
            .def("observeRecentCommands",     &MissionSpec::observeRecentCommands)
            .def("observeHotBar",             &MissionSpec::observeHotBar)
            .def("observeFullInventory",      &MissionSpec::observeFullInventory)
            .def("observeGrid",               &MissionSpec::observeGrid)
            .def("observeDistance",           &MissionSpec::observeDistance)
            .def("observeChat",               &MissionSpec::observeChat)
            .def("removeAllCommandHandlers",  &MissionSpec::removeAllCommandHandlers)
            .def("allowAllContinuousMovementCommands", &MissionSpec::allowAllContinuousMovementCommands)
            .def("allowContinuousMovementCommand", &MissionSpec::allowContinuousMovementCommand)
            .def("allowAllDiscreteMovementCommands", &MissionSpec::allowAllDiscreteMovementCommands)
            .def("allowDiscreteMovementCommand", &MissionSpec::allowDiscreteMovementCommand)
            .def("allowAllAbsoluteMovementCommands", &MissionSpec::allowAllAbsoluteMovementCommands)
            .def("allowAbsoluteMovementCommand", &MissionSpec::allowAbsoluteMovementCommand)
            .def("allowAllInventoryCommands", &MissionSpec::allowAllInventoryCommands)
            .def("allowInventoryCommand",     &MissionSpec::allowInventoryCommand)
            .def("allowAllChatCommands",      &MissionSpec::allowAllChatCommands)
            .def("getSummary",                &MissionSpec::getSummary)
            .def("getNumberOfAgents",         &MissionSpec::getNumberOfAgents)
            .def("isVideoRequested",          &MissionSpec::isVideoRequested)
            .def("getVideoWidth",             &MissionSpec::getVideoWidth)
            .def("getVideoHeight",            &MissionSpec::getVideoHeight)
            .def("getVideoChannels",          &MissionSpec::getVideoChannels)
            .def("getListOfCommandHandlers",  &getListOfCommandHandlers)
            .def("getAllowedCommands",        &getAllowedCommands)
            .def(tostring(const_self))
        ,
        class_< MissionRecordSpec >("MissionRecordSpec")
            .def(constructor<>())
            .def(constructor < std::string >())
            .def("recordMP4",               &recordMP4)
            .def("recordObservations",      &MissionRecordSpec::recordObservations)
            .def("recordRewards",           &MissionRecordSpec::recordRewards)
            .def("recordCommands",          &MissionRecordSpec::recordCommands)
            .def("setDestination",          &MissionRecordSpec::setDestination)
            .def(tostring(const_self))
        ,
        class_< ClientInfo >("ClientInfo")
            .def(constructor<>())
            .def(constructor<const std::string &>())
            .def(constructor<const std::string &, int>())
            .def_readonly("ip_address",     &ClientInfo::ip_address)
            .def_readonly("port",           &ClientInfo::port)
            .def(tostring(const_self))
        ,
        class_< ClientPool >("ClientPool")
            .def(constructor<>())
            .def("add",                     &ClientPool::add)
            .def(tostring(const_self))
        ,
        class_<ParameterSet>("ParameterSet")
            .def(constructor<>())
            .def(constructor<const std::string &>())
            .def("toJson",              &ParameterSet::toJson)
            .def("set",                 &ParameterSet::set)
            .def("get",                 &ParameterSet::get)
            .def("setInt",              &ParameterSet::setInt)
            .def("getInt",              &ParameterSet::getInt)
            .def("setDouble",           &ParameterSet::setDouble)
            .def("getDouble",           &ParameterSet::getDouble)
            .def("setBool",             &ParameterSet::setBool)
            .def("getBool",             &ParameterSet::getBool)
        ,
        class_< TimestampedString, boost::shared_ptr< TimestampedString > >("TimestampedString")
            .def("timestamp",             &getPosixTimeAsLong<TimestampedString>)
            .def_readonly("text",         &TimestampedString::text)
            .def(tostring(const_self))
        ,
        class_< TimestampedReward, boost::shared_ptr< TimestampedReward > >("TimestampedReward")
            .def("timestamp",             &getPosixTimeAsLong<TimestampedReward>)
            .def("hasValueOnDimension",   &TimestampedReward::hasValueOnDimension)
            .def("getValueOnDimension",   &TimestampedReward::getValueOnDimension)
            .def("getValue",              &TimestampedReward::getValue)
            .def(tostring(const_self))
        ,
        class_< TimestampedVideoFrame, boost::shared_ptr< TimestampedVideoFrame > >("TimestampedVideoFrame")
            .enum_("FrameType")
                [
                    value("VIDEO", TimestampedVideoFrame::VIDEO),
                    value("DEPTH_MAP", TimestampedVideoFrame::DEPTH_MAP),
                    value("LUMINANCE", TimestampedVideoFrame::LUMINANCE),
                    value("COLOUR_MAP", TimestampedVideoFrame::COLOUR_MAP)
                ]
            .def("timestamp",             &getPosixTimeAsLong<TimestampedVideoFrame>)
            .def_readonly("width",        &TimestampedVideoFrame::width)
            .def_readonly("height",       &TimestampedVideoFrame::height)
            .def_readonly("channels",     &TimestampedVideoFrame::channels)
            .def_readonly("xPos",         &TimestampedVideoFrame::xPos)
            .def_readonly("yPos",         &TimestampedVideoFrame::yPos)
            .def_readonly("zPos",         &TimestampedVideoFrame::zPos)
            .def_readonly("yaw",          &TimestampedVideoFrame::yaw)
            .def_readonly("pitch",        &TimestampedVideoFrame::pitch)
            .def_readonly("frametype",    &TimestampedVideoFrame::frametype)
            .def_readonly("pixels",       &TimestampedVideoFrame::pixels,               return_stl_iterator )
            .def(tostring(const_self))
      #ifdef TORCH
        ,
        def("getTorchTensorFromPixels", &getTorchTensorFromPixels)
      #endif
    ];
    register_exception_handler<xml_schema::exception>(&translateXMLSchemaException);
    register_exception_handler<MissionException>(&translateMissionException);
    return 0;
}
