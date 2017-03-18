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

package malmo

/*
#include "go_missionspec.h"
*/
import "C"

// MissionSpec specifies a mission to be run.
type MissionSpec struct {
	mission_spec C.ptMissionSpec // pointer to C.MissionSpec
}

// NewMissionSpec constructs a mission with default parameters: a flat world with a 10 seconds time limit and continuous movement.
func NewMissionSpec() (o *MissionSpec) {
	o = new(MissionSpec)
	o.mission_spec = C.new_mission_spec()
	return
}

// NewMissionSpecXML Constructs a mission from the supplied XML as specified here: <a href="../Schemas/Mission.html">Schemas/Mission.html</a>
// xml -- The full XML of the mission.
// validate -- If true, then throws an xml_schema::exception if the XML is not compliant with the schema.
func NewMissionSpecXML(xml string, validate bool) {
	panic("TODO")
}

// Free deallocates MissionSpec object
func (o *MissionSpec) Free() {
	if o.mission_spec != nil {
		C.free_mission_spec(o.mission_spec)
	}
}

// GetAsXML gets the mission specification as an XML string. Only use if you want to save the mission to file.
// prettyPrint -- If true, add indentation and newlines to the XML to make it more readable.
// returns The mission specification as an XML string.
func (o MissionSpec) GetAsXML(prettyPrint bool) string {
	panic("TODO")
}

// -------------------- settings for the server -------------------------

// SetSummary Sets the summary description of the mission.
// summary -- The string describing the mission. Shorter strings display best.
func (o *MissionSpec) SetSummary(summary string) {
	panic("TODO")
}

// TimeLimitInSeconds Sets the time limit for the mission.
// s -- The time limit in seconds.
func (o *MissionSpec) TimeLimitInSeconds(s float32) {
	C.mission_spec_time_limit_in_seconds(o.mission_spec, C.float(s))
}

// CreateDefaultTerrain makes a world using Minecraft's terrain generator, instead of the default flat world.
// Calling this will reset the world seed and forceReset flag - see setWorldSeed() and forceWorldReset().
func (o *MissionSpec) CreateDefaultTerrain() {
	panic("TODO")
}

// SetWorldSeed sets the seed used for Minecraft's terrain generation.
// Call this after the world generator has been set (eg after calling createDefaultTerrain() ).
func (o *MissionSpec) SetWorldSeed(seed string) {
	panic("TODO")
}

// ForceWorldReset forces Minecraft to reload the world rather than use the current one (if appropriate).
// Call this after the world generator has been set (eg after calling createDefaultTerrain() ).
func (o *MissionSpec) ForceWorldReset() {
	panic("TODO")
}

// SetTimeOfDay sets the time of day for the start of the mission.
// t -- The time of day, in Minecraft ticks (thousandths of an hour since dawn).
// eg. 0 = Dawn, 6000 = Noon, 12000 = Sunset, 18000 = Midnight.
// \param allowTimeToPass If false then the sun does not move.
func (o *MissionSpec) SetTimeOfDay(t int, allowTimeToPass bool) {
	panic("TODO")
}

// DrawBlock draws a Minecraft block in the world.
// x -- The east-west location.
// y -- The up-down location.
// z -- The north-south location.
// blockType -- A string corresponding to one of the Minecraft block types.
func (o *MissionSpec) DrawBlock(x, y, z int, blockType string) {
	panic("TODO")
}

// DrawCuboid draws a solid cuboid in the world.
// x1 -- The west-most location.
// y1 -- The down-most location.
// z1 -- The north-most location.
// x2 -- The east-most location.
// y2 -- The up-most location.
// z2 -- The south-most location.
// blockType -- A string corresponding to one of the Minecraft block types.
func (o *MissionSpec) DrawCuboid(x1, y1, z1, x2, y2, z2 int, blockType string) {
	panic("TODO")
}

// DrawItem draws a Minecraft item in the world.
// x -- The east-west location.
// y -- The up-down location.
// z -- The north-south location.
// itemType -- A string corresponding to one of the Minecraft item types.
func (o *MissionSpec) DrawItem(x, y, z int, itemType string) {
	panic("TODO")
}

// DrawSphere draws a solid sphere of blocks in the world.
// x -- The east-west location of the center.
// y -- The up-down location of the center.
// z -- The north-south location of the center.
// radius -- The radius of the sphere.
// blockType -- A string corresponding to one of the Minecraft block types.
func (o *MissionSpec) DrawSphere(x, y, z, radius int, blockType string) {
	panic("TODO")
}

