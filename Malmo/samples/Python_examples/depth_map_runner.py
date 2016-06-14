import MalmoPython
import random
import time
import logging
import struct
import socket
import os
import sys
from PIL import Image

logging.basicConfig(level=logging.INFO)

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG) # set to INFO if you want fewer messages

# create a file handler
#handler = logging.FileHandler('depthmaprunner.log')
#handler.setLevel(logging.DEBUG)

# create a logging format
#formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
#handler.setFormatter(formatter)

# add the handlers to the logger
#logger.addHandler(handler)

#-------------------------------------------------------------------------------------------------------------------------------------

def processFrame( frame ):
    '''Track through the middle line of the depth data and find the max discontinuities'''
    global current_yaw_delta_from_depth

    y = int(video_height / 2)
    rowstart = y * video_width
    
    v = 0
    v_max = 0
    v_max_pos = 0
    v_min = 0
    v_min_pos = 0
    
    dv = 0
    dv_max = 0
    dv_max_pos = 0
    dv_max_sign = 0
    
    d2v = 0
    d2v_max = 0
    d2v_max_pos = 0
    d2v_max_sign = 0
    
    for x in range(0, video_width):
        nv = frame[(rowstart + x) * 4 + 3]
        ndv = nv - v
        nd2v = ndv - dv

        if nv > v_max or x == 0:
            v_max = nv
            v_max_pos = x
            
        if nv < v_min or x == 0:
            v_min = nv
            v_min_pos = x

        if abs(ndv) > dv_max or x == 1:
            dv_max = abs(ndv)
            dv_max_pos = x
            dv_max_sign = ndv > 0
            
        if abs(nd2v) > d2v_max or x == 2:
            d2v_max = abs(nd2v)
            d2v_max_pos = x
            d2v_max_sign = nd2v > 0
            
        d2v = nd2v
        dv = ndv
        v = nv
    
    logger.info("d2v, dv, v: " + str(d2v) + ", " + str(dv) + ", " + str(v))

    # To visualise what is going on, do the following:
    if 0:
        imageframe = Image.frombytes( 'RGBA', ( video_width, video_height ), frame )
        imageframe = imageframe.transpose( Image.FLIP_TOP_BOTTOM )
        draw = ImageDraw.Draw(imageframe)
        draw.line((0, y, video_width, y), fill=128)
        draw.line((d2v_max_pos, 0, d2v_max_pos, video_height), fill=128)
        del draw
        # Malmo.logVideo( imageframe )

    # We want to steer towards the greatest d2v (ie the biggest discontinuity in the gradient of the depth map).
    # If it's a possitive value, then it represents a rapid change from close to far - eg the left-hand edge of a gap.
    # Aiming to put this point in the leftmost quarter of the screen will cause us to aim for the gap.
    # If it's a negative value, it represents a rapid change from far to close - eg the right-hand edge of a gap.
    # Aiming to put this point in the rightmost quarter of the screen will cause us to aim for the gap.
    if dv_max_sign:
        edge = video_width / 4
    else:
        edge = 3 * video_width / 4

    # Now, if there is something noteworthy in d2v, steer according to the above comment:
    if d2v_max > 8:
        current_yaw_delta_from_depth = (float(d2v_max_pos - edge) / video_width)
    else:
        # Nothing obvious to aim for, so aim for the farthest point:
        if v_max < 255:
            current_yaw_delta_from_depth = (float(v_max_pos) / video_width) - 0.5
        else:
            # No real data to be had in d2v or v, so just go by the direction we were already travelling in:
            if current_yaw_delta_from_depth < 0:
                current_yaw_delta_from_depth = -1
            else:
                current_yaw_delta_from_depth = 1
    
#----------------------------------------------------------------------------------------------------------------------------------

current_yaw_delta_from_depth = 0
video_width = 432
video_height = 240
   
