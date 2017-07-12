// ------------------------------------------------------------------------------------------------
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

// Tutorial sample #8: The Classroom Decorator

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Threading;
using Microsoft.Research.Malmo;

namespace tutorial_8
{
    class Program
    {
        static void Main()
        {
            string missionXML = @"<?xml version=""1.0"" encoding=""UTF-8"" ?>
<Mission xmlns=""http://ProjectMalmo.microsoft.com"" xmlns:xsi=""http://www.w3.org/2001/XMLSchema-instance"">
  <About>
    <Summary>Find the goal!</Summary>
  </About>

  <ServerSection>
    <ServerInitialConditions>
      <Time>
        <StartTime>14000</StartTime>
        <AllowPassageOfTime>false</AllowPassageOfTime>
      </Time>
    </ServerInitialConditions>
    <ServerHandlers>
      <FlatWorldGenerator generatorString=""3;7,220*1,5*3,2;3;,biome_1"" />
      <ClassroomDecorator>
        <complexity>
          <building>0.5</building>
          <path>0.5</path>
          <division>1</division>
          <obstacle>1</obstacle>
          <hint>0</hint>
        </complexity>
      </ClassroomDecorator>
      <ServerQuitFromTimeUp timeLimitMs=""30000"" description=""out_of_time""/>
      <ServerQuitWhenAnyAgentFinishes />
    </ServerHandlers>
  </ServerSection>

  <AgentSection mode=""Survival"">
    <Name>James Bond</Name>
    <AgentStart>
      <Placement x=""-203.5"" y=""81.0"" z=""217.5""/>
    </AgentStart>
    <AgentHandlers>
      <ObservationFromFullStats />
      <ContinuousMovementCommands turnSpeedDegs=""180"">
        <ModifierList type=""deny-list"">
          <command>attack</command>
        </ModifierList>
      </ContinuousMovementCommands>
      <RewardForMissionEnd rewardForDeath=""-10000"">
        <Reward description=""found_goal"" reward=""1000"" />
        <Reward description=""out_of_time"" reward=""-1000"" />
      </RewardForMissionEnd>
      <RewardForTouchingBlockType>
        <Block type=""gold_ore diamond_ore redstone_ore"" reward=""20"" />
      </RewardForTouchingBlockType>
      <AgentQuitFromTouchingBlockType>
        <Block type=""gold_block diamond_block redstone_block"" description=""found_goal"" />
      </AgentQuitFromTouchingBlockType>
    </AgentHandlers>
  </AgentSection>
</Mission>";

            // Create default Malmo objects:
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

            int num_repeats;
            if (agent_host.receivedArgument("test"))
            {
                num_repeats = 1;
            }
            else
            {
                num_repeats = 10;
            }

            for(int i = 0; i < num_repeats; i++)
            {
                MissionSpec my_mission = new MissionSpec(missionXML, true);
                MissionRecordSpec my_mission_record = new MissionRecordSpec();

                // Attempt to start a mission:
                int max_retries = 10;
                for(int retry = 0; retry < max_retries; retry++)
                {
                    try
                    {
                        agent_host.startMission(my_mission, my_mission_record);
                        retry = max_retries;
                    }
                    catch(Exception e)
                    {
                        if (retry == max_retries - 1)
                        {
                            Console.WriteLine("Error starting mission: {0}", e);
                            Environment.Exit(1);
                        }
                        else
                        {
                            Thread.Sleep(2000);
                        }
                    }
                }

                // Loop until mission starts:
                Console.WriteLine("Waiting for the mission to start ");
                WorldState world_state = agent_host.getWorldState();
                while (!world_state.has_mission_begun)
                {
                    Console.Write(".");
                    Thread.Sleep(100);
                    world_state = agent_host.getWorldState();
                    foreach(TimestampedString error in world_state.errors)
                    {
                        Console.WriteLine("Error: {0}", error.text);
                    }
                }

                Console.WriteLine();
                Console.WriteLine("Mission running ");

                // Loop until mission ends:
                while (world_state.is_mission_running)
                {
                    Console.Write(".");
                    Thread.Sleep(100);
                    world_state = agent_host.getWorldState();
                    foreach(TimestampedString error in world_state.errors)
                    {
                        Console.WriteLine("Error: {0}", error.text);
                    }
                }

                Console.WriteLine();
                Console.WriteLine("Mission ended");
                // Mission has ended.
            }
        }
    }
}
