using System;
using System.Threading;
using System.Collections.Generic;
using Microsoft.Research.Malmo;
using Newtonsoft.Json.Linq;

namespace tutorial_5
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
                    <StartTime>1000</StartTime>
                    <AllowPassageOfTime>false</AllowPassageOfTime>
                </Time>
                <Weather>clear</Weather>
              </ServerInitialConditions>
              <ServerHandlers>
                  <FlatWorldGenerator generatorString=""3;7,44*49,73,35:1,159:4,95:13,35:13,159:11,95:10,159:14,159:6,35:6,95:6;12;""/>
                  <DrawingDecorator>
                    <DrawSphere x=""-27"" y=""70"" z=""0"" radius=""30"" type=""air""/>" + Menger(-40, 40, -13, 27, "stone", "smooth_granite", "air") + @"
                    <DrawCuboid x1=""-25"" y1=""39"" z1=""-2"" x2=""-29"" y2=""39"" z2=""2"" type=""lava""/>
                    <DrawCuboid x1=""-26"" y1=""39"" z1=""-1"" x2=""-28"" y2=""39"" z2=""1"" type=""obsidian""/>
                    <DrawBlock x=""-27"" y=""39"" z=""0"" type=""diamond_block""/>
                  </DrawingDecorator>
                  <ServerQuitFromTimeUp timeLimitMs=""30000""/>
                  <ServerQuitWhenAnyAgentFinishes/>
                </ServerHandlers>
              </ServerSection>
              
              <AgentSection mode=""Survival"">
                <Name>MalmoTutorialBot</Name>
                <AgentStart>
                    <Placement x=""0.5"" y=""56.0"" z=""0.5"" yaw=""90""/>
                    <Inventory>
                        <InventoryItem slot=""8"" type=""diamond_pickaxe""/>
                    </Inventory>
                </AgentStart>
                <AgentHandlers>
                  <ObservationFromFullStats/>
                  <ObservationFromGrid>
                      <Grid name=""floor3x3"">
                        <min x=""-1"" y=""-1"" z=""-1""/>
                        <max x=""1"" y=""-1"" z=""1""/>
                      </Grid>
                  </ObservationFromGrid>
                  <ContinuousMovementCommands turnSpeedDegs=""180""/>
                  <InventoryCommands/>
                  <AgentQuitFromTouchingBlockType>
                      <Block type=""diamond_block"" />
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
                Console.WriteLine("Error:{0}", e.Message);
                Console.WriteLine(agent_host.getUsage());
                Environment.Exit(1);
            }
            if (agent_host.receivedArgument("help"))
            {
                Console.WriteLine(agent_host.getUsage());
                Environment.Exit(0);
            }

            MissionSpec my_mission = new MissionSpec(missionXML, true);
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
                        Console.WriteLine("Error starting mission:{0}", e.Message);
                        Environment.Exit(1);
                    }
                    else
                    {
                        Thread.Sleep(2000);
                    }
                }
            }

            // Loop until mission starts
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
                if(world_state.number_of_observations_since_last_state > 0) // Have any observations come in?
                {
                    string msg = world_state.observations[world_state.observations.Count-1].text;
                    JObject observations = JObject.Parse(msg);
                    JToken grid = observations.GetValue("floor3x3");
                    // ADD SOME CODE HERE TO SAVE YOUR AGENT
                }
            }
            Console.WriteLine();
            Console.WriteLine("Mission ended");
            // Mission has ended.

        }
        static public string Menger(int xorg, int yorg, int zorg, int size, string blocktype, string variant, string holetype)
        {
            // draw solid chunk
            string genstring = GenCuboidWithVariant(xorg, yorg, zorg, xorg + size - 1, yorg + size - 1, zorg + size - 1, blocktype, variant) + "\n";
            // now remove holes
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
            string cuboid = String.Format("<DrawCuboid x1=\"{0}\" y1=\"{1}\" z1=\"{2}\" x2=\"{3}\" y2=\"{4}\" z2=\"{5}\" type=\"{6}\"/>", x1, y1, z1, x2, y2, z2, blocktype);
            return cuboid;
        }
        static public string GenCuboidWithVariant(int x1, int y1, int z1, int x2, int y2, int z2, string blocktype, string variant)
        {
            string cuboidWithVariant = String.Format("<DrawCuboid x1=\"{0}\" y1=\"{1}\" z1=\"{2}\" x2=\"{3}\" y2=\"{4}\" z2=\"{5}\" type=\"{6}\" variant=\"{7}\"/>", x1, y1, z1, x2, y2, z2, blocktype, variant);
            return cuboidWithVariant;
        }
    }
}
