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

// Sample to demonstrate use of the DefaultWorldGenerator, ContinuousMovementCommands, timestamps and ObservationFromFullStats.
// Runs an agent in a standard Minecraft world, randomly seeded, uses timestamps and observations
// to calculate speed of movement, and chooses tiny "programmes" to execute if the speed drops to below a certain threshold.
// Mission continues until the agent dies.

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Research.Malmo;
using Newtonsoft.Json.Linq;

namespace default_world_test
{
    class Program
    {
        static void Main()
        {
            // Variety of strategies for dealing with loss of motion:
            List<string> commandSequences = new List<string>();
            commandSequences.Add("jump 1;move 1;wait 1;jump 0;move 1;wait 2"); commandSequences.Add("turn 0.5;wait 1;turn 0;move 1;wait 2"); commandSequences.Add("turn -0.5;wait 1;turn 0;move 1;wait 2"); commandSequences.Add("move 0;attack 1;wait 5;pitch 0.5;wait 1;pitch 0;attack 1;wait 5;pitch -0.5;wait 1;pitch 0;attack 0;move 1;wait 2"); commandSequences.Add("move 0;pitch 1;wait 2;pitch 0;use 1;jump 1;wait 6;use 0;jump 0;pitch -1;wait 1;pitch 0;wait 2;move 1;wait 2");

            MissionSpec my_mission = new MissionSpec(GetMissionXML(), true);

            AgentHost agent_host = new AgentHost();
            try
            {
                agent_host.parse(new StringVector(Environment.GetCommandLineArgs()));
            }

            catch(Exception e)
            {
                Console.WriteLine("ERROR: {0}", e);
                Console.WriteLine(agent_host.getUsage());
                Environment.Exit(1);
            }
            if (agent_host.receivedArgument("help"))
            {
                Console.WriteLine(agent_host.getUsage());
                Environment.Exit(0);
            }

            if (agent_host.receivedArgument("test"))
            {
                my_mission.timeLimitInSeconds(20); // else mission runs forever
            }

            // Attempt to start the mission:
            int max_retries = 3;
            for(int retry = 0; retry < max_retries; retry++)
            {
                MissionRecordSpec my_mission_record = new MissionRecordSpec();
                try
                {
                    agent_host.startMission(my_mission, my_mission_record);
                    break;
                }
                catch(Exception e)
                {
                    if(retry == max_retries - 1)
                    {
                        Console.WriteLine("Error starting mission {0}", e.Message);
                        Console.WriteLine("is the game running?");
                        Environment.Exit(1);
                    }
                    else
                    {
                        Thread.Sleep(2000);
                    }
                }
            }

            // Wait for the mission to start:
            WorldState world_state = agent_host.getWorldState();
            while (!world_state.has_mission_begun)
            {
                Thread.Sleep(100);
                world_state = agent_host.getWorldState();
            }

            string currentSequence = "move 1; wait 4";  // start off by moving
            double currentSpeed = 0;
            double distTravelledAtLastCheck = 0;
            DateTime timeStampAtLastCheck = DateTime.Now;
            int cyclesPerCheck = 10;    // controls how quickly the agent responds to getting stuck, and the amount of time it waits for on a "wait" command.
            int currentCycle = 0;
            int waitCycles = 0;

            // Main loop
            while (world_state.is_mission_running)
            {
                world_state = agent_host.getWorldState();
                if(world_state.number_of_observations_since_last_state > 0)
                {
                    string obvsText = world_state.observations[world_state.observations.Count - 1].text;
                    currentCycle += 1;
                    if(currentCycle == cyclesPerCheck)  // Time to check our speed and decrement our wait counter (if set):
                    {
                        currentCycle = 0;
                        if(waitCycles > 0)
                        {
                            waitCycles -= 1;
                        }
                        // Now use the latest observation to calculate our approximate speed:
                        JObject data = JObject.Parse(obvsText); // observation comes in as a JSON string...
                        double dist = (double)data.GetValue("DistanceTravelled",0); //... containing a "DistanceTravelled" field (amongst other things).
                        DateTime timestamp = world_state.observations[world_state.observations.Count - 1].timestamp;

                        double delta_dist = dist - distTravelledAtLastCheck;
                        TimeSpan delta_time = timestamp - timeStampAtLastCheck;
                        currentSpeed = 1000000 * delta_dist / (delta_time.TotalMilliseconds * 1000);

                        distTravelledAtLastCheck = dist;
                        timeStampAtLastCheck = timestamp;
                    }
                }

                if(waitCycles == 0)
                {
                    // Time to execute the next command, if we have one:
                    if(currentSequence != "")
                    {
                        List<string> commands = new List<string>();
                        foreach (string i in currentSequence.Split(';'))
                        {
                            commands.Add(i);
                        }
                        string command = commands[0];
                        commands.RemoveAt(0);
                        if(commands.Count > 1)
                        {
                            currentSequence = string.Join(";",commands);
                        }
                        else
                        {
                            currentSequence = "";
                        }
                        Console.WriteLine(command);
                        if (command.Contains("wait"))
                        {
                            waitCycles = int.Parse(command.Split(' ')[1]);
                        }
                        else
                        {
                            agent_host.sendCommand(command);
                        }
                    }
                }

                Random randomGenerator = new Random();

                if((currentSequence == "") && (currentSpeed < 50) && (waitCycles == 0))
                {
                    currentSequence = commandSequences[randomGenerator.Next(0, commandSequences.Count - 1)];
                    Console.WriteLine("Stuck! Chosen programme: " + currentSequence);
                }
            }
            // Mission has ended
        }
        static public string GetMissionXML()
        {
            // Build an XML mission string that uses the DefaultWorldGenerator.
            return @"<?xml version=""1.0"" encoding=""UTF-8"" ?>
    <Mission xmlns=""http://ProjectMalmo.microsoft.com"" xmlns:xsi=""http://www.w3.org/2001/XMLSchema-instance"">
        <About>
            <Summary>Normal life</Summary>
        </About>

        <ServerSection>
            <ServerHandlers>
                <DefaultWorldGenerator />
            </ServerHandlers>
        </ServerSection>

        <AgentSection mode=""Survival"">
            <Name>Rover</Name>
            <AgentStart>
                <Inventory>
                    <InventoryBlock slot=""0"" type=""glowstone"" quantity=""63""/>
                </Inventory>
            </AgentStart>
            <AgentHandlers>
                <ContinuousMovementCommands/>
                <ObservationFromFullStats/>
            </AgentHandlers>
        </AgentSection>

    </Mission>";
        }
    }
}
