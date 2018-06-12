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

// Boost:
#include <boost/python.hpp>
#include <boost/python/exception_translator.hpp>
#include <boost/python/register_ptr_to_python.hpp>
#include <boost/python/suite/indexing/vector_indexing_suite.hpp>
#include <boost/python/to_python_converter.hpp>

// Malmo:
#include <AgentHost.h>
#include <Logger.h>
#ifdef WRAP_ALE
    #include <ALEAgentHost.h>
#endif
#include <ClientPool.h>
#include <MissionSpec.h>
#include <ParameterSet.h>
using namespace malmo;

// STL:
#include <sstream>
#include <cstdint>

// Python:
#include <datetime.h>

// Converts a python list to a vector of strings. Throws a python exception if the conversion fails.
std::vector< std::string > listToStrings( const boost::python::list& list )
{
    std::vector<std::string> strings;
    for( int i = 0; i < len( list ); i++ )
        strings.push_back( boost::python::extract< std::string >( list[i] ) );
    return strings;
}

// A Python wrapper around ArgumentParser::parse().
void parsePythonList( ArgumentParser* p, const boost::python::list& list )
{
    p->parse( listToStrings( list ) );
}

// Make sure we get all of the useful information from std::exception
void translateXMLStdException(std::exception const& e)
{
    std::ostringstream oss;
    oss << "Caught std::exception: " << e.what() << "\n";
    PyErr_SetString(PyExc_RuntimeError, oss.str().c_str() );
}

PyObject* missionExceptionType = NULL;

PyObject* createExceptionClass(const char* name, PyObject* baseTypeObj = PyExc_Exception)
{
    std::string scopeName = boost::python::extract<std::string>(boost::python::scope().attr("__name__"));
    std::string qualifiedName0 = scopeName + "." + name;
    char* qualifiedName1 = const_cast<char*>(qualifiedName0.c_str());

    PyObject* typeObj = PyErr_NewException(qualifiedName1, baseTypeObj, 0);
    if (!typeObj)
        boost::python::throw_error_already_set();
    boost::python::scope().attr(name) = boost::python::handle<>(boost::python::borrowed(typeObj));
    return typeObj;
}

void translateMissionException(MissionException const& e)
{
    boost::python::object wrapped_exception(e);
    boost::python::object exc_type(boost::python::handle<>(boost::python::borrowed(missionExceptionType)));
    exc_type.attr("details") = wrapped_exception;
    PyErr_SetString(missionExceptionType, e.what());
}

void (AgentHost::*startMissionSimple)(const MissionSpec&, const MissionRecordSpec&) = &AgentHost::startMission;
void (AgentHost::*startMissionComplex)(const MissionSpec&, const ClientPool&, const MissionRecordSpec&, int, std::string) = &AgentHost::startMission;

void (AgentHost::*sendCommand)(std::string) = &AgentHost::sendCommand;
void (AgentHost::*sendCommandWithKey)(std::string, std::string) = &AgentHost::sendCommand;

void (MissionRecordSpec::*recordMP4General)(int, int64_t bit_rate) = &MissionRecordSpec::recordMP4;
void (MissionRecordSpec::*recordMP4Specific)(TimestampedVideoFrame::FrameType, int, int64_t, bool) = &MissionRecordSpec::recordMP4;

#ifdef WRAP_ALE
void (ALEAgentHost::*startALEMissionSimple)(const MissionSpec&, const MissionRecordSpec&) = &ALEAgentHost::startMission;
void (ALEAgentHost::*startALEMissionComplex)(const MissionSpec&, const ClientPool&, const MissionRecordSpec&, int, std::string) = &ALEAgentHost::startMission;
#endif

struct ptime_to_python_datetime
{
    static PyObject* convert(boost::posix_time::ptime const& pt)
    {
        // Convert posix_time into python DateTime object:
        boost::gregorian::date date = pt.date();
        boost::posix_time::time_duration td = pt.time_of_day();
        static std::int64_t resolution = boost::posix_time::time_duration::ticks_per_second();
        std::int64_t fracsecs = td.fractional_seconds();
        std::int64_t usecs = (resolution > 1000000) ? fracsecs / (resolution / 1000000) : fracsecs * (1000000 / resolution);

        return PyDateTime_FromDateAndTime((int)date.year(), (int)date.month(), (int)date.day(), (int)td.hours(), (int)td.minutes(), (int)td.seconds(), (int)usecs);
    }
};

struct unsigned_char_vec_to_python_array
{
    static PyObject* convert(std::vector<unsigned char> const& vec)
    {
        const char* buffer = reinterpret_cast<const char*>(vec.data());
        return PyByteArray_FromStringAndSize(buffer, vec.size());
    }
};

