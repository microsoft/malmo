using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Threading;
using Microsoft.Research.Malmo;
using Newtonsoft.Json.Linq;

namespace tutorial_7
{
    class Program
    {
        static void Main()
        {
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
                float gp = i / 10;
                MissionSpec my_mission = new MissionSpec(GetMissionXML("random",gp),true);
                MissionRecordSpec my_mission_record = new MissionRecordSpec();

                // Attempt to start a mission:

                int max_retries = 3;
                for(int retry = 0; retry< max_retries; retry++)
                {
                    try
                    {
                        agent_host.startMission(my_mission, my_mission_record);
                        break;
                    }
                    catch(Exception e)
                    {
                        if(retry == max_retries - 1)
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
                    foreach (TimestampedString error in world_state.errors)
                    {
                        Console.WriteLine("Error: {0}", error.text);
                    }
                }

                Console.WriteLine();
                Console.WriteLine("Mission running");

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
        static public string GetMissionXML(string seed,float gp)
        {
            return @"<?xml version=""1.0"" encoding=""UTF-8"" standalone=""no"" ?>
            <Mission xmlns=""http://ProjectMalmo.microsoft.com"" xmlns:xsi=""http://www.w3.org/2001/XMLSchema-instance"">
            
              <About>
                <Summary>Hello world!</Summary>
              </About>
              
            <ServerSection>
              <ServerInitialConditions>
                <Time>
                    <StartTime>1000</StartTime>
                    <AllowPassageOfTime>false</AllowPassageOfTime>
                </Time>
                <Weather>clear</Weather>
              </ServerInitialConditions>
              <ServerHandlers>
                  <FlatWorldGenerator generatorString=""3;7,44*49,73,35:1,159:4,95:13,35:13,159:11,95:10,159:14,159:6,35:6,95:6;12;""/>
                  <DrawingDecorator>
                    <DrawSphere x=""-27"" y=""70"" z=""0"" radius=""30"" type=""air""/>
                  </DrawingDecorator>
                  <MazeDecorator>
                    <Seed>" + seed + @"</Seed>
                    <SizeAndPosition width=""10"" length=""10"" height=""10"" xOrigin=""-32"" yOrigin=""69"" zOrigin=""-5""/>
                    <StartBlock type=""emerald_block"" fixedToEdge=""true""/>
                    <EndBlock type=""redstone_block"" fixedToEdge=""true""/>
                    <PathBlock type=""diamond_block""/>
                    <FloorBlock type=""air""/>
                    <GapBlock type=""air""/>
                    <GapProbability>" + gp.ToString() + @"</GapProbability>
                    <AllowDiagonalMovement>false</AllowDiagonalMovement>
                  </MazeDecorator>
                  <ServerQuitFromTimeUp timeLimitMs=""30000""/>
                  <ServerQuitWhenAnyAgentFinishes/>
                </ServerHandlers>
              </ServerSection>
              
              <AgentSection mode=""Survival"">
                <Name>MalmoTutorialBot</Name>
                <AgentStart>
                    <Placement x=""0.5"" y=""56.0"" z=""0.5""/>
                </AgentStart>
                <AgentHandlers>
                    <AgentQuitFromTouchingBlockType>
                        <Block type=""redstone_block""/>
                    </AgentQuitFromTouchingBlockType>
                </AgentHandlers>
              </AgentSection>
            </Mission>";
        }
    }
}
