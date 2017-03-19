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

#ifndef MISSIONSPEC_H
#define MISSIONSPEC_H

#ifdef __cplusplus
extern "C" {
#endif

// max number of characters for error message text from C++ to Go
#define MS_ERROR_BUFFER_SIZE 1024

// max number of characters for summary text from C++ to Go
#define MS_SUMMARY_BUFFER_SIZE 1024

// max number of characters for xml text from C++ to Go
#define MS_XML_BUFFER_SIZE 1000000

// global variable to hold the results of commandhandler
#define MS_MAX_COMMAND_HANDLERS 100
#define MS_COMMAND_HANDLER_SIZE 128
char MS_COMMAND_HANDLERS[MS_MAX_COMMAND_HANDLERS][MS_COMMAND_HANDLER_SIZE];
int MS_COMMAND_HANDLERS_NUMBER;

// global variable to hold the results of commandhandler
#define MS_MAX_ACTIVE_COMMAND_HANDLERS 1000
char MS_ACTIVE_COMMAND_HANDLERS[MS_MAX_ACTIVE_COMMAND_HANDLERS][MS_COMMAND_HANDLER_SIZE];
int MS_ACTIVE_COMMAND_HANDLERS_NUMBER;

// macro to help with calling MissionSpec methods and handling exception errors
//   pt  -- pointer to MissionSpec
//   err -- error message buffer
#define MS_CALL(command)                                          \
    MissionSpec * mission_spec = (MissionSpec*)pt;                \
    try {                                                         \
        command                                                   \
    } catch (const exception& e) {                                \
        std::string message = std::string("ERROR: ") + e.what();  \
        strncpy(err, message.c_str(), MS_ERROR_BUFFER_SIZE);      \
        return 1;                                                 \
    }                                                             \
    return 0;

// pointer to MissionSpec type
typedef void* ptMissionSpec;

// constructor
ptMissionSpec new_mission_spec();

// alternative constructor
ptMissionSpec new_mission_spec_xml(const char* xml, int validate);

// destructor
void free_mission_spec(ptMissionSpec mission_spec);

// All functions return:
//  0 = OK
//  1 = failed; e.g. exception happend => see ERROR_MESSAGE

int mission_spec_get_as_xml(ptMissionSpec pt, char* err, int prettyPrint, char* xml);

// -------------------- settings for the server -------------------------

int mission_spec_set_summary            (ptMissionSpec pt, char* err, const char* summary);
int mission_spec_time_limit_in_seconds  (ptMissionSpec pt, char* err, float s);
int mission_spec_create_default_terrain (ptMissionSpec pt, char* err);
int mission_spec_set_world_seed         (ptMissionSpec pt, char* err, const char *seed);
int mission_spec_force_world_reset      (ptMissionSpec pt, char* err);
int mission_spec_set_time_of_day        (ptMissionSpec pt, char* err, int t, int allowTimeToPass);
int mission_spec_draw_block             (ptMissionSpec pt, char* err, int x, int y, int z, const char* blockType);
int mission_spec_draw_cuboid            (ptMissionSpec pt, char* err, int x1, int y1, int z1, int x2, int y2, int z2, const char* blockType);
int mission_spec_draw_item              (ptMissionSpec pt, char* err, int x, int y, int z, const char* itemType);
int mission_spec_draw_sphere            (ptMissionSpec pt, char* err, int x, int y, int z, int radius, const char* blockType);
int mission_spec_draw_line              (ptMissionSpec pt, char* err, int x1, int y1, int z1, int x2, int y2, int z2, const char* blockType);

// -------------------- settings for the agents -------------------------

int mission_spec_start_at                     (ptMissionSpec pt, char* err, float x, float y, float z);
int mission_spec_start_at_with_pitch_and_yaw  (ptMissionSpec pt, char* err, float x, float y, float z, float pitch, float yaw);
int mission_spec_end_at                       (ptMissionSpec pt, char* err, float x, float y, float z, float tolerance);
int mission_spec_set_mode_to_creative         (ptMissionSpec pt, char* err);
int mission_spec_set_mode_to_spectator        (ptMissionSpec pt, char* err);
int mission_spec_request_video                (ptMissionSpec pt, char* err, int width, int height);
int mission_spec_request_video_with_depth     (ptMissionSpec pt, char* err, int width, int height);
int mission_spec_set_viewpoint                (ptMissionSpec pt, char* err, int viewpoint);
int mission_spec_reward_for_reaching_position (ptMissionSpec pt, char* err, float x, float y, float z, float amount, float tolerance);
int mission_spec_observe_recent_commands      (ptMissionSpec pt, char* err);
int mission_spec_observe_hot_bar              (ptMissionSpec pt, char* err);
int mission_spec_observe_full_inventory       (ptMissionSpec pt, char* err);
int mission_spec_observe_grid                 (ptMissionSpec pt, char* err, int x1, int y1, int z1, int x2, int y2, int z2, const char* name);
int mission_spec_observe_distance             (ptMissionSpec pt, char* err, float x, float y, float z, const char* name);
int mission_spec_observe_chat                 (ptMissionSpec pt, char* err);

// -------------------- settings for the agents : command handlers -------------------------

int mission_spec_remove_all_command_handlers            (ptMissionSpec pt, char* err);
int mission_spec_allow_all_continuous_movement_commands (ptMissionSpec pt, char* err);
int mission_spec_allow_continuous_movement_command      (ptMissionSpec pt, char* err, const char* verb);
int mission_spec_allow_all_discrete_movement_commands   (ptMissionSpec pt, char* err);
int mission_spec_allow_discrete_movement_command        (ptMissionSpec pt, char* err, const char* verb);
int mission_spec_allow_all_absolute_movement_commands   (ptMissionSpec pt, char* err);
int mission_spec_allow_absolute_movement_command        (ptMissionSpec pt, char* err, const char* verb);
int mission_spec_allow_all_inventory_commands           (ptMissionSpec pt, char* err);
int mission_spec_allow_inventory_command                (ptMissionSpec pt, char* err, const char* verb);
int mission_spec_allow_all_chat_commands                (ptMissionSpec pt, char* err);

// ------------------------- information --------------------------------------

int mission_spec_get_summary                  (ptMissionSpec pt, char* err, char* summary);
int mission_spec_get_number_of_agents         (ptMissionSpec pt, char* err, int *response);
int mission_spec_is_video_requested           (ptMissionSpec pt, char* err, int role, int *response);
int mission_spec_get_video_width              (ptMissionSpec pt, char* err, int role, int *response);
int mission_spec_get_video_height             (ptMissionSpec pt, char* err, int role, int *response);
int mission_spec_get_video_channels           (ptMissionSpec pt, char* err, int role, int *response);
int mission_spec_get_list_of_command_handlers (ptMissionSpec pt, char* err, int role);
int mission_spec_get_allowed_commands         (ptMissionSpec pt, char* err, int role, const char* command_handler);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif // MISSIONSPEC_H