// Defines the API available to Python.
BOOST_PYTHON_MODULE(MalmoPython)
{
    using namespace boost::python;
    missionExceptionType = createExceptionClass("MissionException", PyExc_RuntimeError);

    // Bind the converter for posix_time to python DateTime
    PyDateTime_IMPORT;
    to_python_converter<boost::posix_time::ptime, ptime_to_python_datetime>();
    to_python_converter<std::vector<unsigned char>, unsigned_char_vec_to_python_array>();

    enum_< MissionException::MissionErrorCode >("MissionErrorCode")
        .value("MISSION_BAD_ROLE_REQUEST", MissionException::MISSION_BAD_ROLE_REQUEST)
        .value("MISSION_BAD_VIDEO_REQUEST", MissionException::MISSION_BAD_VIDEO_REQUEST)
        .value("MISSION_ALREADY_RUNNING", MissionException::MISSION_ALREADY_RUNNING)
        .value("MISSION_INSUFFICIENT_CLIENTS_AVAILABLE", MissionException::MISSION_INSUFFICIENT_CLIENTS_AVAILABLE)
        .value("MISSION_TRANSMISSION_ERROR", MissionException::MISSION_TRANSMISSION_ERROR)
        .value("MISSION_SERVER_WARMING_UP", MissionException::MISSION_SERVER_WARMING_UP)
        .value("MISSION_SERVER_NOT_FOUND", MissionException::MISSION_SERVER_NOT_FOUND)
        .value("MISSION_NO_COMMAND_PORT", MissionException::MISSION_NO_COMMAND_PORT)
        .value("MISSION_BAD_INSTALLATION", MissionException::MISSION_BAD_INSTALLATION)
        .value("MISSION_CAN_NOT_KILL_BUSY_CLIENT", MissionException::MISSION_CAN_NOT_KILL_BUSY_CLIENT)
        .value("MISSION_CAN_NOT_KILL_IRREPLACEABLE_CLIENT", MissionException::MISSION_CAN_NOT_KILL_IRREPLACEABLE_CLIENT)
        ;

    enum_< Logger::LoggingSeverityLevel >("LoggingSeverityLevel")
        .value("LOG_OFF", Logger::LOG_OFF)
        .value("LOG_ERRORS", Logger::LOG_ERRORS)
        .value("LOG_WARNINGS", Logger::LOG_WARNINGS)
        .value("LOG_INFO", Logger::LOG_INFO)
        .value("LOG_FINE", Logger::LOG_FINE)
        .value("LOG_TRACE", Logger::LOG_TRACE)
        .value("LOG_ALL", Logger::LOG_ALL)
        ;

    enum_< Logger::LoggingComponent >("LoggingComponent")
        .value("LOG_TCP", Logger::LOG_TCP)
        .value("LOG_RECORDING", Logger::LOG_RECORDING)
        .value("LOG_VIDEO", Logger::LOG_VIDEO)
        .value("LOG_AGENTHOST", Logger::LOG_AGENTHOST)
        .value("LOG_ALL_COMPONENTS", Logger::LOG_ALL_COMPONENTS)
        ;

    def("setLogging", &Logger::setLogging);
    def("appendToLog", &Logger::appendToLog);
    def("setLoggingComponent", &Logger::setLoggingComponent);

    class_< MissionException >("MissionExceptionDetails", init< const std::string&, MissionException::MissionErrorCode >())
        .add_property("errorCode", &MissionException::getMissionErrorCode)
        .add_property("message", &MissionException::getMessage);

    class_< ArgumentParser, boost::noncopyable >("ArgumentParser", init< const std::string& >())
        .def( "parse",                     &parsePythonList )
        .def( "addOptionalIntArgument",    &ArgumentParser::addOptionalIntArgument )
        .def( "addOptionalFloatArgument",  &ArgumentParser::addOptionalFloatArgument )
        .def( "addOptionalStringArgument", &ArgumentParser::addOptionalStringArgument )
        .def( "addOptionalFlag",           &ArgumentParser::addOptionalFlag )
        .def( "getUsage",                  &ArgumentParser::getUsage )
        .def( "receivedArgument",          &ArgumentParser::receivedArgument )
        .def( "getIntArgument",            &ArgumentParser::getIntArgument )
        .def( "getFloatArgument",          &ArgumentParser::getFloatArgument )
        .def( "getStringArgument",         &ArgumentParser::getStringArgument )
    ;
    class_< WorldState >( "WorldState", no_init )
        .def_readonly( "is_mission_running",                      &WorldState::is_mission_running )
        .def_readonly( "has_mission_begun",                       &WorldState::has_mission_begun )
        .def_readonly( "number_of_observations_since_last_state", &WorldState::number_of_observations_since_last_state )
        .def_readonly( "number_of_rewards_since_last_state",      &WorldState::number_of_rewards_since_last_state )
        .def_readonly( "number_of_video_frames_since_last_state", &WorldState::number_of_video_frames_since_last_state )
        .def_readonly( "observations",                            &WorldState::observations )
        .def_readonly( "rewards",                                 &WorldState::rewards )
        .def_readonly( "video_frames",                            &WorldState::video_frames )
        .def_readonly( "mission_control_messages",                &WorldState::mission_control_messages )
        .def_readonly( "errors",                                  &WorldState::errors )
        .def(self_ns::str(self_ns::self))
    ;
    enum_< AgentHost::VideoPolicy >( "VideoPolicy" )
        .value( "LATEST_FRAME_ONLY",  AgentHost::LATEST_FRAME_ONLY )
        .value( "KEEP_ALL_FRAMES",    AgentHost::KEEP_ALL_FRAMES )
    ;
    enum_< AgentHost::RewardsPolicy >( "RewardsPolicy" )
        .value( "LATEST_REWARD_ONLY",  AgentHost::LATEST_REWARD_ONLY )
        .value( "SUM_REWARDS",         AgentHost::SUM_REWARDS )
        .value( "KEEP_ALL_REWARDS",    AgentHost::KEEP_ALL_REWARDS )
    ;
    enum_< AgentHost::ObservationsPolicy >( "ObservationsPolicy" )
        .value( "LATEST_OBSERVATION_ONLY",  AgentHost::LATEST_OBSERVATION_ONLY )
        .value( "KEEP_ALL_OBSERVATIONS",    AgentHost::KEEP_ALL_OBSERVATIONS )
    ;

    class_< AgentHost, bases< ArgumentParser >, boost::noncopyable >("AgentHost", init<>())
        .def( "startMission",                   startMissionSimple )
        .def( "startMission",                   startMissionComplex )
        .def( "killClient",                     &AgentHost::killClient )
        .def( "peekWorldState",                 &AgentHost::peekWorldState )
        .def( "getWorldState",                  &AgentHost::getWorldState )
        .def( "setVideoPolicy",                 &AgentHost::setVideoPolicy )
        .def( "setRewardsPolicy",               &AgentHost::setRewardsPolicy )
        .def( "setObservationsPolicy",          &AgentHost::setObservationsPolicy )
        .def( "sendCommand",                    sendCommand )
        .def( "sendCommand",                    sendCommandWithKey )
        .def("getRecordingTemporaryDirectory",  &AgentHost::getRecordingTemporaryDirectory)
        .def( "setDebugOutput",                 &AgentHost::setDebugOutput )
        .def(self_ns::str(self_ns::self))
    ;
#ifdef WRAP_ALE
    class_< ALEAgentHost, bases< ArgumentParser >, boost::noncopyable >("ALEAgentHost", init<>())
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
        .def(self_ns::str(self_ns::self))
        ;
#endif
    class_< MissionSpec >( "MissionSpec", init<>() )
        .def(init< const std::string&, bool >())
        .def("getAsXML",                  &MissionSpec::getAsXML)
        .def("setSummary",                &MissionSpec::setSummary)
        .def("timeLimitInSeconds",        &MissionSpec::timeLimitInSeconds)
        .def("createDefaultTerrain",      &MissionSpec::createDefaultTerrain)
        .def("forceWorldReset",           &MissionSpec::forceWorldReset)
        .def("setWorldSeed",              &MissionSpec::setWorldSeed)
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
        .def("getListOfCommandHandlers",  &MissionSpec::getListOfCommandHandlers)
        .def("getAllowedCommands",        &MissionSpec::getAllowedCommands)
        .def(self_ns::str(self_ns::self))
    ;
    class_< MissionRecordSpec >("MissionRecordSpec", init<>())
        .def(init < std::string >())
        .def("recordMP4",               recordMP4General)
        .def("recordMP4",               recordMP4Specific)
        .def("recordBitmaps",           &MissionRecordSpec::recordBitmaps)
        .def("recordObservations",      &MissionRecordSpec::recordObservations)
        .def("recordRewards",           &MissionRecordSpec::recordRewards)
        .def("recordCommands",          &MissionRecordSpec::recordCommands)
        .def("setDestination",          &MissionRecordSpec::setDestination)
        .def(self_ns::str(self_ns::self))
    ;
    register_ptr_to_python< boost::shared_ptr< ClientInfo > >();
    class_< ClientInfo >("ClientInfo", init<>())
        .def(init<const std::string &>())
        .def(init<const std::string &, int>()) // address & control_port
        .def(init<const std::string &, int, int>()) // address, control port and command port
        .def_readonly("ip_address",     &ClientInfo::ip_address)
        .def_readonly("control_port",           &ClientInfo::control_port)
        .def_readonly("command_port",           &ClientInfo::command_port)
        .def(self_ns::str(self_ns::self))
    ;
    class_< std::vector< boost::shared_ptr< ClientInfo > > >( "ClientInfoVector" )
        .def( vector_indexing_suite< std::vector< boost::shared_ptr< ClientInfo > >, true >() )
    ;
    class_< ClientPool >("ClientPool", init<>())
        .def("add",                     &ClientPool::add)
        .def_readonly("clients",       &ClientPool::clients)
        .def(self_ns::str(self_ns::self))
    ;
    class_<ParameterSet>("ParameterSet", init<>())
        .def(init<const std::string &>())
        .def("toJson",                  &ParameterSet::toJson)
        .def("set",                     &ParameterSet::set)
        .def("get",                     &ParameterSet::get)
        .def("setInt",                  &ParameterSet::setInt)
        .def("getInt",                  &ParameterSet::getInt)
        .def("setDouble",               &ParameterSet::setDouble)
        .def("getDouble",               &ParameterSet::getDouble)
        .def("setBool",                 &ParameterSet::setBool)
        .def("getBool",                 &ParameterSet::getBool)
    ;
    register_ptr_to_python< boost::shared_ptr< TimestampedString > >();
    class_< TimestampedString >("TimestampedString", no_init)
        .add_property( "timestamp",   make_getter(&TimestampedString::timestamp, return_value_policy<return_by_value>()))
        .def_readonly( "text",        &TimestampedString::text )
        .def(self_ns::str(self_ns::self))
    ;
    register_ptr_to_python< boost::shared_ptr< TimestampedReward > >();
    class_< TimestampedReward >( "TimestampedReward", no_init )
        .add_property( "timestamp",   make_getter(&TimestampedReward::timestamp, return_value_policy<return_by_value>()))
        .def("hasValueOnDimension",   &TimestampedReward::hasValueOnDimension)
        .def("getValueOnDimension",   &TimestampedReward::getValueOnDimension)
        .def("getValue",              &TimestampedReward::getValue)
        .def(self_ns::str(self_ns::self))
    ;

    enum_< TimestampedVideoFrame::FrameType >("FrameType")
        .value("VIDEO", TimestampedVideoFrame::VIDEO)
        .value("DEPTH_MAP", TimestampedVideoFrame::DEPTH_MAP)
        .value("LUMINANCE", TimestampedVideoFrame::LUMINANCE)
        .value("COLOUR_MAP", TimestampedVideoFrame::COLOUR_MAP);

    register_ptr_to_python< boost::shared_ptr< TimestampedVideoFrame > >();
    class_< TimestampedVideoFrame >( "TimestampedVideoFrame", no_init )
        .add_property( "timestamp",   make_getter(&TimestampedVideoFrame::timestamp, return_value_policy<return_by_value>()))
        .def_readonly( "width",       &TimestampedVideoFrame::width )
        .def_readonly( "height",      &TimestampedVideoFrame::height )
        .def_readonly( "channels",    &TimestampedVideoFrame::channels )
        .def_readonly( "xPos",        &TimestampedVideoFrame::xPos)
        .def_readonly( "yPos",        &TimestampedVideoFrame::yPos)
        .def_readonly( "zPos",        &TimestampedVideoFrame::zPos)
        .def_readonly( "yaw",         &TimestampedVideoFrame::yaw)
        .def_readonly( "pitch",       &TimestampedVideoFrame::pitch)
        .def_readonly( "frametype",   &TimestampedVideoFrame::frametype)
        .add_property( "pixels",      make_getter(&TimestampedVideoFrame::pixels, return_value_policy<return_by_value>()))
        .def(self_ns::str(self_ns::self))
    ;
    class_< std::vector< boost::shared_ptr< TimestampedString > > >( "TimestampedStringVector" )
        .def( vector_indexing_suite< std::vector< boost::shared_ptr< TimestampedString > >, true >() )
    ;
    class_< std::vector< boost::shared_ptr< TimestampedReward > > >( "TimestampedRewardVector" )
        .def( vector_indexing_suite< std::vector< boost::shared_ptr< TimestampedReward > >, true >() )
    ;
    class_< std::vector< boost::shared_ptr< TimestampedVideoFrame > > >( "TimestampedVideoFrameVector" )
        .def( vector_indexing_suite< std::vector< boost::shared_ptr< TimestampedVideoFrame > >, true >() )
    ;
    class_< std::vector< std::string > >( "StringVector" )
        .def( vector_indexing_suite< std::vector< std::string >, true >() )
    ;
    register_exception_translator<std::exception>(&translateXMLStdException);
    register_exception_translator<MissionException>(&translateMissionException);
}
