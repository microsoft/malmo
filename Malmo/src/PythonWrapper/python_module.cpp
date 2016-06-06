// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

// Malmo:
#include <AgentHost.h>
#ifdef WRAP_ALE
    #include <ALEAgentHost.h>
#endif
#include <ClientPool.h>
#include <MissionSpec.h>
#include <ParameterSet.h>
using namespace malmo;

// Boost:
#include <boost/python.hpp>
#include <boost/python/exception_translator.hpp>
#include <boost/python/register_ptr_to_python.hpp>
#include <boost/python/suite/indexing/vector_indexing_suite.hpp>
#include <boost/python/to_python_converter.hpp>

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

// Make sure we get all of the useful information from xml_schema::exception
void translateXMLSchemaException(xml_schema::exception const& e)
{
    std::ostringstream oss;
    oss << "Caught xml_schema::exception: " << e.what() << "\n" << e;
    PyErr_SetString(PyExc_RuntimeError, oss.str().c_str() );
}

void (AgentHost::*startMissionSimple)(const MissionSpec&, const MissionRecordSpec&) = &AgentHost::startMission;
void (AgentHost::*startMissionComplex)(const MissionSpec&, const ClientPool&, const MissionRecordSpec&, int, std::string) = &AgentHost::startMission;

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

        return PyDateTime_FromDateAndTime((int)date.year(), (int)date.month(), (int)date.day(), td.hours(), td.minutes(), td.seconds(), (int)usecs);
    }
};

// Defines the API available to Python.
BOOST_PYTHON_MODULE(MalmoPython)
{
    using namespace boost::python;

    // Bind the converter for posix_time to python DateTime
    PyDateTime_IMPORT;
    to_python_converter<boost::posix_time::ptime, ptime_to_python_datetime>();

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
    register_ptr_to_python< boost::shared_ptr< WorldState > >();
    class_< WorldState >( "WorldState", no_init )
        .def_readonly( "is_mission_running",                      &WorldState::is_mission_running )
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
        .def( "startMission",              startMissionSimple )
        .def( "startMission",              startMissionComplex )
        .def( "getWorldState",             &AgentHost::getWorldState )
        .def( "setVideoPolicy",            &AgentHost::setVideoPolicy )
        .def( "setRewardsPolicy",          &AgentHost::setRewardsPolicy )
        .def( "setObservationsPolicy",     &AgentHost::setObservationsPolicy )
        .def( "sendCommand",               &AgentHost::sendCommand )
	.def(self_ns::str(self_ns::self))
    ;
#ifdef WRAP_ALE
    class_< ALEAgentHost, bases< ArgumentParser >, boost::noncopyable >("ALEAgentHost", init<>())
        .def("startMission", startALEMissionSimple)
        .def("startMission", startALEMissionComplex)
        .def("getWorldState", &ALEAgentHost::getWorldState)
        .def("setVideoPolicy", &ALEAgentHost::setVideoPolicy)
        .def("setRewardsPolicy", &ALEAgentHost::setRewardsPolicy)
        .def("setObservationsPolicy", &ALEAgentHost::setObservationsPolicy)
        .def("sendCommand", &ALEAgentHost::sendCommand)
	.def(self_ns::str(self_ns::self))
        ;
#endif
    class_< MissionSpec >( "MissionSpec", init<>() )
        .def(init< const std::string&, bool >())
        .def("getAsXML",                  &MissionSpec::getAsXML)
        .def("timeLimitInSeconds",        &MissionSpec::timeLimitInSeconds)
        .def("createDefaultTerrain",      &MissionSpec::createDefaultTerrain)
        .def("setTimeOfDay",              &MissionSpec::setTimeOfDay)
        .def("drawBlock",                 &MissionSpec::drawBlock)
        .def("drawCuboid",                &MissionSpec::drawCuboid)
        .def("drawItem",                  &MissionSpec::drawItem)
        .def("drawSphere",                &MissionSpec::drawSphere)
        .def("drawLine",                  &MissionSpec::drawLine)
        .def("startAt",                   &MissionSpec::startAt)
        .def("endAt",                     &MissionSpec::endAt)
        .def("setModeToCreative",         &MissionSpec::setModeToCreative)
        .def("setModeToSpectator",        &MissionSpec::setModeToSpectator)
        .def("requestVideo",              &MissionSpec::requestVideo)
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
        .def("getNumberOfAgents",         &MissionSpec::getNumberOfAgents)
        .def("isVideoRequested",          &MissionSpec::isVideoRequested)
        .def("getVideoWidth",             &MissionSpec::getVideoWidth)
        .def("getVideoHeight",            &MissionSpec::getVideoHeight)
        .def("getVideoChannels",          &MissionSpec::getVideoChannels)
	.def(self_ns::str(self_ns::self))
    ;
    class_< MissionRecordSpec >("MissionRecordSpec", init<>())
        .def(init < std::string >())
        .def("recordMP4",               &MissionRecordSpec::recordMP4)
        .def("recordObservations",      &MissionRecordSpec::recordObservations)
        .def("recordRewards",           &MissionRecordSpec::recordRewards)
        .def("recordCommands",          &MissionRecordSpec::recordCommands)
        .def("getTemporaryDirectory",   &MissionRecordSpec::getTemporaryDirectory)
	.def(self_ns::str(self_ns::self))
    ;
    class_< ClientInfo >("ClientInfo", init<>())
        .def(init<const std::string &>())
        .def(init<const std::string &, int>())
        .def_readonly("ip_address",     &ClientInfo::ip_address)
        .def_readonly("port",           &ClientInfo::port)
	.def(self_ns::str(self_ns::self))
    ;
    class_< ClientPool >("ClientPool", init<>())
        .def("add",                     &ClientPool::add)
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
    class_< TimestampedString >("TimestampedString", no_init)
        .add_property( "timestamp", make_getter(&TimestampedString::timestamp, return_value_policy<return_by_value>()))
        .def_readonly( "text",        &TimestampedString::text )
	.def(self_ns::str(self_ns::self))
    ;
    class_< TimestampedFloat >( "TimestampedFloat", no_init )
        .add_property( "timestamp", make_getter(&TimestampedString::timestamp, return_value_policy<return_by_value>()))
        .def_readonly( "value",       &TimestampedFloat::value )
	.def(self_ns::str(self_ns::self))
    ;
    class_< TimestampedVideoFrame >( "TimestampedVideoFrame", no_init )
        .add_property( "timestamp", make_getter(&TimestampedString::timestamp, return_value_policy<return_by_value>()))
        .def_readonly( "width",       &TimestampedVideoFrame::width )
        .def_readonly( "height",      &TimestampedVideoFrame::height )
        .def_readonly( "channels",    &TimestampedVideoFrame::channels )
        .def_readonly( "pixels",      &TimestampedVideoFrame::pixels )
	.def(self_ns::str(self_ns::self))
    ;
    class_< std::vector< TimestampedString > >( "TimestampedStringVector" )
        .def( vector_indexing_suite< std::vector< TimestampedString > >() )
    ;
    class_< std::vector< TimestampedFloat > >( "TimestampedFloatVector" )
        .def( vector_indexing_suite< std::vector< TimestampedFloat > >() )
    ;
    class_< std::vector< TimestampedVideoFrame > >( "TimestampedVideoFrameVector" )
        .def( vector_indexing_suite< std::vector< TimestampedVideoFrame > >() )
    ;
    class_< std::vector< unsigned char > >( "UnsignedCharVector")
        .def( vector_indexing_suite< std::vector< unsigned char > >() )
    ;
    register_exception_translator<xml_schema::exception>(&translateXMLSchemaException);
}
