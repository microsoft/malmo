# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

import MalmoPython

agent_host = MalmoPython.ALEAgentHost()
agent_host.setVideoPolicy( MalmoPython.VideoPolicy.LATEST_FRAME_ONLY )
agent_host.setRewardsPolicy( MalmoPython.RewardsPolicy.SUM_REWARDS )
agent_host.setObservationsPolicy( MalmoPython.ObservationsPolicy.LATEST_OBSERVATION_ONLY )

world_state = agent_host.getWorldState()

assert not world_state.is_mission_running, 'World state says mission is already running.'

assert world_state.number_of_observations_since_last_state == 0, 'World state says observations already received.'

assert world_state.number_of_rewards_since_last_state == 0, 'World state says rewards already received.'

assert world_state.number_of_video_frames_since_last_state == 0, 'World state says video frames already received.'

assert len( world_state.observations ) == 0, 'World state has observations stored.'

assert len( world_state.rewards ) == 0, 'World state has rewards stored.'

assert len( world_state.video_frames ) == 0, 'World state has video frames stored.'

print agent_host.getUsage()
