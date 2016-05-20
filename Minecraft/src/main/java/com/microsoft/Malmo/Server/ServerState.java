package com.microsoft.Malmo.Server;

import com.microsoft.Malmo.IState;

/** Set of states used by MissionStateTracker to ensure the Mod remains in a valid state.<br>
 * Note: these do not necessarily occur in the order presented here.
 * If adding states here, please also add a MissionStateEpisode class to MissionStateTracker,
 * and a line to the switch statement in MissionStateTracker.getStateEpisodeForState().
 */
public enum ServerState implements IState
{
    WAITING_FOR_MOD_READY,
	DORMANT,
	BUILDING_WORLD,
	WAITING_FOR_AGENTS_TO_ASSEMBLE,
	RUNNING,
	MISSION_ENDED,
	MISSION_ABORTED,
	WAITING_FOR_AGENTS_TO_QUIT,
	ERROR,
	CLEAN_UP
}