// DrawLine draws a line of blocks in the world.
// x1 -- The east-west location of the first end.
// y1 -- The up-down location of the first end.
// z1 -- The north-south location of the first end.
// x2 -- The east-west location of the second end.
// y2 -- The up-down location of the second end.
// z2 -- The north-south location of the second end.
// blockType -- A string corresponding to one of the Minecraft block types.
func (o *MissionSpec) DrawLine(x1, y1, z1, x2, y2, z2, blockType string) {
	panic("TODO")
}

// -------------------- settings for the agents -------------------------

// Sets the start location for the agent. Only supports single agent missions.
// Integer coordinates are at the corners of blocks, so to start in the center of a block, use e.g. 4.5 instead of 4.0.
// x -- The east-west location.
// y -- The up-down location.
// z -- The north-south location.
func (o *MissionSpec) StartAt(x, y, z float32) {
	panic("TODO")
}

// Sets the start location and angles for the agent. Only supports single agent missions.
// Integer coordinates are at the corners of blocks, so to start in the center of a block, use e.g. 4.5 instead of 4.0.
// x -- The east-west location.
// y -- The up-down location.
// z -- The north-south location.
// yaw -- The yaw in degrees (180 = north, 270 = east, 0 = south, 90 = west)
// pitch -- The pitch in degrees (-90 = straight up, 90 = straight down, 0 = horizontal)
func (o *MissionSpec) StartAtWithPitchAndYaw(x, y, z, pitch, yaw float32) {
	panic("TODO")
}

// Sets the end location for the agent. Only supports single agent missions.
// Can be called more than once if there are multiple positions that end the mission for this agent.
// Integer coordinates are at the corners of blocks, so to end in the center of a block, use e.g. 4.5 instead of 4.0.
// x -- The east-west location.
// y -- The up-down location.
// z -- The north-south location.
// tolerance -- The radius that the agent must be within. Euclidean distance.
func (o *MissionSpec) EndAt(x, y, z, tolerance float32) {
	panic("TODO")
}

// Sets the player mode for the agent to creative, allowing them to fly and to not sustain damage. Only supports single agent missions.
func (o *MissionSpec) SetModeToCreative() {
	panic("TODO")
}

// Sets the player mode for the agent to spectator, allowing them to fly and pass through objects. Only supports single agent missions.
func (o *MissionSpec) SetModeToSpectator() {
	panic("TODO")
}

// Asks for image data to be sent from Minecraft for the agent. Only supports single agent missions.
// Data will be delivered in a TimestampedVideoFrame structure as RGBRGBRGB...
// The default camera viewpoint will be used (first-person view) - use setViewpoint to change this.
// width -- The width of the image in pixels. Ensure this is divisible by 4.
// height -- The height of the image in pixels. Ensure this is divisible by 2.
func (o *MissionSpec) RequestVideo(width, height int) {
	panic("TODO")
}

// Asks for image data and depth data to be sent from Minecraft for the agent. Only supports single agent missions.
// Data will be delivered in a TimestampedVideoFrame structure as RGBDRGBDRGBD...
// If saving the video to file only the depth will be recorded, as greyscale.
// width -- The width of the image in pixels. Ensure this is divisible by 4.
// height -- The height of the image in pixels. Ensure this is divisible by 2.
func (o *MissionSpec) RequestVideoWithDepth(width, height int) {
	panic("TODO")
}

// Sets the camera position. Modifies the existing video request, so call this after requestVideo or requestVideoWithDepth.
// \param viewpoint The camera position to use. 0 = first person, 1 = behind, 2 = facing.
func (o *MissionSpec) SetViewpoint(viewpoint int) {
	panic("TODO")
}

// Asks for a reward to be sent to the agent when it reaches a certain position. Only supports single agent missions.
// Integer coordinates are at the corners of blocks, so for rewards in the center of a block, use e.g. 4.5 instead of 4.0.
// x -- The east-west location.
// y -- The up-down location.
// z -- The north-south location.
// amount -- The reward value to send.
// tolerance -- The radius that the agent must be within to receive the reward. Euclidean distance.
func (o *MissionSpec) RewardForReachingPosition(x, y, z, amount, tolerance float32) {
	panic("TODO")
}