missionXML = '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://ProjectMalmo.microsoft.com Mission.xsd">
    
      <About>
        <Summary>Run the maze!</Summary>
      </About>
      
     <ServerSection>
        <ServerHandlers>
            <FlatWorldGenerator generatorString="3;7,220*1,5*3,2;3;,biome_1" />
            <MazeDecorator>
                <SizeAndPosition length="20" width="20" xOrigin="0" yOrigin="215" zOrigin="410" height="180"/>
                <GapProbability variance="0.1">0.9</GapProbability>
                <Seed>random</Seed>
                <MaterialSeed>random</MaterialSeed>
                <AllowDiagonalMovement>false</AllowDiagonalMovement>
                <StartBlock fixedToEdge="true" type="emerald_block" height="1"/>
                <EndBlock fixedToEdge="true" type="redstone_block" height="12"/>
                <PathBlock type="glowstone" colour="WHITE ORANGE MAGENTA LIGHT_BLUE YELLOW LIME PINK GRAY SILVER CYAN PURPLE BLUE BROWN GREEN RED BLACK" height="1"/>
                <FloorBlock type="air"/>
                <SubgoalBlock type="beacon"/>
                <GapBlock type="stained_hardened_clay" colour="WHITE ORANGE MAGENTA LIGHT_BLUE YELLOW LIME PINK GRAY SILVER CYAN PURPLE BLUE BROWN GREEN RED BLACK" height="3"/>
            </MazeDecorator>
            <ServerQuitFromTimeUp timeLimitMs="30000"/>
            <ServerQuitWhenAnyAgentFinishes />
        </ServerHandlers>
    </ServerSection>

    <AgentSection>
        <Name>Jason Bourne</Name>
        <AgentStart>
            <Placement x="-204" y="81" z="217"/> <!-- will be overwritten by MazeDecorator -->
        </AgentStart>
        <AgentHandlers>
            <VideoProducer want_depth="true">
                <Width>''' + str(video_width) + '''</Width>
                <Height>''' + str(video_height) + '''</Height>
            </VideoProducer>
            <ContinuousMovementCommands turnSpeedDegs="720" />
            <AgentQuitFromTouchingBlockType>
                <Block type="redstone_block"/>
            </AgentQuitFromTouchingBlockType>
        </AgentHandlers>
    </AgentSection>
  </Mission>'''

sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)  # flush print output immediately

validate = True
my_mission = MalmoPython.MissionSpec( missionXML, validate )

agent_host = MalmoPython.AgentHost()
try:
    agent_host.parse( sys.argv )
except RuntimeError as e:
    print 'ERROR:',e
    print agent_host.getUsage()
    exit(1)
if agent_host.receivedArgument("help"):
    print agent_host.getUsage()
    exit(0)

agent_host.setObservationsPolicy(MalmoPython.ObservationsPolicy.LATEST_OBSERVATION_ONLY)
agent_host.setVideoPolicy(MalmoPython.VideoPolicy.LATEST_FRAME_ONLY)

for iRepeat in range(30000):

    my_mission_record = MalmoPython.MissionRecordSpec()

    try:
        agent_host.startMission( my_mission, my_mission_record )
    except RuntimeError as e:
        logger.error("Error starting mission: %s" % e)
        exit(1)

    logger.info('Mission %s', iRepeat)
    logger.info("Waiting for the mission to start")
    world_state = agent_host.getWorldState()
    while not world_state.is_mission_running:
        sys.stdout.write(".")
        time.sleep(0.1)
        world_state = agent_host.getWorldState()
    print

    agent_host.sendCommand( "move 1" )

    # main loop:
    while world_state.is_mission_running:
        world_state = agent_host.getWorldState()
        while world_state.number_of_video_frames_since_last_state < 1 and world_state.is_mission_running:
            logger.info("Waiting for frames...")
            time.sleep(0.05)
            world_state = agent_host.getWorldState()

        logger.info("Got frame!")
        
        if world_state.is_mission_running:
            processFrame(world_state.video_frames[0].pixels)
            
            agent_host.sendCommand( "turn " + str(current_yaw_delta_from_depth) )

    logger.info("Mission has stopped.")
    time.sleep(1) # let the Mod recover
