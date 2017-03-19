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
#include <MissionSpec.h>
using namespace malmo;

// STL:
#include <vector>
#include <string>
#include <exception>
using namespace std;

// Local:
#include "go_missionspec.h"

ptMissionSpec new_mission_spec() {
    try {
        MissionSpec * pt = new MissionSpec;
        return (void*)pt;
    } catch (const exception& e) {
        // returning NULL pointer
    }
    return NULL;
}

ptMissionSpec new_mission_spec_xml(const char* xml, int validate) {
    bool dovalidate = false;
    if (validate == 1) {
        dovalidate = true;
    }
    try {
        MissionSpec * pt = new MissionSpec(xml, dovalidate);
        return (void*)pt;
    } catch (const exception& e) {
        // returning NULL pointer
    }
    return NULL;
}

void free_mission_spec(ptMissionSpec mission_spec) {
    if (mission_spec != NULL) {
        MissionSpec * pt = (MissionSpec*)mission_spec;
        delete pt;
    }
}

int mission_spec_get_as_xml(ptMissionSpec pt, char* err, int prettyPrint, char* xml) {
    MS_CALL(
        bool pprint = false;
        if (prettyPrint == 1) {
            pprint = true;
        }
        string str_xml = mission_spec->getAsXML(pprint);
        if (str_xml.size() > MS_XML_BUFFER_SIZE) {
            strncpy(err, "Size of XML exceeds capacity", MS_ERROR_BUFFER_SIZE);
            return 1;
        }
        strncpy(xml, str_xml.c_str(), MS_XML_BUFFER_SIZE);
    )
}

// -------------------- settings for the server -------------------------

int mission_spec_set_summary            (ptMissionSpec pt, char* err, const char* summary)                                                   { MS_CALL(mission_spec->setSummary          (summary);) }
int mission_spec_time_limit_in_seconds  (ptMissionSpec pt, char* err, float s)                                                               { MS_CALL(mission_spec->timeLimitInSeconds  (s);) }
int mission_spec_create_default_terrain (ptMissionSpec pt, char* err)                                                                        { MS_CALL(mission_spec->createDefaultTerrain();) }
int mission_spec_set_world_seed         (ptMissionSpec pt, char* err, const char *seed)                                                      { MS_CALL(mission_spec->setWorldSeed        (seed);) }
int mission_spec_force_world_reset      (ptMissionSpec pt, char* err)                                                                        { MS_CALL(mission_spec->forceWorldReset     ();) }
int mission_spec_set_time_of_day        (ptMissionSpec pt, char* err, int t, int allowTimeToPass)                                            { MS_CALL(mission_spec->setTimeOfDay        (t, allowTimeToPass);) }
int mission_spec_draw_block             (ptMissionSpec pt, char* err, int x, int y, int z, const char* blockType)                            { MS_CALL(mission_spec->drawBlock           (x, y, z, blockType);) }
int mission_spec_draw_cuboid            (ptMissionSpec pt, char* err, int x1, int y1, int z1, int x2, int y2, int z2, const char* blockType) { MS_CALL(mission_spec->drawCuboid          (x1, y1, z1, x2, y2, z2, blockType);) }
int mission_spec_draw_item              (ptMissionSpec pt, char* err, int x, int y, int z, const char* itemType)                             { MS_CALL(mission_spec->drawItem            (x, y, z, itemType);) }
int mission_spec_draw_sphere            (ptMissionSpec pt, char* err, int x, int y, int z, int radius, const char* blockType)                { MS_CALL(mission_spec->drawSphere          (x, y, z, radius, blockType);) }
int mission_spec_draw_line              (ptMissionSpec pt, char* err, int x1, int y1, int z1, int x2, int y2, int z2, const char* blockType) { MS_CALL(mission_spec->drawLine            (x1, y1, z1, x2, y2, z2, blockType);) }

// -------------------- settings for the agents -------------------------

int mission_spec_start_at                     (ptMissionSpec pt, char* err, float x, float y, float z)                                        { MS_CALL(mission_spec->startAt                  (x, y, z);) }
int mission_spec_start_at_with_pitch_and_yaw  (ptMissionSpec pt, char* err, float x, float y, float z, float pitch, float yaw)                { MS_CALL(mission_spec->startAtWithPitchAndYaw   (x, y, z, pitch, yaw);) }
int mission_spec_end_at                       (ptMissionSpec pt, char* err, float x, float y, float z, float tolerance)                       { MS_CALL(mission_spec->endAt                    (x, y, z, tolerance);) }
int mission_spec_set_mode_to_creative         (ptMissionSpec pt, char* err)                                                                   { MS_CALL(mission_spec->setModeToCreative        ();) }
int mission_spec_set_mode_to_spectator        (ptMissionSpec pt, char* err)                                                                   { MS_CALL(mission_spec->setModeToSpectator       ();) }
int mission_spec_request_video                (ptMissionSpec pt, char* err, int width, int height)                                            { MS_CALL(mission_spec->requestVideo             (width, height);) }
int mission_spec_request_video_with_depth     (ptMissionSpec pt, char* err, int width, int height)                                            { MS_CALL(mission_spec->requestVideoWithDepth    (width, height);) }
int mission_spec_set_viewpoint                (ptMissionSpec pt, char* err, int viewpoint)                                                    { MS_CALL(mission_spec->setViewpoint             (viewpoint);) }
int mission_spec_reward_for_reaching_position (ptMissionSpec pt, char* err, float x, float y, float z, float amount, float tolerance)         { MS_CALL(mission_spec->rewardForReachingPosition(x, y, z, amount, tolerance);) }
int mission_spec_observe_recent_commands      (ptMissionSpec pt, char* err)                                                                   { MS_CALL(mission_spec->observeRecentCommands    ();) }
int mission_spec_observe_hot_bar              (ptMissionSpec pt, char* err)                                                                   { MS_CALL(mission_spec->observeHotBar            ();) }
int mission_spec_observe_full_inventory       (ptMissionSpec pt, char* err)                                                                   { MS_CALL(mission_spec->observeFullInventory     ();) }
int mission_spec_observe_grid                 (ptMissionSpec pt, char* err, int x1, int y1, int z1, int x2, int y2, int z2, const char* name) { MS_CALL(mission_spec->observeGrid              (x1, y1, z1, x2, y2, z2, name);) }
int mission_spec_observe_distance             (ptMissionSpec pt, char* err, float x, float y, float z, const char* name)                      { MS_CALL(mission_spec->observeDistance          (x, y, z, name);) }
int mission_spec_observe_chat                 (ptMissionSpec pt, char* err)                                                                   { MS_CALL(mission_spec->observeChat              ();) }

