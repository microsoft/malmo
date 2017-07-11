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

// Tutorial sample #3: Drawing

using System;
using System.Threading;
using Microsoft.Research.Malmo;

namespace tutorial_3
{
    class Program
    {
        static void Main()
        {
            string missionXML = @"<?xml version=""1.0"" encoding=""UTF-8"" standalone=""no"" ?>
            <Mission xmlns=""http://ProjectMalmo.microsoft.com"" xmlns:xsi=""http://www.w3.org/2001/XMLSchema-instance"">
            
              <About>
                <Summary>Hello world!</Summary>
              </About>
              
            <ServerSection>
              <ServerInitialConditions>
                <Time>
                    <StartTime>12000</StartTime>
                    <AllowPassageOfTime>false</AllowPassageOfTime>
                </Time>
                <Weather>clear</Weather>
              </ServerInitialConditions>
              <ServerHandlers>
                  <FlatWorldGenerator generatorString=""3;7,44*49,73,35:1,159:4,95:13,35:13,159:11,95:10,159:14,159:6,35:6,95:6;12;""/>
                  <DrawingDecorator>
                    <DrawSphere x=""-27"" y=""70"" z=""0"" radius=""30"" type=""air""/>" + Menger(-40, 40, -13, 27, "wool", "air") + @"
                  </DrawingDecorator>
                  <ServerQuitFromTimeUp timeLimitMs=""30000""/>
                  <ServerQuitWhenAnyAgentFinishes/>
                </ServerHandlers>
              </ServerSection>
              
              <AgentSection mode=""Survival"">
                <Name>MalmoTutorialBot</Name>
                <AgentStart>
                    <Placement x=""0.5"" y=""56.0"" z=""0.5"" yaw=""90""/>
                </AgentStart>
                <AgentHandlers>
                  <ObservationFromFullStats/>
                  <ContinuousMovementCommands turnSpeedDegs=""180""/>
                </AgentHandlers>
              </AgentSection>
            </Mission>";

            // Create default Malmo objects

            AgentHost agent_host = new AgentHost();
            try
            {
                agent_host.parse(new StringVector(Environment.GetCommandLineArgs()));
            }
            catch (Exception e)
            {
                Console.Error.WriteLine("ERROR:{0}", e.Message);
                Console.Error.WriteLine(agent_host.getUsage());
                Environment.Exit(1);
            }
            if (agent_host.receivedArgument("help"))
            {
                Console.Error.WriteLine(agent_host.getUsage());
                Environment.Exit(0);
            }
            MissionSpec my_mission = new MissionSpec(missionXML,true);
            MissionRecordSpec my_mission_record = new MissionRecordSpec();

            // Attempt to start a mission:
            int max_retries = 3;
            for(int retry = 0; retry < max_retries; retry++)
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
                        Console.WriteLine("Error starting mission:{0}",e.Message);
                        Environment.Exit(1);
                    }
                    else
                    {
                        Thread.Sleep(2000);
                    }
                }
            }
            //Loop until mission starts
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
                    Console.WriteLine("Error:{}", error.text);
                }
            }
            Console.WriteLine();
            Console.WriteLine("Mission ended");
            // Mission has ended.
        }
        static public string Menger(int xorg, int yorg, int zorg, int size, string blocktype, string holetype)
        {
            //draw solid chunk
            string genstring = GenCuboid(xorg, yorg, zorg, xorg + size - 1, yorg + size - 1, zorg + size - 1, blocktype) + "\n";
            //now remove holes
            int unit = size;
            while (unit >= 3)
            {
                int w = unit / 3;
                for (int i = 0; i < size; i += unit)
                {
                    for (int j = 0; j < size; j += unit)
                    {
                        int x = xorg + i;
                        int y = yorg + j;
                        genstring += GenCuboid(x + w, y + w, zorg, (x + 2 * w) - 1, (y + 2 * w) - 1, zorg + size - 1, holetype) + "\n";
                        y = yorg + i;
                        int z = zorg + j;
                        genstring += GenCuboid(xorg, y + w, z + w, xorg + size - 1, (y + 2 * w) - 1, (z + 2 * w) - 1, holetype) + "\n";
                        genstring += GenCuboid(x + w, yorg, z + w, (x + 2 * w) - 1, yorg + size - 1, (z + 2 * w) - 1, holetype) + "\n";
                    }
                }
                unit /= 3;
            }
            return genstring;
        }
        static public string GenCuboid(int x1, int y1, int z1, int x2, int y2, int z2, string blocktype)
        {
            string cuboid = String.Format("<DrawCuboid x1='{0}' y1='{1}' z1='{2}' x2='{3}' y2='{4}' z2='{5}' type='{6}'/>", x1.ToString(), y1.ToString(), z1.ToString(), x2.ToString(), y2.ToString(), z2.ToString(), blocktype);
            return (cuboid);
        }
    }
}
