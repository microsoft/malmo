// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

// Malmo:
#include <AgentHost.h>
#include <ClientPool.h>
#include <MissionInitSpec.h>
#include <StringServer.h>
#include <TCPClient.h>
#include <WorldState.h>
using namespace malmo;

// Boost:
#include <boost/shared_ptr.hpp>
#include <boost/thread.hpp>
using namespace boost::posix_time;

// STL:
#include <iostream>
using namespace std;

const short width = 240;
const int channels = 3;
const int num_pixels = width * width * channels;
const int num_frames = 50;
const milliseconds sleep_time(100);
const int commands_port = 10030;

boost::shared_ptr<MissionInitSpec> mission_init;

void handleControlMessages(TimestampedString message)
{
    // this will do nothing due to the lack of a header.
}

void runGameShim( )
{
    boost::asio::io_service io_service;
    boost::this_thread::sleep(sleep_time);

    mission_init->setClientCommandsPort(commands_port);
    const bool pretty_print = false;
    SendStringOverTCP(io_service, mission_init->getAgentAddress(), mission_init->getAgentMissionControlPort(), mission_init->getAsXML(pretty_print), true);

    vector<unsigned char> buffer(num_pixels);
    for (int i = 0; i < num_frames; i++){
        for (int r = 0, p = 0; r < width; r++){
            for (int c = 0; c < width; c++, p += 3){
                buffer[p] = r;
                buffer[p + 2] = width - c;
            }
        }

        int boxPos = (i * 200) / num_frames;
        for (int r = 0; r < 40; r++){
            for (int c = 0; c < 40; c++){
                int p = (boxPos + r) * width * 3 + (boxPos + c) * 3;
                buffer[p] = 255;
                buffer[p + 1] = 255;
                buffer[p + 2] = 255;
            }
        }

        buffer[0] = i;
        SendOverTCP(io_service, mission_init->getAgentAddress(), mission_init->getAgentVideoPort(), buffer, true);
        {
            std::stringstream observation;
            observation << "{" << '"' << "frame" << '"' << ":" << i << ", " << '"' << "step" << '"' << " : 0 }";
            SendStringOverTCP(io_service, mission_init->getAgentAddress(), mission_init->getAgentObservationsPort(), observation.str(), true);
        }

        boost::this_thread::sleep(sleep_time / 2);

        {
            std::stringstream observation;
            observation << "{" << '"' << "frame" << '"' << ":" << i << ", " << '"' << "step" << '"' << " : 1 }";
            SendStringOverTCP(io_service, mission_init->getAgentAddress(), mission_init->getAgentObservationsPort(), observation.str(), true);
        }

        boost::this_thread::sleep(sleep_time / 2);
    }

    SendStringOverTCP(io_service, mission_init->getAgentAddress(), mission_init->getAgentRewardsPort(), "{\"Reward\" : 100 }", true);

    std::stringstream end_xml;
    end_xml << "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><MissionEnded xmlns=\"http://ProjectMalmo.microsoft.com\"><Status>ENDED</Status><HumanReadableStatus>Mission ended normally</HumanReadableStatus><FinalReward>123.58</FinalReward></MissionEnded>";
    SendStringOverTCP(io_service, mission_init->getAgentAddress(), mission_init->getAgentMissionControlPort(), end_xml.str(), true);
    
    io_service.run();
}

void handleCommandMessages(TimestampedString message)
{
    cout << message.text << endl;
}

