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