// Asks for the list of commands acted upon since the last timestep to be returned in the observations. Only supports single agent missions.
// The commands are returned in a JSON entry called 'CommandsSinceLastObservation'.
// Documentation link: <a href="../Schemas/MissionHandlers.html#element_ObservationFromRecentCommands">Schemas/MissionHandlers.html</a>
func (o *MissionSpec) ObserveRecentCommands() {
	panic("TODO")
}

// Asks for the contents of the player's hot-bar to be included in the observations. Only supports single agent missions.
// The commands are returned in JSON entries 'Hotbar_0_size', 'Hotbar_0_item', etc.
// Documentation link: <a href="../Schemas/MissionHandlers.html#element_ObservationFromHotBar">Schemas/MissionHandlers.html</a>
func (o *MissionSpec) ObserveHotBar() {
	panic("TODO")
}

// Asks for the full item inventory of the player to be included in the observations. Only supports single agent missions.
// The commands are returned in JSON entries 'Inventory_0_size', 'Inventory_0_item', etc.
// Documentation link: <a href="../Schemas/MissionHandlers.html#element_ObservationFromFullInventory">Schemas/MissionHandlers.html</a>
func (o *MissionSpec) ObserveFullInventory() {
	panic("TODO")
}

// Asks for observations of the block types within a cuboid relative to the agent's position. Only supports single agent missions.
// The commands are returned in a JSON entry 'Cells'.
// Documentation link: <a href="../Schemas/MissionHandlers.html#element_ObservationFromGrid">Schemas/MissionHandlers.html</a>
// x1 -- The west-most location.
// y1 -- The down-most location.
// z1 -- The north-most location.
// x2 -- The east-most location.
// y2 -- The up-most location.
// z2 -- The south-most location.
// name -- An name to identify the JSON array that will be returned.
func (o *MissionSpec) ObserveGrid(x1, y1, z1, x2, y2, z2 int, name string) {
	panic("TODO")
}

// Asks for the Euclidean distance to a location to be included in the observations. Only supports single agent missions.
// Integer coordinates are at the corners of blocks, so for distances from the center of a block, use e.g. 4.5 instead of 4.0.
// The commands are returned in a JSON element 'distanceFromNAME', where NAME is replaced with the name of the point.
// Documentation link: <a href="../Schemas/MissionHandlers.html#element_ObservationFromDistance">Schemas/MissionHandlers.html</a>
// x -- The east-west location.
// y -- The up-down location.
// z -- The north-south location.
// name -- A label for this observation. The observation will be called "distanceFrom<name>".
func (o *MissionSpec) ObserveDistance(x, y, z float32, name string) {
	panic("TODO")
}

// Asks for chat messages to be included in the observations.
func (o *MissionSpec) ObserveChat() {
	panic("TODO")
}

// -------------------- settings for the agents : command handlers -------------------------

// Remove any existing command handlers from the mission specification. Use with other functions to add exactly the command handlers you want.
// Only applies to the first agent in the mission. For multi-agent missions, specify the command handlers for each in the XML.
func (o *MissionSpec) RemoveAllCommandHandlers() {
	panic("TODO")
}

// Adds a continuous movement command handler if none present, with neither an allow-list or a deny-list, thus allowing any command to be sent.
// Only applies to the first agent in the mission. For multi-agent missions, specify the command handlers for each in the XML.
func (o *MissionSpec) AllowAllContinuousMovementCommands() {
	panic("TODO")
}

// Adds an allow-list to the continuous movement command handler if none present. Adds the specified verb to the allow-list.
// Note that when an allow-list is present, only the commands listed are allowed.
// Adds a continuous movement command handler if none present. Removes the deny-list from the continuous movement command handler if present.
// Only applies to the first agent in the mission. For multi-agent missions, specify the command handlers for each in the XML.
// verb -- The command verb, e.g. "move".
func (o *MissionSpec) AllowContinuousMovementCommand(verb string) {
	panic("TODO")
}

// Adds a discrete movement command handler if none present, with neither an allow-list or a deny-list, thus allowing any command to be sent.
// Only applies to the first agent in the mission. For multi-agent missions, specify the command handlers for each in the XML.
func (o *MissionSpec) AllowAllDiscreteMovementCommands() {
	panic("TODO")
}

