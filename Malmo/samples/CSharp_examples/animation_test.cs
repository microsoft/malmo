// Copyright (c) 2016 Microsoft Corporation
// 
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
// associated documentation files (the "Software"), to deal in the Software without restriction,
// including without limitation the rights to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
// 
// The above copyright notice and this permission notice shall be included in all copies or
// substantial portions of the Software.
// 
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
// NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// ------------------------------------------------------------------------------------------------

using System;
using System.IO;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Research.Malmo;

namespace animation_test
{
    class Program
    {
        static void Main()
        {
            string recordingsDirectory = "AnimationRecordings";
            try
            {
                Directory.CreateDirectory(recordingsDirectory);
            }
            catch(Exception exception)
            {
                Console.WriteLine("ERROR: {0}", exception.Message);
            }

            bool validate = true;
            MissionSpec my_mission = new MissionSpec(GetMissionXML(), validate);
            AgentHost agent_host = new AgentHost();
            try
            {
                agent_host.parse(new StringVector(Environment.GetCommandLineArgs()));
            }
            catch(Exception e)
            {
                Console.WriteLine("ERROR: {0}", e.Message);
                Console.WriteLine(agent_host.getUsage());
                Environment.Exit(1);
            }
            if (agent_host.receivedArgument("help"))
            {
                Console.WriteLine(agent_host.getUsage());
                Environment.Exit(0);
            }

            ClientPool my_client_pool = new ClientPool();
            ClientInfo my_client_info = new ClientInfo("127.0.0.1", 10000);
            my_client_pool.add(my_client_info);

            int num_reps;
            if (agent_host.receivedArgument("test"))
            {
                num_reps = 1;
            }
            else
            {
                num_reps = 30000;
            }

            for(int iRepeat = 0;iRepeat < num_reps; iRepeat++)
            {
                // Set up a recording
                MissionRecordSpec my_mission_record = new MissionRecordSpec(recordingsDirectory + "//" + "Mission_" + iRepeat + ".tgz");
                my_mission_record.recordRewards();
                my_mission_record.recordMP4(24, 400000);
                int max_retries = 3;
                for(int retry = 0; retry < max_retries; retry++)
                {
                    try
                    {
                        // Attempt to start the mission:
                        agent_host.startMission(my_mission, my_client_pool, my_mission_record, 0, "missionEndTestExperiment");
                        break;
                    }
                    catch(Exception e)
                    {
                        if(retry == max_retries - 1)
                        {
                            Console.WriteLine("Error starting mission {0}", e.Message);
                            Console.WriteLine("Is the game running?");
                            Environment.Exit(1);
                        }
                        else
                        {
                            Thread.Sleep(2000);
                        }
                    }
                }
                WorldState world_state = agent_host.getWorldState();
                while (!world_state.has_mission_begun)
                {
                    Thread.Sleep(100);
                    world_state = agent_host.getWorldState();
                }

                double reward = 0;  // keep track of reward for this mission.
                // start running:
                agent_host.sendCommand("move 1");
                agent_host.sendCommand("turn 0.1");
                // main loop:
                while (world_state.is_mission_running)
                {
                    world_state = agent_host.getWorldState();
                    if(world_state.number_of_rewards_since_last_state > 0)
                    {
                        // A reward signal has come in - see what it is:
                        double delta = world_state.rewards[0].getValue();
                        if(delta != 0)
                        {
                            Console.WriteLine("New reward: {0}", delta);
                            reward += delta;
                        }
                    }
                    Thread.Sleep(100);
                }
                // mission has ended
                Console.WriteLine("Mission {0}: Reward = {1}", iRepeat + 1, reward);
                Thread.Sleep(500); // Give the mod a little time to prepare for the next mission.
            }
        }
        static public string GetMissionXML()
        {
            return @"<?xml version=""1.0"" encoding=""UTF-8"" ?>
    <Mission xmlns=""http://ProjectMalmo.microsoft.com"" xmlns:xsi=""http://www.w3.org/2001/XMLSchema-instance"">
        <About>
            <Summary>Moving Times</Summary>
        </About>

        <ServerSection>
            <ServerHandlers>
                <FlatWorldGenerator generatorString=""3;7,220*1,5*3,2;3;,biome_1"" />
                <DrawingDecorator>
                    <DrawCuboid x1=""-21"" y1=""226"" z1=""-21"" x2=""21"" y2=""236"" z2=""21"" type=""air""/>
                    <DrawCuboid x1=""-21"" y1=""226"" z1=""-21"" x2=""21"" y2=""226"" z2=""21"" type=""lava""/>
                    <DrawCuboid x1=""-20"" y1=""226"" z1=""-20"" x2=""20"" y2=""16"" z2=""20"" type=""gold_block"" />
                </DrawingDecorator>" + GetAnimation() + @"
                <ServerQuitFromTimeUp timeLimitMs=""150000"" description=""out_of_time""/>
                <ServerQuitWhenAnyAgentFinishes />
            </ServerHandlers>
        </ServerSection>

        <AgentSection mode=""Survival"">
            <Name>Walt</Name>
            <AgentStart>
                <Placement x=""0.5"" y=""227.0"" z=""0.5""/>
                <Inventory>
                </Inventory>
            </AgentStart>
            <AgentHandlers>
                <RewardForMissionEnd rewardForDeath=""-1000.0"">
                    <Reward description=""out_of_time"" reward=""-900.0""/>
                    <Reward description=""found_goal"" reward=""100000.0""/>
                </RewardForMissionEnd>
                <RewardForTouchingBlockType>
                    <Block type=""slime"" reward=""-100.0""/>
                </RewardForTouchingBlockType>
                <AgentQuitFromTouchingBlockType>
                    <Block type=""skull"" description=""found_goal""/>
                </AgentQuitFromTouchingBlockType>
                <ContinuousMovementCommands turnSpeedDegs=""240""/>
            </AgentHandlers>
        </AgentSection>

    </Mission>";
        }
        static public string GetAnimation()
        {
            // Silly test of animations: add a few slime constructions and send them moving linearly around the "playing field"...
            // Create a slowly descending roof...
            // And an orbiting pumpkin with its own skull sattelite.
            string xml = "";
            Random random_generator = new Random();
            for (int x = 0; x < 4; x++)
            {
                xml += @"
            <AnimationDecorator ticksPerUpdate=""10"">
                <Linear>
                    <CanvasBounds>
                        <min x=""-20"" y=""226"" z=""-20""/>
                        <max x=""20"" y=""230"" z=""20""/>
                    </CanvasBounds>
                    <InitialPos x=""" + (random_generator.Next(-16,16)) + @""" y=""228"" z=""" + (random_generator.Next(-16,16)) + @"""/>
                    <InitialVelocity x=""" + (random_generator.NextDouble()-0.5)+@""" y=""0"" z="""+ (random_generator.NextDouble() - 0.5)+@"""/>
                </Linear>
                <DrawingDecorator>
                    <DrawBlock x=""0"" y=""1"" z=""0"" type=""slime""/>
                    <DrawBlock x=""0"" y=""-1"" z=""0"" type=""slime""/>
                    <DrawBlock x=""1"" y=""0"" z=""0"" type=""slime""/>
                    <DrawBlock x=""-1"" y=""0"" z=""0"" type=""slime""/>
                    <DrawBlock x=""0"" y=""0"" z=""1"" type=""slime""/>
                    <DrawBlock x=""0"" y=""0"" z=""-1"" type=""slime""/>
                </DrawingDecorator>
            </AnimationDecorator>";
            }
            return xml + @"
            <AnimationDecorator>
                <Linear>
                    <CanvasBounds>
                        <min x=""-21"" y=""225"" z=""-21""/>
                        <max x=""21"" y=""247"" z=""21""/>
                    </CanvasBounds>
                    <InitialPos x=""0"" y=""246"" z=""0""/>
                    <InitialVelocity x=""0"" y=""-0.025"" z=""0""/>
                </Linear>
                <DrawingDecorator>
                    <DrawCuboid x1=""-20"" y1=""0"" z1=""-20"" x2=""20"" y2=""1"" z2=""20"" type=""obsidian""/>
                </DrawingDecorator>
            </AnimationDecorator>
            <AnimationDecorator>
                <Parametric seed=""random"">
                  <x>15*sin(t/20.0)</x>
                  <y>227-(t/120.0)</y>
                  <z>15*cos(t/20.0)</z>
                </Parametric>
                <DrawingDecorator>
                    <DrawBlock x=""0"" y=""2"" z=""0"" type=""fence""/>
                    <DrawBlock x=""0"" y=""3"" z=""0"" type=""fence""/>
                    <DrawBlock x=""0"" y=""4"" z=""0"" type=""pumpkin""/>
                </DrawingDecorator>
            </AnimationDecorator>
            <AnimationDecorator>
                <Parametric seed=""random"">
                  <x>(15*sin(t/20.0))+(2*sin(t/2.0))</x>
                  <y>227-(t/120.0)+2*cos(t/1.5)</y>
                  <z>(15*cos(t/20.0))+(2*cos(t/2.0))</z>
                </Parametric>
                <DrawingDecorator>
                    <DrawBlock x=""0"" y=""2"" z=""0"" type=""skull""/>
                </DrawingDecorator>
            </AnimationDecorator>";
        }
    }
}