int runAgentHost(std::string filename)
{
    // Specify a (hopefully) non-existant location:
    bool threwError = false;
    try{
        MissionRecordSpec mission_record("c://path//that//probably//does//not//exist//output.tgz");
    }
    catch (const exception&){
        threwError = true;
    }

    if (!threwError){
        cout << "Was able to create MissionRecordSpec with bad path." << endl;
        return EXIT_FAILURE;
    }

    MissionSpec mission;
    mission.timeLimitInSeconds( 10 );
    mission.requestVideo( 240, 240 );

    MissionRecordSpec mission_record(filename);
    mission_record.recordMP4(10, 400000);
    mission_record.recordObservations();
    mission_record.recordRewards();
    mission_record.recordCommands();

    {
        MissionRecord deleteTest(mission_record);
    }
    {
        MissionRecord deleteTest(mission_record);
    }

    threwError = false;
    try{
        std::string temp_dir = mission_record.getTemporaryDirectory();
    }
    catch (const exception&){
        threwError = true;
    }

    if (!threwError){
        cout << "Was able to access temporary directory before record creation." << endl;
        return EXIT_FAILURE;
    }
    
    ClientInfo client_info( "127.0.0.1" );
  
    boost::asio::io_service io_service;
    
    StringServer clientMissionControlServer(io_service, client_info.port, handleControlMessages);
    clientMissionControlServer.confirmWithFixedReply( "MALMOOK" );
    clientMissionControlServer.expectSizeHeader(false);
    clientMissionControlServer.start();
    
    StringServer clientCommandsServer( io_service, commands_port, handleCommandMessages);
    clientCommandsServer.expectSizeHeader(false);
    clientCommandsServer.start();

    boost::thread bt(boost::bind(&boost::asio::io_service::run, &io_service));

    boost::this_thread::sleep(sleep_time);

    AgentHost agent_host;
    try {
        agent_host.startMission(mission, mission_record);
    }
    catch (const exception& e) {
        cout << "Error starting mission: " << e.what() << "\n" << endl;
        return EXIT_FAILURE;
    }

    try{
        std::string temp_dir = mission_record.getTemporaryDirectory();
    }
    catch (const exception&){
        cout << "Unable to access temporary directory after mission start.";
        return EXIT_FAILURE;
    }
    
    mission_init = agent_host.getMissionInit();

    boost::thread gameShim(runGameShim);

    boost::shared_ptr<WorldState> world_state;

    cout << "Waiting for the mission to start" << flush;
    do {
        cout << "." << flush;
        boost::this_thread::sleep(boost::posix_time::milliseconds(100));
        world_state = agent_host.getWorldState();
        for (auto e : world_state->errors)
            cout << "Error: " << e.text << endl;
    } while (!world_state->is_mission_running);
    cout << endl;

    do {
        {
            ostringstream oss;
            oss << "turn " << rand() / float(RAND_MAX) << "\n";
            agent_host.sendCommand(oss.str());
        }
        {
            ostringstream oss;
            oss << "move " << rand() / float(RAND_MAX) << "\n";
            agent_host.sendCommand(oss.str());
        }
        boost::this_thread::sleep(boost::posix_time::milliseconds(500));
        world_state = agent_host.getWorldState();
        cout << "video,observations,rewards received: "
            << world_state->number_of_video_frames_since_last_state << ","
            << world_state->number_of_observations_since_last_state << ","
            << world_state->number_of_rewards_since_last_state << endl;
        cout << "video,observations,rewards stored: "
            << world_state->video_frames.size() << ","
            << world_state->observations.size() << ","
            << world_state->rewards.size() << endl;
        if (!world_state->rewards.empty())
            cout << "Summed reward: " << world_state->rewards.front().value << endl;
        for (auto e : world_state->errors)
            cout << "Error: " << e.text << endl;
    } while (world_state->is_mission_running);

    cout << "Mission has stopped." << endl;

    gameShim.join();
    io_service.stop();
    bt.join();

    return EXIT_SUCCESS;
}

int main()
{
    std::string filename = "persistence.tgz";
    boost::filesystem::remove(filename);
    int code = runAgentHost(filename);

    if (code != EXIT_SUCCESS){
        return code;
    }

    if (boost::filesystem::exists(filename)){
        return EXIT_SUCCESS;
    }

    return EXIT_FAILURE;
}
