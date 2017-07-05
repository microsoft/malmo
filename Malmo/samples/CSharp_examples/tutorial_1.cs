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

// Tutorial sample #1: Run simple mission

using System;
using System.Threading;
using Microsoft.Research.Malmo;

namespace tutorial_1
{
    class Program
    {
        public static void Main()
        {
            AgentHost agentHost = new AgentHost();
            try
            {
                agentHost.parse(new StringVector(Environment.GetCommandLineArgs()));
            }
            catch(Exception ex)
            {
                Console.Error.WriteLine("ERROR: {0}", ex.Message);
                Console.Error.WriteLine(agentHost.getUsage());
                Environment.Exit(1);
            }
            if(agentHost.receivedArgument("help"))
            {
                Console.Error.WriteLine(agentHost.getUsage());
                Environment.Exit(0);
            }
            MissionSpec my_mission = new MissionSpec();
            MissionRecordSpec my_mission_record = new MissionRecordSpec("./saved_data.tgz");

            // Attempt to start a mission:
            int max_retries = 3;
            for(int retry = 0;retry < max_retries;retry++)
                try
                {
                    agentHost.startMission(my_mission, my_mission_record);
                    break;
                }
                catch(MissionException ex)
                {
                    if(retry == max_retries - 1)
                    {
                        Console.WriteLine("Error starting mission:{0}", ex.Message);
                        Environment.Exit(1);
                    }
                    else
                    {
                        Thread.Sleep(2000);
                    }
                }
            // Loop until mission starts:
            Console.WriteLine("Waiting for the mission to start ");
            WorldState world_state;
            do
            {
                Console.Write(".");
                Thread.Sleep(100);
                world_state = agentHost.getWorldState();
                foreach (TimestampedString error in world_state.errors)
                {
                    Console.Error.WriteLine("Error:{0}", error.text);
                }
            }
            while (!world_state.has_mission_begun);

            Console.WriteLine();
            Console.WriteLine("Mission running ");
            Console.WriteLine(my_mission.getAsXML(true));

            // Enter your commands here
            // agentHost.sendCommand("turn -0.5");
            // agentHost.sendCommand("move 1");
            // agentHost.sendCommand("jump 1");

            // Loop until mission ends:
            while (world_state.is_mission_running)
            {
                Console.Write(".");
                Thread.Sleep(100);
                world_state = agentHost.getWorldState();
                foreach(TimestampedString error in world_state.errors)
                {
                    Console.WriteLine("Error:{0}", error.text);
                }
            }
            Console.WriteLine();
            Console.WriteLine("Mission ended");
            // Mission has ended
        }
    }
}
