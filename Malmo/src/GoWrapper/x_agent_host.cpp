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
#include <MissionSpec.h>
#include <MissionRecordSpec.h>
#include <ClientPool.h>
#include <WorldState.h>
using namespace malmo;

// STL:
#include <cstddef>
#include <cstring>
#include <string>
#include <exception>
#include <iostream>
using namespace std;

// Boost:
#include <boost/date_time/posix_time/posix_time_types.hpp>

// Local:
#include "x_definitions.h"
#include "x_agent_host.h"

// Cgo:
#include "_cgo_export.h"

// auxiliary ---------------------------------------------------------------------------------------

// converts ptime to TimestampData
timestamp_t timestamp_from_ptime(boost::posix_time::ptime const& pt) {

    timestamp_t ts;

    boost::gregorian::date date = pt.date();
    boost::posix_time::time_duration td = pt.time_of_day();

    ts.year    = (int)date.year();
    ts.month   = (int)date.month();
    ts.day     = (int)date.day();
    ts.hours   = td.hours();
    ts.minutes = td.minutes();
    ts.seconds = td.seconds();

    static std::int64_t resolution = boost::posix_time::time_duration::ticks_per_second();
    std::int64_t fracsecs = td.fractional_seconds();
    std::int64_t usecs = (resolution > 1000000) ? fracsecs / (resolution / 1000000) : fracsecs * (1000000 / resolution);

    ts.nanoseconds = td.fractional_seconds() * 1000;

    return ts;
}

// converts "pure" struct to MissionRecordSpec
MissionRecordSpec mrs_from_struct(mission_record_spec_t mrs) {
    MissionRecordSpec spec;
    if (mrs.record_mp4 == 1 && mrs.mp4_fps > 0 && mrs.mp4_bit_rate > 0) {
        spec.recordMP4(mrs.mp4_fps, mrs.mp4_bit_rate);
    }
    if (mrs.record_observations == 1) {
        spec.recordObservations();
    }
    if (mrs.record_rewards == 1) {
        spec.recordRewards();
    }
    if (mrs.record_commands == 1) {
        spec.recordCommands();
    }
    string dest(mrs.destination);
    if (dest.size() > 0) {
        spec.setDestination(dest);
    }
    return spec;
}

// constructor, destructor and enums ---------------------------------------------------------------

ptAgentHost new_agent_host() {
    try {
        AgentHost * pt = new AgentHost;
        return (void*)pt;
    } catch (const exception& e) {
        // returning NULL pointer
    }
    return NULL;
}

void free_agent_host(ptAgentHost agent_host) {
    if (agent_host != NULL) {
        AgentHost * pt = (AgentHost*)agent_host;
        delete pt;
    }
}

void agent_host_initialise_enums(
	int *latest_frame_only,
	int *keep_all_frames,

	int *latest_reward_only,
	int *sum_rewards,
	int *keep_all_rewards,

	int *latest_observation_only,
	int *keep_all_observations
) {
	*latest_frame_only       = AgentHost::LATEST_FRAME_ONLY;
	*keep_all_frames         = AgentHost::KEEP_ALL_FRAMES;
                                                                  
	*latest_reward_only      = AgentHost::LATEST_REWARD_ONLY;
	*sum_rewards             = AgentHost::SUM_REWARDS;
	*keep_all_rewards        = AgentHost::KEEP_ALL_REWARDS;
                                                                  
	*latest_observation_only = AgentHost::LATEST_OBSERVATION_ONLY;
	*keep_all_observations   = AgentHost::KEEP_ALL_OBSERVATIONS;
}

// methods from argument parser --------------------------------------------------------------------

int agent_host_parse(ptAgentHost pt, char* err, int argc, const char** argv) {
    AH_CALL(
        agent_host->parseArgs(argc, argv);
    )
}

int agent_host_add_optional_int_argument    (ptAgentHost pt, char* err, const char* name, const char* description, int defaultValue)         { AH_CALL(agent_host->addOptionalIntArgument   (name, description, defaultValue);) }
int agent_host_add_optional_float_argument  (ptAgentHost pt, char* err, const char* name, const char* description, double defaultValue)      { AH_CALL(agent_host->addOptionalFloatArgument (name, description, defaultValue);) }
int agent_host_add_optional_string_argument (ptAgentHost pt, char* err, const char* name, const char* description, const char* defaultValue) { AH_CALL(agent_host->addOptionalStringArgument(name, description, defaultValue);) }
int agent_host_add_optional_flag            (ptAgentHost pt, char* err, const char* name, const char* description)                           { AH_CALL(agent_host->addOptionalFlag          (name, description);) }

