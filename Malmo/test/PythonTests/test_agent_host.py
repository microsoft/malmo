from __future__ import print_function
# ------------------------------------------------------------------------------------------------
# Copyright (c) 2016 Microsoft Corporation
# 
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
# associated documentation files (the "Software"), to deal in the Software without restriction,
# including without limitation the rights to use, copy, modify, merge, publish, distribute,
# sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
# 
# The above copyright notice and this permission notice shall be included in all copies or
# substantial portions of the Software.
# 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
# NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
# DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
# ------------------------------------------------------------------------------------------------

import MalmoPython

agent_host = MalmoPython.AgentHost()
agent_host.setVideoPolicy( MalmoPython.VideoPolicy.LATEST_FRAME_ONLY )
agent_host.setRewardsPolicy( MalmoPython.RewardsPolicy.SUM_REWARDS )
agent_host.setObservationsPolicy( MalmoPython.ObservationsPolicy.LATEST_OBSERVATION_ONLY )

world_state = agent_host.getWorldState()

assert not world_state.has_mission_begun, 'World state says mission has already begun.'

assert not world_state.is_mission_running, 'World state says mission is already running.'

assert world_state.number_of_observations_since_last_state == 0, 'World state says observations already received.'

assert world_state.number_of_rewards_since_last_state == 0, 'World state says rewards already received.'

assert world_state.number_of_video_frames_since_last_state == 0, 'World state says video frames already received.'

assert len( world_state.observations ) == 0, 'World state has observations stored.'

assert len( world_state.rewards ) == 0, 'World state has rewards stored.'

assert len( world_state.video_frames ) == 0, 'World state has video frames stored.'

print(agent_host.getUsage())