// Adds an allow-list to the discrete movement command handler if none present. Adds the specified verb to the allow-list.
// Note that when an allow-list is present, only the commands listed are allowed.
// Adds a discrete movement command handler if none present. Removes the deny-list from the discrete movement command handler if present.
// Only applies to the first agent in the mission. For multi-agent missions, specify the command handlers for each in the XML.
// verb -- The command verb, e.g. "movenorth".
func (o *MissionSpec) AllowDiscreteMovementCommand(verb string) {
	panic("TODO")
}

// Adds an absolute movement command handler if none present, with neither an allow-list or a deny-list, thus allowing any command to be sent.
// Only applies to the first agent in the mission. For multi-agent missions, specify the command handlers for each in the XML.
func (o *MissionSpec) AllowAllAbsoluteMovementCommands() {
	panic("TODO")
}

// Adds an allow-list to the absolute movement command handler if none present. Adds the specified verb to the allow-list.
// Note that when an allow-list is present, only the commands listed are allowed.
// Adds an absolute movement command handler if none present. Removes the deny-list from the absolute movement command handler if present.
// Only applies to the first agent in the mission. For multi-agent missions, specify the command handlers for each in the XML.
// verb -- The command verb, e.g. "tpx".
func (o *MissionSpec) AllowAbsoluteMovementCommand(verb string) {
	panic("TODO")
}

// Adds an inventory command handler if none present, with neither an allow-list or a deny-list, thus allowing any command to be sent.
// Only applies to the first agent in the mission. For multi-agent missions, specify the command handlers for each in the XML.
func (o *MissionSpec) AllowAllInventoryCommands() {
	panic("TODO")
}

// Adds an allow-list to the inventory command handler if none present. Adds the specified verb to the allow-list.
// Note that when an allow-list is present, only the commands listed are allowed.
// Adds an inventory command handler if none present. Removes the deny-list from the inventory command handler if present.
// Only applies to the first agent in the mission. For multi-agent missions, specify the command handlers for each in the XML.
// verb -- The command verb, e.g. "selectInventoryItem".
func (o *MissionSpec) AllowInventoryCommand(verb string) {
	panic("TODO")
}

// Adds a chat command handler if none present, with neither an allow-list or a deny-list, thus allowing any command to be sent.
// Only applies to the first agent in the mission. For multi-agent missions, specify the command handlers for each in the XML.
func (o *MissionSpec) AllowAllChatCommands() {
	panic("TODO")
}

// ------------------------- information --------------------------------------

// Returns the short description of the mission.
// returns A string containing the summary.
func (o MissionSpec) GetSummary() string {
	panic("TODO")
	return ""
}

// Returns the number of agents involved in this mission.
// returns The number of agents.
func (o MissionSpec) GetNumberOfAgents() int {
	panic("TODO")
	return 0
}

// Gets whether video has been requested for one of the agents involved in this mission.
// role -- The agent index. Zero based.
// returns True if video was requested.
func (o MissionSpec) IsVideoRequested(role int) bool {
	panic("TODO")
	return false
}

// Returns the width of the requested video for one of the agents involved in this mission.
// role -- The agent index. Zero based.
// returns The width of the video in pixels.
func (o MissionSpec) GetVideoWidth(role int) int {
	panic("TODO")
	return 0
}

// Returns the height of the requested video for one of the agents involved in this mission.
// role -- The agent index. Zero based.
// returns The height of the video in pixels.
func (o MissionSpec) GetVideoHeight(role int) int {
	panic("TODO")
	return 0
}

// Returns the number of channels in the requested video for one of the agents involved in this mission.
// role -- The agent index. Zero based.
// \returns The number of channels in the requested video: 3 for RGB, 4 for RGBD.
func (o MissionSpec) GetVideoChannels(role int) int {
	panic("TODO")
	return 0
}

// Returns a list of the names of the active command handlers for one of the agents involved in this mission.
// role -- The agent index. Zero based.
// returns The list of command handler names: 'ContinuousMovement', 'DiscreteMovement', 'Chat', 'Inventory' etc.
func (o MissionSpec) GetListOfCommandHandlers(role int) []string {
	panic("TODO")
	return nil
}

// Returns a list of the names of the allowed commands for one of the agents involved in this mission.
// role -- The agent index. Zero based.
// command_handler -- The name of the command handler, as returned by getListOfCommandHandlers().
// returns The list of allowed commands: 'move', 'turn', 'attack' etc.
func (o MissionSpec) GetAllowedCommands(role int, command_handler string) []string {
	panic("TODO")
	return nil
}