int agent_host_get_usage(ptAgentHost pt, char* err, char* usage) {
    AH_CALL(
        string str_usage = agent_host->getUsage();
        if (str_usage.size() > AH_USAGE_BUFFER_SIZE) {
            strncpy(err, "Size of usage string exceeds capacity", AH_ERROR_BUFFER_SIZE);
            return 1;
        }
        strncpy(usage, str_usage.c_str(), AH_USAGE_BUFFER_SIZE);
    )
}

int agent_host_received_argument(ptAgentHost pt, char* err, const char* name, int* response) {
    AH_CALL(
        if (agent_host->receivedArgument(name)) {
            *response = 1;
        } else {
            *response = 0;
        }
    )
}

int agent_host_get_int_argument(ptAgentHost pt, char* err, const char* name, int* response) {
    AH_CALL(
        *response = agent_host->getIntArgument(name);
    )
}

int agent_host_get_float_argument(ptAgentHost pt, char* err, const char* name, double* response) {
    AH_CALL(
        *response = agent_host->getFloatArgument(name);
    )
}

int agent_host_get_string_argument(ptAgentHost pt, char* err, const char* name, char* response) {
    AH_CALL(
        string str_arg = agent_host->getStringArgument(name);
        if (str_arg.size() > AH_STRING_ARG_SIZE) {
            strncpy(err, "Size of string argument exceeds capacity", AH_ERROR_BUFFER_SIZE);
            return 1;
        }
        strncpy(response, str_arg.c_str(), AH_STRING_ARG_SIZE);
    )
}

// methods from agent host -------------------------------------------------------------------------

int agent_host_start_mission(ptAgentHost pt, char* err, ptMissionSpec ptmission, client_pool_t cp, mission_record_spec_t mrs, int role, const char* unique_experiment_id) {
    AH_CALL(
        MissionSpec* mission = (MissionSpec*)ptmission;
        ClientPool client_pool;
        for (int i=0; i<cp.size; i++) {
            ClientInfo info(cp.addresses[i], cp.ports[i]);
            client_pool.add(info);
        }
        MissionRecordSpec spec = mrs_from_struct(mrs);
        agent_host->startMission(*mission, client_pool, spec, role, unique_experiment_id);
    )
}

int agent_host_start_mission_simple(ptAgentHost pt, char* err, ptMissionSpec ptmission, mission_record_spec_t mrs) {
    AH_CALL(
        MissionSpec* mission = (MissionSpec*)ptmission;
        MissionRecordSpec spec = mrs_from_struct(mrs);
        agent_host->startMission(*mission, spec);
    )
}

//#include <boost/make_shared.hpp>

const int REWARDS_MAX_NUMBER_DIMENSIONS = 4; // '4' allows for, e.g., 1,2,3 as 3D indices or 0,1,2

