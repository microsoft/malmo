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

// Tutorial sample #2: Run simple mission using raw XML
// More interesting generator string: "3;7,44*49,73,35:1,159:4,95:13,35:13,159:11,95:10,159:14,159:6,35:6,95:6;12;"

using System;
using System.Threading;
using Microsoft.Research.Malmo;

namespace tutorial_2
{
    class Program
    {
        static void Main()
        {
            String missionXML=@"<?xml version=""1.0"" encoding=""UTF-8"" standalone=""no"" ?>
            <Mission xmlns=""http://ProjectMalmo.microsoft.com"" xmlns:xsi=""http://www.w3.org/2001/XMLSchema-instance"">
            
              <About>
                <Summary>Hello world!</Summary>
              </About>
              
              <ServerSection>
                <ServerHandlers>
                  <FlatWorldGenerator generatorString=""3;7,220*1,5*3,2;3;,biome_1""/>
                  <ServerQuitFromTimeUp timeLimitMs=""30000""/>
                  <ServerQuitWhenAnyAgentFinishes/>
                </ServerHandlers>
              </ServerSection>
              
              <AgentSection mode=""Survival"">
                <Name>MalmoTutorialBot</Name>
                <AgentStart/>
                <AgentHandlers>
                  <ObservationFromFullStats/>
                  <ContinuousMovementCommands turnSpeedDegs=""180""/>
                </AgentHandlers>
              </AgentSection>
            </Mission>";

            // Create default Malmo objects:

            AgentHost agent_host = new AgentHost();
            try
            {
                agent_host.parse(new StringVector(Environment.GetCommandLineArgs()));
            }
            catch(Exception ex)
            {
                Console.Error.WriteLine("Error:{0}", ex.Message);
                Console.Error.WriteLine(agent_host.getUsage());
                Environment.Exit(1);
            }
            if (agent_host.receivedArgument("help"))
            {
                Console.Error.WriteLine(agent_host.getUsage());
                Environment.Exit(0);
            }

            MissionSpec my_mission = new MissionSpec(missionXML, true);
            MissionRecordSpec my_mission_record = new MissionRecordSpec();

            // Attempt to start a mission:
            int max_retries = 3;
            for(int retry = 0;retry < max_retries; retry++)
            {
                try
                {
                    agent_host.startMission(my_mission, my_mission_record);
                    break;
                }
                catch(Exception ex)
                {
                    if(retry == max_retries - 1)
                    {
                        Console.WriteLine("Error starting mission:{0}", ex.Message);
                        Environment.Exit(1);
                    }
                    else
                    {
                        Thread.Sleep(1000);
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
                    Console.WriteLine("Error:{0}", error.text);
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
                    Console.WriteLine("Error:{0}", error.text);
                }
            }
            Console.WriteLine();
            Console.WriteLine("Mission ended");
            // Mission has ended.

        }
    }
}
