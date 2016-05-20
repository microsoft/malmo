package com.microsoft.Malmo.Client;

import com.microsoft.Malmo.IState;

/** Set of states used by MissionStateTracker to ensure the Mod remains in a valid state.<br>
 * Note: these do not necessarily occur in the order presented here.
 * If adding states here, please also add a MissionStateEpisode class to MissionStateTracker,
 * and a line to the switch statement in MissionStateTracker.getStateEpisodeForState().
 */
public enum ClientState implements IState
{
    WAITING_FOR_MOD_READY,
	DORMANT,
	CREATING_HANDLERS,
	EVALUATING_WORLD_REQUIREMENTS,
	PAUSING_OLD_SERVER,
	CLOSING_OLD_SERVER,
	CREATING_NEW_WORLD,
	WAITING_FOR_SERVER_READY,
	WAITING_FOR_MINECRAFT_READY,
	RUNNING,
	IDLING,
	MISSION_ENDED,
	MISSION_ABORTED,
	WAITING_FOR_SERVER_MISSION_END,
	// Error conditions:
	ERROR_DUFF_HANDLERS,
	ERROR_INTEGRATED_SERVER_UNREACHABLE,
	ERROR_NO_WORLD,
	ERROR_CANNOT_CREATE_WORLD,
	ERROR_CANNOT_START_AGENT,
	ERROR_LOST_AGENT
}