static inline void set_world_state(WorldState& ws, goptWorldState goptws) {

    // TODO: remove these debugging lines
    /*
    boost::posix_time::ptime ts = boost::posix_time::microsec_clock::universal_time();
    TimestampedString tss = TimestampedString(ts, "hello error thing");
    TimestampedReward tsr;
    tsr.createFromSimpleString(ts, "0:123,1:456,2:789,3:321");
    ws.errors.push_back (boost::make_shared<TimestampedString>(tss));
    ws.rewards.push_back(boost::make_shared<TimestampedReward>(tsr));
    */

    for (boost::shared_ptr<TimestampedVideoFrame> video_frame : ws.video_frames) {
        timestamp_t ts = timestamp_from_ptime(video_frame->timestamp);
        videoframe_t vf;
        vf.width    = video_frame->width;
        vf.height   = video_frame->height;
        vf.channels = video_frame->channels;
        vf.pitch    = video_frame->pitch;
        vf.yaw      = video_frame->yaw;
        vf.xPos     = video_frame->xPos;
        vf.yPos     = video_frame->yPos;
        vf.zPos     = video_frame->zPos;
        int npixels = video_frame->pixels.size();
	    unsigned char* pixels = (unsigned char*)malloc(npixels * sizeof(unsigned char));
        for (int i=0; i < npixels; i++) {
            pixels[i] = video_frame->pixels[i];
        }
        _callfromcpp_world_state_append_videoframe(goptws, &ts, &vf, npixels, pixels);
        free(pixels);
    }

    _callfromcpp_world_state_set_values(goptws,
        ws.has_mission_begun  ? 1 : 0,
        ws.is_mission_running ? 1 : 0,
        ws.number_of_video_frames_since_last_state,
        ws.number_of_rewards_since_last_state,
        ws.number_of_observations_since_last_state
    );

    for (boost::shared_ptr<TimestampedReward> reward : ws.rewards) {
        timestamp_t ts = timestamp_from_ptime(reward->timestamp);
        int dim_max = -1;
        for (int i=0; i < REWARDS_MAX_NUMBER_DIMENSIONS; i++) {
            if (reward->hasValueOnDimension(i)) {
                dim_max = i > dim_max ? i : dim_max;
            }
        }
        int ndim = dim_max + 1;
	    double* values = (double*)malloc(ndim * sizeof(double));
        for (int i=0; i < ndim; i++) {
            if (reward->hasValueOnDimension(i)) {
                values[i] = reward->getValueOnDimension(i);
            } else {
                values[i] = 0;
            }
        }
        _callfromcpp_world_state_append_reward(goptws, &ts, ndim, values);
        free(values);
    }

    for (boost::shared_ptr<TimestampedString> observation : ws.observations) {
        timestamp_t ts = timestamp_from_ptime(observation->timestamp);
        _callfromcpp_world_state_append_observation(goptws, &ts, const_cast<char*>(observation->text.c_str()), observation->text.size());
    }

    for (boost::shared_ptr<TimestampedString> controlmessage : ws.mission_control_messages) {
        timestamp_t ts = timestamp_from_ptime(controlmessage->timestamp);
        _callfromcpp_world_state_append_controlmessage(goptws, &ts, const_cast<char*>(controlmessage->text.c_str()), controlmessage->text.size());
    }

    for (boost::shared_ptr<TimestampedString> error : ws.errors) {
        timestamp_t ts = timestamp_from_ptime(error->timestamp);
        _callfromcpp_world_state_append_error(goptws, &ts, const_cast<char*>(error->text.c_str()), error->text.size());
    }
}

int agent_host_peek_world_state(ptAgentHost pt, char* err, goptWorldState goptws) {
    AH_CALL(
        WorldState ws = agent_host->peekWorldState();
        set_world_state(ws, goptws);
    )
}

int agent_host_get_world_state(ptAgentHost pt, char* err, goptWorldState goptws) {
    AH_CALL(
        WorldState ws = agent_host->getWorldState();
        set_world_state(ws, goptws);
    )
}

int agent_host_get_recording_temporary_directory(ptAgentHost pt, char* err, char* response) {
    AH_CALL(
        string str = agent_host->getRecordingTemporaryDirectory();
        if (str.size() > AH_RECDIR_BUFFER_SIZE) {
            strncpy(err, "Size of recording_directory string exceeds capacity", AH_ERROR_BUFFER_SIZE);
            return 1;
        }
        strncpy(response, str.c_str(), AH_RECDIR_BUFFER_SIZE);
    )
}

int agent_host_set_debug_output(ptAgentHost pt, char* err, int debug) {
    AH_CALL(
        bool flag = false;
        if (debug == 1) {
            flag = true;
        }
        agent_host->setDebugOutput(flag);
    )
}

int agent_host_set_video_policy(ptAgentHost pt, char* err, int videoPolicy) {
    AH_CALL(
        agent_host->setVideoPolicy((AgentHost::VideoPolicy)videoPolicy);
    )
}

int agent_host_set_rewards_policy(ptAgentHost pt, char* err, int rewardsPolicy) {
    AH_CALL(
        agent_host->setRewardsPolicy((AgentHost::RewardsPolicy)rewardsPolicy);
    )
}

int agent_host_set_observations_policy(ptAgentHost pt, char* err, int observationsPolicy) {
    AH_CALL(
        agent_host->setObservationsPolicy((AgentHost::ObservationsPolicy)observationsPolicy);
    )
}

int agent_host_send_command(ptAgentHost pt, char* err, const char* command) {
    AH_CALL(
        agent_host->sendCommand(command);
    )
}

int agent_host_send_command_turnbased(ptAgentHost pt, char* err, const char* command, const char* key) {
    AH_CALL(
        agent_host->sendCommand(command, key);
    )
}
