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

// Demo of reward for damaging mobs - create an arena filled with pigs and sheep,
// and reward the agent positively for attacking sheep, and negatively for attacking pigs.
// Using this reward signal to train the agent is left as an exercise for the reader...
// this demo just uses ObservationFromRay and ObservationFromNearbyEntities to determine
// when and where to attack.

using System;
using System.Threading;
using System.Threading.Tasks;
using System.Linq;
using System.Collections.Generic;
using Microsoft.Research.Malmo;
using System.IO;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace hit_test
{
    class Program
    {
        static void Main()
        {
            int ARENA_WIDTH = 20;
            int ARENA_BREADTH = 20;
            string recordingsDirectory = "HuntRecordings";
            try
            {
                Directory.CreateDirectory(recordingsDirectory);
            }
            catch(Exception e)
            {
                Console.WriteLine("ERROR: {0}", e.Message);
            }

            bool validate = true;
            ClientPool my_client_pool = new ClientPool();
            ClientInfo my_client_info = new ClientInfo("127.0.0.1", 10000);
            my_client_pool.add(my_client_info);

            AgentHost agent_host = new AgentHost();
            try
            {
                agent_host.parse(new StringVector(Environment.GetCommandLineArgs()));
            }
            catch(Exception e)
            {
                Console.WriteLine("ERROR: ", e.Message);
                Console.WriteLine(agent_host.getUsage());
                Environment.Exit(1);
            }
            if (agent_host.receivedArgument("help"))
            {
                Console.WriteLine(agent_host.getUsage());
                Environment.Exit(0);
            }

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
                string mission_xml = GetMissionXML("Go hunting! #" + iRepeat.ToString(), ARENA_WIDTH, ARENA_BREADTH);
                MissionSpec my_mission = new MissionSpec(mission_xml, validate);
                int max_retries = 3;
                for(int retry = 0; retry < max_retries; retry++)
                {
                    try
                    {
                        // Set up a recording
                        MissionRecordSpec my_mission_record = new MissionRecordSpec(recordingsDirectory + "//" + "Mission_" + iRepeat.ToString() + ".tgz");
                        my_mission_record.recordRewards();
                        // Attempt to start the mission:
                        agent_host.startMission(my_mission, my_client_pool, my_mission_record, 0, "hunterExperiment");
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

                // main loop:
                double total_reward = 0;
                int pig_population = 0;
                int sheep_population = 0;
                double self_x = 0;
                double self_z = 0;
                double current_yaw = 0;

                while (world_state.is_mission_running)
                {
                    world_state = agent_host.getWorldState();
                    if(world_state.number_of_observations_since_last_state > 0)
                    {
                        string msg = world_state.observations[world_state.observations.Count - 1].text;
                        JObject ob = JObject.Parse(msg);
                        // Use the line-of-sight observation to determine when to hit and when not to hit:
                        if (ob.ToString().Contains("LineOfSight"))
                        {
                            JToken los = ob["LineOfSight"];
                            JToken type = los["type"];
                            if(type.ToString() == "Sheep")
                            {
                                agent_host.sendCommand("attack 1");
                                agent_host.sendCommand("attack 0");
                            }
                        }
                        // Get our position/orientation:
                        if (ob.ToString().Contains("Yaw"))
                        {
                            current_yaw = (int)ob["Yaw"];
                        }
                        if (ob.ToString().Contains("XPos"))
                        {
                            self_x = (int)ob["XPos"];
                        }
                        if (ob.ToString().Contains("ZPos"))
                        {
                            self_z = (int)ob["ZPos"];
                        }
                        // Use the nearby-entities observation to decide which way to move, and to keep track
                        // of population sizes - allows us some measure of "progress".
                        if (ob.ToString().Contains("entities"))
                        {
                            List<EntityInfo> entities = new List<EntityInfo>();
                            foreach (JObject k in ob["entities"])
                            {
                                EntityInfo entity = new EntityInfo();
                                try
                                {
                                    entity.x = (double)k["x"];
                                }
                                catch
                                {
                                    entity.x = 0;
                                }
                                try
                                {
                                    entity.y = (double)k["y"];
                                }
                                catch
                                {
                                    entity.y = 0;
                                }
                                try
                                {
                                    entity.z = (double)k["z"];
                                }
                                catch
                                {
                                    entity.z = 0;
                                }
                                try
                                {
                                    entity.yaw = (double)k["yaw"];
                                }
                                catch
                                {
                                    entity.yaw = 0;
                                }
                                try
                                {
                                    entity.colour = (string)k["colour"];
                                }
                                catch
                                {
                                    entity.colour = "";
                                }
                                try
                                {
                                    entity.life = (double)k["life"];
                                }
                                catch
                                {
                                    entity.life = 0;
                                }
                                try
                                {
                                    entity.name = (string)k["name"];
                                }
                                catch
                                {
                                    entity.name = "";
                                }
                                try
                                {
                                    entity.pitch = (double)k["pitch"];
                                }
                                catch
                                {
                                    entity.pitch = 0;
                                }
                                try
                                {
                                    entity.quantity = (int)k["quantity"];
                                }
                                catch
                                {
                                    entity.quantity = 1;
                                }
                                try
                                {
                                    entity.variation = (string)k["variation"];
                                }
                                catch
                                {
                                    entity.variation = "";
                                }
                                entities.Add(entity);
                            }
                            int num_pigs = 0;
                            int num_sheep = 0;
                            double x_pull = 0;
                            double z_pull = 0;
                            foreach( EntityInfo e in entities)
                            {
                                if(e.name == "Sheep")
                                {
                                    num_sheep += 1;
                                    // Each sheep contriibutes to the direction we should head in...
                                    List<double> dist_list = new List<double>();
                                    dist_list.Add((e.x - self_x) * (e.x - self_x) + (e.z - self_z) * (e.z - self_z));
                                    dist_list.Add(0.0001);
                                    double dist = dist_list.Max();
                                    // Prioritise going after wounded sheep. Max sheep health is 8, according to Minecraft wiki...
                                    double weight = 9 - e.life;
                                    x_pull += (weight * (e.x - self_x) / dist);
                                    z_pull += (weight * (e.z - self_z) / dist);
                                }
                                else if(e.name == "Pig")
                                {
                                    num_pigs += 1;
                                }
                            }
                            // Determine the direction we need to turn in order to head towards the "sheepiest" point:
                            double yaw = -180 * Math.Atan2(x_pull, z_pull) / Math.PI;
                            double difference = yaw - current_yaw;
                            while(difference < -180)
                            {
                                difference += 360;
                            }
                            while(difference > 180)
                            {
                                difference -= 360;
                            }
                            difference /= 180;
                            agent_host.sendCommand("turn " + difference.ToString());

                            // move slower when turning faster - helps with "orbiting" problem
                            double move_speed;
                            if(Math.Abs(difference) < 0.5)
                            {
                                move_speed = 1;
                            }
                            else
                            {
                                move_speed = 0;
                            }

                            agent_host.sendCommand("move " + move_speed.ToString());
                            if((num_sheep != sheep_population) || (num_pigs != pig_population))
                            {
                                // Print an update of our "progress":
                                sheep_population = num_sheep;
                                pig_population = num_pigs;
                                int tot = sheep_population + pig_population;
                                if (tot != 0)
                                {
                                    int p = num_pigs * 40 / tot;
                                    Console.WriteLine(p);
                                    string pig_ratio = "";
                                    for (int i = 0; i < p; i++)
                                    {
                                        pig_ratio += "P";
                                    }
                                    string sheep_ratio = "";
                                    for (int i = 0; i < 40-p; i++)
                                    {
                                        sheep_ratio += "S";
                                    }
                                    Console.WriteLine("PIGS:SHEEP {0}|{1} ({2} {3})",pig_ratio,sheep_ratio,num_pigs,num_sheep);
                                }
                            }

                        }
                    }
                    if(world_state.number_of_rewards_since_last_state > 0)
                    {
                        // Keep track of our total reward:
                        total_reward += world_state.rewards[world_state.rewards.Count - 1].getValue();
                    }
                }
                // mission has ended
                foreach(TimestampedString error in world_state.errors)
                {
                    Console.WriteLine("Error: {0}", error.text);
                }
                if(world_state.number_of_rewards_since_last_state > 0)
                {
                    // A reward signal has to come in - see what it is:
                    total_reward += world_state.rewards[world_state.rewards.Count - 1].getValue();
                }
                Console.WriteLine();
                Console.WriteLine("=========================================");
                Console.WriteLine("Total score this round: {0}", total_reward);
                Console.WriteLine("=========================================");
                Console.WriteLine();
                Thread.Sleep(1000);
            }
        }
        static public string GetCorner(string index,bool top,bool left,int expand,int y,int ARENA_WIDTH,int ARENA_BREADTH)
        {
            // Return part of the XML string that defines the requested corner
            string x;
            if(left == true)
            {
                x = (-(expand + ARENA_WIDTH / 2)).ToString();
            }
            else
            {
                x = (expand + ARENA_WIDTH / 2).ToString();
            }

            string z;
            if(top == true)
            {
                z = (-(expand + ARENA_BREADTH / 2)).ToString();
            }
            else
            {
                z = (expand + ARENA_BREADTH / 2).ToString();
            }

            return String.Format(@"x{0}=""{1}"" y{2}=""{3}"" z{4}=""{5}""",index,x,index,y,index,z);
        }
        static public string GetSpawnEndTag(int i)
        {
            List<string> animals = new List<string>();
            animals.Add("Sheep");
            animals.Add("Pig");
            return @" type=""mob_spawner"" variant=""" + animals[i % 2] + @"""/>";
        }
        static public string GetMissionXML(string summary,int ARENA_WIDTH,int ARENA_BREADTH)
        {
            // Build an XML mission string
            // We put the spawners inside an animation object, to move them out of range of the player after a short period of time.
            // Otherwise they will just keep spawning - as soon as the agent kills a sheep, it will be replaced.
            // (Could use DrawEntity to create the pigs/sheep, rather than using spawners... but this way is much more fun.)

            return @"<?xml version=""1.0"" encoding=""UTF-8"" ?>
    <Mission xmlns=""http://ProjectMalmo.microsoft.com"" xmlns:xsi=""http://www.w3.org/2001/XMLSchema-instance"">
        <About>
            <Summary>" + summary + @"</Summary>
        </About>

        <ModSettings>
            <MsPerTick>50</MsPerTick>
        </ModSettings>
        <ServerSection>
            <ServerInitialConditions>
                <Time>
                    <StartTime>1000</StartTime>
                    <AllowPassageOfTime>true</AllowPassageOfTime>
                </Time>
                <AllowSpawning>true</AllowSpawning>
                <AllowedMobs>Pig Sheep</AllowedMobs>
            </ServerInitialConditions>
            <ServerHandlers>
                <FlatWorldGenerator generatorString=""3;7,202*1,5*3,2;3;,biome_1"" />
                <DrawingDecorator>
                    <DrawCuboid " + GetCorner("1",true,true,10,206,ARENA_WIDTH,ARENA_BREADTH) + @" " + GetCorner("2",false,false,10,246,ARENA_WIDTH,ARENA_BREADTH) + @" type=""grass""/>
                    <DrawCuboid " + GetCorner("1",true,true,0,207,ARENA_WIDTH,ARENA_BREADTH) + @" " + GetCorner("2",false,false,0,246, ARENA_WIDTH, ARENA_BREADTH) + @" type=""air""/>
                </DrawingDecorator>
                <AnimationDecorator ticksPerUpdate=""10"">
                <Linear>
                    <CanvasBounds>
                        <min x=""" + (-ARENA_BREADTH/2).ToString() + @""" y=""205"" z=""" + (-ARENA_BREADTH/2).ToString() + @"""/>
                        <max x=""" + (ARENA_WIDTH/2).ToString() + @""" y=""217"" z=""" + (ARENA_WIDTH/2).ToString() + @"""/>
                    </CanvasBounds>
                    <InitialPos x=""0"" y=""207"" z=""0""/>
                    <InitialVelocity x=""0"" y=""0.025"" z=""0""/>
                </Linear>
                <DrawingDecorator>
                    <DrawLine " + GetCorner("1",true,true,-2,0,ARENA_WIDTH,ARENA_BREADTH) + @" " + GetCorner("2",true,false,-2, 0, ARENA_WIDTH, ARENA_BREADTH) + GetSpawnEndTag(1) + @"
                    <DrawLine " + GetCorner("1",true,true,-2, 0, ARENA_WIDTH, ARENA_BREADTH) + @" " + GetCorner("2",false,true,-2, 0, ARENA_WIDTH, ARENA_BREADTH) + GetSpawnEndTag(1) + @"
                    <DrawLine " + GetCorner("1",false,false,-2, 0, ARENA_WIDTH, ARENA_BREADTH) + @" " + GetCorner("2",true,false,-2, 0, ARENA_WIDTH, ARENA_BREADTH) + GetSpawnEndTag(1) + @"
                    <DrawLine " + GetCorner("1",false,false,-2, 0, ARENA_WIDTH, ARENA_BREADTH) + @" " + GetCorner("2",false,true,-2, 0, ARENA_WIDTH, ARENA_BREADTH) + GetSpawnEndTag(1) + @"
                    <DrawLine " + GetCorner("1",true,true,-3, 0, ARENA_WIDTH, ARENA_BREADTH) + @" " + GetCorner("2",true,false,-3, 0, ARENA_WIDTH, ARENA_BREADTH) + GetSpawnEndTag(2) + @"
                    <DrawLine " + GetCorner("1",true,true,-3, 0, ARENA_WIDTH, ARENA_BREADTH) + @" " + GetCorner("2",false,true,-3, 0, ARENA_WIDTH, ARENA_BREADTH) + GetSpawnEndTag(2) + @"
                    <DrawLine " + GetCorner("1",false,false,-3, 0, ARENA_WIDTH, ARENA_BREADTH) + @" " + GetCorner("2",true,false,-3, 0, ARENA_WIDTH, ARENA_BREADTH) + GetSpawnEndTag(2) + @"
                    <DrawLine " + GetCorner("1",false,false,-3, 0, ARENA_WIDTH, ARENA_BREADTH) + @" " + GetCorner("2",false,true,-3, 0, ARENA_WIDTH, ARENA_BREADTH) + GetSpawnEndTag(2) + @"
                </DrawingDecorator>
                </AnimationDecorator>
               <ServerQuitWhenAnyAgentFinishes />
               <ServerQuitFromTimeUp timeLimitMs=""120000""/>
            </ServerHandlers>
        </ServerSection>

        <AgentSection mode=""Survival"">
            <Name>The Hunter</Name>
            <AgentStart>
                <Placement x=""0.5"" y=""207.0"" z=""0.5"" pitch=""20""/>
                <Inventory>
                    <InventoryItem type=""diamond_pickaxe"" slot=""0""/>
                </Inventory>
            </AgentStart>
            <AgentHandlers>
                <ContinuousMovementCommands turnSpeedDegs=""420""/>
                <ObservationFromRay/>
                <RewardForDamagingEntity>
                    <Mob type=""Sheep"" reward=""1""/>
                    <Mob type=""Pig"" reward=""-1""/>
                </RewardForDamagingEntity>
                <ObservationFromNearbyEntities>
                    <Range name=""entities"" xrange=""" + (ARENA_WIDTH).ToString() + @""" yrange=""2"" zrange=""" + (ARENA_BREADTH).ToString() + @""" />
                </ObservationFromNearbyEntities>
                <ObservationFromFullStats/>

            </AgentHandlers>
        </AgentSection>

    </Mission>";
        }
        public struct EntityInfo
        {
            public double x;
            public double y;
            public double z;
            public double yaw;
            public double pitch;
            public string name;
            public string colour;
            public string variation;
            public int quantity;
            public double life;
            EntityInfo(double x,double y,double z,double yaw,double pitch,string name,string colour,string variation,int quantity,double life)
            {
                this.x = x;
                this.y = y;
                this.z = z;
                this.yaw = yaw;
                this.pitch = pitch;
                this.name = name;
                this.colour = colour;
                this.variation = variation;
                this.quantity = quantity;
                this.life = life;
            }
        }
    }
}