// -------------------- settings for the agents : command handlers -------------------------

int mission_spec_remove_all_command_handlers            (ptMissionSpec pt, char* err)                   { MS_CALL(mission_spec->removeAllCommandHandlers          ();) }
int mission_spec_allow_all_continuous_movement_commands (ptMissionSpec pt, char* err)                   { MS_CALL(mission_spec->allowAllContinuousMovementCommands();) }
int mission_spec_allow_continuous_movement_command      (ptMissionSpec pt, char* err, const char* verb) { MS_CALL(mission_spec->allowContinuousMovementCommand    (verb);) }
int mission_spec_allow_all_discrete_movement_commands   (ptMissionSpec pt, char* err)                   { MS_CALL(mission_spec->allowAllDiscreteMovementCommands  ();) }
int mission_spec_allow_discrete_movement_command        (ptMissionSpec pt, char* err, const char* verb) { MS_CALL(mission_spec->allowDiscreteMovementCommand      (verb);) }
int mission_spec_allow_all_absolute_movement_commands   (ptMissionSpec pt, char* err)                   { MS_CALL(mission_spec->allowAllAbsoluteMovementCommands  ();) }
int mission_spec_allow_absolute_movement_command        (ptMissionSpec pt, char* err, const char* verb) { MS_CALL(mission_spec->allowAbsoluteMovementCommand      (verb);) }
int mission_spec_allow_all_inventory_commands           (ptMissionSpec pt, char* err)                   { MS_CALL(mission_spec->allowAllInventoryCommands         ();) }
int mission_spec_allow_inventory_command                (ptMissionSpec pt, char* err, const char* verb) { MS_CALL(mission_spec->allowInventoryCommand             (verb);) }
int mission_spec_allow_all_chat_commands                (ptMissionSpec pt, char* err)                   { MS_CALL(mission_spec->allowAllChatCommands              ();) }

// ------------------------- information --------------------------------------

int mission_spec_get_summary(ptMissionSpec pt, char* err, char* summary) {
    MS_CALL(
        string str_summary = mission_spec->getSummary();
        if (str_summary.size() > MS_SUMMARY_BUFFER_SIZE) {
            strncpy(err, "Size of summary text exceeds capacity", MS_ERROR_BUFFER_SIZE);
            return 1;
        }
        strncpy(summary, str_summary.c_str(), MS_SUMMARY_BUFFER_SIZE);
    )
}

int mission_spec_get_number_of_agents(ptMissionSpec pt, char* err, int *response) {
    MS_CALL(
        *response = mission_spec->getNumberOfAgents();
    )
}

int mission_spec_is_video_requested(ptMissionSpec pt, char* err, int role, int *response) {
    MS_CALL(
        if (mission_spec->isVideoRequested(role)) {
            *response = 1;
        } else {
            *response = 0;
        }
    )
}

int mission_spec_get_video_width(ptMissionSpec pt, char* err, int role, int *response) {
    MS_CALL(
        *response = mission_spec->getVideoWidth(role);
    )
}

int mission_spec_get_video_height(ptMissionSpec pt, char* err, int role, int *response) {
    MS_CALL(
        *response = mission_spec->getVideoHeight(role);
    )
}

int mission_spec_get_video_channels(ptMissionSpec pt, char* err, int role, int *response) {
    MS_CALL(
        *response = mission_spec->getVideoChannels(role);
    )
}

int mission_spec_get_list_of_command_handlers(ptMissionSpec pt, char* err, int role, int* size, char** list) {
    MS_CALL(
        vector<string> vec_list = mission_spec->getListOfCommandHandlers(role);
        if (vec_list.size() > MS_MAX_COMMAND_HANDLERS) {
            strncpy(err, "Number of command handlers exceeds capacity", MS_ERROR_BUFFER_SIZE);
            return 1;
        }
        *size = vec_list.size();
        for (int i=0; i < vec_list.size(); ++i) {
            strncpy(list[i], vec_list[i].c_str(), MS_COMMAND_HANDLER_NCHARS);
        }
    )
}

int mission_spec_get_allowed_commands(ptMissionSpec pt, char* err, int role, const char* command_handler, int* size, char** list) {
    MS_CALL(
        vector<string> vec_list = mission_spec->getAllowedCommands(role, command_handler);
        if (vec_list.size() > MS_MAX_ACTIVE_COMMAND_HANDLERS) {
            strncpy(err, "Number of allowed command handlers exceeds capacity", MS_ERROR_BUFFER_SIZE);
            return 1;
        }
        *size = vec_list.size();
        for (int i=0; i < vec_list.size(); ++i) {
            strncpy(list[i], vec_list[i].c_str(), MS_COMMAND_HANDLER_NCHARS);
        }
    )
}
